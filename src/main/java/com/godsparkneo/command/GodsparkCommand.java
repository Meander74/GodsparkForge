package com.godsparkneo.command;

import com.godsparkneo.export.ColonyStateExporter;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.core.commands.arguments.ColonyIdArgument;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;

public final class GodsparkCommand {
    private GodsparkCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("godspark")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("export")
                        .then(Commands.argument("colonyId", ColonyIdArgument.id())
                                .executes(ctx -> executeExport(ctx.getSource(), ColonyIdArgument.getColony(ctx, "colonyId"))))
                )
        );
    }

    private static int executeExport(CommandSourceStack source, IColony colony) {
        MinecraftServer server = source.getServer();
        Path outputDir = server.getServerDirectory().resolve("godspark-exports");

        ColonyStateExporter.ExportResult result = ColonyStateExporter.export(colony, outputDir);

        if (result.success()) {
            source.sendSuccess(() -> Component.literal(result.message()), false);
        } else {
            source.sendFailure(Component.literal(result.message()));
        }

        return result.success() ? 1 : 0;
    }
}
