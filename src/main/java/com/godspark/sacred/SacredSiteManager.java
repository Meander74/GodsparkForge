package com.godspark.sacred;

import com.godspark.GodsparkConfig;
import com.godspark.observer.ColonyObserver;
import com.godspark.observer.ColonySnapshot;
import com.godspark.observer.ObservedColony;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class SacredSiteManager {

    public static final int PRAYER_STONE_BIND_RADIUS = 128;
    public static final boolean PRAYER_STONE_REQUIRE_SAME_DIMENSION = true;
    public static final int PRAYER_STONE_MIN_COLONY_CITIZEN_COUNT = 1;

    private static final Logger LOGGER = LogManager.getLogger(SacredSiteManager.class);
    private static final SacredSiteManager INSTANCE = new SacredSiteManager();

    private final Map<SacredSiteKey, SacredSiteRecord> sitesByPos = new ConcurrentHashMap<>();
    private final Map<Integer, Set<SacredSiteKey>> sitesByColony = new ConcurrentHashMap<>();
    private volatile Runnable dirtyListener = () -> {};
    private boolean restoring;

    private SacredSiteManager() {}

    public static SacredSiteManager getInstance() {
        return INSTANCE;
    }

    public void setDirtyListener(Runnable dirtyListener) {
        this.dirtyListener = dirtyListener == null ? () -> {} : dirtyListener;
    }

    public boolean registerPrayerStone(ServerLevel level, BlockPos pos,
                                       Map<Integer, ObservedColony> colonies, long gameTick) {
        if (level == null || pos == null) return false;
        return registerPrayerStone(level.dimension(), pos, colonies, gameTick);
    }

    public boolean registerPrayerStone(ResourceKey<Level> dimension, BlockPos pos,
                                       Map<Integer, ObservedColony> colonies, long gameTick) {
        if (dimension == null || pos == null) return false;
        return registerPrayerStone(dimension.location(), pos, toCandidates(colonies), gameTick);
    }

    public boolean registerPrayerStone(ResourceLocation dimension, BlockPos pos,
                                       Iterable<PrayerStoneCandidate> colonies, long gameTick) {
        if (dimension == null || pos == null) return false;

        PrayerStoneCandidate nearest = findNearestValidColony(dimension, pos, colonies);
        if (nearest == null && !isPrayerStoneAllowUnbound()) {
            unregister(dimension, pos);
            LOGGER.debug("[GS/Sacred] Prayer Stone at {} in {} has no colony within {} blocks",
                pos.toShortString(), dimension, getPrayerStoneBindRadius());
            return false;
        }

        int colonyId = nearest != null ? nearest.colonyId() : -1;
        SacredSiteRecord record = new SacredSiteRecord(
            SacredSiteType.PRAYER_STONE,
            pos.immutable(),
            dimension,
            colonyId,
            gameTick,
            gameTick
        );
        register(record);
        return colonyId >= 0;
    }

    public void register(SacredSiteRecord record) {
        if (record == null) return;
        SacredSiteKey key = new SacredSiteKey(record.dimension(), record.pos());
        SacredSiteRecord previous = sitesByPos.put(key, record);
        if (previous != null) {
            removeFromColonyIndex(key, previous.colonyId());
        }
        if (record.colonyId() >= 0) {
            sitesByColony.computeIfAbsent(record.colonyId(), id -> ConcurrentHashMap.newKeySet()).add(key);
        }
        if (!restoring) markDirty();
        LOGGER.info("[GS/Sacred] Registered {} for colony #{} at {} in {}",
            record.type().getKey(), record.colonyId(), record.pos().toShortString(), record.dimension());
    }

    public void unregister(Level level, BlockPos pos) {
        if (level == null) return;
        unregister(level.dimension(), pos);
    }

    public void unregister(ResourceKey<Level> dimension, BlockPos pos) {
        if (dimension == null || pos == null) return;
        unregister(dimension.location(), pos);
    }

    public void unregister(ResourceLocation dimension, BlockPos pos) {
        if (dimension == null || pos == null) return;
        SacredSiteKey key = new SacredSiteKey(dimension, pos);
        SacredSiteRecord removed = sitesByPos.remove(key);
        if (removed == null) return;

        removeFromColonyIndex(key, removed.colonyId());
        if (!restoring) markDirty();
        LOGGER.info("[GS/Sacred] Unregistered {} for colony #{} at {} in {}",
            removed.type().getKey(), removed.colonyId(), pos.toShortString(), dimension);
    }

    public boolean hasPrayerStoneAnchor(int colonyId) {
        return getPrayerStoneAnchorScore(colonyId) > 0;
    }

    public int getPrayerStoneAnchorScore(int colonyId) {
        Set<SacredSiteKey> keys = sitesByColony.get(colonyId);
        if (keys == null || keys.isEmpty()) return 0;
        for (SacredSiteKey key : keys) {
            SacredSiteRecord record = sitesByPos.get(key);
            if (record != null && record.type() == SacredSiteType.PRAYER_STONE) {
                return 1;
            }
        }
        return 0;
    }

    @Nullable
    public SacredSiteRecord getSite(ResourceKey<Level> dimension, BlockPos pos) {
        if (dimension == null || pos == null) return null;
        return sitesByPos.get(SacredSiteKey.from(dimension, pos));
    }

    public Map<SacredSiteKey, SacredSiteRecord> getAllSites() {
        return Collections.unmodifiableMap(new HashMap<>(sitesByPos));
    }

    public List<SacredSiteRecord> snapshotSites() {
        return List.copyOf(new ArrayList<>(sitesByPos.values()));
    }

    public int siteCount() {
        return sitesByPos.size();
    }

    public Map<SacredSiteKey, SacredSiteRecord> getSitesInDimension(ResourceKey<Level> dimension) {
        if (dimension == null) return Collections.emptyMap();
        ResourceLocation dim = dimension.location();
        Map<SacredSiteKey, SacredSiteRecord> result = new HashMap<>();
        for (Map.Entry<SacredSiteKey, SacredSiteRecord> entry : sitesByPos.entrySet()) {
            if (entry.getKey().dimension().equals(dim)) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return Collections.unmodifiableMap(result);
    }

    public void restoreFrom(Collection<SacredSiteRecord> records) {
        restoring = true;
        try {
            Map<SacredSiteKey, SacredSiteRecord> replacement = new HashMap<>();
            Map<Integer, Set<SacredSiteKey>> colonyIndex = new HashMap<>();
            int dropped = 0;

            for (SacredSiteRecord record : records) {
                if (record == null) { dropped++; continue; }
                SacredSiteKey key = new SacredSiteKey(record.dimension(), record.pos());
                SacredSiteRecord existing = replacement.put(key, record);
                if (existing != null) {
                    LOGGER.debug("[GS/Sacred] Duplicate site at {} in {} - keeping last record",
                        record.pos().toShortString(), record.dimension());
                }
                if (record.colonyId() >= 0) {
                    colonyIndex.computeIfAbsent(record.colonyId(), k -> ConcurrentHashMap.newKeySet()).add(key);
                }
            }

            sitesByPos.clear();
            sitesByColony.clear();
            sitesByPos.putAll(replacement);
            for (Map.Entry<Integer, Set<SacredSiteKey>> entry : colonyIndex.entrySet()) {
                Set<SacredSiteKey> keySet = ConcurrentHashMap.newKeySet();
                keySet.addAll(entry.getValue());
                sitesByColony.put(entry.getKey(), keySet);
            }

            LOGGER.info("[GS/Sacred] Restored {} sacred sites ({} dropped)", replacement.size(), dropped);
        } finally {
            restoring = false;
        }
    }

    public int revalidateBindings(ServerLevel level, ColonyObserver observer) {
        if (level == null) return 0;
        long gameTick = level.getGameTime();
        int changed = 0;

        List<SacredSiteRecord> current = snapshotSites();
        for (SacredSiteRecord site : current) {
            if (site == null) continue;
            if (site.type() != SacredSiteType.PRAYER_STONE) continue;

            ResourceLocation dim = site.dimension();
            if (!dim.equals(level.dimension().location())) continue;

            ObservedColony observed = observer.getColony(site.colonyId());
            boolean valid = false;

            if (observed != null) {
                ColonySnapshot snapshot = observed.getLatest();
                if (snapshot != null && snapshot.dimension().location().equals(dim)) {
                    double distSqr = snapshot.center().distSqr(site.pos());
                    if (distSqr <= (double) getPrayerStoneBindRadius() * getPrayerStoneBindRadius()) {
                        valid = true;
                        SacredSiteRecord updated = new SacredSiteRecord(
                            site.type(), site.pos(), site.dimension(),
                            site.colonyId(), site.registeredAtTick(), gameTick
                        );
                        sitesByPos.put(new SacredSiteKey(site.dimension(), site.pos()), updated);
                    }
                }
            }

            if (!valid) {
                SacredSiteKey key = new SacredSiteKey(site.dimension(), site.pos());
                sitesByPos.remove(key);
                removeFromColonyIndex(key, site.colonyId());
                changed++;
                LOGGER.info("[GS/Sacred] Revalidation: removed {} at {} in {} (colony #{} invalid)",
                    site.type().getKey(), site.pos().toShortString(), site.dimension(), site.colonyId());
            }
        }

        if (changed > 0 && !restoring) markDirty();
        LOGGER.info("[GS/Sacred] Revalidation complete: {} sites removed", changed);
        return changed;
    }

    public void clear() {
        sitesByPos.clear();
        sitesByColony.clear();
        if (!restoring) markDirty();
        LOGGER.info("[GS/Sacred] All sacred sites cleared");
    }

    @Nullable
    private static PrayerStoneCandidate findNearestValidColony(ResourceLocation dimension, BlockPos pos,
                                                               Iterable<PrayerStoneCandidate> colonies) {
        if (colonies == null) return null;

        double maxDistanceSqr = (double) getPrayerStoneBindRadius() * getPrayerStoneBindRadius();
        PrayerStoneCandidate best = null;
        double bestDistance = Double.MAX_VALUE;

        for (PrayerStoneCandidate snapshot : colonies) {
            if (snapshot == null) continue;
            if (PRAYER_STONE_REQUIRE_SAME_DIMENSION && !dimension.equals(snapshot.dimension())) continue;
            if (snapshot.citizenCount() < PRAYER_STONE_MIN_COLONY_CITIZEN_COUNT) continue;
            if (snapshot.center() == null) continue;

            double distance = snapshot.center().distSqr(pos);
            if (distance > maxDistanceSqr) continue;
            if (best == null || distance < bestDistance
                || (distance == bestDistance && snapshot.colonyId() < best.colonyId())) {
                best = snapshot;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static Iterable<PrayerStoneCandidate> toCandidates(Map<Integer, ObservedColony> colonies) {
        if (colonies == null || colonies.isEmpty()) return Collections.emptyList();
        List<PrayerStoneCandidate> candidates = new ArrayList<>();
        for (ObservedColony observed : colonies.values()) {
            if (observed == null || observed.getLatest() == null) continue;
            ColonySnapshot snapshot = observed.getLatest();
            candidates.add(new PrayerStoneCandidate(
                snapshot.colonyId(),
                snapshot.name(),
                snapshot.dimension().location(),
                snapshot.center(),
                snapshot.citizenCount()
            ));
        }
        return candidates;
    }

    private static int getPrayerStoneBindRadius() {
        try {
            return GodsparkConfig.SACRED_PRAYER_STONE_BIND_RADIUS.get();
        } catch (IllegalStateException e) {
            return PRAYER_STONE_BIND_RADIUS;
        }
    }

    private static boolean isPrayerStoneAllowUnbound() {
        try {
            return GodsparkConfig.SACRED_PRAYER_STONE_ALLOW_UNBOUND.get();
        } catch (IllegalStateException e) {
            return false;
        }
    }

    private void removeFromColonyIndex(SacredSiteKey key, int colonyId) {
        if (colonyId < 0) return;
        Set<SacredSiteKey> keys = sitesByColony.get(colonyId);
        if (keys == null) return;
        keys.remove(key);
        if (keys.isEmpty()) {
            sitesByColony.remove(colonyId);
        }
    }

    private void markDirty() {
        dirtyListener.run();
    }

    public record SacredSiteKey(ResourceLocation dimension, BlockPos pos) {
        public SacredSiteKey {
            if (dimension == null) throw new IllegalArgumentException("dimension");
            if (pos == null) throw new IllegalArgumentException("pos");
            pos = pos.immutable();
        }

        public static SacredSiteKey from(ResourceKey<Level> dimension, BlockPos pos) {
            return new SacredSiteKey(dimension.location(), pos);
        }
    }

    public record PrayerStoneCandidate(
        int colonyId,
        String colonyName,
        ResourceLocation dimension,
        BlockPos center,
        int citizenCount
    ) {}
}