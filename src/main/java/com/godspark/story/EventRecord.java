package com.godspark.story;

public record EventRecord(
    StoryEvent event,
    EventState state,
    int persistenceCount,
    int missingCycles,
    long firstSeenTick,
    long lastSeenTick,
    long resolvedTick
) {
    public boolean isResolved() {
        return state == EventState.RESOLVED;
    }

    public boolean isPersistent() {
        return state == EventState.PERSISTENT;
    }

    public boolean isActive() {
        return state == EventState.ACTIVE;
    }
}
