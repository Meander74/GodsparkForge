package com.godspark.prayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PrayerSeedBank {

    private static final int MAX_PRAYERS_PER_COLONY = 20;

    private final Map<Integer, List<PrayerSeed>> prayersByColony = new HashMap<>();

    public boolean offer(PrayerSeed seed) {
        List<PrayerSeed> colonyPrayers = prayersByColony.computeIfAbsent(seed.colonyId(), k -> new ArrayList<>());

        for (PrayerSeed existing : colonyPrayers) {
            if (existing.sourceKey().equals(seed.sourceKey())) {
                return false;
            }
        }

        colonyPrayers.add(seed);

        while (colonyPrayers.size() > MAX_PRAYERS_PER_COLONY) {
            colonyPrayers.remove(0);
        }

        return true;
    }

    public List<PrayerSeed> getPrayers(int colonyId) {
        List<PrayerSeed> colonyPrayers = prayersByColony.get(colonyId);
        if (colonyPrayers == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(colonyPrayers));
    }

    public List<PrayerSeed> getAllPrayers() {
        List<PrayerSeed> result = new ArrayList<>();
        for (List<PrayerSeed> colonyPrayers : prayersByColony.values()) {
            result.addAll(colonyPrayers);
        }
        result.sort((a, b) -> Integer.compare(b.intensity(), a.intensity()));
        return result;
    }

    public void expireOld(long gameTick) {
        for (List<PrayerSeed> colonyPrayers : prayersByColony.values()) {
            colonyPrayers.removeIf(seed -> seed.expiresAtTick() > 0 && gameTick > seed.expiresAtTick());
        }

        prayersByColony.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    public void clear() {
        prayersByColony.clear();
    }

    public boolean isEmpty() {
        return prayersByColony.isEmpty();
    }
}