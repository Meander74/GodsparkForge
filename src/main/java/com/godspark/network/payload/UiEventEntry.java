package com.godspark.network.payload;

import net.minecraft.network.FriendlyByteBuf;

public record UiEventEntry(
    int colonyId,
    String storyEventType,
    String pressureType,
    String severity,
    String state,
    int persistenceCount,
    long createdAtTick
) {
    public UiEventEntry {
        storyEventType = PayloadSanitizer.cap(storyEventType, 64);
        pressureType = PayloadSanitizer.cap(pressureType, 32);
        severity = PayloadSanitizer.cap(severity, 16);
        state = PayloadSanitizer.cap(state, 16);
        if (persistenceCount < 0) persistenceCount = 0;
    }

    public static void encode(UiEventEntry e, FriendlyByteBuf buf) {
        buf.writeVarInt(e.colonyId());
        buf.writeUtf(e.storyEventType(), 64);
        buf.writeUtf(e.pressureType(), 32);
        buf.writeUtf(e.severity(), 16);
        buf.writeUtf(e.state(), 16);
        buf.writeVarInt(e.persistenceCount());
        buf.writeVarLong(e.createdAtTick());
    }

    public static UiEventEntry decode(FriendlyByteBuf buf) {
        return new UiEventEntry(
            buf.readVarInt(), buf.readUtf(64), buf.readUtf(32),
            buf.readUtf(16), buf.readUtf(16), buf.readVarInt(), buf.readVarLong()
        );
    }
}