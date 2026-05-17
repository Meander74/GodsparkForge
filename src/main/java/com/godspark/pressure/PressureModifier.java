package com.godspark.pressure;

public record PressureModifier(
    int colonyId,
    PressureType pressureType,
    int amount,
    long createdAtTick,
    long expiresAtTick,
    String source
) {
    public boolean isExpired(long currentTick) {
        return currentTick >= expiresAtTick;
    }

    public long remainingTicks(long currentTick) {
        return Math.max(0, expiresAtTick - currentTick);
    }
}