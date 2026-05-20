package com.godspark.world;

import com.godspark.divine.ValidatedIntent;
import com.godspark.divine.IntentType;
import com.godspark.pressure.PressureType;

public enum LightWorldEffect {
    GUARDIANS_VIGIL(PressureType.SECURITY, "Guardian's Vigil"),
    GREENS_MERCY(PressureType.FOOD, "Green Mercy");

    private final PressureType domain;
    private final String displayName;

    LightWorldEffect(PressureType domain, String displayName) {
        this.domain = domain;
        this.displayName = displayName;
    }

    public PressureType domain() { return domain; }
    public String displayName() { return displayName; }

    public static LightWorldEffect fromIntent(ValidatedIntent vi) {
        if (vi == null || !vi.isEffectEligible()) return null;
        if (vi.intent() == null || vi.intent().intentType() != IntentType.BLESS_COLONY) return null;
        if (vi.intent().domain() == null) return null;
        return switch (vi.intent().domain()) {
            case SECURITY -> GUARDIANS_VIGIL;
            case FOOD -> GREENS_MERCY;
            default -> null;
        };
    }
}
