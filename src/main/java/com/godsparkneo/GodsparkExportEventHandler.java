package com.godsparkneo;

import com.godsparkneo.command.GodsparkCommand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class GodsparkExportEventHandler {
    @SubscribeEvent
    public static void onCommandsRegister(final RegisterCommandsEvent event) {
        GodsparkCommand.register(event.getDispatcher());
    }
}
