package com.godspark.client;

import com.godspark.network.payload.UiSnapshot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class GodsparkClientState {

    private static volatile UiSnapshot lastSnapshot = null;
    private static volatile long lastSnapshotMillis = 0L;
    private static volatile int lastRequestId = 0;
    private static volatile int selectedColonyId = -1;

    private GodsparkClientState() {}

    public static void applySnapshot(UiSnapshot snapshot) {
        lastSnapshot = snapshot;
        lastSnapshotMillis = System.currentTimeMillis();
    }

    public static UiSnapshot getSnapshot() {
        return lastSnapshot;
    }

    public static long getLastSnapshotMillis() {
        return lastSnapshotMillis;
    }

    public static int nextRequestId() {
        return ++lastRequestId;
    }

    public static int getSelectedColonyId() {
        return selectedColonyId;
    }

    public static void setSelectedColonyId(int colonyId) {
        selectedColonyId = colonyId;
    }
}