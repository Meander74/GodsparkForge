package com.godspark.sacred;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public record SacredSiteRecord(
    SacredSiteType type,
    BlockPos pos,
    ResourceLocation dimension,
    int colonyId,
    long registeredAtTick
) {}
