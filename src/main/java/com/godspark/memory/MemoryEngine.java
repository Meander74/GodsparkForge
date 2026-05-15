package com.godspark.memory;

import com.godspark.pressure.PressureType;
import com.godspark.story.EventRecord;
import com.godspark.story.EventSeverity;
import com.godspark.story.EventState;

import java.util.ArrayList;
import java.util.List;

public final class MemoryEngine {

    public List<ColonyMemory> generateMemories(
        List<EventRecord> transitions,
        MemoryBank memoryBank,
        long gameTick
    ) {
        List<ColonyMemory> memories = new ArrayList<>();

        for (EventRecord record : transitions) {
            if (record.isResolved()) {
                handleResolved(record, memoryBank, gameTick, memories);
            } else {
                handleActive(record, memoryBank, gameTick, memories);
            }
        }

        return memories;
    }

    private void handleActive(
        EventRecord record,
        MemoryBank memoryBank,
        long gameTick,
        List<ColonyMemory> memories
    ) {
        boolean becamePersistent = record.state() == EventState.ACTIVE && record.persistenceCount() == 2;

        if (becamePersistent) {
            memories.add(createSignificantEventMemory(record, gameTick));
            if (record.event().severity() == EventSeverity.HIGH) {
                memories.add(createTraumaMemory(record, gameTick));
            }
        }

        checkAndCreatePattern(record, memoryBank, gameTick, memories);
    }

    private void handleResolved(
        EventRecord record,
        MemoryBank memoryBank,
        long gameTick,
        List<ColonyMemory> memories
    ) {
        if (record.persistenceCount() >= 3) {
            ColonyMemory significant = createResolvedSignificantMemory(record, gameTick);
            memories.add(significant);
        }

        if (record.event().severity() == EventSeverity.HIGH && record.persistenceCount() >= 2) {
            memories.add(createTriumphMemory(record, gameTick));
        }
    }

    private ColonyMemory createSignificantEventMemory(EventRecord record, long gameTick) {
        int intensity = calculateIntensity(record.event().severity(), record.persistenceCount(), false);
        String content = String.format(
            "%s %s has persisted in %s.",
            record.event().pressureType().getDisplayName(),
            record.event().eventType().description().toLowerCase(),
            record.event().colonyName()
        );

        return new ColonyMemory(
            record.event().colonyId(),
            record.event().colonyName(),
            MemoryType.SIGNIFICANT_EVENT,
            record.event().pressureType(),
            record.event().severity(),
            content,
            intensity,
            0,
            gameTick,
            gameTick,
            gameTick,
            1
        );
    }

    private ColonyMemory createTraumaMemory(EventRecord record, long gameTick) {
        int intensity = calculateIntensity(record.event().severity(), record.persistenceCount(), true);
        String content = String.format(
            "%s remembers the threat of %s.",
            record.event().colonyName(),
            record.event().pressureType().getDisplayName().toLowerCase()
        );

        return new ColonyMemory(
            record.event().colonyId(),
            record.event().colonyName(),
            MemoryType.TRAUMA,
            record.event().pressureType(),
            record.event().severity(),
            content,
            intensity,
            0,
            gameTick,
            gameTick,
            gameTick,
            1
        );
    }

    private ColonyMemory createResolvedSignificantMemory(EventRecord record, long gameTick) {
        int intensity = calculateIntensity(record.event().severity(), record.persistenceCount(), false);
        String content = String.format(
            "%s endured a period of %s %s that lasted %d cycles.",
            record.event().colonyName(),
            record.event().severity().name().toLowerCase(),
            record.event().pressureType().getDisplayName().toLowerCase(),
            record.persistenceCount()
        );

        return new ColonyMemory(
            record.event().colonyId(),
            record.event().colonyName(),
            MemoryType.SIGNIFICANT_EVENT,
            record.event().pressureType(),
            record.event().severity(),
            content,
            intensity,
            0,
            gameTick,
            gameTick,
            gameTick,
            1
        );
    }

    private ColonyMemory createTriumphMemory(EventRecord record, long gameTick) {
        int intensity = Math.min(100, 60 + record.persistenceCount() * 5);
        String content = String.format(
            "%s overcame a %s %s crisis.",
            record.event().colonyName(),
            record.event().severity().name().toLowerCase(),
            record.event().pressureType().getDisplayName().toLowerCase()
        );

        return new ColonyMemory(
            record.event().colonyId(),
            record.event().colonyName(),
            MemoryType.TRIUMPH,
            record.event().pressureType(),
            record.event().severity(),
            content,
            intensity,
            0,
            gameTick,
            gameTick,
            gameTick,
            1
        );
    }

    private void checkAndCreatePattern(
        EventRecord record,
        MemoryBank memoryBank,
        long gameTick,
        List<ColonyMemory> memories
    ) {
        PressureType pressureType = record.event().pressureType();
        int colonyId = record.event().colonyId();
        int existingCount = memoryBank.countMemories(colonyId, pressureType);

        if (existingCount >= 2) {
            int intensity = Math.min(100, 50 + existingCount * 5);
            String content = String.format(
                "%s %s has become a recurring pattern.",
                record.event().pressureType().getDisplayName(),
                record.event().eventType().description().toLowerCase()
            );

            ColonyMemory pattern = new ColonyMemory(
                colonyId,
                record.event().colonyName(),
                MemoryType.PATTERN,
                pressureType,
                record.event().severity(),
                content,
                intensity,
                0,
                gameTick,
                gameTick,
                gameTick,
                1
            );
            memories.add(pattern);
        }
    }

    private static int calculateIntensity(EventSeverity severity, int persistenceCount, boolean isTrauma) {
        int base = switch (severity) {
            case LOW -> 25;
            case MEDIUM -> 50;
            case HIGH -> 75;
        };

        int persistenceBonus = Math.min(25, persistenceCount * 5);

        int stateBonus = 0;
        if (isTrauma) {
            stateBonus = 10;
        } else if (persistenceCount >= 3) {
            stateBonus = 10;
        }

        return Math.min(100, Math.max(0, base + persistenceBonus + stateBonus));
    }
}
