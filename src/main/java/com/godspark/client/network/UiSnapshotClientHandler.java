package com.godspark.client.network;

import com.godspark.client.GodsparkClientState;
import com.godspark.network.packet.UiSnapshotPacket;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class UiSnapshotClientHandler {

    private UiSnapshotClientHandler() {}

    public static void handle(UiSnapshotPacket packet) {
        if (packet == null || packet.snapshot() == null) {
            return;
        }
        GodsparkClientState.applySnapshot(packet.snapshot());
    }
}