package com.godspark.world;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.BlockPos;

import java.util.List;

public interface GuardTargetResolver {
    List<LivingEntity> resolveGuards(ServerLevel level, BlockPos center, int radius);
}
