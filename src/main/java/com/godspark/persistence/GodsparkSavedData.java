package com.godspark.persistence;

import com.godspark.GodsparkMod;
import com.godspark.pressure.PressureType;
import com.godspark.story.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class GodsparkSavedData extends SavedData {

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

    public static GodsparkSavedData load(CompoundTag tag) {
        GodsparkSavedData data = new GodsparkSavedData();

        if (tag.contains("activeEvents")) {
            ListTag activeList = tag.getList("activeEvents", 10);
            for (int i = 0; i < activeList.size(); i++) {
                CompoundTag eventTag = activeList.getCompound(i);
                SavedEventRecord record = readEventRecord(eventTag);
                String key = record.colonyId() + ":" + record.pressureType().name();
                data.activeEvents.put(key, record);
            }
        }

        if (tag.contains("resolvedEvents")) {
            ListTag resolvedList = tag.getList("resolvedEvents", 10);
            for (int i = 0; i < resolvedList.size(); i++) {
                CompoundTag eventTag = resolvedList.getCompound(i);
                SavedEventRecord record = readEventRecord(eventTag);
                data.resolvedEvents.addLast(record);
            }
        }

        return data;
    }

    public static GodsparkSavedData createDefault() {
        return new GodsparkSavedData();
    }

    private static SavedEventRecord readEventRecord(CompoundTag tag) {
        return new SavedEventRecord(
            StoryEventType.valueOf(tag.getString("eventType")),
            tag.getInt("colonyId"),
            tag.getString("colonyName"),
            PressureType.valueOf(tag.getString("pressureType")),
            EventSeverity.valueOf(tag.getString("severity")),
            tag.getInt("pressureValue"),
            tag.getInt("threshold"),
            tag.getString("description"),
            tag.getLong("gameTick"),
            EventState.valueOf(tag.getString("state")),
            tag.getInt("persistenceCount"),
            tag.getInt("missingCycles"),
            tag.getLong("firstSeenTick"),
            tag.getLong("lastSeenTick"),
            tag.getLong("resolvedTick")
        );
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
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

    public void restoreToStateManager() {
        GodsparkMod.EVENT_STATE_MANAGER.clear();

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
            GodsparkMod.EVENT_STATE_MANAGER.restoreActive(record);
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
            GodsparkMod.EVENT_STATE_MANAGER.restoreResolved(record);
        }

        GodsparkMod.LOGGER.info(
            "[Godspark SavedData] Restored {} active, {} resolved events",
            activeEvents.size(),
            resolvedEvents.size()
        );
    }

    public void captureFromStateManager() {
        activeEvents.clear();
        resolvedEvents.clear();

        for (EventRecord record : GodsparkMod.EVENT_STATE_MANAGER.getAllActive()) {
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

        for (EventRecord record : GodsparkMod.EVENT_STATE_MANAGER.getAllResolved()) {
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

        setDirty();
    }
}
