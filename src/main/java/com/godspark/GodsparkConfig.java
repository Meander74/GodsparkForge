package com.godspark;

import net.minecraftforge.common.ForgeConfigSpec;

public final class GodsparkConfig {

    private GodsparkConfig() {}

    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue AI_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<String> AI_ENDPOINT;
    public static final ForgeConfigSpec.ConfigValue<String> AI_MODEL;
    public static final ForgeConfigSpec.IntValue AI_TIMEOUT_MS;
    public static final ForgeConfigSpec.IntValue AI_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.IntValue AI_MAX_TOKENS;
    public static final ForgeConfigSpec.DoubleValue AI_TEMPERATURE;
    public static final ForgeConfigSpec.IntValue SACRED_PRAYER_STONE_BIND_RADIUS;
    public static final ForgeConfigSpec.BooleanValue SACRED_PRAYER_STONE_ALLOW_UNBOUND;

    public static final ForgeConfigSpec.BooleanValue WORLD_EFFECTS_ENABLED;
    public static final ForgeConfigSpec.BooleanValue WORLD_EFFECTS_ALLOW_DEBUG_ANSWER_COMMAND;
    public static final ForgeConfigSpec.IntValue WORLD_EFFECT_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.IntValue WORLD_EFFECT_RESISTANCE_DURATION;
    public static final ForgeConfigSpec.IntValue WORLD_EFFECT_CROP_PULSE_ATTEMPTS;
    public static final ForgeConfigSpec.IntValue WORLD_EFFECT_CROP_PULSE_SUCCESS_CAP;
    public static final ForgeConfigSpec.IntValue WORLD_EFFECT_GUARDIAN_RADIUS;
    public static final ForgeConfigSpec.IntValue WORLD_EFFECT_GREEN_MERCY_RADIUS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Godspark AI Configuration").push("ai");

        AI_ENABLED = builder
            .comment("Enable AI integration. When disabled, template-only fallback is used.",
                     "Set to true and restart server to use Ollama or other OpenAI-compatible endpoints.")
            .define("enabled", false);

        AI_ENDPOINT = builder
            .comment("OpenAI-compatible chat completions endpoint.",
                     "For Ollama: http://localhost:11434/v1/chat/completions",
                     "For OpenAI: https://api.openai.com/v1/chat/completions")
            .define("endpoint", "http://localhost:11434/v1/chat/completions");

        AI_MODEL = builder
            .comment("Model name. For Ollama: qwen3.5:9b, llama3:8b, etc.",
                     "Leave blank for endpoints that don't require a model field.")
            .define("model", "qwen3.5:9b");

        AI_TIMEOUT_MS = builder
            .comment("HTTP timeout in milliseconds for AI requests.",
                     "Increase for slower hardware (e.g., 30000 for CPU inference).")
            .defineInRange("timeoutMs", 30000, 5000, 120000);

        AI_COOLDOWN_TICKS = builder
            .comment("Ticks between successful AI calls per colony (1200 = 60 seconds).",
                     "Also used as backoff on failure.")
            .defineInRange("cooldownTicks", 6000, 600, 60000);

        AI_MAX_TOKENS = builder
            .comment("Maximum tokens in AI response.")
            .defineInRange("maxTokens", 512, 64, 4096);

        AI_TEMPERATURE = builder
            .comment("Temperature for AI responses. Lower = more deterministic.",
                     "For structured JSON output, use 0.3-0.5.")
            .defineInRange("temperature", 0.4, 0.0, 2.0);

        builder.pop();

        builder.comment("Godspark Sacred Site Configuration").push("sacred");

        SACRED_PRAYER_STONE_BIND_RADIUS = builder
            .comment("Maximum distance in blocks for a Prayer Stone to bind to a colony.")
            .defineInRange("prayerStoneBindRadius", 128, 16, 512);

        SACRED_PRAYER_STONE_ALLOW_UNBOUND = builder
            .comment("Allow Prayer Stones with no nearby colony to register as sacred anchors.",
                     "Patch 6A.3 keeps this false: unbound stones stay silent.")
            .define("prayerStoneAllowUnbound", false);

        builder.pop();

        builder.comment("Light World Effects — Phase 5D.2",
                        "World effects are opt-in and disabled by default.",
                        "When disabled, EFFECT_ELIGIBLE intents still apply pressure modifiers but no world mutation.")
               .push("worldEffects");

        WORLD_EFFECTS_ENABLED = builder
            .comment("Enable light world effects (Guardian's Vigil resistance, Green Mercy crop pulses).",
                     "Defaults to OFF. Turn on only after validating Phase 5D.1 in-game.")
            .define("enabled", false);

        WORLD_EFFECTS_ALLOW_DEBUG_ANSWER_COMMAND = builder
            .comment("Allow /godspark answer to trigger world effects.",
                     "For prototype testing only. Final gameplay should use shrine/UI interaction.",
                     "Requires worldEffects.enabled=true.")
            .define("allowDebugAnswerCommand", false);

        WORLD_EFFECT_COOLDOWN_TICKS = builder
            .comment("Shared cooldown in ticks between world effects per colony+dimension.",
                     "24000 = 20 minutes. Independent of pressure modifier cooldown.")
            .defineInRange("cooldownTicks", 24000, 6000, 72000);

        WORLD_EFFECT_RESISTANCE_DURATION = builder
            .comment("Duration of Resistance I from Guardian's Vigil in ticks.",
                     "6000 = 5 minutes.")
            .defineInRange("resistanceDurationTicks", 6000, 1200, 12000);

        WORLD_EFFECT_CROP_PULSE_ATTEMPTS = builder
            .comment("Max sampled positions/probes for Green Mercy.",
                     "Not all probes find crops; applies to loaded chunks only.")
            .defineInRange("cropPulseAttempts", 64, 8, 256);

        WORLD_EFFECT_CROP_PULSE_SUCCESS_CAP = builder
            .comment("Max crops that Green Mercy can successfully advance per pulse.")
            .defineInRange("cropPulseSuccessCap", 8, 1, 32);

        WORLD_EFFECT_GUARDIAN_RADIUS = builder
            .comment("Radius around colony center for Guardian's Vigil guard search.")
            .defineInRange("guardianVigilRadius", 128, 16, 256);

        WORLD_EFFECT_GREEN_MERCY_RADIUS = builder
            .comment("Radius around colony center for Green Mercy crop probing.")
            .defineInRange("greenMercyRadius", 128, 16, 256);

        builder.pop();

        SPEC = builder.build();
    }
}
