package com.godsparkneo;

import net.neoforged.neoforge.common.NeoForge;

public final class GodsparkNeoBootstrap {
    private GodsparkNeoBootstrap() {}

    public static void init() {
        NeoForge.EVENT_BUS.register(GodsparkExportEventHandler.class);
    }
}
