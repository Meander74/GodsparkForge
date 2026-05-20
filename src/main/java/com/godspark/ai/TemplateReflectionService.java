package com.godspark.ai;

import com.godspark.GodsparkMod;
import com.godspark.memory.ColonyMemory;
import com.godspark.memory.MemoryType;
import com.godspark.observer.ColonySnapshot;
import com.godspark.observer.ObservedColony;
import com.godspark.prayer.PrayerSeed;
import com.godspark.pressure.PressureSnapshot;
import com.godspark.pressure.PressureType;
import com.godspark.story.EventRecord;
import com.godspark.story.EventSeverity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class TemplateReflectionService {

    private TemplateReflectionService() {}

    public static AiReflection reflect(int colonyId) {
        Map<Integer, ObservedColony> colonies = GodsparkMod.COLONY_OBSERVER.getObservedColonies();
        ObservedColony observed = colonies.get(colonyId);
        if (observed == null) return null;

        ColonySnapshot snapshot = observed.getLatest();
        if (snapshot == null) return null;

        Map<Integer, PressureSnapshot> pressures = GodsparkMod.PRESSURE_ENGINE.getSnapshots();
        PressureSnapshot pressure = pressures.get(colonyId);
        if (pressure == null) return null;

        List<EventRecord> activeEvents = GodsparkMod.EVENT_STATE_MANAGER.getActiveEvents().stream()
            .filter(r -> r.event().colonyId() == colonyId)
            .toList();

        List<ColonyMemory> memories = GodsparkMod.MEMORY_BANK.getStrongestMemories(colonyId, 5);
        List<PrayerSeed> prayers = GodsparkMod.PRAYER_SEED_BANK.getPrayers(colonyId);

        Map<PressureType, Integer> adjustments = GodsparkMod.MEMORY_INFLUENCE.computeAdjustments(
            colonyId, GodsparkMod.MEMORY_BANK
        );

        PressureType dominantPt = findDominant(pressure);
        int domValue = pressure.values().getOrDefault(dominantPt, 0);
        EventSeverity domSeverity = classifySeverity(domValue, dominantPt);
        String mood = computeMood(dominantPt, domSeverity, memories);
        List<String> reasonCodes = buildReasonCodes(dominantPt, domSeverity, memories, activeEvents, adjustments);
        List<String> tags = buildTags(dominantPt, mood, memories);
        String reflection = buildReflection(snapshot.name(), dominantPt, domSeverity, memories, activeEvents, prayers);
        String oracle = buildOracle(snapshot.name(), prayers);

        return new AiReflection(
            1,
            colonyId,
            snapshot.name(),
            dominantPt.getDisplayName(),
            mood,
            domValue,
            reflection,
            oracle,
            reasonCodes,
            tags,
            1.0,
            "TEMPLATE"
        );
    }

    private static PressureType findDominant(PressureSnapshot pressure) {
        PressureType result = PressureType.FOOD;
        int max = -1;
        for (PressureType pt : PressureType.values()) {
            int val = pressure.values().getOrDefault(pt, 0);
            if (val > max) {
                max = val;
                result = pt;
            }
        }
        return result;
    }

    private static EventSeverity classifySeverity(int value, PressureType pt) {
        int high = switch (pt) {
            case HOUSING -> 80;
            case COMFORT -> 85;
            default -> 90;
        };
        int medium = switch (pt) {
            case HOUSING -> 50;
            default -> 70;
        };
        int low = switch (pt) {
            case HOUSING -> 25;
            default -> 40;
        };

        if (value > high) return EventSeverity.HIGH;
        if (value > medium) return EventSeverity.MEDIUM;
        if (value > low) return EventSeverity.LOW;
        return null;
    }

    private static String computeMood(PressureType dominant, EventSeverity severity, List<ColonyMemory> memories) {
        boolean hasTrauma = memories.stream().anyMatch(m ->
            m.pressureType() == dominant && m.memoryType() == MemoryType.TRAUMA);
        boolean hasTriumph = memories.stream().anyMatch(m ->
            m.pressureType() == dominant && m.memoryType() == MemoryType.TRIUMPH);

        if (severity == EventSeverity.HIGH) {
            return hasTrauma ? "Desperate" : "Crisis";
        }
        if (severity == EventSeverity.MEDIUM) {
            return hasTrauma ? "Anxious" : (hasTriumph ? "Cautious" : "Uneasy");
        }
        if (severity == EventSeverity.LOW) {
            return hasTrauma ? "Wary" : (hasTriumph ? "Relieved" : "Concerned");
        }
        return hasTriumph ? "Hopeful" : "Stable";
    }

    private static List<String> buildReasonCodes(PressureType dominant, EventSeverity severity,
                                                   List<ColonyMemory> memories, List<EventRecord> activeEvents,
                                                   Map<PressureType, Integer> adjustments) {
        List<String> codes = new ArrayList<>();

        if (severity != null) {
            codes.add(dominant.name() + "_PRESSURE_" + severity.name());
        }

        for (ColonyMemory m : memories) {
            if (m.pressureType() == dominant) {
                String code = dominant.name() + "_" + m.memoryType().name();
                if (!codes.contains(code)) codes.add(code);
            }
        }

        for (EventRecord r : activeEvents) {
            if (r.event().pressureType() == dominant) {
                codes.add(dominant.name() + "_EVENT_" + r.event().severity().name());
            }
        }

        int adj = adjustments.getOrDefault(dominant, 0);
        if (adj != 0) {
            codes.add(dominant.name() + "_INFLUENCE_" + (adj < 0 ? "LOWER" : "RAISE"));
        }

        return codes;
    }

    private static List<String> buildTags(PressureType dominant, String mood, List<ColonyMemory> memories) {
        List<String> tags = new ArrayList<>();
        tags.add(dominant.name().toLowerCase(Locale.ROOT));

        String moodLower = mood.toLowerCase(Locale.ROOT);
        if (!moodLower.equals("stable") && !moodLower.equals("hopeful")) {
            tags.add(moodLower);
        }

        boolean hasTrauma = memories.stream().anyMatch(m ->
            m.pressureType() == dominant && m.memoryType() == MemoryType.TRAUMA);
        boolean hasPattern = memories.stream().anyMatch(m ->
            m.pressureType() == dominant && m.memoryType() == MemoryType.PATTERN);
        boolean hasTriumph = memories.stream().anyMatch(m ->
            m.pressureType() == dominant && m.memoryType() == MemoryType.TRIUMPH);

        if (hasTrauma) tags.add("trauma");
        if (hasPattern) tags.add("pattern");
        if (hasTriumph) tags.add("triumph");
        if (hasTrauma && hasPattern) tags.add("scarred");
        if (hasTriumph && !hasTrauma) tags.add("resilient");

        return tags;
    }

    private static String buildReflection(String colonyName, PressureType dominant, EventSeverity severity,
                                           List<ColonyMemory> memories, List<EventRecord> activeEvents,
                                           List<PrayerSeed> prayers) {
        String domLabel = dominant.getDisplayName().toLowerCase(Locale.ROOT);

        if (severity == null) {
            boolean hasTrauma = memories.stream().anyMatch(m -> m.memoryType() == MemoryType.TRAUMA);
            if (hasTrauma) {
                return colonyName + " is stable for now, though old fears still echo beneath the surface.";
            }
            return colonyName + " is stable, with no pressing crises. The people live in relative peace.";
        }

        String opening = switch (severity) {
            case HIGH -> "The people of " + colonyName + " are gripped by a severe " + domLabel + " crisis.";
            case MEDIUM -> colonyName + " feels the growing strain of " + domLabel + " pressure.";
            case LOW -> "A quiet " + domLabel + " concern lingers over " + colonyName + ".";
        };

        StringBuilder sb = new StringBuilder(opening);

        List<EventRecord> relevantEvents = activeEvents.stream()
            .filter(r -> r.event().pressureType() == dominant)
            .toList();
        List<EventRecord> eventsToUse = relevantEvents.isEmpty() ? activeEvents : relevantEvents;

        if (!eventsToUse.isEmpty()) {
            sb.append(" ");
            for (int i = 0; i < Math.min(2, eventsToUse.size()); i++) {
                if (i > 0) sb.append(" ");
                sb.append(eventsToUse.get(i).event().description());
                sb.append(".");
            }
        }

        boolean hasTrauma = memories.stream().anyMatch(m ->
            m.pressureType() == dominant && m.memoryType() == MemoryType.TRAUMA);
        boolean hasPattern = memories.stream().anyMatch(m ->
            m.pressureType() == dominant && m.memoryType() == MemoryType.PATTERN);
        boolean hasTriumph = memories.stream().anyMatch(m ->
            m.pressureType() == dominant && m.memoryType() == MemoryType.TRIUMPH);

        if (hasTrauma && hasPattern) {
            sb.append(" The colony remembers this pain well - it has become a scarred pattern.");
        } else if (hasTrauma) {
            sb.append(" The memory of past " + domLabel + " hardship still haunts them.");
        } else if (hasPattern) {
            sb.append(" This is not the first time " + domLabel + " has troubled them.");
        }

        if (hasTriumph) {
            sb.append(" Yet they have overcome before — and that memory gives them strength.");
        }

        if (!prayers.isEmpty()) {
            sb.append(" They pray for respite.");
        }

        return sb.toString();
    }

    private static String buildOracle(String colonyName, List<PrayerSeed> prayers) {
        if (prayers.isEmpty()) {
            return "All is as it should be for " + colonyName + ".";
        }

        return prayers.get(0).content();
    }
}
