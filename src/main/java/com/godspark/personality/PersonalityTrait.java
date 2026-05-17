package com.godspark.personality;

public enum PersonalityTrait {
    AGGRESSIVE("Aggressive", "The colony favors strength and readiness above all."),
    PEACEFUL("Peaceful", "The colony prioritizes harmony and stability."),
    CAUTIOUS("Cautious", "The colony guards against threats with vigilance."),
    RESILIENT("Resilient", "The colony endures hardship without losing heart."),
    TRADE_FOCUSED("Trade-Focused", "The colony's heart beats in commerce and exchange."),
    ISOLATIONIST("Isolationist", "The colony turns inward, trusting no one."),
    EXPANSIONIST("Expansionist", "The colony hungers to grow and spread."),
    SPIRITUAL("Spiritual", "The colony's soul reaches toward the divine."),
    INDUSTRIAL("Industrial", "The colony finds meaning in craft and production."),
    COMMUNAL("Communal", "The colony cherishes the bonds between its people."),
    AGRARIAN("Agrarian", "The colony is rooted in the earth and harvest.");

    private final String displayName;
    private final String description;

    PersonalityTrait(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}