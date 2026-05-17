package com.godspark.server.network;

import com.godspark.GodsparkMod;
import com.godspark.ai.AiConfig;
import com.godspark.memory.ColonyMemory;
import com.godspark.network.payload.*;
import com.godspark.observer.ColonySnapshot;
import com.godspark.observer.ObservedColony;
import com.godspark.prayer.PrayerSeed;
import com.godspark.pressure.PressureSnapshot;
import com.godspark.story.EventRecord;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class UiSnapshotBuilder {

    private UiSnapshotBuilder() {}

    public static UiSnapshot build(ServerPlayer player) {
        long serverTick = player.serverLevel().getGameTime();

        List<UiColonyEntry> colonies = new ArrayList<>();
        List<UiPressureEntry> pressures = new ArrayList<>();
        List<UiEventEntry> events = new ArrayList<>();
        List<UiMemoryEntry> memories = new ArrayList<>();
        List<UiPrayerEntry> prayers = new ArrayList<>();

        // --- Colonies + Pressures ---
        try {
            Map<Integer, ObservedColony> observed = GodsparkMod.COLONY_OBSERVER.getObservedColonies();
            Map<Integer, PressureSnapshot> snapshots = GodsparkMod.PRESSURE_ENGINE.getSnapshots();

            for (ObservedColony oc : observed.values()) {
                ColonySnapshot s = oc.getLatest();
                if (s == null) continue;

                colonies.add(new UiColonyEntry(
                    s.colonyId(),
                    s.name(),
                    s.citizenCount(),
                    s.housingCapacity(),
                    s.guardCount(),
                    s.happiness(),
                    s.foodBuildingCount(),
                    s.industryBuildingCount(),
                    s.sacredBuildingCount(),
                    s.warehouseCount(),
                    s.hasActiveRaid()
                ));

                PressureSnapshot ps = snapshots.get(s.colonyId());
                if (ps != null) {
                    for (Map.Entry<com.godspark.pressure.PressureType, Integer> entry : ps.values().entrySet()) {
                        int effective = entry.getValue();
                        pressures.add(new UiPressureEntry(
                            s.colonyId(),
                            entry.getKey().name(),
                            -1,
                            effective
                        ));
                    }
                }
            }
        } catch (Exception e) {
            GodsparkMod.LOGGER.warn("[Godspark UI] Failed to gather colonies/pressures", e);
        }

        // --- Events ---
        try {
            List<EventRecord> active = GodsparkMod.EVENT_STATE_MANAGER.getActiveEvents();
            for (EventRecord rec : active) {
                events.add(new UiEventEntry(
                    rec.event().colonyId(),
                    rec.event().eventType().name(),
                    rec.event().pressureType().name(),
                    rec.event().severity().name(),
                    rec.isResolved() ? "RESOLVED" : (rec.isPersistent() ? "PERSISTENT" : "ACTIVE"),
                    rec.persistenceCount(),
                    rec.event().gameTick()
                ));
            }
        } catch (Exception e) {
            GodsparkMod.LOGGER.warn("[Godspark UI] Failed to gather events", e);
        }

        // --- Memories ---
        try {
            Map<Integer, java.util.Deque<ColonyMemory>> byColony = GodsparkMod.MEMORY_BANK.getAllMemoriesByColony();
            for (Map.Entry<Integer, java.util.Deque<ColonyMemory>> entry : byColony.entrySet()) {
                for (ColonyMemory m : entry.getValue()) {
                    memories.add(new UiMemoryEntry(
                        entry.getKey(),
                        m.memoryType().name(),
                        m.pressureType() == null ? "NONE" : m.pressureType().name(),
                        m.intensity(),
                        m.createdAtTick()
                    ));
                }
            }
        } catch (Exception e) {
            GodsparkMod.LOGGER.warn("[Godspark UI] Failed to gather memories", e);
        }

        // --- Prayers ---
        try {
            List<PrayerSeed> allPrayers = GodsparkMod.PRAYER_SEED_BANK.getAllPrayers();
            for (PrayerSeed p : allPrayers) {
                prayers.add(new UiPrayerEntry(
                    p.colonyId(),
                    p.prayerType().name(),
                    p.channel().name(),
                    p.pressureType().name(),
                    p.intensity(),
                    p.content()
                ));
            }
        } catch (Exception e) {
            GodsparkMod.LOGGER.warn("[Godspark UI] Failed to gather prayers", e);
        }

        // --- Status ---
        int activeEventCount = (int) events.stream()
            .filter(e -> !"RESOLVED".equals(e.state()))
            .count();

        UiStatusInfo status = new UiStatusInfo(
            true,
            isMineColoniesLoaded(),
            isAiEnabled(),
            colonies.size(),
            activeEventCount,
            memories.size(),
            prayers.size()
        );

        return new UiSnapshot(status, colonies, pressures, events, memories, prayers, serverTick);
    }

    private static boolean isMineColoniesLoaded() {
        try {
            return net.minecraftforge.fml.ModList.get().isLoaded("minecolonies");
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isAiEnabled() {
        try {
            return GodsparkMod.AI_REFLECTION_SERVICE != null
                && AiConfig.DEFAULT.enabled();
        } catch (Exception e) {
            return false;
        }
    }
}