package com.godspark.prayer;

import com.godspark.pressure.PressureType;
import com.godspark.story.EventSeverity;

import java.util.List;

public record PrayerSeed(
    int colonyId,
    String colonyName,
    PrayerType prayerType,
    PrayerChannel channel,
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
) {
    public boolean isPublicPrayer() {
        return channel.isPublic();
    }

    public boolean isMiracleEligible() {
        return channel.isMiracleEligible();
    }
}