package com.godspark.story;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class EventQueue {

    private static final int DEFAULT_MAX_EVENTS = 100;
    private static final long DEFAULT_COOLDOWN_TICKS = 6000L;

    private final int maxEvents;
    private final long cooldownTicks;
    private final Deque<StoryEvent> events = new ArrayDeque<>();
    private final Map<EventKey, Long> lastAcceptedTicks = new HashMap<>();

    public EventQueue() {
        this(DEFAULT_MAX_EVENTS, DEFAULT_COOLDOWN_TICKS);
    }

    public EventQueue(int maxEvents, long cooldownTicks) {
        this.maxEvents = maxEvents;
        this.cooldownTicks = cooldownTicks;
    }

    public boolean offer(StoryEvent event) {
        EventKey key = new EventKey(event.colonyId(), event.eventType());
        long lastTick = lastAcceptedTicks.getOrDefault(key, Long.MIN_VALUE);

        if (event.gameTick() - lastTick < cooldownTicks) {
            return false;
        }

        lastAcceptedTicks.put(key, event.gameTick());
        events.addLast(event);

        while (events.size() > maxEvents) {
            events.removeFirst();
        }

        return true;
    }

    public List<StoryEvent> recentEvents(int limit) {
        int safeLimit = Math.max(0, Math.min(limit, events.size()));
        List<StoryEvent> result = new ArrayList<>(safeLimit);

        int skip = events.size() - safeLimit;
        int index = 0;

        for (StoryEvent event : events) {
            if (index++ >= skip) {
                result.add(event);
            }
        }

        return result;
    }

    public int size() {
        return events.size();
    }

    private record EventKey(int colonyId, StoryEventType eventType) {}
}
