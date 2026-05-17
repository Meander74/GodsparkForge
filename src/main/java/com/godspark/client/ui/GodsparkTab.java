package com.godspark.client.ui;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum GodsparkTab {
    STATUS("Status"),
    COLONIES("Colonies"),
    PRESSURES("Pressures"),
    EVENTS("Events"),
    MEMORIES("Memories"),
    PRAYERS("Prayers");

    private final String displayName;

    GodsparkTab(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}