package com.godspark.prayer;

import com.godspark.personality.PersonalityTrait;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public enum PrayerTone {
    DEVOUT("Devout", 2, List.of("with reverence", "in holy devotion", "faithfully", "in blessed hope")),
    FEARFUL("Fearful", 1, List.of("with trembling voices", "anxiously", "in uneasy whispers", "with quiet dread")),
    DEFIANT("Defiant", 3, List.of("fiercely", "with unbroken resolve", "boldly", "with steadfast courage")),
    MOURNFUL("Mournful", 1, List.of("in sorrow", "with heavy hearts", "lamenting", "in grief")),
    GRATEFUL("Grateful", 0, List.of("with thankful hearts", "joyfully", "in gratitude", "with warm praise")),
    HOPEFUL("Hopeful", 1, List.of("with quiet hope", "patiently", "looking toward dawn", "in gentle faith")),
    STOIC("Stoic", 0, List.of("steadily", "with quiet endurance", "without complaint", "resolutely")),
    URGENT("Urgent", 3, List.of("desperately", "with fervent cries", "in pressing need", "with urgent pleas"));

    private static final Map<PersonalityTrait, PrayerTone> TRAIT_TO_TONE = new EnumMap<>(PersonalityTrait.class);

    static {
        TRAIT_TO_TONE.put(PersonalityTrait.AGGRESSIVE, DEFIANT);
        TRAIT_TO_TONE.put(PersonalityTrait.PEACEFUL, HOPEFUL);
        TRAIT_TO_TONE.put(PersonalityTrait.CAUTIOUS, FEARFUL);
        TRAIT_TO_TONE.put(PersonalityTrait.RESILIENT, STOIC);
        TRAIT_TO_TONE.put(PersonalityTrait.TRADE_FOCUSED, HOPEFUL);
        TRAIT_TO_TONE.put(PersonalityTrait.ISOLATIONIST, FEARFUL);
        TRAIT_TO_TONE.put(PersonalityTrait.EXPANSIONIST, DEFIANT);
        TRAIT_TO_TONE.put(PersonalityTrait.SPIRITUAL, DEVOUT);
        TRAIT_TO_TONE.put(PersonalityTrait.INDUSTRIAL, STOIC);
        TRAIT_TO_TONE.put(PersonalityTrait.COMMUNAL, GRATEFUL);
        TRAIT_TO_TONE.put(PersonalityTrait.AGRARIAN, GRATEFUL);
    }

    private final String displayName;
    private final int intensityMod;
    private final List<String> phrases;

    PrayerTone(String displayName, int intensityMod, List<String> phrases) {
        this.displayName = displayName;
        this.intensityMod = intensityMod;
        this.phrases = phrases;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getIntensityMod() {
        return intensityMod;
    }

    public List<String> getPhrases() {
        return phrases;
    }

    public static PrayerTone fromTrait(PersonalityTrait trait) {
        return TRAIT_TO_TONE.getOrDefault(trait, STOIC);
    }

    public String pickPhrase(int seed) {
        if (phrases.isEmpty()) return "quietly";
        return phrases.get(Math.floorMod(seed, phrases.size()));
    }
}
