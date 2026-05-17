package com.godspark.client;

import com.godspark.GodsparkConstants;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GodsparkConstants.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GodsparkClientModEvents {

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        GodsparkKeyMappings.register(event);
    }
}