package com.godsparkneo.export;

import java.util.List;

public class ThreatData {
    private final boolean isRaided;
    private final boolean willRaidTonight;
    private final int nightsSinceLastRaid;
    private final int colonyRaidLevel;
    private final List<Integer> activeEventIds;

    public ThreatData(boolean isRaided, boolean willRaidTonight,
                      int nightsSinceLastRaid, int colonyRaidLevel,
                      List<Integer> activeEventIds) {
        this.isRaided = isRaided;
        this.willRaidTonight = willRaidTonight;
        this.nightsSinceLastRaid = nightsSinceLastRaid;
        this.colonyRaidLevel = colonyRaidLevel;
        this.activeEventIds = activeEventIds;
    }

    public boolean isRaided() { return isRaided; }
    public boolean isWillRaidTonight() { return willRaidTonight; }
    public int getNightsSinceLastRaid() { return nightsSinceLastRaid; }
    public int getColonyRaidLevel() { return colonyRaidLevel; }
    public List<Integer> getActiveEventIds() { return activeEventIds; }
}
