package com.godspark.observer;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record ColonySnapshot(
    int colonyId,
    String name,
    BlockPos center,
    int citizenCount,
    int buildingCount,
    int warehouseCount,
    double happiness,
    int guardCount,
    int foodBuildingCount,
    int housingCapacity,
    int industryBuildingCount,
    int sacredBuildingCount,
    int gatheringBuildingCount,
    boolean hasActiveRaid,
    ResourceKey<Level> dimension,
    long gameTick
) {}
