package com.godspark.pressure;

import com.godspark.GodsparkMod;
import com.godspark.observer.ObservedColony;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public final class PressureEngine {
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

            values.put(PressureType.FOOD, clampPressure(100 - latest.foodBuildingCount() * 20));

            int securityBase = 100 - latest.guardCount() * 25;
            int securityRaidBonus = latest.hasActiveRaid() ? 30 : 0;
            values.put(PressureType.SECURITY, clampPressure(securityBase + securityRaidBonus));

            int housingDeficit = latest.citizenCount() - latest.housingCapacity();
            values.put(PressureType.HOUSING, housingDeficit > 0 ? clampPressure(housingDeficit * 30) : 0);

            values.put(PressureType.COMFORT, clampPressure((10.0 - latest.happiness()) * 10.0));

            values.put(PressureType.INDUSTRY, clampPressure(100 - latest.industryBuildingCount() * 15));

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
}
