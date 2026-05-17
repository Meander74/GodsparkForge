package com.godspark.network.payload;

import net.minecraft.network.FriendlyByteBuf;

public record UiPressureEntry(
    int colonyId,
    String pressureType,
    int baseValue,
    int effectiveValue
) {
    public UiPressureEntry {
        pressureType = PayloadSanitizer.cap(pressureType, 32);
        if (baseValue < 0) baseValue = 0;
        if (baseValue > 100) baseValue = 100;
        if (effectiveValue < 0) effectiveValue = 0;
        if (effectiveValue > 100) effectiveValue = 100;
    }

    public static void encode(UiPressureEntry p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.colonyId());
        buf.writeUtf(p.pressureType(), 32);
        buf.writeVarInt(p.baseValue());
        buf.writeVarInt(p.effectiveValue());
    }

    public static UiPressureEntry decode(FriendlyByteBuf buf) {
        return new UiPressureEntry(buf.readVarInt(), buf.readUtf(32), buf.readVarInt(), buf.readVarInt());
    }
}