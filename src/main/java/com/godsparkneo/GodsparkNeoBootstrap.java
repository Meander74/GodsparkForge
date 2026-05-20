package com.godsparkneo;

import net.neoforged.neoforge.common.NeoForge;

import java.util.concurrent.atomic.AtomicBoolean;

public final class GodsparkNeoBootstrap {
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    private GodsparkNeoBootstrap() {}

    public static void init() {
        if (!INITIALIZED.compareAndSet(false, true)) {
            return;
        }
        NeoForge.EVENT_BUS.register(GodsparkExportEventHandler.class);
    }
}
