package com.godspark.personality;

import com.godspark.pressure.PressureType;

import java.util.EnumMap;
import java.util.Map;

public final class PersonalityInfluence {

    private static final int MIN_THRESHOLD_ADJUST = -20;
    private static final int MAX_THRESHOLD_ADJUST = 10;

    private static final Map<PersonalityTrait, Map<PressureType, Integer>> TRAIT_ADJUSTMENTS = new EnumMap<>(PersonalityTrait.class);

    static {
        TRAIT_ADJUSTMENTS.put(PersonalityTrait.AGGRESSIVE, Map.of(
            PressureType.SECURITY, -5,
            PressureType.COMFORT, 3
        ));
        TRAIT_ADJUSTMENTS.put(PersonalityTrait.PEACEFUL, Map.of(
            PressureType.SECURITY, 5,
            PressureType.COMFORT, -3
        ));
        TRAIT_ADJUSTMENTS.put(PersonalityTrait.CAUTIOUS, Map.of(
            PressureType.SECURITY, -3,
            PressureType.FOOD, -2,
            PressureType.COMFORT, -3
        ));
        TRAIT_ADJUSTMENTS.put(PersonalityTrait.RESILIENT, Map.of(
            PressureType.FOOD, 2,
            PressureType.SECURITY, 2,
            PressureType.HOUSING, 2,
            PressureType.COMFORT, 2,
            PressureType.INDUSTRY, 2
        ));
        TRAIT_ADJUSTMENTS.put(PersonalityTrait.TRADE_FOCUSED, Map.of(
            PressureType.INDUSTRY, -3,
            PressureType.COMFORT, 2
        ));
        TRAIT_ADJUSTMENTS.put(PersonalityTrait.ISOLATIONIST, Map.of(
            PressureType.SECURITY, 3,
            PressureType.COMFORT, -2
        ));
        TRAIT_ADJUSTMENTS.put(PersonalityTrait.EXPANSIONIST, Map.of(
            PressureType.HOUSING, -4,
            PressureType.COMFORT, -2
        ));
        TRAIT_ADJUSTMENTS.put(PersonalityTrait.SPIRITUAL, Map.of(
            PressureType.COMFORT, -5,
            PressureType.INDUSTRY, 3
        ));
        TRAIT_ADJUSTMENTS.put(PersonalityTrait.INDUSTRIAL, Map.of(
            PressureType.INDUSTRY, -5,
            PressureType.COMFORT, 2
        ));
        TRAIT_ADJUSTMENTS.put(PersonalityTrait.COMMUNAL, Map.of(
            PressureType.HOUSING, -3,
            PressureType.COMFORT, -2
        ));
        TRAIT_ADJUSTMENTS.put(PersonalityTrait.AGRARIAN, Map.of(
            PressureType.FOOD, -4,
            PressureType.COMFORT, -2
        ));
    }

    public Map<PressureType, Integer> computeAdjustments(ColonyPersonality personality) {
        EnumMap<PressureType, Integer> adjustments = new EnumMap<>(PressureType.class);
        for (PressureType pt : PressureType.values()) {
            adjustments.put(pt, 0);
        }

        if (personality == null) {
            return adjustments;
        }

        Map<PressureType, Integer> primaryMap = TRAIT_ADJUSTMENTS.get(personality.primaryTrait());
        if (primaryMap != null) {
            for (Map.Entry<PressureType, Integer> entry : primaryMap.entrySet()) {
                int current = adjustments.getOrDefault(entry.getKey(), 0);
                adjustments.put(entry.getKey(), current + entry.getValue());
            }
        }

        Map<PressureType, Integer> secondaryMap = TRAIT_ADJUSTMENTS.get(personality.secondaryTrait());
        if (secondaryMap != null) {
            for (Map.Entry<PressureType, Integer> entry : secondaryMap.entrySet()) {
                int half = entry.getValue() >= 0 ? entry.getValue() / 2 : -((-entry.getValue()) / 2);
                int current = adjustments.getOrDefault(entry.getKey(), 0);
                adjustments.put(entry.getKey(), current + half);
            }
        }

        for (Map.Entry<PressureType, Integer> entry : adjustments.entrySet()) {
            int raw = entry.getValue();
            int clamped = raw < 0 ? Math.max(MIN_THRESHOLD_ADJUST, raw) : Math.min(MAX_THRESHOLD_ADJUST, raw);
            entry.setValue(clamped);
        }

        return adjustments;
    }

    public static int combineAdjustments(int memoryAdjust, int personalityAdjust) {
        int total = memoryAdjust + personalityAdjust;
        return total < 0 ? Math.max(MIN_THRESHOLD_ADJUST, total) : Math.min(MAX_THRESHOLD_ADJUST, total);
    }
}
