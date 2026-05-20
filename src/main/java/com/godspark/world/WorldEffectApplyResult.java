package com.godspark.world;

public record WorldEffectApplyResult(
    LightWorldEffect effect,
    boolean applied,
    int affectedCount,
    String reason
) {}
