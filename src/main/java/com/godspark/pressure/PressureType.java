package com.godspark.pressure;

public enum PressureType {
    FOOD("Food"),
    SECURITY("Security"),
    HOUSING("Housing"),
    COMFORT("Comfort"),
    INDUSTRY("Industry");

    private final String displayName;

    PressureType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
