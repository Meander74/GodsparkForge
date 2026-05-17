package com.godspark.network;

import com.godspark.GodsparkConstants;
import com.godspark.network.packet.OpenDashboardPacket;
import com.godspark.network.packet.RequestUiSnapshotPacket;
import com.godspark.network.packet.UiSnapshotPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class GodsparkNetwork {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
        .named(new ResourceLocation(GodsparkConstants.MOD_ID, "main"))
        .clientAcceptedVersions(PROTOCOL_VERSION::equals)
        .serverAcceptedVersions(PROTOCOL_VERSION::equals)
        .networkProtocolVersion(() -> PROTOCOL_VERSION)
        .simpleChannel();

    private GodsparkNetwork() {}

    public static void register() {
        int id = 0;

        CHANNEL.messageBuilder(RequestUiSnapshotPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
            .encoder(RequestUiSnapshotPacket::encode)
            .decoder(RequestUiSnapshotPacket::decode)
            .consumerMainThread(RequestUiSnapshotPacket::handle)
            .add();

        CHANNEL.messageBuilder(UiSnapshotPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(UiSnapshotPacket::encode)
            .decoder(UiSnapshotPacket::decode)
            .consumerMainThread(UiSnapshotPacket::handle)
            .add();

        CHANNEL.messageBuilder(OpenDashboardPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(OpenDashboardPacket::encode)
            .decoder(OpenDashboardPacket::decode)
            .consumerMainThread(OpenDashboardPacket::handle)
            .add();
    }
}