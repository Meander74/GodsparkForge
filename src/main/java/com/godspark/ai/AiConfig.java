package com.godspark.ai;

public record AiConfig(
    boolean enabled,
    String endpoint,
    String model,
    int timeoutMs,
    int cooldownTicks,
    int maxTokens,
    double temperature
) {
    public static final AiConfig DEFAULT = new AiConfig(
        false,
        "http://127.0.0.1:8080/v1/chat/completions",
        "",
        10000,
        12000,
        256,
        0.7
    );
}
