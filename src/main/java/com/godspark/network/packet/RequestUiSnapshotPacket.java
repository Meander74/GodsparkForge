package com.godspark.network.packet;

import com.godspark.GodsparkMod;
import com.godspark.network.GodsparkNetwork;
import com.godspark.network.payload.UiSnapshot;
import com.godspark.server.network.UiSnapshotBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public record RequestUiSnapshotPacket(int requestId) {

    private static final long MIN_REQUEST_INTERVAL_MS = 1000L;
    private static final Map<UUID, Long> LAST_REQUEST_MS = new ConcurrentHashMap<>();

    public static void encode(RequestUiSnapshotPacket pkt, FriendlyByteBuf buf) {
        buf.writeVarInt(pkt.requestId());
    }

    public static RequestUiSnapshotPacket decode(FriendlyByteBuf buf) {
        return new RequestUiSnapshotPacket(buf.readVarInt());
    }

    public static void handle(RequestUiSnapshotPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            if (!player.hasPermissions(2)) {
                GodsparkMod.LOGGER.debug("[Godspark UI] Denied UI snapshot request from non-op player {}",
                    player.getGameProfile().getName());
                return;
            }

            long now = System.currentTimeMillis();
            Long last = LAST_REQUEST_MS.get(player.getUUID());
            if (last != null && now - last < MIN_REQUEST_INTERVAL_MS) {
                return;
            }
            LAST_REQUEST_MS.put(player.getUUID(), now);

            try {
                UiSnapshot snapshot = UiSnapshotBuilder.build(player);
                GodsparkNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new UiSnapshotPacket(pkt.requestId(), snapshot)
                );
            } catch (Exception e) {
                GodsparkMod.LOGGER.warn("[Godspark UI] Failed to build UI snapshot for {}",
                    player.getName().getString(), e);
            }
        });
        context.setPacketHandled(true);
    }
}