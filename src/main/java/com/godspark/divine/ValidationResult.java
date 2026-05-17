package com.godspark.divine;

public enum ValidationResult {
    ORACLE_APPROVED("Oracle Approved"),
    EFFECT_ELIGIBLE("Effect Eligible"),
    REJECTED("Rejected"),
    DOWNGRADED("Downgraded");

    private final String displayName;

    ValidationResult(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean permitsPressureModifier() {
        return this == EFFECT_ELIGIBLE;
    }
}