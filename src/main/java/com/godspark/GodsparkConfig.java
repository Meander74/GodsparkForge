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

        SPEC = builder.build();
    }
}
