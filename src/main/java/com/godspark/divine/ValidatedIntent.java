package com.godspark.divine;

import java.util.List;

public record ValidatedIntent(
    DivineIntent intent,
    ValidationResult result,
    List<String> validationNotes
) {
    public static ValidatedIntent approved(DivineIntent intent, List<String> notes) {
        return new ValidatedIntent(intent, ValidationResult.ORACLE_APPROVED, notes);
    }

    public static ValidatedIntent rejected(DivineIntent intent, List<String> notes) {
        return new ValidatedIntent(intent, ValidationResult.REJECTED, notes);
    }

    public boolean isOracleApproved() {
        return result == ValidationResult.ORACLE_APPROVED;
    }

    public boolean isEffectEligible() {
        return result == ValidationResult.EFFECT_ELIGIBLE;
    }

    public boolean isDowngraded() {
        return result == ValidationResult.DOWNGRADED;
    }

    public boolean isRejected() {
        return result == ValidationResult.REJECTED;
    }

    public boolean permitsPressureModifier() {
        return result.permitsPressureModifier();
    }

    public String validationSummary() {
        return String.format("[%s] %s -> %s (%s)",
            result.getDisplayName(),
            intent != null ? intent.intentType().getDisplayName() : "NULL",
            intent != null ? intent.domainDisplayName() : "N/A",
            String.join(", ", validationNotes));
    }
}