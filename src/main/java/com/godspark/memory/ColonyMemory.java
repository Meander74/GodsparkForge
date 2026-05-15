package com.godspark.memory;

import com.godspark.pressure.PressureType;
import com.godspark.story.EventSeverity;

public record ColonyMemory(
    int colonyId,
    String colonyName,
    MemoryType memoryType,
    PressureType pressureType,
    EventSeverity severity,
    String content,
    int intensity,
    int reinforcementCount,
    long createdAtTick,
    long lastRecalledTick,
    long lastReinforcedTick,
    int decayRate
) {}
