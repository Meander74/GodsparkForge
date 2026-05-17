package com.godspark.network.payload;

import net.minecraft.network.FriendlyByteBuf;

public record UiStatusInfo(
    boolean modActive,
    boolean mineColoniesDetected,
    boolean aiEnabled,
    int colonyCount,
    int activeEventCount,
    int totalMemories,
    int totalPrayers
) {
    public UiStatusInfo {
        if (colonyCount < 0) colonyCount = 0;
        if (activeEventCount < 0) activeEventCount = 0;
        if (totalMemories < 0) totalMemories = 0;
        if (totalPrayers < 0) totalPrayers = 0;
    }

    public static void encode(UiStatusInfo s, FriendlyByteBuf buf) {
        buf.writeBoolean(s.modActive());
        buf.writeBoolean(s.mineColoniesDetected());
        buf.writeBoolean(s.aiEnabled());
        buf.writeVarInt(s.colonyCount());
        buf.writeVarInt(s.activeEventCount());
        buf.writeVarInt(s.totalMemories());
        buf.writeVarInt(s.totalPrayers());
    }

    public static UiStatusInfo decode(FriendlyByteBuf buf) {
        return new UiStatusInfo(
            buf.readBoolean(), buf.readBoolean(), buf.readBoolean(),
            buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt()
        );
    }
}