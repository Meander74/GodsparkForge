package com.godspark.divine;

import java.util.UUID;

public record DivineAnswer(
    int colonyId,
    String colonyName,
    UUID playerId,
    String playerName,
    String rawText,
    long submittedAtTick
) {}