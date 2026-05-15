package com.godspark.story;

public enum EventSeverity {
    LOW(1),
    MEDIUM(2),
    HIGH(3);

    private final int rank;

    EventSeverity(int rank) {
        this.rank = rank;
    }

    public int rank() {
        return rank;
    }
}
