package com.godspark.story;

import com.godspark.observer.ColonySnapshot;
import com.godspark.observer.ObservedColony;
import com.godspark.pressure.PressureSnapshot;
import com.godspark.pressure.PressureType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class EventGenerator {

    public List<StoryEvent> generate(
        Map<Integer, PressureSnapshot> pressureSnapshots,
        Map<Integer, ObservedColony> observedColonies,
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

            for (PressureType pressureType : PressureType.values()) {
                int pressureValue = pressureSnapshot.values().getOrDefault(pressureType, 0);

                StoryEventType bestMatch = findHighestMatchingEvent(
                    pressureType,
                    pressureValue,
                    colonySnapshot
                );

                if (bestMatch == null) {
                    continue;
                }

                events.add(new StoryEvent(
                    bestMatch,
                    colonyId,
                    colonySnapshot.name(),
                    pressureType,
                    bestMatch.severity(),
                    pressureValue,
                    bestMatch.threshold(),
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
        ColonySnapshot colonySnapshot
    ) {
        return Arrays.stream(StoryEventType.values())
            .filter(type -> type.pressureType() == pressureType)
            .filter(type -> pressureValue > type.threshold())
            .filter(type -> !type.requiresActiveRaid() || colonySnapshot.hasActiveRaid())
            .max(Comparator.comparingInt(type -> type.severity().rank()))
            .orElse(null);
    }
}
