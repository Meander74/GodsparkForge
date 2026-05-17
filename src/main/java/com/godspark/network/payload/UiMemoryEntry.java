package com.godspark.network.payload;

import net.minecraft.network.FriendlyByteBuf;

public record UiMemoryEntry(
    int colonyId,
    String memoryType,
    String pressureType,
    int intensity,
    long createdAtTick
) {
    public UiMemoryEntry {
        memoryType = PayloadSanitizer.cap(memoryType, 32);
        pressureType = PayloadSanitizer.cap(pressureType, 32);
        if (intensity < 0) intensity = 0;
        if (intensity > 100) intensity = 100;
    }

    public static void encode(UiMemoryEntry m, FriendlyByteBuf buf) {
        buf.writeVarInt(m.colonyId());
        buf.writeUtf(m.memoryType(), 32);
        buf.writeUtf(m.pressureType(), 32);
        buf.writeVarInt(m.intensity());
        buf.writeVarLong(m.createdAtTick());
    }

    public static UiMemoryEntry decode(FriendlyByteBuf buf) {
        return new UiMemoryEntry(
            buf.readVarInt(), buf.readUtf(32), buf.readUtf(32),
            buf.readVarInt(), buf.readVarLong()
        );
    }
}