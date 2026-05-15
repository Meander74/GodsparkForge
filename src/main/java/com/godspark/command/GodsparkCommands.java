package com.godspark.command;

import com.godspark.GodsparkConstants;
import com.godspark.GodsparkMod;
import com.godspark.memory.ColonyMemory;
import com.godspark.memory.MemoryInfluence;
import com.godspark.memory.MemoryType;
import com.godspark.observer.ObservedColony;
import com.godspark.pressure.PressureSnapshot;
import com.godspark.pressure.PressureType;
import com.godspark.story.StoryEvent;
import com.godspark.story.EventRecord;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;

import java.util.List;
import java.util.Map;

public final class GodsparkCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("godspark");

        root.then(Commands.literal("status").executes(ctx -> {
            CommandSourceStack source = ctx.getSource();
            boolean minecoloniesLoaded = ModList.get().isLoaded("minecolonies");
            boolean createLoaded = ModList.get().isLoaded("create");
            int colonyCount = GodsparkMod.COLONY_OBSERVER.getObservedColonies().size();
            boolean pressureActive = !GodsparkMod.PRESSURE_ENGINE.getSnapshots().isEmpty();

            source.sendSuccess(() -> Component.literal(
                "Godspark Status:\n" +
                "  Loaded: true\n" +
                "  Version: " + GodsparkConstants.VERSION + "\n" +
                "  Minecraft: 1.20.1\n" +
                "  Loader: Forge\n" +
                "  MineColonies detected: " + minecoloniesLoaded + "\n" +
                "  Create detected: " + createLoaded + "\n" +
                "  Observed colonies: " + colonyCount + "\n" +
                "  Pressure engine: " + (pressureActive ? "active" : "idle") + "\n" +
                "  AI bridge: disabled"
            ), false);
            return 1;
        }));

        root.then(Commands.literal("colonies").executes(ctx -> {
            CommandSourceStack source = ctx.getSource();
            boolean minecoloniesLoaded = ModList.get().isLoaded("minecolonies");

            if (!minecoloniesLoaded) {
                source.sendSuccess(() -> Component.literal("MineColonies is not loaded."), false);
                return 0;
            }

            Map<Integer, ObservedColony> colonies = GodsparkMod.COLONY_OBSERVER.getObservedColonies();
            if (colonies.isEmpty()) {
                source.sendSuccess(() -> Component.literal("Observed colonies: 0\nNo colonies detected yet. Create a colony in MineColonies first."), false);
                return 1;
            }

            StringBuilder sb = new StringBuilder("Observed colonies:\n");
            for (ObservedColony colony : colonies.values()) {
                var snapshot = colony.getLatest();
                if (snapshot == null) continue;
                sb.append(String.format(
                    "  - ID: %d | Name: %s | Citizens: %d | Buildings: %d\n",
                    snapshot.colonyId(),
                    snapshot.name(),
                    snapshot.citizenCount(),
                    snapshot.buildingCount()
                ));
            }
            source.sendSuccess(() -> Component.literal(sb.toString()), false);
            return 1;
        }));

        root.then(Commands.literal("pressures").executes(ctx -> {
            CommandSourceStack source = ctx.getSource();
            Map<Integer, PressureSnapshot> snapshots = GodsparkMod.PRESSURE_ENGINE.getSnapshots();

            if (snapshots.isEmpty()) {
                source.sendSuccess(() -> Component.literal(
                    "Pressures (placeholder):\n" +
                    "  food_pressure: 0\n" +
                    "  security_pressure: 0\n" +
                    "  housing_pressure: 0\n" +
                    "  comfort_pressure: 0\n" +
                    "  industry_pressure: 0"
                ), false);
                return 1;
            }

            StringBuilder sb = new StringBuilder("Pressures:\n");
            for (PressureSnapshot snapshot : snapshots.values()) {
                sb.append(String.format("  Colony %d:\n", snapshot.colonyId()));
                for (PressureType type : PressureType.values()) {
                    int value = snapshot.values().getOrDefault(type, 0);
                    sb.append(String.format("    %s: %d\n", type.getDisplayName(), value));
                }
            }
            source.sendSuccess(() -> Component.literal(sb.toString()), false);
            return 1;
        }));

        root.then(Commands.literal("events")
            .executes(ctx -> showEvents(ctx.getSource(), 10))
            .then(Commands.argument("count", IntegerArgumentType.integer(1, 50))
                .executes(ctx -> showEvents(
                    ctx.getSource(),
                    IntegerArgumentType.getInteger(ctx, "count")
                ))
            )
        );

        root.then(Commands.literal("memories")
            .executes(ctx -> showMemories(ctx.getSource(), -1))
            .then(Commands.argument("colonyId", IntegerArgumentType.integer(1))
                .executes(ctx -> showMemories(
                    ctx.getSource(),
                    IntegerArgumentType.getInteger(ctx, "colonyId")
                ))
            )
        );

        root.then(Commands.literal("influences")
            .executes(ctx -> showInfluences(ctx.getSource()))
        );

        dispatcher.register(root);
    }

    private static int showEvents(CommandSourceStack source, int count) {
        List<EventRecord> active = GodsparkMod.EVENT_STATE_MANAGER.getActiveEvents();
        List<EventRecord> resolved = GodsparkMod.EVENT_STATE_MANAGER.getResolvedEvents(count);

        if (active.isEmpty() && resolved.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No Godspark story events recorded yet."), false);
            return 1;
        }

        StringBuilder sb = new StringBuilder();

        if (!active.isEmpty()) {
            sb.append("Active Godspark events:\n");
            for (EventRecord record : active) {
                String stateTag = record.isPersistent()
                    ? String.format("PERSISTENT x%d", record.persistenceCount())
                    : String.format("ACTIVE x%d", record.persistenceCount());
                sb.append(String.format(
                    "  [%s][%s] %s: %s (pressure=%d)\n",
                    stateTag,
                    record.event().pressureType().getDisplayName(),
                    record.event().colonyName(),
                    record.event().description(),
                    record.event().pressureValue()
                ));
            }
        }

        if (!resolved.isEmpty()) {
            sb.append("\nRecently resolved:\n");
            for (EventRecord record : resolved) {
                long durationTicks = record.resolvedTick() - record.firstSeenTick();
                long durationMinutes = durationTicks / 1200;
                sb.append(String.format(
                    "  [RESOLVED][%s] %s: %s (lasted %d cycles)\n",
                    record.event().pressureType().getDisplayName(),
                    record.event().colonyName(),
                    record.event().description(),
                    record.persistenceCount()
                ));
            }
        }

        source.sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    private static int showMemories(CommandSourceStack source, int colonyId) {
        if (GodsparkMod.MEMORY_BANK.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No Godspark memories recorded yet."), false);
            return 1;
        }

        StringBuilder sb = new StringBuilder();

        if (colonyId > 0) {
            List<ColonyMemory> colonyMemories = GodsparkMod.MEMORY_BANK.getStrongestMemories(colonyId, 10);
            if (colonyMemories.isEmpty()) {
                source.sendSuccess(() -> Component.literal(
                    String.format("No memories recorded for colony #%d.", colonyId)
                ), false);
                return 1;
            }

            String colonyName = colonyMemories.get(0).colonyName();
            sb.append(String.format("Memories for %s (#%d):\n", colonyName, colonyId));
            for (ColonyMemory memory : colonyMemories) {
                sb.append(formatMemoryLine(memory));
            }
        } else {
            sb.append("Godspark memories:\n");
            List<ColonyMemory> allMemories = GodsparkMod.MEMORY_BANK.getAllMemories();
            int count = 0;
            for (ColonyMemory memory : allMemories) {
                if (count >= 20) break;
                sb.append(formatMemoryLine(memory));
                count++;
            }
        }

        source.sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    private static String formatMemoryLine(ColonyMemory memory) {
        return String.format(
            "  [%s %d][%s] %s (reinforced=%d)\n",
            memory.memoryType().getDisplayName(),
            memory.intensity(),
            memory.pressureType().getDisplayName(),
            memory.content(),
            memory.reinforcementCount()
        );
    }

    private static int showInfluences(CommandSourceStack source) {
        Map<Integer, ObservedColony> colonies = GodsparkMod.COLONY_OBSERVER.getObservedColonies();
        if (colonies.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No colonies observed yet."), false);
            return 1;
        }

        MemoryInfluence influence = new MemoryInfluence();
        StringBuilder sb = new StringBuilder("Memory threshold adjustments:\n");

        for (ObservedColony colony : colonies.values()) {
            var latest = colony.getLatest();
            if (latest == null) continue;

            Map<PressureType, Integer> adjustments = influence.computeAdjustments(
                colony.getColonyId(), GodsparkMod.MEMORY_BANK
            );

            boolean hasAny = adjustments.values().stream().anyMatch(v -> v != 0);
            if (!hasAny) {
                sb.append(String.format("  Colony %d (%s): no adjustments\n",
                    colony.getColonyId(), latest.name()));
                continue;
            }

            sb.append(String.format("  Colony %d (%s):\n", colony.getColonyId(), latest.name()));
            for (PressureType pt : PressureType.values()) {
                int adj = adjustments.getOrDefault(pt, 0);
                if (adj != 0) {
                    String direction = adj < 0 ? "lower" : "raise";
                    sb.append(String.format("    %s: %+d (%s threshold by %d)\n",
                        pt.getDisplayName(), adj, direction, Math.abs(adj)));
                }
            }
        }

        source.sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }
}
