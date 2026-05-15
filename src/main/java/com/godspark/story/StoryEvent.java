package com.godspark.story;

import com.godspark.pressure.PressureType;

public record StoryEvent(
    StoryEventType eventType,
    int colonyId,
    String colonyName,
    PressureType pressureType,
    EventSeverity severity,
    int pressureValue,
    int threshold,
    String description,
    long gameTick
) {}
