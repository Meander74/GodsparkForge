package com.godspark.prayer;

public enum PrayerType {
    PLEA("Plea"),
    VIGIL("Vigil"),
    THANKS("Thanks"),
    LAMENT("Lament"),
    HOPE("Hope");

    private final String displayName;

    PrayerType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}