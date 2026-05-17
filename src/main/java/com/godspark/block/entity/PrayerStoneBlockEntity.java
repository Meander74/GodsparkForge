package com.godspark.block.entity;

import com.godspark.GodsparkMod;
import com.godspark.registry.GodsparkBlockEntities;
import com.godspark.sacred.SacredSiteManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class PrayerStoneBlockEntity extends BlockEntity {

    private boolean registered = false;

    public PrayerStoneBlockEntity(BlockPos pos, BlockState state) {
        super(GodsparkBlockEntities.PRAYER_STONE.get(), pos, state);
    }

    @Override
    public void setLevel(net.minecraft.world.level.Level level) {
        super.setLevel(level);
        if (level instanceof ServerLevel && !registered) {
            registerWithManager();
        }
    }

    private void registerWithManager() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        boolean bound = SacredSiteManager.getInstance().registerPrayerStone(
            serverLevel,
            getBlockPos(),
            GodsparkMod.COLONY_OBSERVER.getObservedColonies(),
            serverLevel.getServer().getTickCount()
        );
        registered = bound;
        if (bound) {
            GodsparkMod.LOGGER.info("[GS/Sacred] Prayer Stone bound at {} in {}",
                getBlockPos().toShortString(), level.dimension().location());
        }
    }
}
