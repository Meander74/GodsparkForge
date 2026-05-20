package com.godsparkneo.export.dto;

import net.minecraft.core.BlockPos;

public record BlockPosExport(int x, int y, int z) {
    public static BlockPosExport from(BlockPos pos) {
        if (pos == null) return new BlockPosExport(0, 0, 0);
        return new BlockPosExport(pos.getX(), pos.getY(), pos.getZ());
    }
}
