package com.godspark.story;

import com.godspark.pressure.PressureType;

public enum StoryEventType {
    FOOD_LOW(PressureType.FOOD, EventSeverity.LOW, 40, "Food supply is fragile", false),
    FOOD_MEDIUM(PressureType.FOOD, EventSeverity.MEDIUM, 70, "Food shortage risk is rising", false),
    FOOD_HIGH(PressureType.FOOD, EventSeverity.HIGH, 90, "Famine risk threatens the colony", false),

    SECURITY_LOW(PressureType.SECURITY, EventSeverity.LOW, 40, "Guards are stretched thin", false),
    SECURITY_MEDIUM(PressureType.SECURITY, EventSeverity.MEDIUM, 70, "Colony defenses look vulnerable", false),
    SECURITY_HIGH(PressureType.SECURITY, EventSeverity.HIGH, 90, "Raid in progress - defenses may fail", true),

    HOUSING_LOW(PressureType.HOUSING, EventSeverity.LOW, 25, "Citizens are seeking shelter", false),
    HOUSING_MEDIUM(PressureType.HOUSING, EventSeverity.MEDIUM, 50, "Housing shortage is disrupting the colony", false),
    HOUSING_HIGH(PressureType.HOUSING, EventSeverity.HIGH, 80, "Mass homelessness is causing despair", false),

    COMFORT_LOW(PressureType.COMFORT, EventSeverity.LOW, 40, "Citizens are growing discontent", false),
    COMFORT_MEDIUM(PressureType.COMFORT, EventSeverity.MEDIUM, 70, "Quality of life is declining", false),
    COMFORT_HIGH(PressureType.COMFORT, EventSeverity.HIGH, 85, "Citizens may abandon the colony", false),

    INDUSTRY_LOW(PressureType.INDUSTRY, EventSeverity.LOW, 40, "Production capacity is limited", false),
    INDUSTRY_MEDIUM(PressureType.INDUSTRY, EventSeverity.MEDIUM, 70, "Production bottlenecks are slowing growth", false),
    INDUSTRY_HIGH(PressureType.INDUSTRY, EventSeverity.HIGH, 90, "The colony lacks an industrial base", false);

    private final PressureType pressureType;
    private final EventSeverity severity;
    private final int threshold;
    private final String description;
    private final boolean requiresActiveRaid;

    StoryEventType(PressureType pressureType, EventSeverity severity, int threshold, String description, boolean requiresActiveRaid) {
        this.pressureType = pressureType;
        this.severity = severity;
        this.threshold = threshold;
        this.description = description;
        this.requiresActiveRaid = requiresActiveRaid;
    }

    public PressureType pressureType() {
        return pressureType;
    }

    public EventSeverity severity() {
        return severity;
    }

    public int threshold() {
        return threshold;
    }

    public String description() {
        return description;
    }

    public boolean requiresActiveRaid() {
        return requiresActiveRaid;
    }
}
