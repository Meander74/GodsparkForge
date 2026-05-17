package com.godspark.event;

import com.godspark.GodsparkMod;
import com.godspark.GodsparkConstants;
import com.godspark.memory.ColonyMemory;
import com.godspark.observer.ColonySnapshot;
import com.godspark.observer.ObservedColony;
import com.godspark.persistence.GodsparkSavedData;
import com.godspark.personality.ColonyPersonality;
import com.godspark.prayer.PrayerSeed;
import com.godspark.pressure.PressureModifier;
import com.godspark.pressure.PressureModifierManager;
import com.godspark.pressure.PressureSnapshot;
import com.godspark.pressure.PressureType;
import com.godspark.sacred.SacredSiteManager;
import com.godspark.story.EventRecord;
import com.godspark.story.StoryEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GodsparkServerEvents {
    private static long tickCounter = 0;
    private static final boolean SUMMARY_LOGGING = true;
    @Nullable
    private static GodsparkSavedData savedData = null;

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        savedData = server.overworld().getDataStorage().computeIfAbsent(
            GodsparkSavedData::load,
            GodsparkSavedData::createDefault,
            GodsparkSavedData.DATA_KEY
        );
        savedData.restoreTo(GodsparkMod.EVENT_STATE_MANAGER);
        savedData.restoreMemoriesTo(GodsparkMod.MEMORY_BANK);
        SacredSiteManager.getInstance().setDirtyListener(savedData::setDirty);

        GodsparkMod.reloadAiConfig();
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        if (savedData != null) {
            savedData.captureFrom(GodsparkMod.EVENT_STATE_MANAGER);
            savedData.captureMemoriesFrom(GodsparkMod.MEMORY_BANK);
            GodsparkMod.LOGGER.info("[Godspark SavedData] Captured final event state and memories");
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        GodsparkMod.COLONY_OBSERVER.clear();
        GodsparkMod.PRESSURE_ENGINE.clear();
        GodsparkMod.EVENT_QUEUE.clear();
        GodsparkMod.EVENT_STATE_MANAGER.clear();
        GodsparkMod.MEMORY_BANK.clear();
        GodsparkMod.PRAYER_SEED_BANK.clear();
        GodsparkMod.AI_REFLECTION_SERVICE.clearCooldowns();
        GodsparkMod.PERSONALITY_ENGINE.clearCache();
        GodsparkMod.PRESSURE_MODIFIER_MANAGER.clear();
        SacredSiteManager.getInstance().clear();
        SacredSiteManager.getInstance().setDirtyListener(null);
        tickCounter = 0;
        savedData = null;
        GodsparkMod.LOGGER.info("[Godspark] Static services cleared");
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        tickCounter++;

        GodsparkMod.PRESSURE_MODIFIER_MANAGER.tick(tickCounter);

        MinecraftServer server = event.getServer();
        if (server == null) {
            return;
        }

        if (tickCounter % GodsparkConstants.OBSERVER_INTERVAL_TICKS == 0) {
            GodsparkMod.COLONY_OBSERVER.scan(server);
        }

        if (tickCounter % GodsparkConstants.PRESSURE_INTERVAL_TICKS == 0) {
            GodsparkMod.PRESSURE_ENGINE.compute(GodsparkMod.COLONY_OBSERVER.getObservedColonies());

            Map<Integer, ColonyPersonality> personalities = new HashMap<>();
            for (int colonyId : GodsparkMod.COLONY_OBSERVER.getObservedColonies().keySet()) {
                ColonyPersonality p = GodsparkMod.PERSONALITY_ENGINE.getCached(colonyId);
                if (p != null) {
                    personalities.put(colonyId, p);
                }
            }

            List<StoryEvent> candidates = GodsparkMod.EVENT_GENERATOR.generate(
                GodsparkMod.PRESSURE_ENGINE.getSnapshots(),
                GodsparkMod.COLONY_OBSERVER.getObservedColonies(),
                GodsparkMod.MEMORY_BANK,
                personalities,
                server.getTickCount()
            );

            List<EventRecord> transitions = GodsparkMod.EVENT_STATE_MANAGER.processEvents(
                candidates,
                server.getTickCount()
            );

            List<ColonyMemory> newMemories = GodsparkMod.MEMORY_ENGINE.generateMemories(
                transitions,
                GodsparkMod.MEMORY_BANK,
                server.getTickCount()
            );

            for (ColonyMemory memory : newMemories) {
                ColonyMemory stored = GodsparkMod.MEMORY_BANK.addOrReinforce(memory, server.getTickCount());
                GodsparkMod.LOGGER.info(
                    "[Godspark Memory] [{}][{}] {}: {} (intensity={})",
                    stored.memoryType().getDisplayName(),
                    stored.pressureType().getDisplayName(),
                    stored.colonyName(),
                    stored.content(),
                    stored.intensity()
                );
            }

            if (!transitions.isEmpty() && savedData != null) {
                savedData.captureFrom(GodsparkMod.EVENT_STATE_MANAGER);
                savedData.captureMemoriesFrom(GodsparkMod.MEMORY_BANK);
            }

            for (EventRecord record : transitions) {
                GodsparkMod.LOGGER.info(
                    "[Godspark Event] [{} → {}][{}] {}: {} (pressure={}, persistence={})",
                    record.isResolved() ? "ACTIVE/PERSISTENT" : "NEW/UPGRADE",
                    record.state(),
                    record.event().pressureType().getDisplayName(),
                    record.event().colonyName(),
                    record.event().description(),
                    record.event().pressureValue(),
                    record.persistenceCount()
                );
            }

            for (StoryEvent storyEvent : candidates) {
                if (GodsparkMod.EVENT_QUEUE.offer(storyEvent)) {
                    GodsparkMod.LOGGER.info(
                        "[Godspark Event] [{}][{}] {}: {} (pressure={}, threshold={})",
                        storyEvent.severity(),
                        storyEvent.pressureType().getDisplayName(),
                        storyEvent.colonyName(),
                        storyEvent.description(),
                        storyEvent.pressureValue(),
                        storyEvent.threshold()
                    );
                }
            }

            GodsparkMod.PRAYER_SEED_BANK.expireOld(server.getTickCount());

            Map<Integer, Boolean> prayerStoneAnchors = new HashMap<>();
            for (Map.Entry<Integer, ObservedColony> entry : GodsparkMod.COLONY_OBSERVER.getObservedColonies().entrySet()) {
                prayerStoneAnchors.put(
                    entry.getKey(),
                    SacredSiteManager.getInstance().hasPrayerStoneAnchor(entry.getKey())
                );
            }

            List<PrayerSeed> prayerSeeds = GodsparkMod.PRAYER_SEED_GENERATOR.generate(
                GodsparkMod.PRESSURE_ENGINE.getSnapshots(),
                GodsparkMod.COLONY_OBSERVER.getObservedColonies(),
                GodsparkMod.EVENT_STATE_MANAGER.getActiveEvents(),
                transitions,
                GodsparkMod.MEMORY_BANK,
                GodsparkMod.MEMORY_INFLUENCE,
                personalities,
                prayerStoneAnchors,
                server.getTickCount()
            );

            for (PrayerSeed seed : prayerSeeds) {
                if (GodsparkMod.PRAYER_SEED_BANK.offer(seed)) {
                    GodsparkMod.LOGGER.info(
                        "[Godspark Prayer] [{} {} {}][{}] {}: {} (reasons={})",
                        seed.channel().getDisplayName(),
                        seed.prayerType().getDisplayName(),
                        seed.intensity(),
                        seed.pressureType().getDisplayName(),
                        seed.colonyName(),
                        seed.content(),
                        String.join(", ", seed.reasonCodes())
                    );
                }
            }

            GodsparkMod.PERSONALITY_ENGINE.updateFromObservations(
                GodsparkMod.COLONY_OBSERVER.getObservedColonies(),
                server.getTickCount()
            );

            logColonySummary();
        }
    }

    private static void logColonySummary() {
        if (!SUMMARY_LOGGING) return;
        Map<Integer, ObservedColony> colonies = GodsparkMod.COLONY_OBSERVER.getObservedColonies();
        if (colonies.isEmpty()) return;

        Map<Integer, PressureSnapshot> pressures = GodsparkMod.PRESSURE_ENGINE.getSnapshots();

        for (Map.Entry<Integer, ObservedColony> entry : colonies.entrySet()) {
            int colonyId = entry.getKey();
            ObservedColony observed = entry.getValue();
            ColonySnapshot snapshot = observed.getLatest();
            if (snapshot == null) continue;

            PressureSnapshot pressure = pressures.get(colonyId);

            StringBuilder sb = new StringBuilder();
            sb.append(String.format(
                "[Godspark Summary] Colony #%d (%s): citizens=%d buildings=%d housing=%d happy=%.1f raid=%s",
                colonyId, snapshot.name(), snapshot.citizenCount(), snapshot.buildingCount(),
                snapshot.housingCapacity(), snapshot.happiness(), snapshot.hasActiveRaid()
            ));
            sb.append(String.format(
                " | foodBuildings=%d securityBuildings=%d industryBuildings=%d warehouse=%d sacred=%d prayerStoneAnchor=%s",
                snapshot.foodBuildingCount(), snapshot.guardCount(),
                snapshot.industryBuildingCount(), snapshot.warehouseCount(),
                snapshot.sacredBuildingCount(),
                SacredSiteManager.getInstance().hasPrayerStoneAnchor(colonyId)
            ));

            if (pressure != null) {
                sb.append("\n  PRESSURES:");
                for (PressureType type : PressureType.values()) {
                    sb.append(String.format(" %s=%d", type.getDisplayName(), pressure.values().get(type)));
                }
            }

            List<EventRecord> activeEvents = GodsparkMod.EVENT_STATE_MANAGER.getActiveEvents().stream()
                .filter(r -> r.event().colonyId() == colonyId)
                .toList();
            if (!activeEvents.isEmpty()) {
                sb.append("\n  EVENTS: ");
                for (int i = 0; i < activeEvents.size(); i++) {
                    EventRecord r = activeEvents.get(i);
                    if (i > 0) sb.append(" | ");
                    sb.append(String.format("%s_%s(%s)",
                        r.event().pressureType().getDisplayName(),
                        r.event().severity(),
                        r.state().name()
                    ));
                }
            } else {
                sb.append("\n  EVENTS: none");
            }

            List<ColonyMemory> topMemories = GodsparkMod.MEMORY_BANK.getStrongestMemories(colonyId, 5);
            if (!topMemories.isEmpty()) {
                sb.append("\n  MEMORIES: ");
                for (int i = 0; i < topMemories.size(); i++) {
                    ColonyMemory m = topMemories.get(i);
                    if (i > 0) sb.append(" | ");
                    sb.append(String.format("%s:%s(%d)",
                        m.pressureType().getDisplayName(),
                        m.memoryType().getDisplayName(),
                        m.intensity()
                    ));
                }
            } else {
                sb.append("\n  MEMORIES: none");
            }

            List<PrayerSeed> colonyPrayers = GodsparkMod.PRAYER_SEED_BANK.getPrayers(colonyId);
            if (!colonyPrayers.isEmpty()) {
                sb.append("\n  PRAYERS: ");
                for (int i = 0; i < colonyPrayers.size(); i++) {
                    PrayerSeed p = colonyPrayers.get(i);
                    if (i > 0) sb.append(" | ");
                    sb.append(String.format("%s:%s(%d)",
                        p.channel().getDisplayName(),
                        p.prayerType().getDisplayName(),
                        p.intensity()
                    ));
                }
            } else {
                sb.append("\n  PRAYERS: none");
            }

            com.godspark.personality.ColonyPersonality personality = GodsparkMod.PERSONALITY_ENGINE.getCached(colonyId);
            if (personality != null) {
                sb.append(String.format("\n  PERSONALITY: %s/%s",
                    personality.primaryTrait().getDisplayName(),
                    personality.secondaryTrait().getDisplayName()));
            }

            GodsparkMod.LOGGER.info(sb.toString());
        }
    }
}
