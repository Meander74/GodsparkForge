package com.godspark.memory;

import com.godspark.pressure.PressureType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class MemoryBank {

    private static final int MAX_MEMORIES_PER_COLONY = 50;

    private final Map<Integer, Deque<ColonyMemory>> memoriesByColony = new HashMap<>();

    public ColonyMemory addOrReinforce(ColonyMemory memory, long gameTick) {
        Deque<ColonyMemory> colonyMemories = memoriesByColony.computeIfAbsent(
            memory.colonyId(), k -> new LinkedList<>()
        );

        String dedupKey = memory.memoryType().name() + ":" + memory.pressureType().name();

        List<ColonyMemory> existingList = new ArrayList<>(colonyMemories);
        for (int i = 0; i < existingList.size(); i++) {
            ColonyMemory existing = existingList.get(i);
            String existingKey = existing.memoryType().name() + ":" + existing.pressureType().name();

            if (Objects.equals(existingKey, dedupKey)) {
                int newIntensity = Math.min(100, existing.intensity() + 10);
                ColonyMemory reinforced = new ColonyMemory(
                    existing.colonyId(),
                    existing.colonyName(),
                    existing.memoryType(),
                    existing.pressureType(),
                    memory.severity().rank() > existing.severity().rank() ? memory.severity() : existing.severity(),
                    memory.content().length() > existing.content().length() ? memory.content() : existing.content(),
                    newIntensity,
                    existing.reinforcementCount() + 1,
                    existing.createdAtTick(),
                    gameTick,
                    gameTick,
                    existing.decayRate()
                );
                colonyMemories.remove(existing);
                colonyMemories.addFirst(reinforced);
                return reinforced;
            }
        }

        colonyMemories.addFirst(memory);
        while (colonyMemories.size() > MAX_MEMORIES_PER_COLONY) {
            colonyMemories.removeLast();
        }
        return memory;
    }

    public List<ColonyMemory> getMemories(int colonyId) {
        Deque<ColonyMemory> colonyMemories = memoriesByColony.get(colonyId);
        if (colonyMemories == null) {
            return Collections.emptyList();
        }
        List<ColonyMemory> sorted = new ArrayList<>(colonyMemories);
        sorted.sort((a, b) -> {
            int intensityCompare = Integer.compare(b.intensity(), a.intensity());
            if (intensityCompare != 0) return intensityCompare;
            return Long.compare(b.lastReinforcedTick(), a.lastReinforcedTick());
        });
        return sorted;
    }

    public List<ColonyMemory> getStrongestMemories(int colonyId, int limit) {
        List<ColonyMemory> all = getMemories(colonyId);
        return all.subList(0, Math.min(limit, all.size()));
    }

    public List<ColonyMemory> getAllMemories() {
        List<ColonyMemory> result = new ArrayList<>();
        for (Deque<ColonyMemory> colonyMemories : memoriesByColony.values()) {
            result.addAll(colonyMemories);
        }
        result.sort((a, b) -> {
            int intensityCompare = Integer.compare(b.intensity(), a.intensity());
            if (intensityCompare != 0) return intensityCompare;
            return Long.compare(b.lastReinforcedTick(), a.lastReinforcedTick());
        });
        return result;
    }

    public int countMemories(int colonyId, PressureType pressureType) {
        Deque<ColonyMemory> colonyMemories = memoriesByColony.get(colonyId);
        if (colonyMemories == null) {
            return 0;
        }
        int count = 0;
        for (ColonyMemory memory : colonyMemories) {
            if (memory.pressureType() == pressureType) {
                count++;
            }
        }
        return count;
    }

    public Map<Integer, Deque<ColonyMemory>> getAllMemoriesByColony() {
        Map<Integer, Deque<ColonyMemory>> copy = new HashMap<>();
        for (Map.Entry<Integer, Deque<ColonyMemory>> entry : memoriesByColony.entrySet()) {
            copy.put(entry.getKey(), new LinkedList<>(entry.getValue()));
        }
        return copy;
    }

    public void loadMemories(Map<Integer, List<ColonyMemory>> memories) {
        memoriesByColony.clear();
        for (Map.Entry<Integer, List<ColonyMemory>> entry : memories.entrySet()) {
            Deque<ColonyMemory> deque = new LinkedList<>(entry.getValue());
            while (deque.size() > MAX_MEMORIES_PER_COLONY) {
                deque.removeLast();
            }
            memoriesByColony.put(entry.getKey(), deque);
        }
    }

    public void clear() {
        memoriesByColony.clear();
    }

    public boolean isEmpty() {
        return memoriesByColony.isEmpty();
    }
}
