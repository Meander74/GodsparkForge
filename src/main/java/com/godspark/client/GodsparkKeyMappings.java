package com.godspark.client;

import com.godspark.GodsparkConstants;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;

@OnlyIn(Dist.CLIENT)
public final class GodsparkKeyMappings {

    public static final String CATEGORY = "key.categories." + GodsparkConstants.MOD_ID;

    public static final KeyMapping OPEN_DASHBOARD = new KeyMapping(
        "key." + GodsparkConstants.MOD_ID + ".dashboard",
        InputConstants.Type.KEYSYM,
        InputConstants.UNKNOWN.getValue(),
        CATEGORY
    );

    private GodsparkKeyMappings() {}

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN_DASHBOARD);
    }
}