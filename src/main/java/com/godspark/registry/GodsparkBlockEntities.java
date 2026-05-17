package com.godspark.registry;

import com.godspark.GodsparkConstants;
import com.godspark.block.entity.PrayerStoneBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class GodsparkBlockEntities {

    private GodsparkBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, GodsparkConstants.MOD_ID);

    public static final RegistryObject<BlockEntityType<PrayerStoneBlockEntity>> PRAYER_STONE =
        BLOCK_ENTITIES.register("prayer_stone", () ->
            BlockEntityType.Builder.of(PrayerStoneBlockEntity::new, GodsparkBlocks.PRAYER_STONE.get())
                .build(null)
        );
}