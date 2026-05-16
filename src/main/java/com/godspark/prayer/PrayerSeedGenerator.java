package com.godspark.prayer;

import com.godspark.memory.ColonyMemory;
import com.godspark.memory.MemoryBank;
import com.godspark.memory.MemoryInfluence;
import com.godspark.memory.MemoryType;
import com.godspark.observer.ColonySnapshot;
import com.godspark.observer.ObservedColony;
import com.godspark.pressure.PressureSnapshot;
import com.godspark.pressure.PressureType;
import com.godspark.story.EventRecord;
import com.godspark.story.EventSeverity;
import com.godspark.story.EventState;
import com.godspark.story.StoryEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PrayerSeedGenerator {

    private static final int MAX_PRAYERS_PER_CYCLE = 3;
    private static final long PRAYER_TTL_TICKS = 12000L;

    private static final int PERSISTENT_BONUS = 10;
    private static final int TRAUMA_BONUS = 10;
    private static final int RAID_BONUS = 10;
    private static final int THANKS_PERSISTENCE_BONUS = 5;

    public List<PrayerSeed> generate(
        Map<Integer, PressureSnapshot> pressureSnapshots,
        Map<Integer, ObservedColony> observedColonies,
        List<EventRecord> activeEvents,
        List<EventRecord> transitions,
        MemoryBank memoryBank,
        MemoryInfluence memoryInfluence,
        long gameTick
    ) {
        if (pressureSnapshots == null || pressureSnapshots.isEmpty()) {
            return List.of();
        }

        List<PrayerSeed> seeds = new ArrayList<>();

        for (Map.Entry<Integer, PressureSnapshot> entry : pressureSnapshots.entrySet()) {
            int colonyId = entry.getKey();
            PressureSnapshot pressureSnapshot = entry.getValue();
            ObservedColony observedColony = observedColonies.get(colonyId);

            if (observedColony == null || observedColony.getLatest() == null) {
                continue;
            }

            ColonySnapshot colonySnapshot = observedColony.getLatest();
            List<PrayerSeed> colonySeeds = generateForColony(
                colonyId, colonySnapshot, pressureSnapshot,
                activeEvents, transitions, memoryBank,
                memoryInfluence, gameTick
            );

            seeds.addAll(colonySeeds);
        }

        return seeds;
    }

    private List<PrayerSeed> generateForColony(
        int colonyId,
        ColonySnapshot colonySnapshot,
        PressureSnapshot pressureSnapshot,
        List<EventRecord> activeEvents,
        List<EventRecord> transitions,
        MemoryBank memoryBank,
        MemoryInfluence memoryInfluence,
        long gameTick
    ) {
        Map<PressureType, Integer> thresholdAdjustments = memoryBank.isEmpty()
            ? Map.of()
            : memoryInfluence.computeAdjustments(colonyId, memoryBank);

        List<PrayerSeed> seeds = new ArrayList<>();
        String colonyName = colonySnapshot.name();

        List<PrayerSeed> thanksSeeds = generateThanks(colonyId, colonyName, transitions, memoryBank, gameTick);
        seeds.addAll(thanksSeeds);

        List<PrayerSeed> vigilSeeds = generateVigil(colonyId, colonyName, colonySnapshot, pressureSnapshot, activeEvents, thresholdAdjustments, gameTick);
        seeds.addAll(vigilSeeds);

        List<PrayerSeed> lamentSeeds = generateLament(colonyId, colonyName, pressureSnapshot, activeEvents, memoryBank, thresholdAdjustments, gameTick);
        seeds.addAll(lamentSeeds);

        List<PrayerSeed> pleaSeeds = generatePlea(colonyId, colonyName, pressureSnapshot, activeEvents, memoryBank, thresholdAdjustments, gameTick);
        seeds.addAll(pleaSeeds);

        if (seeds.size() < MAX_PRAYERS_PER_CYCLE) {
            List<PrayerSeed> hopeSeeds = generateHope(colonyId, colonyName, pressureSnapshot, thresholdAdjustments, gameTick);
            seeds.addAll(hopeSeeds);
        }

        seeds.sort((a, b) -> Integer.compare(b.intensity(), a.intensity()));

        return seeds.subList(0, Math.min(seeds.size(), MAX_PRAYERS_PER_CYCLE));
    }

    private List<PrayerSeed> generateThanks(
        int colonyId, String colonyName,
        List<EventRecord> transitions, MemoryBank memoryBank,
        long gameTick
    ) {
        List<PrayerSeed> seeds = new ArrayList<>();

        for (EventRecord record : transitions) {
            if (!record.isResolved() || record.persistenceCount() < 3) {
                continue;
            }

            PressureType pressureType = record.event().pressureType();
            int intensity = Math.min(100, 50 + record.persistenceCount() * THANKS_PERSISTENCE_BONUS);
            List<String> reasonCodes = new ArrayList<>();
            reasonCodes.add("EVENT_RESOLVED");
            if (record.persistenceCount() >= 3) {
                reasonCodes.add("EVENT_WAS_PERSISTENT");
            }

            String content = formatThanks(colonyName, pressureType);
            String sourceKey = "resolved:" + colonyId + ":" + pressureType.name() + ":THANKS";

            seeds.add(new PrayerSeed(
                colonyId, colonyName, PrayerType.THANKS,
                pressureType, record.event().severity(),
                record.event().pressureValue(), intensity, 0,
                content, reasonCodes,
                gameTick, gameTick + PRAYER_TTL_TICKS, sourceKey
            ));
        }

        return seeds;
    }

    private List<PrayerSeed> generateVigil(
        int colonyId, String colonyName,
        ColonySnapshot colonySnapshot,
        PressureSnapshot pressureSnapshot,
        List<EventRecord> activeEvents,
        Map<PressureType, Integer> thresholdAdjustments,
        long gameTick
    ) {
        List<PrayerSeed> seeds = new ArrayList<>();

        if (!colonySnapshot.hasActiveRaid()) {
            int securityPressure = pressureSnapshot.values().getOrDefault(PressureType.SECURITY, 0);
            boolean hasSecurityEvent = activeEvents.stream()
                .anyMatch(e -> e.event().pressureType() == PressureType.SECURITY && !e.isResolved());

            if (securityPressure < 80 && !hasSecurityEvent) {
                return seeds;
            }
        }

        int securityPressure = pressureSnapshot.values().getOrDefault(PressureType.SECURITY, 0);
        int intensity = Math.min(100, securityPressure + RAID_BONUS);
        List<String> reasonCodes = new ArrayList<>();
        reasonCodes.add("SECURITY_PRESSURE_HIGH");

        if (colonySnapshot.hasActiveRaid()) {
            reasonCodes.add("ACTIVE_RAID");
            intensity = Math.min(100, intensity + RAID_BONUS);
        }

        boolean persistentSecurity = activeEvents.stream()
            .anyMatch(e -> e.event().pressureType() == PressureType.SECURITY && e.isPersistent());
        if (persistentSecurity) {
            reasonCodes.add("EVENT_PERSISTENT");
            intensity = Math.min(100, intensity + PERSISTENT_BONUS);
        }

        int memoryInfluence = thresholdAdjustments.getOrDefault(PressureType.SECURITY, 0);
        if (memoryInfluence != 0) {
            reasonCodes.add("MEMORY_INFLUENCE_ACTIVE");
        }
        intensity = Math.min(100, Math.max(0, intensity + memoryInfluence));

        String content = formatVigil(colonyName);
        String sourceKey = "event:" + colonyId + ":SECURITY:VIGIL";

        seeds.add(new PrayerSeed(
            colonyId, colonyName, PrayerType.VIGIL,
            PressureType.SECURITY, EventSeverity.HIGH,
            securityPressure, intensity, memoryInfluence,
            content, reasonCodes,
            gameTick, gameTick + PRAYER_TTL_TICKS, sourceKey
        ));

        return seeds;
    }

    private List<PrayerSeed> generateLament(
        int colonyId, String colonyName,
        PressureSnapshot pressureSnapshot,
        List<EventRecord> activeEvents,
        MemoryBank memoryBank,
        Map<PressureType, Integer> thresholdAdjustments,
        long gameTick
    ) {
        List<PrayerSeed> seeds = new ArrayList<>();

        for (PressureType pressureType : PressureType.values()) {
            List<ColonyMemory> traumaMemories = memoryBank.getMemories(colonyId).stream()
                .filter(m -> m.memoryType() == MemoryType.TRAUMA && m.pressureType() == pressureType)
                .toList();

            if (traumaMemories.isEmpty()) {
                continue;
            }

            int pressureValue = pressureSnapshot.values().getOrDefault(pressureType, 0);
            if (pressureValue < 40) {
                continue;
            }

            int intensity = Math.min(100, pressureValue + TRAUMA_BONUS);
            List<String> reasonCodes = new ArrayList<>();
            reasonCodes.add("TRAUMA_MEMORY");

            boolean persistentEvent = activeEvents.stream()
                .anyMatch(e -> e.event().pressureType() == pressureType && e.isPersistent());
            if (persistentEvent) {
                reasonCodes.add("EVENT_PERSISTENT");
                intensity = Math.min(100, intensity + PERSISTENT_BONUS);
            }

            int memoryInfluence = thresholdAdjustments.getOrDefault(pressureType, 0);
            if (memoryInfluence != 0) {
                reasonCodes.add("MEMORY_INFLUENCE_ACTIVE");
            }
            intensity = Math.min(100, Math.max(0, intensity + memoryInfluence));

            EventSeverity severity = pressureValue >= 70 ? EventSeverity.HIGH : EventSeverity.MEDIUM;
            String content = formatLament(colonyName, pressureType);
            String sourceKey = "memory:" + colonyId + ":" + pressureType.name() + ":LAMENT";

            seeds.add(new PrayerSeed(
                colonyId, colonyName, PrayerType.LAMENT,
                pressureType, severity,
                pressureValue, intensity, memoryInfluence,
                content, reasonCodes,
                gameTick, gameTick + PRAYER_TTL_TICKS, sourceKey
            ));
        }

        return seeds;
    }

    private List<PrayerSeed> generatePlea(
        int colonyId, String colonyName,
        PressureSnapshot pressureSnapshot,
        List<EventRecord> activeEvents,
        MemoryBank memoryBank,
        Map<PressureType, Integer> thresholdAdjustments,
        long gameTick
    ) {
        List<PrayerSeed> seeds = new ArrayList<>();

        for (EventRecord record : activeEvents) {
            if (record.isResolved()) {
                continue;
            }

            PressureType pressureType = record.event().pressureType();
            int pressureValue = pressureSnapshot.values().getOrDefault(pressureType, 0);

            boolean hasTrauma = memoryBank.getMemories(colonyId).stream()
                .anyMatch(m -> m.memoryType() == MemoryType.TRAUMA && m.pressureType() == pressureType);

            int intensity = pressureValue;
            if (record.isPersistent()) {
                intensity = Math.min(100, intensity + PERSISTENT_BONUS);
            }
            if (hasTrauma) {
                intensity = Math.min(100, intensity + TRAUMA_BONUS);
            }

            int memoryInfluence = thresholdAdjustments.getOrDefault(pressureType, 0);
            if (memoryInfluence != 0) {
                intensity = Math.min(100, Math.max(0, intensity + memoryInfluence));
            }

            List<String> reasonCodes = new ArrayList<>();
            String prefix = record.isPersistent() ? pressureType.getDisplayName().toUpperCase(Locale.ROOT) + "_PRESSURE_HIGH" : pressureType.getDisplayName().toUpperCase(Locale.ROOT) + "_PRESSURE_" + record.event().severity().name();
            reasonCodes.add(prefix);
            if (record.isPersistent()) {
                reasonCodes.add("EVENT_PERSISTENT");
            } else {
                reasonCodes.add("EVENT_ACTIVE");
            }
            if (hasTrauma) {
                reasonCodes.add("TRAUMA_MEMORY");
            }
            if (memoryInfluence != 0) {
                reasonCodes.add("MEMORY_INFLUENCE_ACTIVE");
            }

            String content = formatPlea(colonyName, pressureType);
            String sourceKey = "event:" + colonyId + ":" + pressureType.name() + ":" + (record.isPersistent() ? "PERSISTENT" : "ACTIVE");

            seeds.add(new PrayerSeed(
                colonyId, colonyName, PrayerType.PLEA,
                pressureType, record.event().severity(),
                pressureValue, intensity, memoryInfluence,
                content, reasonCodes,
                gameTick, gameTick + PRAYER_TTL_TICKS, sourceKey
            ));
        }

        return seeds;
    }

    private List<PrayerSeed> generateHope(
        int colonyId, String colonyName,
        PressureSnapshot pressureSnapshot,
        Map<PressureType, Integer> thresholdAdjustments,
        long gameTick
    ) {
        List<PrayerSeed> seeds = new ArrayList<>();

        for (PressureType pressureType : PressureType.values()) {
            int pressureValue = pressureSnapshot.values().getOrDefault(pressureType, 0);
            if (pressureValue < 25 || pressureValue > 60) {
                continue;
            }

            int intensity = pressureValue;
            int memoryInfluence = thresholdAdjustments.getOrDefault(pressureType, 0);
            intensity = Math.min(100, Math.max(0, intensity + memoryInfluence));

            List<String> reasonCodes = new ArrayList<>();
            reasonCodes.add(pressureType.getDisplayName().toUpperCase(Locale.ROOT) + "_PRESSURE_LOW");

            String content = formatHope(colonyName, pressureType);
            String sourceKey = "pressure:" + colonyId + ":" + pressureType.name() + ":HOPE";

            seeds.add(new PrayerSeed(
                colonyId, colonyName, PrayerType.HOPE,
                pressureType, EventSeverity.LOW,
                pressureValue, intensity, memoryInfluence,
                content, reasonCodes,
                gameTick, gameTick + PRAYER_TTL_TICKS, sourceKey
            ));
        }

        return seeds;
    }

    private String formatPlea(String colonyName, PressureType pressureType) {
        return switch (pressureType) {
            case FOOD -> colonyName + " prays for full granaries and steady hands.";
            case SECURITY -> colonyName + " calls for shields and watchful eyes against the darkness.";
            case HOUSING -> colonyName + " seeks shelter for all who dwell within its walls.";
            case COMFORT -> colonyName + " longs for peace and contentment among its people.";
            case INDUSTRY -> colonyName + " prays for forge and farm to meet the needs of all.";
        };
    }

    private String formatVigil(String colonyName) {
        return "Torches burn late in " + colonyName + ". The people keep vigil against the dark.";
    }

    private String formatLament(String colonyName, PressureType pressureType) {
        return switch (pressureType) {
            case FOOD -> colonyName + " remembers hunger. The old fear returns with empty bowls.";
            case SECURITY -> colonyName + " remembers the raids. Wounds heal slowly in the militia.";
            case HOUSING -> colonyName + " remembers the homeless. Roofs that were promised still wait.";
            case COMFORT -> colonyName + " remembers harder days. The weight of discontent lingers.";
            case INDUSTRY -> colonyName + " remembers lean production. The forge grows cold too often.";
        };
    }

    private String formatThanks(String colonyName, PressureType pressureType) {
        return switch (pressureType) {
            case FOOD -> colonyName + " gives thanks for meals shared after a season of want.";
            case SECURITY -> colonyName + " gives thanks for the guard that stands through the night.";
            case HOUSING -> colonyName + " gives thanks for the roofs that now shelter every family.";
            case COMFORT -> colonyName + " gives thanks for the peace that has returned to its streets.";
            case INDUSTRY -> colonyName + " gives thanks for the forge that burns steady once more.";
        };
    }

    private String formatHope(String colonyName, PressureType pressureType) {
        return switch (pressureType) {
            case FOOD -> "Families in " + colonyName + " speak hopefully of harvests yet to come.";
            case SECURITY -> "The people of " + colonyName + " trust their watch will hold.";
            case HOUSING -> "Families in " + colonyName + " speak hopefully of homes yet to be built.";
            case COMFORT -> "Citizens of " + colonyName + " look toward brighter days ahead.";
            case INDUSTRY -> "Workers in " + colonyName + " dream of workshops running at full stride.";
        };
    }
}