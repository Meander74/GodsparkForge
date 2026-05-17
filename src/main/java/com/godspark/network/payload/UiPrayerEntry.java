package com.godspark.network.payload;

import net.minecraft.network.FriendlyByteBuf;

public record UiPrayerEntry(
    int colonyId,
    String prayerType,
    String channel,
    String pressureType,
    int intensity,
    String content
) {
    public static void encode(UiPrayerEntry p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.colonyId());
        buf.writeUtf(p.prayerType(), 32);
        buf.writeUtf(p.channel(), 32);
        buf.writeUtf(p.pressureType(), 32);
        buf.writeVarInt(p.intensity());
        buf.writeUtf(p.content() == null ? "" : p.content(), 512);
    }

    public static UiPrayerEntry decode(FriendlyByteBuf buf) {
        return new UiPrayerEntry(
            buf.readVarInt(), buf.readUtf(32), buf.readUtf(32),
            buf.readUtf(32), buf.readVarInt(), buf.readUtf(512)
        );
    }
}