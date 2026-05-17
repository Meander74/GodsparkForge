package com.godspark.network.packet;

import com.godspark.network.payload.UiSnapshot;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record UiSnapshotPacket(int requestId, UiSnapshot snapshot) {

    public static void encode(UiSnapshotPacket pkt, FriendlyByteBuf buf) {
        buf.writeVarInt(pkt.requestId());
        UiSnapshot.encode(pkt.snapshot(), buf);
    }

    public static UiSnapshotPacket decode(FriendlyByteBuf buf) {
        int requestId = buf.readVarInt();
        UiSnapshot snapshot = UiSnapshot.decode(buf);
        return new UiSnapshotPacket(requestId, snapshot);
    }

    public static void handle(UiSnapshotPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> com.godspark.client.network.UiSnapshotClientHandler.handle(pkt)));
        context.setPacketHandled(true);
    }
}