package com.godspark.registry;

import com.godspark.GodsparkConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class GodsparkCreativeTab {

    public static final DeferredRegister<CreativeModeTab> TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, GodsparkConstants.MOD_ID);

    public static final RegistryObject<CreativeModeTab> GODSPARK_TAB = TABS.register(
        "godspark_tab",
        () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.godspark_tab"))
            .icon(() -> new ItemStack(GodsparkItems.PRAYER_STONE.get()))
            .displayItems((params, output) -> {
                output.accept(GodsparkItems.PRAYER_STONE.get());
            })
            .build()
    );

    private GodsparkCreativeTab() {
    }
}