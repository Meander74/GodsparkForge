package com.godspark.world;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.BlockPos;

import java.util.List;

public final class NoopGuardResolver implements GuardTargetResolver {
    @Override
    public List<LivingEntity> resolveGuards(ServerLevel level, BlockPos center, int radius) {
        return List.of();
    }
}
