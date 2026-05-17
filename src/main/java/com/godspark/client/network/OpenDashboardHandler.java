package com.godspark.client.network;

import com.godspark.client.ui.GodsparkScreen;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class OpenDashboardHandler {

    private OpenDashboardHandler() {}

    public static void handle() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null) {
            mc.setScreen(new GodsparkScreen());
        }
    }
}