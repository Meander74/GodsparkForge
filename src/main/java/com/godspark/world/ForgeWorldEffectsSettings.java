package com.godspark.world;

import com.godspark.GodsparkConfig;

public final class ForgeWorldEffectsSettings implements WorldEffectsSettings {

    public static final ForgeWorldEffectsSettings INSTANCE = new ForgeWorldEffectsSettings();

    private ForgeWorldEffectsSettings() {}

    @Override
    public boolean enabled() { return GodsparkConfig.WORLD_EFFECTS_ENABLED.get(); }

    @Override
    public boolean allowDebugAnswerCommand() { return GodsparkConfig.WORLD_EFFECTS_ALLOW_DEBUG_ANSWER_COMMAND.get(); }

    @Override
    public int cooldownTicks() { return GodsparkConfig.WORLD_EFFECT_COOLDOWN_TICKS.get(); }

    @Override
    public int resistanceDurationTicks() { return GodsparkConfig.WORLD_EFFECT_RESISTANCE_DURATION.get(); }

    @Override
    public int cropPulseAttempts() { return GodsparkConfig.WORLD_EFFECT_CROP_PULSE_ATTEMPTS.get(); }

    @Override
    public int cropPulseSuccessCap() { return GodsparkConfig.WORLD_EFFECT_CROP_PULSE_SUCCESS_CAP.get(); }

    @Override
    public int guardianVigilRadius() { return GodsparkConfig.WORLD_EFFECT_GUARDIAN_RADIUS.get(); }

    @Override
    public int greenMercyRadius() { return GodsparkConfig.WORLD_EFFECT_GREEN_MERCY_RADIUS.get(); }
}
