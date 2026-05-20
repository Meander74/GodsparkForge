package com.godspark.sacred;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record SacredSiteRecord(
    SacredSiteType type,
    BlockPos pos,
    ResourceLocation dimension,
    int colonyId,
    long registeredAtTick,
    long lastSeenTick
) {
    public SacredSiteRecord {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(dimension, "dimension");
        if (registeredAtTick < 0) registeredAtTick = 0;
        if (lastSeenTick <= 0) lastSeenTick = registeredAtTick;
        pos = pos.immutable();
    }

    public SacredSiteRecord(SacredSiteType type, BlockPos pos, ResourceLocation dimension, int colonyId, long registeredAtTick) {
        this(type, pos, dimension, colonyId, registeredAtTick, registeredAtTick);
    }
}