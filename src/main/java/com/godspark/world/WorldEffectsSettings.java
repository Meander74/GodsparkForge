package com.godspark.world;

public interface WorldEffectsSettings {
    boolean enabled();
    boolean allowDebugAnswerCommand();
    int cooldownTicks();
    int resistanceDurationTicks();
    int cropPulseAttempts();
    int cropPulseSuccessCap();
    int guardianVigilRadius();
    int greenMercyRadius();
}
