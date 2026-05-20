package com.godspark.world;

import com.godspark.GodsparkMod;
import com.godspark.divine.ValidatedIntent;
import com.godspark.observer.ColonySnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public final class WorldEffectEngine {

    private static final Logger LOGGER = LogManager.getLogger(WorldEffectEngine.class);

    private final Map<WorldEffectCooldownKey, WorldEffectCooldownRecord> cooldowns = new HashMap<>();
    private final WorldEffectApplier applier;
    private final WorldEffectsSettings settings;
    private Runnable dirtyListener;
    private boolean dirty;

    public WorldEffectEngine(WorldEffectApplier applier, WorldEffectsSettings settings) {
        this.applier = applier;
        this.settings = settings;
    }

    public WorldEffectApplyResult tryApply(ValidatedIntent vi, MinecraftServer server, long gameTick) {
        if (vi == null) {
            return new WorldEffectApplyResult(null, false, 0, "null_intent");
        }

        if (!settings.enabled()) {
            return new WorldEffectApplyResult(null, false, 0, "world_effects_disabled");
        }

        if (!settings.allowDebugAnswerCommand()) {
            return new WorldEffectApplyResult(null, false, 0, "debug_answer_not_allowed");
        }

        LightWorldEffect effect = LightWorldEffect.fromIntent(vi);
        if (effect == null) {
            return new WorldEffectApplyResult(null, false, 0, "no_matching_effect");
        }

        int colonyId = vi.intent().colonyId();
        ColonySnapshot snap = resolveSnapshot(colonyId);
        if (snap == null) {
            LOGGER.info("[Godspark WorldEffect] No colony snapshot for colony #{}; skipping world effect.", colonyId);
            return new WorldEffectApplyResult(effect, false, 0, "no_colony_snapshot");
        }

        ResourceKey<Level> dimensionKey = snap.dimension();
        ServerLevel level = server.getLevel(dimensionKey);
        if (level == null) {
            LOGGER.info("[Godspark WorldEffect] Dimension {} unavailable for colony #{}; skipping world effect.", dimensionKey.location(), colonyId);
            return new WorldEffectApplyResult(effect, false, 0, "dimension_unavailable");
        }

        WorldEffectCooldownKey key = new WorldEffectCooldownKey(dimensionKey.location(), colonyId);
        WorldEffectCooldownRecord lastFire = cooldowns.get(key);
        if (lastFire != null) {
            long elapsed = gameTick - lastFire.lastFireTick();
            int cooldownTicks = settings.cooldownTicks();
            if (elapsed < cooldownTicks) {
                return new WorldEffectApplyResult(effect, false, 0,
                    "cooldown_active_remaining_" + (cooldownTicks - elapsed));
            }
        }

        BlockPos colonyCenter = snap.center();

        WorldEffectApplyResult result;
        try {
            result = switch (effect) {
                case GUARDIANS_VIGIL -> applier.applyGuardiansVigil(
                    level, colonyCenter,
                    settings.resistanceDurationTicks(),
                    settings.guardianVigilRadius());
                case GREENS_MERCY -> applier.applyGreensMercy(
                    level, colonyCenter,
                    settings.cropPulseAttempts(),
                    settings.cropPulseSuccessCap(),
                    settings.greenMercyRadius());
            };
        } catch (Exception e) {
            LOGGER.warn("[Godspark WorldEffect] Failed to apply {} for colony #{}: {}",
                effect.displayName(), colonyId, e.getMessage());
            return new WorldEffectApplyResult(effect, false, 0, "applier_exception");
        }

        if (result.applied() && result.affectedCount() > 0) {
            cooldowns.put(key, new WorldEffectCooldownRecord(
                key, gameTick, effect.name(), result.affectedCount()));
            markDirty();
        }

        return result;
    }

    public Map<WorldEffectCooldownKey, WorldEffectCooldownRecord> snapshotCooldowns() {
        return Map.copyOf(cooldowns);
    }

    public void restoreCooldowns(Map<WorldEffectCooldownKey, WorldEffectCooldownRecord> data) {
        cooldowns.clear();
        if (data != null) {
            cooldowns.putAll(data);
        }
    }

    public boolean hasDirty() { return dirty; }

    public void consumeDirty() { dirty = false; }

    public void markDirty() { dirty = true; if (dirtyListener != null) dirtyListener.run(); }

    public void setDirtyListener(Runnable listener) { this.dirtyListener = listener; }

    public void clear() {
        cooldowns.clear();
        dirty = false;
    }

    public WorldEffectCooldownRecord getLastRecord(int colonyId, ResourceKey<Level> dimension) {
        if (dimension == null) return null;
        return cooldowns.get(new WorldEffectCooldownKey(dimension.location(), colonyId));
    }

    private static ColonySnapshot resolveSnapshot(int colonyId) {
        var observed = GodsparkMod.COLONY_OBSERVER.getObservedColonies().get(colonyId);
        return observed != null ? observed.getLatest() : null;
    }
}
