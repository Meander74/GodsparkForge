package com.godspark.memory;

public enum MemoryType {
    SIGNIFICANT_EVENT("Significant Event"),
    TRAUMA("Trauma"),
    PATTERN("Pattern"),
    TRIUMPH("Triumph"),
    CULTURAL("Cultural");

    private final String displayName;

    MemoryType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
