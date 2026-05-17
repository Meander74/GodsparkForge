package com.godspark.registry;

import com.godspark.GodsparkConstants;
import com.godspark.block.PrayerStoneBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class GodsparkItems {

    private GodsparkItems() {}

    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, GodsparkConstants.MOD_ID);

    public static final RegistryObject<Item> PRAYER_STONE =
        ITEMS.register("prayer_stone", () ->
            new BlockItem(GodsparkBlocks.PRAYER_STONE.get(), new Item.Properties())
        );
}