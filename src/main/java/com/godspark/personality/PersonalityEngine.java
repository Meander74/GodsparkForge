package com.godspark.personality;

import com.godspark.GodsparkMod;
import com.godspark.memory.ColonyMemory;
import com.godspark.memory.MemoryType;
import com.godspark.observer.ColonySnapshot;
import com.godspark.observer.ObservedColony;
import com.godspark.prayer.PrayerChannel;
import com.godspark.prayer.PrayerSeed;
import com.godspark.pressure.PressureSnapshot;
import com.godspark.pressure.PressureType;
import com.godspark.story.EventRecord;
import com.godspark.story.EventSeverity;
import com.godspark.story.StoryEventType;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PersonalityEngine {

    private static final int TOP_MEMORIES = 10;
    private static final int TOP_EVENTS = 10;
    private static final int TOP_PRAYERS = 5;

    private final Map<Integer, ColonyPersonality> cache = new ConcurrentHashMap<>();

    public ColonyPersonality computePersonality(int colonyId, long gameTick) {
        ObservedColony observed = GodsparkMod.COLONY_OBSERVER.getObservedColonies().get(colonyId);
        if (observed == null) {
            return null;
        }

        ColonySnapshot snapshot = observed.getLatest();
        if (snapshot == null) {
            return null;
        }

        List<ColonyMemory> memories = GodsparkMod.MEMORY_BANK.getStrongestMemories(colonyId, TOP_MEMORIES);
        List<EventRecord> events = GodsparkMod.EVENT_STATE_MANAGER.getActiveEvents().stream()
            .filter(r -> r.event().colonyId() == colonyId)
            .toList();
        List<PrayerSeed> prayers = GodsparkMod.PRAYER_SEED_BANK.getPrayers(colonyId);
        PressureSnapshot pressures = GodsparkMod.PRESSURE_ENGINE.getSnapshots().get(colonyId);

        Map<PersonalityTrait, Integer> scores = computeScores(snapshot, memories, events, prayers, pressures);

        PersonalityTrait primary = dominantTrait(scores);
        PersonalityTrait secondary = secondaryTrait(scores, primary);
        List<PersonalityTrait> evidence = buildEvidence(scores);

        int aggression = scores.getOrDefault(PersonalityTrait.AGGRESSIVE, 0);
        int trade = scores.getOrDefault(PersonalityTrait.TRADE_FOCUSED, 0);
        int expansion = scores.getOrDefault(PersonalityTrait.EXPANSIONIST, 0);
        int spirituality = scores.getOrDefault(PersonalityTrait.SPIRITUAL, 0);

        ColonyPersonality personality = new ColonyPersonality(
            colonyId,
            snapshot.name(),
            primary,
            secondary,
            aggression,
            trade,
            expansion,
            spirituality,
            evidence,
            gameTick
        );

        cache.put(colonyId, personality);
        return personality;
    }

    private Map<PersonalityTrait, Integer> computeScores(
        ColonySnapshot snapshot,
        List<ColonyMemory> memories,
        List<EventRecord> events,
        List<PrayerSeed> prayers,
        PressureSnapshot pressures
    ) {
        Map<PersonalityTrait, Integer> scores = new EnumMap<>(PersonalityTrait.class);
        for (PersonalityTrait t : PersonalityTrait.values()) {
            scores.put(t, 0);
        }

        scoreFromMemories(memories, scores);
        scoreFromEvents(events, scores);
        scoreFromBuildings(snapshot, scores);
        scoreFromPressures(pressures, scores);
        scoreFromPrayers(prayers, scores);

        return scores;
    }

    private void scoreFromMemories(List<ColonyMemory> memories, Map<PersonalityTrait, Integer> scores) {
        for (ColonyMemory mem : memories) {
            switch (mem.memoryType()) {
                case TRAUMA -> {
                    if (mem.pressureType() == PressureType.SECURITY) {
                        scores.merge(PersonalityTrait.AGGRESSIVE, 3, Integer::sum);
                        scores.merge(PersonalityTrait.CAUTIOUS, 2, Integer::sum);
                    }
                    if (mem.pressureType() == PressureType.FOOD) {
                        scores.merge(PersonalityTrait.CAUTIOUS, 1, Integer::sum);
                        scores.merge(PersonalityTrait.AGRARIAN, 1, Integer::sum);
                    }
                    scores.merge(PersonalityTrait.RESILIENT, 1, Integer::sum);
                }
                case TRIUMPH -> {
                    scores.merge(PersonalityTrait.RESILIENT, 3, Integer::sum);
                    scores.merge(PersonalityTrait.PEACEFUL, 1, Integer::sum);
                }
                case PATTERN -> {
                    scores.merge(PersonalityTrait.CAUTIOUS, 2, Integer::sum);
                    scores.merge(PersonalityTrait.INDUSTRIAL, 1, Integer::sum);
                }
                case SIGNIFICANT_EVENT -> {
                    scores.merge(PersonalityTrait.COMMUNAL, 1, Integer::sum);
                    scores.merge(PersonalityTrait.CAUTIOUS, 1, Integer::sum);
                }
                case CULTURAL -> {
                    scores.merge(PersonalityTrait.SPIRITUAL, 2, Integer::sum);
                }
            }
        }
    }

    private void scoreFromEvents(List<EventRecord> events, Map<PersonalityTrait, Integer> scores) {
        for (EventRecord record : events) {
            if (record.event().severity() == EventSeverity.HIGH) {
                scores.merge(PersonalityTrait.CAUTIOUS, 2, Integer::sum);
                scores.merge(PersonalityTrait.AGGRESSIVE, 1, Integer::sum);
            }
            if (record.isPersistent()) {
                scores.merge(PersonalityTrait.RESILIENT, 1, Integer::sum);
                scores.merge(PersonalityTrait.CAUTIOUS, 1, Integer::sum);
            }
        }
    }

    private void scoreFromBuildings(ColonySnapshot snapshot, Map<PersonalityTrait, Integer> scores) {
        int citizens = snapshot.citizenCount();
        int guards = snapshot.guardCount();
        int sacred = snapshot.sacredBuildingCount();
        int industry = snapshot.industryBuildingCount();
        int food = snapshot.foodBuildingCount();
        int warehouse = snapshot.warehouseCount();

        if (guards > 0 && citizens > 0) {
            double guardRatio = (double) guards / citizens;
            if (guardRatio > 0.15) {
                scores.merge(PersonalityTrait.AGGRESSIVE, 3, Integer::sum);
            }
            if (guardRatio < 0.05 && citizens > 5) {
                scores.merge(PersonalityTrait.PEACEFUL, 2, Integer::sum);
            }
        }

        if (sacred > 0) {
            scores.merge(PersonalityTrait.SPIRITUAL, 2 + sacred, Integer::sum);
        }

        if (industry > 1) {
            scores.merge(PersonalityTrait.INDUSTRIAL, 2 + industry, Integer::sum);
        }

        if (food > 0 && citizens > 0) {
            double foodRatio = (double) food / citizens;
            if (foodRatio > 0.2) {
                scores.merge(PersonalityTrait.AGRARIAN, 3, Integer::sum);
            }
        }

        if (warehouse > 0) {
            scores.merge(PersonalityTrait.TRADE_FOCUSED, 2, Integer::sum);
        }

        if (citizens > 10 && guards < 2) {
            scores.merge(PersonalityTrait.ISOLATIONIST, 2, Integer::sum);
            scores.merge(PersonalityTrait.COMMUNAL, 1, Integer::sum);
        } else if (citizens > 10 && guards > 3) {
            scores.merge(PersonalityTrait.EXPANSIONIST, 2, Integer::sum);
        }
    }

    private void scoreFromPressures(PressureSnapshot pressures, Map<PersonalityTrait, Integer> scores) {
        if (pressures == null) return;

        int security = pressures.values().getOrDefault(PressureType.SECURITY, 0);
        int food = pressures.values().getOrDefault(PressureType.FOOD, 0);
        int comfort = pressures.values().getOrDefault(PressureType.COMFORT, 0);
        int industry = pressures.values().getOrDefault(PressureType.INDUSTRY, 0);
        int housing = pressures.values().getOrDefault(PressureType.HOUSING, 0);

        if (security > 70) {
            scores.merge(PersonalityTrait.AGGRESSIVE, 2, Integer::sum);
            scores.merge(PersonalityTrait.CAUTIOUS, 2, Integer::sum);
        }
        if (food > 70) {
            scores.merge(PersonalityTrait.AGRARIAN, 2, Integer::sum);
        }
        if (comfort < 20) {
            scores.merge(PersonalityTrait.COMMUNAL, 1, Integer::sum);
        }
        if (industry > 70) {
            scores.merge(PersonalityTrait.INDUSTRIAL, 2, Integer::sum);
        }
        if (housing < 20 && security < 30) {
            scores.merge(PersonalityTrait.PEACEFUL, 1, Integer::sum);
        }
    }

    private void scoreFromPrayers(List<PrayerSeed> prayers, Map<PersonalityTrait, Integer> scores) {
        for (PrayerSeed seed : prayers) {
            switch (seed.channel()) {
                case CHURCH, SHRINE, TEMPLE -> scores.merge(PersonalityTrait.SPIRITUAL, 2, Integer::sum);
                case PRIVATE -> {}
            }

            switch (seed.prayerType()) {
                case PLEA -> scores.merge(PersonalityTrait.CAUTIOUS, 1, Integer::sum);
                case VIGIL -> scores.merge(PersonalityTrait.SPIRITUAL, 1, Integer::sum);
                case THANKS -> scores.merge(PersonalityTrait.PEACEFUL, 1, Integer::sum);
                case LAMENT -> scores.merge(PersonalityTrait.RESILIENT, 1, Integer::sum);
                case HOPE -> scores.merge(PersonalityTrait.EXPANSIONIST, 1, Integer::sum);
            }
        }
    }

    private PersonalityTrait dominantTrait(Map<PersonalityTrait, Integer> scores) {
        return scores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(PersonalityTrait.RESILIENT);
    }

    private PersonalityTrait secondaryTrait(Map<PersonalityTrait, Integer> scores, PersonalityTrait primary) {
        return scores.entrySet().stream()
            .filter(e -> e.getKey() != primary)
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(PersonalityTrait.RESILIENT);
    }

    private List<PersonalityTrait> buildEvidence(Map<PersonalityTrait, Integer> scores) {
        return scores.entrySet().stream()
            .filter(e -> e.getValue() > 0)
            .sorted((a, b) -> b.getValue() - a.getValue())
            .map(Map.Entry::getKey)
            .toList();
    }

    public ColonyPersonality getCached(int colonyId) {
        return cache.get(colonyId);
    }

    public void clearCache() {
        cache.clear();
    }

    public void updateFromObservations(Map<Integer, ObservedColony> observed, long tick) {
        for (Integer colonyId : observed.keySet()) {
            computePersonality(colonyId, tick);
        }
    }
}