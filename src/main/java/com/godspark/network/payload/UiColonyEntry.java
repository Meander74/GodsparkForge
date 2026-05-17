package com.godspark.network.payload;

import net.minecraft.network.FriendlyByteBuf;

public record UiColonyEntry(
    int colonyId,
    String name,
    int citizenCount,
    int housingCapacity,
    int guardCount,
    double happiness,
    int foodBuildingCount,
    int industryBuildingCount,
    int sacredBuildingCount,
    int warehouseCount,
    boolean hasActiveRaid
) {
    public UiColonyEntry {
        name = PayloadSanitizer.cap(name, 64);
        if (citizenCount < 0) citizenCount = 0;
        if (housingCapacity < 0) housingCapacity = 0;
        if (guardCount < 0) guardCount = 0;
        if (happiness < 0) happiness = 0;
        if (foodBuildingCount < 0) foodBuildingCount = 0;
        if (industryBuildingCount < 0) industryBuildingCount = 0;
        if (sacredBuildingCount < 0) sacredBuildingCount = 0;
        if (warehouseCount < 0) warehouseCount = 0;
    }

    public static void encode(UiColonyEntry c, FriendlyByteBuf buf) {
        buf.writeVarInt(c.colonyId());
        buf.writeUtf(c.name(), 64);
        buf.writeVarInt(c.citizenCount());
        buf.writeVarInt(c.housingCapacity());
        buf.writeVarInt(c.guardCount());
        buf.writeDouble(c.happiness());
        buf.writeVarInt(c.foodBuildingCount());
        buf.writeVarInt(c.industryBuildingCount());
        buf.writeVarInt(c.sacredBuildingCount());
        buf.writeVarInt(c.warehouseCount());
        buf.writeBoolean(c.hasActiveRaid());
    }

    public static UiColonyEntry decode(FriendlyByteBuf buf) {
        return new UiColonyEntry(
            buf.readVarInt(), buf.readUtf(64),
            buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
            buf.readDouble(),
            buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
            buf.readVarInt(), buf.readBoolean()
        );
    }
}