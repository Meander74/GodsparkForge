package com.godspark.pressure;

import com.godspark.GodsparkMod;
import com.godspark.observer.ObservedColony;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public final class PressureEngine {
    private static final double CITIZENS_PER_FOOD_BUILDING = 5.0;
    private static final double CITIZENS_PER_GUARD = 8.0;
    private static final double CITIZENS_PER_INDUSTRY_BUILDING = 6.0;
    private static final int ACTIVE_RAID_SECURITY_SPIKE = 40;
    private static final double TARGET_HAPPINESS = 7.0;

    private final Map<Integer, PressureSnapshot> snapshots = new HashMap<>();

    public void compute(Map<Integer, ObservedColony> colonies) {
        snapshots.clear();

        if (colonies.isEmpty()) {
            return;
        }

        for (ObservedColony observed : colonies.values()) {
            var latest = observed.getLatest();
            if (latest == null) continue;

            Map<PressureType, Integer> values = new EnumMap<>(PressureType.class);

            values.put(PressureType.FOOD, capacityPressure(latest.citizenCount(), latest.foodBuildingCount(), CITIZENS_PER_FOOD_BUILDING));

            int securityBase = capacityPressure(latest.citizenCount(), latest.guardCount(), CITIZENS_PER_GUARD);
            int securityRaidBonus = latest.hasActiveRaid() ? ACTIVE_RAID_SECURITY_SPIKE : 0;
            values.put(PressureType.SECURITY, clampPressure(securityBase + securityRaidBonus));

            values.put(PressureType.HOUSING, computeHousingPressure(latest.citizenCount(), latest.housingCapacity()));

            values.put(PressureType.COMFORT, computeComfortPressure(latest.citizenCount(), latest.happiness()));

            values.put(PressureType.INDUSTRY, capacityPressure(latest.citizenCount(), latest.industryBuildingCount(), CITIZENS_PER_INDUSTRY_BUILDING));

            PressureSnapshot snapshot = new PressureSnapshot(
                observed.getColonyId(),
                values,
                latest.gameTick()
            );

            snapshots.put(observed.getColonyId(), snapshot);
        }

        GodsparkMod.LOGGER.debug("PressureEngine computed pressures for {} colonies", snapshots.size());
    }

    public Map<Integer, PressureSnapshot> getSnapshots() {
        return Map.copyOf(snapshots);
    }

    public void clear() {
        snapshots.clear();
    }

    private static int clampPressure(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0;
        }
        return (int) Math.round(Math.max(0, Math.min(100, value)));
    }

    private static double citizenDemandFactor(int citizenCount) {
        if (citizenCount <= 0) {
            return 0.0;
        }
        return Math.min(1.0, citizenCount / 10.0);
    }

    private static int capacityPressure(int citizenCount, int capacityUnits, double citizensPerUnit) {
        if (citizenCount <= 0) {
            return 0;
        }

        double supportedCitizens = Math.max(0, capacityUnits) * citizensPerUnit;
        double shortageCitizens = Math.max(0.0, citizenCount - supportedCitizens);
        double shortageRatio = shortageCitizens / citizenCount;

        return clampPressure(100.0 * shortageRatio * citizenDemandFactor(citizenCount));
    }

    private static int computeHousingPressure(int citizenCount, int housingCapacity) {
        if (citizenCount <= 0) {
            return 0;
        }

        int shortage = Math.max(0, citizenCount - Math.max(0, housingCapacity));
        if (shortage <= 0) {
            return 0;
        }

        double shortageRatio = shortage / (double) citizenCount;
        return clampPressure(100.0 * shortageRatio * citizenDemandFactor(citizenCount));
    }

    private static int computeComfortPressure(int citizenCount, double happiness) {
        if (citizenCount <= 0 || Double.isNaN(happiness) || Double.isInfinite(happiness)) {
            return 0;
        }

        double deficit = Math.max(0.0, TARGET_HAPPINESS - happiness);
        return clampPressure(deficit * 15.0 * citizenDemandFactor(citizenCount));
    }
}
