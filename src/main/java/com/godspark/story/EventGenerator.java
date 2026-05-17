package com.godspark.story;

import com.godspark.memory.MemoryBank;
import com.godspark.memory.MemoryInfluence;
import com.godspark.observer.ColonySnapshot;
import com.godspark.observer.ObservedColony;
import com.godspark.personality.ColonyPersonality;
import com.godspark.personality.PersonalityInfluence;
import com.godspark.pressure.PressureSnapshot;
import com.godspark.pressure.PressureType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class EventGenerator {

    private final MemoryInfluence memoryInfluence = new MemoryInfluence();
    private final PersonalityInfluence personalityInfluence = new PersonalityInfluence();

    public List<StoryEvent> generate(
        Map<Integer, PressureSnapshot> pressureSnapshots,
        Map<Integer, ObservedColony> observedColonies,
        MemoryBank memoryBank,
        Map<Integer, ColonyPersonality> personalities,
        long gameTick
    ) {
        if (pressureSnapshots == null || pressureSnapshots.isEmpty()
            || observedColonies == null || observedColonies.isEmpty()) {
            return List.of();
        }

        List<StoryEvent> events = new ArrayList<>();

        for (Map.Entry<Integer, PressureSnapshot> entry : pressureSnapshots.entrySet()) {
            int colonyId = entry.getKey();
            PressureSnapshot pressureSnapshot = entry.getValue();
            ObservedColony observedColony = observedColonies.get(colonyId);

            if (pressureSnapshot == null || observedColony == null || observedColony.getLatest() == null) {
                continue;
            }

            ColonySnapshot colonySnapshot = observedColony.getLatest();
            Map<PressureType, Integer> memoryAdjustments = memoryInfluence.computeAdjustments(
                colonyId, memoryBank
            );
            ColonyPersonality personality = personalities != null ? personalities.get(colonyId) : null;
            Map<PressureType, Integer> personalityAdjustments = personalityInfluence.computeAdjustments(personality);

            for (PressureType pressureType : PressureType.values()) {
                int pressureValue = pressureSnapshot.values().getOrDefault(pressureType, 0);
                int memoryAdjust = memoryAdjustments.getOrDefault(pressureType, 0);
                int personalityAdjust = personalityAdjustments.getOrDefault(pressureType, 0);
                int thresholdAdjust = PersonalityInfluence.combineAdjustments(memoryAdjust, personalityAdjust);

                StoryEventType bestMatch = findHighestMatchingEvent(
                    pressureType,
                    pressureValue,
                    colonySnapshot,
                    thresholdAdjust
                );

                if (bestMatch == null) {
                    continue;
                }

                int effectiveThreshold = Math.max(0, Math.min(100, bestMatch.threshold() + thresholdAdjust));

                events.add(new StoryEvent(
                    bestMatch,
                    colonyId,
                    colonySnapshot.name(),
                    pressureType,
                    bestMatch.severity(),
                    pressureValue,
                    effectiveThreshold,
                    bestMatch.description(),
                    gameTick
                ));
            }
        }

        return events;
    }

    private StoryEventType findHighestMatchingEvent(
        PressureType pressureType,
        int pressureValue,
        ColonySnapshot colonySnapshot,
        int thresholdAdjust
    ) {
        return Arrays.stream(StoryEventType.values())
            .filter(type -> type.pressureType() == pressureType)
            .filter(type -> pressureValue > type.threshold() + thresholdAdjust)
            .filter(type -> !type.requiresActiveRaid() || colonySnapshot.hasActiveRaid())
            .max(Comparator.comparingInt(type -> type.severity().rank()))
            .orElse(null);
    }
}