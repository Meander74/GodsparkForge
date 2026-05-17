package com.godspark.divine;

import com.godspark.GodsparkMod;
import com.godspark.ai.AiClient;
import com.godspark.ai.AiConfig;
import com.godspark.memory.ColonyMemory;
import com.godspark.observer.ColonySnapshot;
import com.godspark.observer.ObservedColony;
import com.godspark.prayer.PrayerSeed;
import com.godspark.pressure.PressureSnapshot;
import com.godspark.story.EventRecord;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class DivineAnswerInterpreter {

    private static final int FAILURE_BACKOFF_TICKS = 1200;
    private static final int SUCCESS_COOLDOWN_TICKS = 6000;

    private volatile AiConfig config;
    private volatile AiClient client;
    private final Map<Integer, Long> successCooldowns = new ConcurrentHashMap<>();
    private final Map<Integer, Long> failureBackoffs = new ConcurrentHashMap<>();

    public DivineAnswerInterpreter(AiConfig config) {
        this.config = config;
        this.client = new AiClient(config);
    }

    public void reloadConfig(AiConfig newConfig) {
        this.config = newConfig;
        this.client = new AiClient(newConfig);
        GodsparkMod.LOGGER.info("[Godspark Divine] Config reloaded: enabled={}, endpoint={}, model={}",
            newConfig.enabled(), newConfig.endpoint(), newConfig.model());
    }

    public boolean isAiEnabled() {
        return config.enabled();
    }

    public DivineIntent interpretTemplate(DivineAnswer answer, DivineAnswerContext context, long gameTick) {
        return TemplateDivineInterpreter.interpret(answer, context);
    }

    public ValidatedIntent interpretAndValidate(DivineAnswer answer, DivineAnswerContext context, long gameTick) {
        if (context == null) {
            DivineIntent fallback = new DivineIntent(
                1, answer.colonyId(), answer.colonyName(),
                IntentType.ORACLE_ONLY, null, 0.2,
                false, "",
                "The Spark cannot find the colony's gathered voice.",
                List.of("NO_CONTEXT_AVAILABLE"),
                IntentSource.TEMPLATE
            );
            return ValidatedIntent.rejected(fallback, List.of("NO_CONTEXT_AVAILABLE"));
        }

        DivineIntent intent = TemplateDivineInterpreter.interpret(answer, context);
        return DivineIntentValidator.validate(intent, context);
    }

    public CompletableFuture<DivineIntent> interpretAsync(DivineAnswer answer, DivineAnswerContext context, long gameTick) {
        if (context == null || !context.hasPublicPrayers()) {
            DivineIntent template = TemplateDivineInterpreter.interpret(answer, context);
            ValidatedIntent validated = DivineIntentValidator.validate(template, context);
            return CompletableFuture.completedFuture(validated.intent());
        }

        if (!config.enabled()) {
            DivineIntent template = TemplateDivineInterpreter.interpret(answer, context);
            ValidatedIntent validated = DivineIntentValidator.validate(template, context);
            return CompletableFuture.completedFuture(validated.intent());
        }

        if (isOnCooldown(answer.colonyId(), gameTick, successCooldowns, SUCCESS_COOLDOWN_TICKS)) {
            DivineIntent fallback = TemplateDivineInterpreter.interpret(answer, context);
            ValidatedIntent validated = DivineIntentValidator.validate(fallback, context);
            return CompletableFuture.completedFuture(
                withSource(validated.intent(), IntentSource.AI_COOLDOWN)
            );
        }

        if (isOnCooldown(answer.colonyId(), gameTick, failureBackoffs, FAILURE_BACKOFF_TICKS)) {
            DivineIntent fallback = TemplateDivineInterpreter.interpret(answer, context);
            ValidatedIntent validated = DivineIntentValidator.validate(fallback, context);
            return CompletableFuture.completedFuture(
                withSource(validated.intent(), IntentSource.AI_FALLBACK)
            );
        }

        var messages = DivineAnswerPromptBuilder.buildMessages(answer, context);

        return client.complete(messages).handle((rawJson, error) -> {
            if (error != null) {
                GodsparkMod.LOGGER.warn("[Godspark Divine] Async interpretation failed for colony #{}: {}",
                    answer.colonyId(), error.getMessage());
                failureBackoffs.put(answer.colonyId(), gameTick);
                DivineIntent fallback = TemplateDivineInterpreter.interpret(answer, context);
                ValidatedIntent validated = DivineIntentValidator.validate(fallback, context);
                return withSource(validated.intent(), IntentSource.AI_FALLBACK);
            }

            if (rawJson == null || rawJson.isBlank()) {
                GodsparkMod.LOGGER.warn("[Godspark Divine] Empty async response for colony #{}", answer.colonyId());
                failureBackoffs.put(answer.colonyId(), gameTick);
                DivineIntent fallback = TemplateDivineInterpreter.interpret(answer, context);
                ValidatedIntent validated = DivineIntentValidator.validate(fallback, context);
                return withSource(validated.intent(), IntentSource.AI_FALLBACK);
            }

            String json = extractJson(rawJson);

            try {
                DivineIntent parsed = DivineIntentParser.parse(json, answer.colonyId(), answer.colonyName());
                ValidatedIntent validated = DivineIntentValidator.validate(parsed, context);
                DivineIntent result = validated.intent();
                GodsparkMod.LOGGER.info("[Godspark Divine] Intent for colony #{} (type={}, domain={}, confidence={}, validation={})",
                    answer.colonyId(), result.intentType(), result.domainDisplayName(),
                    String.format("%.0f%%", result.confidence() * 100), validated.result().getDisplayName());
                successCooldowns.put(answer.colonyId(), gameTick);
                return result;
            } catch (Exception e) {
                GodsparkMod.LOGGER.warn("[Godspark Divine] Failed to parse AI JSON for colony #{}: {}",
                    answer.colonyId(), e.getMessage());
                GodsparkMod.LOGGER.debug("[Godspark Divine] Raw response: {}", json);
                failureBackoffs.put(answer.colonyId(), gameTick);
                DivineIntent fallback = TemplateDivineInterpreter.interpret(answer, context);
                ValidatedIntent validated = DivineIntentValidator.validate(fallback, context);
                return withSource(validated.intent(), IntentSource.AI_FALLBACK);
            }
        });
    }

    private static boolean isOnCooldown(int colonyId, long gameTick, Map<Integer, Long> map, long cooldownTicks) {
        Long lastTick = map.get(colonyId);
        if (lastTick == null) return false;
        return (gameTick - lastTick) < cooldownTicks;
    }

    private static DivineIntent withSource(DivineIntent intent, IntentSource source) {
        if (intent == null) return null;
        return new DivineIntent(
            intent.schemaVersion(), intent.colonyId(), intent.colonyName(),
            intent.intentType(), intent.domain(), intent.confidence(),
            intent.matchedPublicPrayer(), intent.matchedPrayerSourceKey(),
            intent.oracleText(), intent.reasonCodes(), source
        );
    }

    private static String extractJson(String raw) {
        String trimmed = raw.trim();

        String direct = tryAsJsonObject(trimmed);
        if (direct != null) return direct;

        String fenced = extractFencedJson(trimmed);
        if (fenced != null) return fenced;

        String balanced = extractFirstValidJsonObject(trimmed);
        if (balanced != null) return balanced;

        return trimmed;
    }

    private static String tryAsJsonObject(String text) {
        try {
            if (com.google.gson.JsonParser.parseString(text).isJsonObject()) {
                return text;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static String extractFencedJson(String text) {
        int fenceStart = text.indexOf("```");
        while (fenceStart >= 0) {
            int contentStart = text.indexOf('\n', fenceStart);
            if (contentStart < 0) return null;

            int fenceEnd = text.indexOf("```", contentStart + 1);
            if (fenceEnd < 0) return null;

            String block = text.substring(contentStart + 1, fenceEnd).trim();
            String valid = tryAsJsonObject(block);
            if (valid != null) return valid;

            fenceStart = text.indexOf("```", fenceEnd + 3);
        }
        return null;
    }

    private static String extractFirstValidJsonObject(String text) {
        for (int start = 0; start < text.length(); start++) {
            if (text.charAt(start) != '{') continue;

            int depth = 0;
            boolean inString = false;
            boolean escaped = false;

            for (int i = start; i < text.length(); i++) {
                char c = text.charAt(i);

                if (escaped) {
                    escaped = false;
                    continue;
                }

                if (c == '\\') {
                    escaped = true;
                    continue;
                }

                if (c == '"') {
                    inString = !inString;
                    continue;
                }

                if (inString) continue;

                if (c == '{') depth++;
                if (c == '}') depth--;

                if (depth == 0) {
                    String candidate = text.substring(start, i + 1);
                    String valid = tryAsJsonObject(candidate);
                    if (valid != null) return valid;
                    break;
                }
            }
        }
        return null;
    }

    public static DivineAnswerContext buildContext(int colonyId, long gameTick) {
        Map<Integer, ObservedColony> colonies = GodsparkMod.COLONY_OBSERVER.getObservedColonies();
        ObservedColony observed = colonies.get(colonyId);
        if (observed == null) return null;

        ColonySnapshot snapshot = observed.getLatest();
        if (snapshot == null) return null;

        Map<Integer, PressureSnapshot> pressures = GodsparkMod.PRESSURE_ENGINE.getSnapshots();
        PressureSnapshot pressureSnapshot = pressures.get(colonyId);

        List<EventRecord> activeEvents = GodsparkMod.EVENT_STATE_MANAGER.getActiveEvents().stream()
            .filter(r -> r.event().colonyId() == colonyId)
            .toList();

        List<PrayerSeed> allPrayers = GodsparkMod.PRAYER_SEED_BANK.getPrayers(colonyId);
        List<PrayerSeed> publicPrayers = allPrayers.stream()
            .filter(PrayerSeed::isPublicPrayer)
            .toList();

        List<ColonyMemory> memories = GodsparkMod.MEMORY_BANK.getStrongestMemories(colonyId, 5);

        return new DivineAnswerContext(snapshot, pressureSnapshot, publicPrayers, activeEvents, memories);
    }

    public void clearCooldowns() {
        successCooldowns.clear();
        failureBackoffs.clear();
    }
}