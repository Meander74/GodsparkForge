package com.godspark.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record OpenDashboardPacket() {

    public static void encode(OpenDashboardPacket pkt, FriendlyByteBuf buf) {
    }

    public static OpenDashboardPacket decode(FriendlyByteBuf buf) {
        return new OpenDashboardPacket();
    }

    public static void handle(OpenDashboardPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> com.godspark.client.network.OpenDashboardHandler.handle()));
        context.setPacketHandled(true);
    }
}