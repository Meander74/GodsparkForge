package com.godspark.registry;

import com.godspark.GodsparkConstants;
import com.godspark.block.PrayerStoneBlock;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class GodsparkBlocks {

    private GodsparkBlocks() {}

    public static final DeferredRegister<Block> BLOCKS =
        DeferredRegister.create(ForgeRegistries.BLOCKS, GodsparkConstants.MOD_ID);

    public static final RegistryObject<Block> PRAYER_STONE =
        BLOCKS.register("prayer_stone", PrayerStoneBlock::new);
}