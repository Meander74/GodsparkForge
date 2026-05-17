package com.godspark.ai;

import java.util.List;

public record AiReflection(
    int schemaVersion,
    int colonyId,
    String colonyName,
    String dominantPressure,
    String mood,
    int intensity,
    String reflection,
    String oracleText,
    List<String> reasonCodes,
    List<String> tags,
    double confidence,
    String source
) {}
