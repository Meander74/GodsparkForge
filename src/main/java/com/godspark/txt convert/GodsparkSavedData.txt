package com.godspark.persistence;

import com.godspark.memory.ColonyMemory;
import com.godspark.memory.MemoryBank;
import com.godspark.memory.MemoryType;
import com.godspark.pressure.PressureType;
import com.godspark.story.EventStateManager;
import com.godspark.story.EventState;
import com.godspark.story.EventSeverity;
import com.godspark.story.EventRecord;
import com.godspark.story.StoryEvent;
import com.godspark.story.StoryEventType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GodsparkSavedData extends SavedData {

    private static final Logger LOGGER = LogManager.getLogger(GodsparkSavedData.class);
    private static final int DATA_VERSION = 2;
    private static final int MAX_RESOLVED_EVENTS = 500;
    private static final int MAX_MEMORIES_PER_COLONY = 50;

    public static final String DATA_KEY = "godspark_data";

    public record SavedEventRecord(
        StoryEventType eventType,
        int colonyId,
        String colonyName,
        PressureType pressureType,
        EventSeverity severity,
        int pressureValue,
        int threshold,
        String description,
        long gameTick,
        EventState state,
        int persistenceCount,
        int missingCycles,
        long firstSeenTick,
        long lastSeenTick,
        long resolvedTick
    ) {}

    private final Map<String, SavedEventRecord> activeEvents = new HashMap<>();
    private final Deque<SavedEventRecord> resolvedEvents = new ArrayDeque<>();
    private final Map<Integer, Deque<ColonyMemory>> memoriesByColony = new HashMap<>();

    public static GodsparkSavedData load(CompoundTag tag) {
        GodsparkSavedData data = new GodsparkSavedData();

        if (tag.contains("activeEvents")) {
            ListTag activeList = tag.getList("activeEvents", Tag.TAG_COMPOUND);
            for (int i = 0; i < activeList.size(); i++) {
                CompoundTag eventTag = activeList.getCompound(i);
                SavedEventRecord record = readEventRecord(eventTag);
                if (record == null) continue;
                String key = record.colonyId() + ":" + record.pressureType().name();
                data.activeEvents.put(key, record);
            }
        }

        if (tag.contains("resolvedEvents")) {
            ListTag resolvedList = tag.getList("resolvedEvents", Tag.TAG_COMPOUND);
            for (int i = 0; i < resolvedList.size(); i++) {
                CompoundTag eventTag = resolvedList.getCompound(i);
                SavedEventRecord record = readEventRecord(eventTag);
                if (record == null) continue;
                data.resolvedEvents.addLast(record);
            }
        }

        data.trimResolvedEvents();

        if (tag.contains("memories")) {
            ListTag memoriesList = tag.getList("memories", Tag.TAG_COMPOUND);
            for (int i = 0; i < memoriesList.size(); i++) {
                CompoundTag memoryTag = memoriesList.getCompound(i);
                ColonyMemory memory = readMemoryRecord(memoryTag);
                if (memory == null) continue;
                data.memoriesByColony.computeIfAbsent(memory.colonyId(), k -> new ArrayDeque<>())
                    .addLast(memory);
            }
            data.trimMemories();
        }

        return data;
    }

    public static GodsparkSavedData createDefault() {
        return new GodsparkSavedData();
    }

    @Nullable
    private static SavedEventRecord readEventRecord(CompoundTag tag) {
        try {
            StoryEventType eventType = parseEnumOrNull(StoryEventType.class, tag.getString("eventType"));
            if (eventType == null) {
                LOGGER.warn("[Godspark SavedData] Skipping record with unknown eventType");
                return null;
            }

            PressureType pressureType = parseEnum(PressureType.class, tag.getString("pressureType"), eventType.pressureType());
            EventSeverity severity = parseEnum(EventSeverity.class, tag.getString("severity"), eventType.severity());
            EventState state = parseEnum(EventState.class, tag.getString("state"), EventState.ACTIVE);

            int threshold = tag.contains("threshold") ? tag.getInt("threshold") : eventType.threshold();
            String description = tag.contains("description") ? tag.getString("description") : eventType.description();
            long resolvedTick = tag.contains("resolvedTick") ? tag.getLong("resolvedTick") : -1L;

            return new SavedEventRecord(
                eventType,
                tag.getInt("colonyId"),
                tag.getString("colonyName"),
                pressureType,
                severity,
                tag.getInt("pressureValue"),
                threshold,
                description,
                tag.getLong("gameTick"),
                state,
                tag.getInt("persistenceCount"),
                tag.getInt("missingCycles"),
                tag.getLong("firstSeenTick"),
                tag.getLong("lastSeenTick"),
                resolvedTick
            );
        } catch (Exception e) {
            LOGGER.warn("[Godspark SavedData] Skipping invalid event record: {}", e.getMessage());
            return null;
        }
    }

    @Nullable
    private static <E extends Enum<E>> E parseEnumOrNull(Class<E> enumClass, String value) {
        try {
            return Enum.valueOf(enumClass, value);
        } catch (Exception e) {
            return null;
        }
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value, E fallback) {
        try {
            return Enum.valueOf(enumClass, value);
        } catch (Exception e) {
            return fallback;
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt("version", DATA_VERSION);

        ListTag activeList = new ListTag();
        for (SavedEventRecord record : activeEvents.values()) {
            activeList.add(writeEventRecord(record));
        }
        tag.put("activeEvents", activeList);

        ListTag resolvedList = new ListTag();
        for (SavedEventRecord record : resolvedEvents) {
            resolvedList.add(writeEventRecord(record));
        }
        tag.put("resolvedEvents", resolvedList);

        ListTag memoriesList = new ListTag();
        for (Deque<ColonyMemory> colonyMemories : memoriesByColony.values()) {
            for (ColonyMemory memory : colonyMemories) {
                memoriesList.add(writeMemoryRecord(memory));
            }
        }
        tag.put("memories", memoriesList);

        return tag;
    }

    private static CompoundTag writeEventRecord(SavedEventRecord record) {
        CompoundTag tag = new CompoundTag();
        tag.putString("eventType", record.eventType().name());
        tag.putInt("colonyId", record.colonyId());
        tag.putString("colonyName", record.colonyName());
        tag.putString("pressureType", record.pressureType().name());
        tag.putString("severity", record.severity().name());
        tag.putInt("pressureValue", record.pressureValue());
        tag.putInt("threshold", record.threshold());
        tag.putString("description", record.description());
        tag.putLong("gameTick", record.gameTick());
        tag.putString("state", record.state().name());
        tag.putInt("persistenceCount", record.persistenceCount());
        tag.putInt("missingCycles", record.missingCycles());
        tag.putLong("firstSeenTick", record.firstSeenTick());
        tag.putLong("lastSeenTick", record.lastSeenTick());
        tag.putLong("resolvedTick", record.resolvedTick());
        return tag;
    }

    @Nullable
    private static ColonyMemory readMemoryRecord(CompoundTag tag) {
        try {
            MemoryType memoryType = parseEnum(MemoryType.class, tag.getString("memoryType"), MemoryType.SIGNIFICANT_EVENT);
            PressureType pressureType = parseEnum(PressureType.class, tag.getString("pressureType"), PressureType.FOOD);
            EventSeverity severity = parseEnum(EventSeverity.class, tag.getString("severity"), EventSeverity.LOW);

            return new ColonyMemory(
                tag.getInt("colonyId"),
                tag.getString("colonyName"),
                memoryType,
                pressureType,
                severity,
                tag.getString("content"),
                tag.getInt("intensity"),
                tag.getInt("reinforcementCount"),
                tag.getLong("createdAtTick"),
                tag.getLong("lastRecalledTick"),
                tag.getLong("lastReinforcedTick"),
                tag.getInt("decayRate")
            );
        } catch (Exception e) {
            LOGGER.warn("[Godspark SavedData] Skipping invalid memory record: {}", e.getMessage());
            return null;
        }
    }

    private static CompoundTag writeMemoryRecord(ColonyMemory memory) {
        CompoundTag tag = new CompoundTag();
        tag.putString("memoryType", memory.memoryType().name());
        tag.putInt("colonyId", memory.colonyId());
        tag.putString("colonyName", memory.colonyName());
        tag.putString("pressureType", memory.pressureType().name());
        tag.putString("severity", memory.severity().name());
        tag.putString("content", memory.content());
        tag.putInt("intensity", memory.intensity());
        tag.putInt("reinforcementCount", memory.reinforcementCount());
        tag.putLong("createdAtTick", memory.createdAtTick());
        tag.putLong("lastRecalledTick", memory.lastRecalledTick());
        tag.putLong("lastReinforcedTick", memory.lastReinforcedTick());
        tag.putInt("decayRate", memory.decayRate());
        return tag;
    }

    private void trimMemories() {
        for (Map.Entry<Integer, Deque<ColonyMemory>> entry : memoriesByColony.entrySet()) {
            while (entry.getValue().size() > MAX_MEMORIES_PER_COLONY) {
                entry.getValue().removeLast();
            }
        }
    }

    public void restoreTo(EventStateManager manager) {
        manager.clear();

        for (SavedEventRecord saved : activeEvents.values()) {
            StoryEvent event = new StoryEvent(
                saved.eventType(),
                saved.colonyId(),
                saved.colonyName(),
                saved.pressureType(),
                saved.severity(),
                saved.pressureValue(),
                saved.threshold(),
                saved.description(),
                saved.gameTick()
            );
            EventRecord record = new EventRecord(
                event,
                saved.state(),
                saved.persistenceCount(),
                saved.missingCycles(),
                saved.firstSeenTick(),
                saved.lastSeenTick(),
                saved.resolvedTick()
            );
            if (record.isResolved()) {
                manager.restoreResolved(record);
            } else {
                manager.restoreActive(record);
            }
        }

        for (SavedEventRecord saved : resolvedEvents) {
            StoryEvent event = new StoryEvent(
                saved.eventType(),
                saved.colonyId(),
                saved.colonyName(),
                saved.pressureType(),
                saved.severity(),
                saved.pressureValue(),
                saved.threshold(),
                saved.description(),
                saved.gameTick()
            );
            EventRecord record = new EventRecord(
                event,
                saved.state(),
                saved.persistenceCount(),
                saved.missingCycles(),
                saved.firstSeenTick(),
                saved.lastSeenTick(),
                saved.resolvedTick()
            );
            if (record.isResolved()) {
                manager.restoreResolved(record);
            } else {
                manager.restoreActive(record);
            }
        }

        LOGGER.info(
            "[Godspark SavedData] Restored {} active, {} resolved events",
            activeEvents.size(),
            resolvedEvents.size()
        );
    }

    public void captureFrom(EventStateManager manager) {
        activeEvents.clear();
        resolvedEvents.clear();

        for (EventRecord record : manager.getAllActive()) {
            StoryEvent event = record.event();
            SavedEventRecord saved = new SavedEventRecord(
                event.eventType(),
                event.colonyId(),
                event.colonyName(),
                event.pressureType(),
                event.severity(),
                event.pressureValue(),
                event.threshold(),
                event.description(),
                event.gameTick(),
                record.state(),
                record.persistenceCount(),
                record.missingCycles(),
                record.firstSeenTick(),
                record.lastSeenTick(),
                record.resolvedTick()
            );
            String key = event.colonyId() + ":" + event.pressureType().name();
            activeEvents.put(key, saved);
        }

        for (EventRecord record : manager.getAllResolved()) {
            StoryEvent event = record.event();
            SavedEventRecord saved = new SavedEventRecord(
                event.eventType(),
                event.colonyId(),
                event.colonyName(),
                event.pressureType(),
                event.severity(),
                event.pressureValue(),
                event.threshold(),
                event.description(),
                event.gameTick(),
                record.state(),
                record.persistenceCount(),
                record.missingCycles(),
                record.firstSeenTick(),
                record.lastSeenTick(),
                record.resolvedTick()
            );
            resolvedEvents.addLast(saved);
        }

        trimResolvedEvents();
        setDirty();
    }

    private void trimResolvedEvents() {
        while (resolvedEvents.size() > MAX_RESOLVED_EVENTS) {
            resolvedEvents.removeFirst();
        }
    }

    public void captureMemoriesFrom(MemoryBank memoryBank) {
        memoriesByColony.clear();
        Map<Integer, Deque<ColonyMemory>> allMemories = memoryBank.getAllMemoriesByColony();
        for (Map.Entry<Integer, Deque<ColonyMemory>> entry : allMemories.entrySet()) {
            Deque<ColonyMemory> trimmed = new ArrayDeque<>();
            for (ColonyMemory memory : entry.getValue()) {
                trimmed.addLast(memory);
            }
            while (trimmed.size() > MAX_MEMORIES_PER_COLONY) {
                trimmed.removeFirst();
            }
            memoriesByColony.put(entry.getKey(), trimmed);
        }
        setDirty();
    }

    public void restoreMemoriesTo(MemoryBank memoryBank) {
        memoryBank.clear();
        Map<Integer, List<ColonyMemory>> memoriesToLoad = new HashMap<>();
        for (Map.Entry<Integer, Deque<ColonyMemory>> entry : memoriesByColony.entrySet()) {
            memoriesToLoad.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        memoryBank.loadMemories(memoriesToLoad);
        LOGGER.info(
            "[Godspark SavedData] Restored {} colony memories",
            memoriesByColony.values().stream().mapToInt(Deque::size).sum()
        );
    }
}
