package com.godspark.client;

import com.godspark.GodsparkConstants;
import com.godspark.client.ui.GodsparkScreen;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GodsparkConstants.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GodsparkClientForgeEvents {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        while (GodsparkKeyMappings.OPEN_DASHBOARD.consumeClick()) {
            if (mc.screen == null) {
                mc.setScreen(new GodsparkScreen());
            }
        }
    }
}