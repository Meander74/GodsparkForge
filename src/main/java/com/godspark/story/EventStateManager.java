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
    private static final int RESOLVE_AFTER_MISSING_CYCLES = 2;
    private static final int MAX_RESOLVED_RECORDS = 100;

    private final Map<EventKey, EventRecord> activeEvents = new HashMap<>();
    private final Deque<EventRecord> resolvedEvents = new ArrayDeque<>();

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
                transitions.add(newRecord);
            } else {
                int newPersistence = existing.persistenceCount() + 1;
                EventState newState = newPersistence >= PERSISTENT_THRESHOLD
                    ? EventState.PERSISTENT
                    : EventState.ACTIVE;

                if (newState != existing.state()) {
                    transitions.add(existing);
                }

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
                    while (resolvedEvents.size() > MAX_RESOLVED_RECORDS) {
                        resolvedEvents.removeFirst();
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

    private record EventKey(int colonyId, PressureType pressureType) {}
}
