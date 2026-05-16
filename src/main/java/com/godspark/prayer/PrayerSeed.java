package com.godspark.prayer;

import com.godspark.pressure.PressureType;
import com.godspark.story.EventSeverity;

import java.util.List;

public record PrayerSeed(
    int colonyId,
    String colonyName,
    PrayerType prayerType,
    PressureType pressureType,
    EventSeverity severity,
    int pressureValue,
    int intensity,
    int memoryInfluence,
    String content,
    List<String> reasonCodes,
    long createdAtTick,
    long expiresAtTick,
    String sourceKey
) {}