package com.godspark.story;

import com.godspark.pressure.PressureType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class EventStateManager {

    private static final int PERSISTENT_THRESHOLD = 3;
    private static final int RESOLVE_AFTER_MISSING_CYCLES = 3;
    private static final int MAX_RESOLVED_RECORDS = 500;

    private final Map<EventKey, EventRecord> activeEvents = new HashMap<>();
    private final Deque<EventRecord> resolvedEvents = new ArrayDeque<>();
    private boolean dirty = false;

    private void markDirty() {
        dirty = true;
    }

    public boolean hasDirty() {
        return dirty;
    }

    public boolean consumeDirty() {
        boolean wasDirty = dirty;
        dirty = false;
        return wasDirty;
    }

    public List<EventRecord> processEvents(List<StoryEvent> currentEvents, long gameTick) {
        List<EventRecord> transitions = new ArrayList<>();
        Map<EventKey, StoryEvent> currentByKey = new HashMap<>();

        for (StoryEvent event : currentEvents) {
            EventKey key = new EventKey(event.colonyId(), event.pressureType());
            currentByKey.put(key, event);
        }

        for (Map.Entry<EventKey, StoryEvent> entry : currentByKey.entrySet()) {
            EventKey key = entry.getKey();
            StoryEvent currentEvent = entry.getValue();
            EventRecord existing = activeEvents.get(key);

            if (existing == null) {
                EventRecord newRecord = new EventRecord(
                    currentEvent,
                    EventState.ACTIVE,
                    1,
                    0,
                    gameTick,
                    gameTick,
                    -1
                );
                activeEvents.put(key, newRecord);
                markDirty();
                transitions.add(newRecord);
            } else {
                int newPersistence = existing.persistenceCount() + 1;
                EventState newState = newPersistence >= PERSISTENT_THRESHOLD
                    ? EventState.PERSISTENT
                    : EventState.ACTIVE;

                EventRecord updated = new EventRecord(
                    currentEvent,
                    newState,
                    newPersistence,
                    0,
                    existing.firstSeenTick(),
                    gameTick,
                    -1
                );
                activeEvents.put(key, updated);
                markDirty();

                if (newState != existing.state()) {
                    transitions.add(updated);
                }
            }
        }

        List<EventKey> toRemove = new ArrayList<>();
        for (Map.Entry<EventKey, EventRecord> entry : activeEvents.entrySet()) {
            EventKey key = entry.getKey();
            if (!currentByKey.containsKey(key)) {
                EventRecord existing = entry.getValue();
                int newMissing = existing.missingCycles() + 1;

                if (newMissing >= RESOLVE_AFTER_MISSING_CYCLES) {
                    EventRecord resolved = new EventRecord(
                        existing.event(),
                        EventState.RESOLVED,
                        existing.persistenceCount(),
                        newMissing,
                        existing.firstSeenTick(),
                        existing.lastSeenTick(),
                        gameTick
                    );
                    toRemove.add(key);
                    resolvedEvents.addLast(resolved);
                    markDirty();
                    while (resolvedEvents.size() > MAX_RESOLVED_RECORDS) {
                        resolvedEvents.removeFirst();
                        markDirty();
                    }
                    transitions.add(resolved);
                } else {
                    EventRecord updated = new EventRecord(
                        existing.event(),
                        existing.state(),
                        existing.persistenceCount(),
                        newMissing,
                        existing.firstSeenTick(),
                        existing.lastSeenTick(),
                        existing.resolvedTick()
                    );
                    activeEvents.put(key, updated);
                    markDirty();
                }
            }
        }

        for (EventKey key : toRemove) {
            activeEvents.remove(key);
        }

        return transitions;
    }

    public List<EventRecord> getActiveEvents() {
        List<EventRecord> result = new ArrayList<>(activeEvents.values());
        result.sort((a, b) -> {
            int stateCompare = b.state().rank() - a.state().rank();
            if (stateCompare != 0) return stateCompare;
            return Long.compare(b.lastSeenTick(), a.lastSeenTick());
        });
        return result;
    }

    public List<EventRecord> getResolvedEvents(int limit) {
        int safeLimit = Math.max(0, Math.min(limit, resolvedEvents.size()));
        List<EventRecord> result = new ArrayList<>(safeLimit);
        int skip = resolvedEvents.size() - safeLimit;
        int index = 0;
        for (EventRecord record : resolvedEvents) {
            if (index++ >= skip) {
                result.add(record);
            }
        }
        return result;
    }

    public int activeCount() {
        return activeEvents.size();
    }

    public int resolvedCount() {
        return resolvedEvents.size();
    }

    public List<EventRecord> getAllActive() {
        return new ArrayList<>(activeEvents.values());
    }

    public List<EventRecord> getAllResolved() {
        return new ArrayList<>(resolvedEvents);
    }

    public void clear() {
        activeEvents.clear();
        resolvedEvents.clear();
        dirty = false;
    }

    public void restoreActive(EventRecord record) {
        EventKey key = new EventKey(record.event().colonyId(), record.event().pressureType());
        activeEvents.put(key, record);
    }

    public void restoreResolved(EventRecord record) {
        resolvedEvents.addLast(record);
        while (resolvedEvents.size() > MAX_RESOLVED_RECORDS) {
            resolvedEvents.removeFirst();
        }
    }

    private record EventKey(int colonyId, PressureType pressureType) {}
}
