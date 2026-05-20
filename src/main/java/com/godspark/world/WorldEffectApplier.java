package com.godspark.world;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;

public interface WorldEffectApplier {
    WorldEffectApplyResult applyGuardiansVigil(ServerLevel level, BlockPos center, int durationTicks, int radius);
    WorldEffectApplyResult applyGreensMercy(ServerLevel level, BlockPos center, int maxAttempts, int successCap, int radius);
}
