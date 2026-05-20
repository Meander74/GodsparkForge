package com.godsparkneo.export;

import net.minecraft.core.BlockPos;

public record BlockPosData(int x, int y, int z) {
    public static BlockPosData from(BlockPos pos) {
        if (pos == null) return new BlockPosData(0, 0, 0);
        return new BlockPosData(pos.getX(), pos.getY(), pos.getZ());
    }
}
