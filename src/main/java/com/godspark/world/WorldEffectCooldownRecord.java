package com.godspark.world;

public record WorldEffectCooldownRecord(
    WorldEffectCooldownKey key,
    long lastFireTick,
    String lastEffect,
    int lastAffectedCount
) {}
