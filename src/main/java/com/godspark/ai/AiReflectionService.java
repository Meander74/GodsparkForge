package com.godspark.ai;

import com.godspark.GodsparkMod;
import com.google.gson.JsonParser;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class AiReflectionService {

    private static final int FAILURE_BACKOFF_TICKS = 1200;

    private volatile AiConfig config;
    private volatile AiClient client;
    private final Map<Integer, Long> successCooldowns = new ConcurrentHashMap<>();
    private final Map<Integer, Long> failureBackoffs = new ConcurrentHashMap<>();

    public AiReflectionService(AiConfig config) {
        this.config = config;
        this.client = new AiClient(config);
    }

    public void reloadConfig(AiConfig newConfig) {
        this.config = newConfig;
        this.client = new AiClient(newConfig);
        GodsparkMod.LOGGER.info("[Godspark AI] Config reloaded: enabled={}, endpoint={}, model={}",
            newConfig.enabled(), newConfig.endpoint(), newConfig.model());
    }

    public AiConfig getConfig() {
        return config;
    }

    public AiReflection reflect(int colonyId, long gameTick) {
        if (!config.enabled()) {
            return TemplateReflectionService.reflect(colonyId);
        }

        if (isOnCooldown(colonyId, gameTick, successCooldowns, config.cooldownTicks())) {
            GodsparkMod.LOGGER.info("[Godspark AI] Colony #{} is on success cooldown, using template reflection", colonyId);
            AiReflection fallback = TemplateReflectionService.reflect(colonyId);
            return withSource(fallback, "AI_COOLDOWN");
        }

        if (isOnCooldown(colonyId, gameTick, failureBackoffs, FAILURE_BACKOFF_TICKS)) {
            GodsparkMod.LOGGER.info("[Godspark AI] Colony #{} is on failure backoff, using template reflection", colonyId);
            AiReflection fallback = TemplateReflectionService.reflect(colonyId);
            return withSource(fallback, "AI_FALLBACK");
        }

        try {
            AiReflection aiResult = callAi(colonyId);
            if (aiResult != null) {
                successCooldowns.put(colonyId, gameTick);
                return withSource(aiResult, "AI");
            }
        } catch (Exception e) {
            GodsparkMod.LOGGER.warn("[Godspark AI] AI reflection failed for colony #{}: {}", colonyId, e.getMessage());
        }

        GodsparkMod.LOGGER.info("[Godspark AI] Falling back to template reflection for colony #{}", colonyId);
        failureBackoffs.put(colonyId, gameTick);
        AiReflection fallback = TemplateReflectionService.reflect(colonyId);
        return withSource(fallback, "AI_FALLBACK");
    }

    public CompletableFuture<AiReflection> reflectAsync(int colonyId, long gameTick) {
        if (!config.enabled()) {
            return CompletableFuture.completedFuture(TemplateReflectionService.reflect(colonyId));
        }

        if (isOnCooldown(colonyId, gameTick, successCooldowns, config.cooldownTicks())) {
            GodsparkMod.LOGGER.info("[Godspark AI] Colony #{} is on success cooldown, using template reflection", colonyId);
            AiReflection fallback = TemplateReflectionService.reflect(colonyId);
            return CompletableFuture.completedFuture(withSource(fallback, "AI_COOLDOWN"));
        }

        if (isOnCooldown(colonyId, gameTick, failureBackoffs, FAILURE_BACKOFF_TICKS)) {
            GodsparkMod.LOGGER.info("[Godspark AI] Colony #{} is on failure backoff, using template reflection", colonyId);
            AiReflection fallback = TemplateReflectionService.reflect(colonyId);
            return CompletableFuture.completedFuture(withSource(fallback, "AI_FALLBACK"));
        }

        var data = TemplateReflectionService.reflect(colonyId);
        if (data == null) {
            return CompletableFuture.completedFuture(null);
        }

        var messages = AiPromptBuilder.buildMessages(colonyId);

        return client.complete(messages)
            .handle((rawJson, error) -> {
                if (error != null) {
                    GodsparkMod.LOGGER.warn("[Godspark AI] Async reflection failed for colony #{}: {}", colonyId, error.getMessage());
                    failureBackoffs.put(colonyId, gameTick);
                    return withSource(TemplateReflectionService.reflect(colonyId), "AI_FALLBACK");
                }

                if (rawJson == null || rawJson.isBlank()) {
                    GodsparkMod.LOGGER.warn("[Godspark AI] Empty async response for colony #{}", colonyId);
                    failureBackoffs.put(colonyId, gameTick);
                    return withSource(TemplateReflectionService.reflect(colonyId), "AI_FALLBACK");
                }

                String json = extractJson(rawJson);

                try {
                    AiReflection parsed = AiResponseParser.parse(json, data.colonyId(), data.colonyName());
                    GodsparkMod.LOGGER.info("[Godspark AI] Generated reflection for colony #{} (confidence={})",
                        colonyId, String.format("%.0f%%", parsed.confidence() * 100));
                    successCooldowns.put(colonyId, gameTick);
                    return withSource(parsed, "AI");
                } catch (Exception e) {
                    GodsparkMod.LOGGER.warn("[Godspark AI] Failed to parse AI JSON for colony #{}: {}", colonyId, e.getMessage());
                    GodsparkMod.LOGGER.debug("[Godspark AI] Raw response: {}", json);
                    failureBackoffs.put(colonyId, gameTick);
                    return withSource(TemplateReflectionService.reflect(colonyId), "AI_FALLBACK");
                }
            });
    }

    private static boolean isOnCooldown(int colonyId, long gameTick, Map<Integer, Long> map, long cooldownTicks) {
        Long lastTick = map.get(colonyId);
        if (lastTick == null) return false;
        return (gameTick - lastTick) < cooldownTicks;
    }

    private AiReflection callAi(int colonyId) throws Exception {
        var data = TemplateReflectionService.reflect(colonyId);
        if (data == null) return null;

        var messages = AiPromptBuilder.buildMessages(colonyId);

        CompletableFuture<String> future = client.complete(messages);

        String rawJson;
        try {
            rawJson = future.get(config.timeoutMs(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            GodsparkMod.LOGGER.warn("[Godspark AI] Request timed out for colony #{}", colonyId);
            return null;
        } catch (ExecutionException e) {
            GodsparkMod.LOGGER.warn("[Godspark AI] Request failed for colony #{}: {}", colonyId, e.getCause().getMessage());
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }

        if (rawJson == null || rawJson.isBlank()) {
            GodsparkMod.LOGGER.warn("[Godspark AI] Empty response for colony #{}", colonyId);
            return null;
        }

        rawJson = extractJson(rawJson);

        try {
            AiReflection parsed = AiResponseParser.parse(rawJson, data.colonyId(), data.colonyName());
            GodsparkMod.LOGGER.info("[Godspark AI] Generated reflection for colony #{} (confidence={})",
                colonyId, String.format("%.0f%%", parsed.confidence() * 100));
            return parsed;
        } catch (Exception e) {
            GodsparkMod.LOGGER.warn("[Godspark AI] Failed to parse AI JSON for colony #{}: {}", colonyId, e.getMessage());
            GodsparkMod.LOGGER.debug("[Godspark AI] Raw response: {}", rawJson);
            return null;
        }
    }

    private static AiReflection withSource(AiReflection r, String source) {
        if (r == null) return null;
        return new AiReflection(
            r.schemaVersion(), r.colonyId(), r.colonyName(),
            r.dominantPressure(), r.mood(), r.intensity(),
            r.reflection(), r.oracleText(),
            r.reasonCodes(), r.tags(), r.confidence(),
            source
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
            if (JsonParser.parseString(text).isJsonObject()) {
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

    public void clearCooldowns() {
        successCooldowns.clear();
        failureBackoffs.clear();
    }

    public boolean isAiEnabled() {
        return config.enabled();
    }
}
