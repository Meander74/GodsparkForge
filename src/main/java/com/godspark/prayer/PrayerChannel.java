package com.godspark.prayer;

import java.util.Locale;

public enum PrayerChannel {
    PRIVATE("Private", false, false),
    COMMONS("Commons", true, false),
    CHURCH("Church", true, true),
    SHRINE("Shrine", true, true),
    TEMPLE("Temple", true, true);

    private final String displayName;
    private final boolean publicChannel;
    private final boolean sacred;

    PrayerChannel(String displayName, boolean publicChannel, boolean sacred) {
        this.displayName = displayName;
        this.publicChannel = publicChannel;
        this.sacred = sacred;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isPublic() {
        return publicChannel;
    }

    public boolean isSacred() {
        return sacred;
    }

    public boolean isMiracleEligible() {
        return publicChannel && sacred;
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}
