package com.godspark.story;

public enum EventState {
    ACTIVE(1),
    PERSISTENT(2),
    RESOLVED(3);

    private final int rank;

    EventState(int rank) {
        this.rank = rank;
    }

    public int rank() {
        return rank;
    }
}
