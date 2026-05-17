package com.godspark.divine;

import com.godspark.memory.ColonyMemory;
import com.godspark.observer.ColonySnapshot;
import com.godspark.prayer.PrayerSeed;
import com.godspark.pressure.PressureSnapshot;
import com.godspark.story.EventRecord;

import java.util.List;

public record DivineAnswerContext(
    ColonySnapshot colonySnapshot,
    PressureSnapshot pressureSnapshot,
    List<PrayerSeed> publicPrayers,
    List<EventRecord> activeEvents,
    List<ColonyMemory> memories
) {
    public DivineAnswerContext {
        publicPrayers = publicPrayers == null ? List.of() : List.copyOf(publicPrayers);
        activeEvents = activeEvents == null ? List.of() : List.copyOf(activeEvents);
        memories = memories == null ? List.of() : List.copyOf(memories);
    }

    public boolean hasPublicPrayers() {
        return !publicPrayers.isEmpty();
    }

    public boolean hasSacredPrayers() {
        return publicPrayers.stream().anyMatch(PrayerSeed::isMiracleEligible);
    }
}