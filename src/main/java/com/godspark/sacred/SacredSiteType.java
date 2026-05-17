package com.godspark.sacred;

public enum SacredSiteType {
    PRAYER_STONE("prayer_stone", 1),
    MINECOLONIES_SACRED("minecolonies_sacred", 3);

    private final String key;
    private final int sacredScore;

    SacredSiteType(String key, int sacredScore) {
        this.key = key;
        this.sacredScore = sacredScore;
    }

    public String getKey() {
        return key;
    }

    public int getSacredScore() {
        return sacredScore;
    }
}
