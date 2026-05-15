package com.godspark.memory;

import com.godspark.pressure.PressureType;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class MemoryInfluence {

    private static final int TRAUMA_THRESHOLD_ADJUST = -10;
    private static final int PATTERN_THRESHOLD_ADJUST = -7;
    private static final int SIGNIFICANT_THRESHOLD_ADJUST = -3;
    private static final int TRIUMPH_THRESHOLD_ADJUST = 5;
    private static final int MAX_THRESHOLD_ADJUST = -20;
    private static final int MAX_THRESHOLD_RAISE = 10;

    public Map<PressureType, Integer> computeAdjustments(int colonyId, MemoryBank memoryBank) {
        EnumMap<PressureType, Integer> adjustments = new EnumMap<>(PressureType.class);
        for (PressureType pt : PressureType.values()) {
            adjustments.put(pt, 0);
        }

        List<ColonyMemory> memories = memoryBank.getMemories(colonyId);
        for (ColonyMemory memory : memories) {
            int adjust = switch (memory.memoryType()) {
                case TRAUMA -> TRAUMA_THRESHOLD_ADJUST;
                case PATTERN -> PATTERN_THRESHOLD_ADJUST;
                case SIGNIFICANT_EVENT -> SIGNIFICANT_THRESHOLD_ADJUST;
                case TRIUMPH -> TRIUMPH_THRESHOLD_ADJUST;
                case CULTURAL -> 0;
            };
            PressureType pt = memory.pressureType();
            int current = adjustments.getOrDefault(pt, 0);
            adjustments.put(pt, current + adjust);
        }

        for (Map.Entry<PressureType, Integer> entry : adjustments.entrySet()) {
            int raw = entry.getValue();
            int clamped = raw < 0 ? Math.max(MAX_THRESHOLD_ADJUST, raw) : Math.min(MAX_THRESHOLD_RAISE, raw);
            entry.setValue(clamped);
        }

        return adjustments;
    }
}