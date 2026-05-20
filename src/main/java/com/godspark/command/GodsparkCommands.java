package com.godspark.command;

import com.godspark.GodsparkConstants;
import com.godspark.GodsparkMod;
import com.godspark.ai.AiConfig;
import com.godspark.ai.AiReflection;
import com.godspark.divine.DivineAnswer;
import com.godspark.divine.DivineAnswerContext;
import com.godspark.divine.DivineAnswerInterpreter;
import com.godspark.divine.DivineIntent;
import com.godspark.divine.DivineIntentValidator;
import com.godspark.divine.IntentType;
import com.godspark.divine.ValidatedIntent;
import com.godspark.divine.ValidationResult;
import com.godspark.memory.ColonyMemory;
import com.godspark.world.WorldEffectApplyResult;
import com.godspark.GodsparkConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import com.godspark.memory.MemoryInfluence;
import com.godspark.memory.MemoryType;
import com.godspark.network.GodsparkNetwork;
import com.godspark.network.packet.OpenDashboardPacket;
import com.godspark.observer.ColonySnapshot;
import com.godspark.observer.ObservedColony;
import com.godspark.personality.ColonyPersonality;
import com.godspark.personality.PersonalityEngine;
import com.godspark.personality.PersonalityInfluence;
import com.godspark.personality.PersonalityTrait;
import com.godspark.prayer.PrayerSeed;
import com.godspark.prayer.PrayerChannel;
import com.godspark.prayer.PrayerSeedBank;
import com.godspark.prayer.PrayerSeedGenerator;
import com.godspark.prayer.PrayerTone;
import com.godspark.pressure.PressureModifier;
import com.godspark.pressure.PressureModifierManager;
import com.godspark.pressure.PressureSnapshot;
import com.godspark.pressure.PressureType;
import com.godspark.sacred.SacredSiteManager;
import com.godspark.story.EventRecord;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;

import java.util.List;
import java.util.Map;
import java.util.UUID;

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
                "  AI bridge: " + (GodsparkMod.AI_REFLECTION_SERVICE.isAiEnabled() ? "enabled" : "disabled")
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

        root.then(Commands.literal("prayers")
            .executes(ctx -> showPrayers(ctx.getSource(), -1))
            .then(Commands.argument("colonyId", IntegerArgumentType.integer(1))
                .executes(ctx -> showPrayers(
                    ctx.getSource(),
                    IntegerArgumentType.getInteger(ctx, "colonyId")
                ))
            )
        );

        root.then(Commands.literal("reflect")
            .executes(ctx -> {
                ctx.getSource().sendSuccess(() -> Component.literal(
                    "Usage: /godspark reflect <colonyId>"
                ), false);
                return 0;
            })
            .then(Commands.argument("colonyId", IntegerArgumentType.integer(1))
                .executes(ctx -> showReflection(
                    ctx.getSource(),
                    IntegerArgumentType.getInteger(ctx, "colonyId")
                ))
            )
        );

        root.then(Commands.literal("answer")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("colonyId", IntegerArgumentType.integer(1))
                .then(Commands.argument("message", StringArgumentType.greedyString())
                    .executes(ctx -> answerColony(
                        ctx.getSource(),
                        IntegerArgumentType.getInteger(ctx, "colonyId"),
                        StringArgumentType.getString(ctx, "message")
                    ))
                )
            )
        );

        root.then(Commands.literal("miracles")
            .executes(ctx -> showMiracles(ctx.getSource(), -1))
            .then(Commands.argument("colonyId", IntegerArgumentType.integer(1))
                .executes(ctx -> showMiracles(
                    ctx.getSource(),
                    IntegerArgumentType.getInteger(ctx, "colonyId")
                ))
            )
        );

        root.then(Commands.literal("ai")
            .then(Commands.literal("status")
                .executes(ctx -> showAiStatus(ctx.getSource()))
            )
        );

        root.then(Commands.literal("personality")
            .executes(ctx -> showPersonality(ctx.getSource(), -1))
            .then(Commands.argument("colonyId", IntegerArgumentType.integer(1))
                .executes(ctx -> showPersonality(
                    ctx.getSource(),
                    IntegerArgumentType.getInteger(ctx, "colonyId")
                ))
            )
        );

        root.then(Commands.literal("ui")
            .requires(source -> source.hasPermission(2))
            .executes(ctx -> openUI(ctx.getSource()))
        );

        root.then(Commands.literal("debug")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("invariants")
                .executes(ctx -> showInvariantDebug(ctx.getSource()))
            )
            .then(Commands.literal("reset")
                .executes(ctx -> debugReset(ctx.getSource()))
            )
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

    private static int showPrayers(CommandSourceStack source, int colonyId) {
        PrayerSeedBank bank = GodsparkMod.PRAYER_SEED_BANK;

        if (bank.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No Godspark prayers recorded yet."), false);
            return 1;
        }

        StringBuilder sb = new StringBuilder();

        if (colonyId > 0) {
            List<PrayerSeed> colonyPrayers = bank.getPrayers(colonyId);
            if (colonyPrayers.isEmpty()) {
                source.sendSuccess(() -> Component.literal(
                    String.format("No prayers recorded for colony #%d.", colonyId)
                ), false);
                return 1;
            }

            String colonyName = colonyPrayers.get(0).colonyName();
            sb.append(String.format("Prayers for %s (#%d):\n", colonyName, colonyId));
            for (PrayerSeed seed : colonyPrayers) {
                sb.append(formatPrayerLine(seed));
            }
        } else {
            sb.append("Godspark prayers:\n");
            List<PrayerSeed> allPrayers = bank.getAllPrayers();
            int count = 0;
            for (PrayerSeed seed : allPrayers) {
                if (count >= 20) break;
                sb.append(formatPrayerLine(seed));
                count++;
            }
        }

        source.sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    private static int showReflection(CommandSourceStack source, int colonyId) {
        Map<Integer, ObservedColony> colonies = GodsparkMod.COLONY_OBSERVER.getObservedColonies();
        if (!colonies.containsKey(colonyId)) {
            source.sendSuccess(() -> Component.literal(
                String.format("Colony #%d not found.", colonyId)
            ), false);
            return 0;
        }

        long gameTick = source.getServer().getTickCount();

        source.sendSuccess(() -> Component.literal(
            String.format("Requesting reflection for colony #%d...", colonyId)
        ), false);

        GodsparkMod.AI_REFLECTION_SERVICE.reflectAsync(colonyId, gameTick)
            .whenComplete((reflection, error) -> {
                source.getServer().execute(() -> {
                    if (error != null) {
                        GodsparkMod.LOGGER.warn("[Godspark AI] Reflection command failed: {}", error.getMessage());
                        source.sendSuccess(() -> Component.literal(
                            "Reflection failed. Check logs for details."
                        ), false);
                        return;
                    }

                    if (reflection == null) {
                        source.sendSuccess(() -> Component.literal(
                            String.format("No data available for colony #%d. Wait for the next observation cycle.", colonyId)
                        ), false);
                        return;
                    }

                    source.sendSuccess(() -> Component.literal(formatReflection(reflection)), false);
                });
            });

        return 1;
    }

    private static String formatReflection(AiReflection reflection) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Reflection for %s (#%d):\n", reflection.colonyName(), reflection.colonyId()));
        sb.append(String.format("  Mood: %s\n", reflection.mood()));
        sb.append(String.format("  Dominant pressure: %s (intensity=%d)\n",
            reflection.dominantPressure(), reflection.intensity()));
        sb.append(String.format("  %s\n", reflection.reflection()));
        if (reflection.oracleText() != null && !reflection.oracleText().isBlank()) {
            sb.append(String.format("  Oracle: \"%s\"\n", reflection.oracleText()));
        }
        sb.append(String.format("  Source: %s (confidence %.0f%%)\n", reflection.source(), reflection.confidence() * 100));
        sb.append(String.format("  Tags: %s\n", String.join(", ", reflection.tags())));
        sb.append(String.format("  Reasons: %s\n", String.join(", ", reflection.reasonCodes())));
        return sb.toString();
    }

    private static String formatPrayerLine(PrayerSeed seed) {
        return String.format(
            "  [%s %s %d][%s] %s (reasons=%s)\n",
            seed.channel().getDisplayName(),
            seed.prayerType().getDisplayName(),
            seed.intensity(),
            seed.pressureType().getDisplayName(),
            seed.content(),
            String.join(", ", seed.reasonCodes())
        );
    }

    private static final int MAX_DIVINE_ANSWER_LENGTH = 500;

    private static int answerColony(CommandSourceStack source, int colonyId, String message) {
        Map<Integer, ObservedColony> colonies = GodsparkMod.COLONY_OBSERVER.getObservedColonies();
        ObservedColony observed = colonies.get(colonyId);
        if (observed == null || observed.getLatest() == null) {
            source.sendSuccess(() -> Component.literal(
                String.format("Colony #%d not found.", colonyId)
            ), false);
            return 0;
        }

        String sanitizedMessage = sanitizeAnswerText(message);
        if (sanitizedMessage.isBlank()) {
            source.sendSuccess(() -> Component.literal(
                "Divine answer text cannot be empty."
            ), false);
            return 0;
        }

        long gameTick = source.getServer().getTickCount();

        DivineAnswerContext context = DivineAnswerInterpreter.buildContext(colonyId, gameTick);
        if (context == null) {
            source.sendSuccess(() -> Component.literal(
                String.format("No data available for colony #%d.", colonyId)
            ), false);
            return 0;
        }

        ColonySnapshot snapshot = context.colonySnapshot();
        UUID playerId = source.getEntity() != null ? source.getEntity().getUUID() : new UUID(0, 0);
        String playerName = source.getEntity() != null ? source.getEntity().getName().getString() : "Server";

        DivineAnswer answer = new DivineAnswer(
            colonyId,
            snapshot.name(),
            playerId,
            playerName,
            sanitizedMessage,
            gameTick
        );

        source.sendSuccess(() -> Component.literal(
            String.format("[DEBUG] Interpreting divine answer for %s (#%d)...",
                snapshot.name(), colonyId)
        ), false);

        GodsparkMod.DIVINE_ANSWER_INTERPRETER.interpretAsync(answer, context, gameTick)
            .whenComplete((intent, error) -> {
                source.getServer().execute(() -> {
                    if (error != null) {
                        GodsparkMod.LOGGER.warn("[Godspark Divine] Command failed: {}", error.getMessage());
                        source.sendFailure(Component.literal("Divine answer interpretation failed. Check logs for details."));
                        return;
                    }

                    if (intent == null) {
                        source.sendFailure(Component.literal(
                            String.format("No divine intent produced for colony #%d.", colonyId)
                        ));
                        return;
                    }

                    ValidatedIntent validated = DivineIntentValidator.validate(intent, context);

                    if (validated.permitsPressureModifier()) {
                        boolean miracleApplied = GodsparkMod.PRESSURE_MODIFIER_MANAGER
                            .tryApplyFromValidatedIntent(validated, context, gameTick);
                        if (miracleApplied) {
                            GodsparkMod.LOGGER.info("[Godspark Divine] Miracle applied for colony #{}: {}",
                                colonyId, validated.result().getDisplayName());
                        }
                    }

                    if (validated.isEffectEligible()) {
                        WorldEffectApplyResult worldResult = GodsparkMod.WORLD_EFFECT_ENGINE
                            .tryApply(validated, source.getServer(), gameTick);
                        if (worldResult.applied() && worldResult.affectedCount() > 0) {
                            GodsparkMod.LOGGER.info("[Godspark WorldEffect] {} applied for colony #{}: {} targets affected",
                                worldResult.effect().displayName(), colonyId, worldResult.affectedCount());
                        }
                    }

                    source.sendSuccess(() -> Component.literal(formatValidatedIntent(validated)), false);
                });
            });

        return 1;
    }

    private static String sanitizeAnswerText(String raw) {
        if (raw == null) return "";
        String cleaned = raw.replace('\n', ' ').replace('\r', ' ')
            .replaceAll("\\p{Cntrl}", "").trim();
        if (cleaned.length() > MAX_DIVINE_ANSWER_LENGTH) {
            return cleaned.substring(0, MAX_DIVINE_ANSWER_LENGTH);
        }
        return cleaned;
    }

    private static String formatValidatedIntent(ValidatedIntent validated) {
        DivineIntent intent = validated.intent();
        if (intent == null) {
            return String.format("Validation: %s\n  Notes: %s",
                validated.result().getDisplayName(),
                String.join(", ", validated.validationNotes()));
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Divine Answer for %s (#%d):\n",
            intent.colonyName(), intent.colonyId()));
        sb.append(String.format("  Intent: %s\n", intent.intentType().getDisplayName()));
        sb.append(String.format("  Domain: %s\n", intent.domainDisplayName()));
        sb.append(String.format("  Validation: %s\n", validated.result().getDisplayName()));
        if (validated.isEffectEligible()) {
            sb.append("  Effect: Miracle eligible -- pressure modifier will be applied\n");
        } else if (validated.isDowngraded()) {
            sb.append("  Effect: Downgraded to oracle only -- no world effect\n");
        }
        sb.append(String.format("  Matched Public Prayer: %s\n",
            intent.matchedPublicPrayer() ? "yes" : "no"));
        sb.append(String.format("  Oracle: \"%s\"\n", intent.oracleText()));
        sb.append(String.format("  Source: %s (confidence %.0f%%)\n",
            intent.source().name(), intent.confidence() * 100));
        sb.append(String.format("  Reasons: %s\n", String.join(", ", intent.reasonCodes())));
        if (!validated.validationNotes().isEmpty()) {
            sb.append(String.format("  Validation Notes: %s\n",
                String.join(", ", validated.validationNotes())));
        }
        return sb.toString();
    }

    private static int showMiracles(CommandSourceStack source, int colonyId) {
        PressureModifierManager modManager = GodsparkMod.PRESSURE_MODIFIER_MANAGER;
        List<PressureModifier> allMods = modManager.getAllModifiers();
        long currentTick = source.getServer().getTickCount();

        String whitelistLine = "Whitelisted miracle domains: " +
            String.join(", ", PressureModifierManager.getWhitelistedDomains().stream()
                .map(PressureType::getDisplayName).toList());

        String worldEffectsLine;
        if (GodsparkConfig.WORLD_EFFECTS_ENABLED.get()) {
            worldEffectsLine = "World effects: ENABLED (cooldown: " + GodsparkConfig.WORLD_EFFECT_COOLDOWN_TICKS.get() + " ticks)";
        } else {
            worldEffectsLine = "World effects: DISABLED (config worldEffects.enabled=false)";
        }

        if (colonyId > 0) {
            List<PressureModifier> colonyMods = modManager.getModifiersForColony(colonyId);
            long cooldown = modManager.getCooldownRemaining(colonyId, currentTick);

            Map<PressureType, Integer> basePressures = null;
            PressureSnapshot ps = GodsparkMod.PRESSURE_ENGINE.getSnapshots().get(colonyId);
            if (ps != null) basePressures = ps.values();

            StringBuilder sb = new StringBuilder();
            if (colonyMods.isEmpty()) {
                sb.append(String.format("No active miracle modifiers for colony #%d.\n", colonyId));
            } else {
                sb.append(String.format("Miracle modifiers for colony #%d:\n", colonyId));
                for (PressureModifier mod : colonyMods) {
                    sb.append(String.format("  %s %s%d (expires in %d ticks) [%s]\n",
                        mod.pressureType().getDisplayName(),
                        mod.amount() < 0 ? "" : "+", mod.amount(),
                        mod.remainingTicks(currentTick),
                        mod.source()));
                    if (basePressures != null) {
                        int base = basePressures.getOrDefault(mod.pressureType(), 0);
                        int effective = modManager.getModifiedPressure(colonyId, mod.pressureType(), base);
                        sb.append(String.format("    base=%d → effective=%d\n", base, effective));
                    }
                }
            }
            sb.append(String.format("  Next miracle available in: %d ticks\n", cooldown));
            sb.append("  ").append(whitelistLine).append('\n');
            sb.append("  ").append(worldEffectsLine).append('\n');

            var observed = GodsparkMod.COLONY_OBSERVER.getObservedColonies().get(colonyId);
            ResourceKey<Level> dimKey = observed != null && observed.getLatest() != null
                ? observed.getLatest().dimension() : null;
            var weRecord = GodsparkMod.WORLD_EFFECT_ENGINE.getLastRecord(colonyId, dimKey);
            if (weRecord != null) {
                sb.append(String.format("  Last world effect: %s at tick %d, affected %d targets\n",
                    weRecord.lastEffect(), weRecord.lastFireTick(), weRecord.lastAffectedCount()));
            } else {
                sb.append("  Last world effect: none\n");
            }

            source.sendSuccess(() -> Component.literal(sb.toString()), false);
            return colonyMods.size();
        }

        if (allMods.isEmpty()) {
            source.sendSuccess(() -> Component.literal(
                "No active miracle modifiers.\n" + whitelistLine + "\n" + worldEffectsLine), false);
            return 0;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Active miracle modifiers (%d):\n", allMods.size()));
        for (PressureModifier mod : allMods) {
            sb.append(String.format("  Colony #%d: %s %s%d (expires in %d ticks) [%s]\n",
                mod.colonyId(),
                mod.pressureType().getDisplayName(),
                mod.amount() < 0 ? "" : "+", mod.amount(),
                mod.remainingTicks(currentTick),
                mod.source()));
        }
        sb.append(whitelistLine).append('\n');
        sb.append(worldEffectsLine).append('\n');
        source.sendSuccess(() -> Component.literal(sb.toString()), false);
        return allMods.size();
    }

    private static int showAiStatus(CommandSourceStack source) {
        boolean aiEnabled = GodsparkMod.DIVINE_ANSWER_INTERPRETER.isAiEnabled();
        AiConfig config = GodsparkMod.getCurrentAiConfig();

        StringBuilder sb = new StringBuilder();
        sb.append("[Godspark AI Status]\n");
        sb.append(String.format("  AI Enabled: %s\n", aiEnabled));
        sb.append(String.format("  Endpoint: %s\n", config.endpoint()));
        sb.append(String.format("  Model: %s\n", config.model()));
        sb.append(String.format("  Timeout: %dms\n", config.timeoutMs()));
        sb.append(String.format("  Temperature: %.1f\n", config.temperature()));

        if (aiEnabled) {
            sb.append("  Reflection: available (async)\n");
            sb.append("  Divine Answer: available (async with template fallback)\n");
        } else {
            sb.append("  Reflection: template only (AI disabled)\n");
            sb.append("  Divine Answer: template only (AI disabled)\n");
        }

        sb.append("\n  Config: serverconfig/godspark-server.toml");

        source.sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    private static int showPersonality(CommandSourceStack source, int colonyId) {
        if (colonyId < 0) {
            Map<Integer, ObservedColony> colonies = GodsparkMod.COLONY_OBSERVER.getObservedColonies();
            if (colonies.isEmpty()) {
                source.sendSuccess(() -> Component.literal("No colonies observed yet."), false);
                return 0;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Colony Personalities:\n");
            long tick = source.getServer().getTickCount();
            for (Map.Entry<Integer, ObservedColony> entry : colonies.entrySet()) {
                ColonyPersonality p = GodsparkMod.PERSONALITY_ENGINE.computePersonality(entry.getKey(), tick);
                if (p != null) {
                    PrayerTone tone = PrayerTone.fromTrait(p.primaryTrait());
                    sb.append(String.format("  #%d %s: %s | Tone: %s\n",
                        p.colonyId(), p.colonyName(), p.shortDescription(), tone.getDisplayName()));
                }
            }
            source.sendSuccess(() -> Component.literal(sb.toString()), false);
            return colonies.size();
        }

        ObservedColony observed = GodsparkMod.COLONY_OBSERVER.getObservedColonies().get(colonyId);
        if (observed == null || observed.getLatest() == null) {
            source.sendSuccess(() -> Component.literal(
                String.format("Colony #%d not found.", colonyId)
            ), false);
            return 0;
        }

        long tick = source.getServer().getTickCount();
        ColonyPersonality p = GodsparkMod.PERSONALITY_ENGINE.computePersonality(colonyId, tick);

        if (p == null) {
            source.sendSuccess(() -> Component.literal(
                String.format("No personality data for colony #%d.", colonyId)
            ), false);
            return 0;
        }

        PrayerTone tone = PrayerTone.fromTrait(p.primaryTrait());
        Map<PressureType, Integer> adjustments = GodsparkMod.PERSONALITY_INFLUENCE.computeAdjustments(p);

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Personality of %s (#%d):\n", p.colonyName(), p.colonyId()));
        sb.append(String.format("  Primary: %s\n", p.primaryTrait().getDisplayName()));
        sb.append(String.format("  Secondary: %s\n", p.secondaryTrait().getDisplayName()));
        sb.append(String.format("  Description: %s\n", p.primaryTrait().getDescription()));
        sb.append(String.format("  Prayer Tone: %s\n", tone.getDisplayName()));
        sb.append(String.format("  Scores: aggression=%d, trade=%d, expansion=%d, spirituality=%d\n",
            p.aggressionScore(), p.tradeWillingness(), p.expansionism(), p.spirituality()));

        boolean hasAnyAdjustment = adjustments.values().stream().anyMatch(v -> v != 0);
        if (hasAnyAdjustment) {
            sb.append("  Threshold Influence:\n");
            for (PressureType pt : PressureType.values()) {
                int adj = adjustments.getOrDefault(pt, 0);
                if (adj != 0) {
                    String direction = adj < 0 ? "easier" : "harder";
                    sb.append(String.format("    %s: %+d (events %s to trigger)\n",
                        pt.getDisplayName(), adj, direction));
                }
            }
        } else {
            sb.append("  Threshold Influence: none (neutral)\n");
        }

        if (!p.traitEvidence().isEmpty()) {
            sb.append("  Evidence: ");
            for (int i = 0; i < p.traitEvidence().size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(p.traitEvidence().get(i).getDisplayName());
            }
            sb.append("\n");
        }

        source.sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    private static int openUI(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendSuccess(() -> Component.literal("Command only available in-game."), false);
            return 0;
        }
        GodsparkNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenDashboardPacket());
        source.sendSuccess(() -> Component.literal("Opening Godspark Dashboard... (press ESC to close)"), false);
        return 1;
    }

    private static int showInvariantDebug(CommandSourceStack source) {
        StringBuilder sb = new StringBuilder("Godspark invariant check:\n");

        boolean commonsSafe = com.godspark.prayer.PrayerChannel.COMMONS.isPublic()
            && !com.godspark.prayer.PrayerChannel.COMMONS.isMiracleEligible();
        appendInvariant(sb, "COMMONS has no miracles", commonsSafe);

        ColonySnapshot noSacred = invariantSnapshot(9001, 0, 0);
        ColonySnapshot gatheringOnly = invariantSnapshot(9002, 0, 1);
        ColonySnapshot oneSacred = invariantSnapshot(9003, 1, 0);
        ColonySnapshot twoSacred = invariantSnapshot(9004, 2, 0);
        ColonySnapshot threeSacred = invariantSnapshot(9005, 3, 0);

        appendInvariant(sb, "No sacred/no stone -> PRIVATE",
            PrayerSeedGenerator.selectPrayerChannel(noSacred, null, false) == PrayerChannel.PRIVATE);
        appendInvariant(sb, "Gathering only -> COMMONS",
            PrayerSeedGenerator.selectPrayerChannel(gatheringOnly, null, false) == PrayerChannel.COMMONS);
        appendInvariant(sb, "Prayer Stone only -> CHURCH",
            PrayerSeedGenerator.selectPrayerChannel(noSacred, null, true) == PrayerChannel.CHURCH);
        appendInvariant(sb, "One sacred building -> CHURCH",
            PrayerSeedGenerator.selectPrayerChannel(oneSacred, null, false) == PrayerChannel.CHURCH);
        appendInvariant(sb, "Two sacred buildings -> SHRINE",
            PrayerSeedGenerator.selectPrayerChannel(twoSacred, null, false) == PrayerChannel.SHRINE);
        appendInvariant(sb, "Three sacred buildings -> TEMPLE",
            PrayerSeedGenerator.selectPrayerChannel(threeSacred, null, false) == PrayerChannel.TEMPLE);

        boolean anchorScoreCapped = GodsparkMod.COLONY_OBSERVER.getObservedColonies().keySet().stream()
            .allMatch(id -> SacredSiteManager.getInstance().getPrayerStoneAnchorScore(id) <= 1);
        appendInvariant(sb, "Prayer Stone anchor score capped", anchorScoreCapped);

        source.sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    private static int debugReset(CommandSourceStack source) {
        GodsparkMod.COLONY_OBSERVER.clear();
        GodsparkMod.PRESSURE_ENGINE.clear();
        GodsparkMod.PRAYER_SEED_BANK.clear();
        SacredSiteManager.getInstance().clear();
        GodsparkMod.EVENT_QUEUE.clear();
        GodsparkMod.EVENT_STATE_MANAGER.clear();
        GodsparkMod.MEMORY_BANK.clear();
        GodsparkMod.PRESSURE_MODIFIER_MANAGER.clear();
        GodsparkMod.PRESSURE_MODIFIER_MANAGER.clearCooldowns();
        GodsparkMod.PERSONALITY_ENGINE.clearCache();
        GodsparkMod.AI_REFLECTION_SERVICE.clearCooldowns();
        GodsparkMod.DIVINE_ANSWER_INTERPRETER.clearCooldowns();
        source.sendSuccess(() -> Component.literal("All Godspark state cleared."), true);
        return 1;
    }

    private static void appendInvariant(StringBuilder sb, String label, boolean pass) {
        sb.append(pass ? "PASS " : "FAIL ").append(label).append('\n');
    }

    private static ColonySnapshot invariantSnapshot(int colonyId, int sacredBuildings, int gatheringBuildings) {
        return new ColonySnapshot(
            colonyId,
            "Invariant Colony " + colonyId,
            net.minecraft.core.BlockPos.ZERO,
            1,
            sacredBuildings + gatheringBuildings,
            0,
            7.0,
            0,
            0,
            1,
            0,
            sacredBuildings,
            gatheringBuildings,
            false,
            net.minecraft.world.level.Level.OVERWORLD,
            0L
        );
    }
}
