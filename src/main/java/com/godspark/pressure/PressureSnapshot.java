package com.godspark.pressure;

import java.util.Map;

public record PressureSnapshot(
    int colonyId,
    Map<PressureType, Integer> values,
    long gameTick
) {
    public PressureSnapshot {
        values = Map.copyOf(values);
    }
}
