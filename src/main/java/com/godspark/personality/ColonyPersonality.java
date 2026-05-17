package com.godspark.personality;

import com.godspark.pressure.PressureType;

import java.util.List;

public record ColonyPersonality(
    int colonyId,
    String colonyName,
    PersonalityTrait primaryTrait,
    PersonalityTrait secondaryTrait,
    int aggressionScore,
    int tradeWillingness,
    int expansionism,
    int spirituality,
    List<PersonalityTrait> traitEvidence,
    long computedAtTick
) {
    public ColonyPersonality {
        if (traitEvidence == null) {
            traitEvidence = List.of();
        }
    }

    public String personalityLabel() {
        return primaryTrait.getDisplayName();
    }

    public String shortDescription() {
        return String.format("%s / %s", primaryTrait.getDisplayName(), secondaryTrait.getDisplayName());
    }
}