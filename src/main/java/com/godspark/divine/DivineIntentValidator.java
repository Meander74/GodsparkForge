package com.godspark.divine;

import com.godspark.memory.ColonyMemory;
import com.godspark.memory.MemoryType;
import com.godspark.prayer.PrayerSeed;
import com.godspark.pressure.PressureType;
import com.godspark.story.EventRecord;
import com.godspark.story.EventSeverity;

import java.util.ArrayList;
import java.util.List;

public final class DivineIntentValidator {

    private DivineIntentValidator() {}

    public static ValidatedIntent validate(DivineIntent intent, DivineAnswerContext context) {
        if (intent == null) {
            return ValidatedIntent.rejected(null, List.of("NULL_INTENT"));
        }

        List<String> notes = new ArrayList<>();

        if (intent.intentType() == IntentType.ORACLE_ONLY || intent.intentType() == IntentType.ANSWER_QUESTION) {
            notes.add(intent.intentType().name() + "_AUTO_APPROVED");
            return new ValidatedIntent(intent, ValidationResult.ORACLE_APPROVED, notes);
        }

        if (context == null) {
            return downgrade(intent, "NO_CONTEXT_AVAILABLE", new ArrayList<>());
        }

        List<String> domainNotes = new ArrayList<>();
        DivineIntent domainAware = ensureDomainIfInferable(intent, context, domainNotes);
        notes.addAll(domainNotes);

        DivineIntent corrected = recalculatePrayerMatch(domainAware, context);

        return switch (corrected.intentType()) {
            case BLESS_COLONY -> validateBlessColony(corrected, context, notes);
            case CREATE_TRIAL -> {
                notes.add("CREATE_TRIAL_NARRATIVE_ONLY");
                notes.add("TRIAL_EFFECTS_NOT_IMPLEMENTED");
                yield new ValidatedIntent(corrected, ValidationResult.ORACLE_APPROVED, notes);
            }
            default -> new ValidatedIntent(corrected, ValidationResult.ORACLE_APPROVED, notes);
        };
    }

    private static ValidatedIntent validateBlessColony(DivineIntent intent, DivineAnswerContext context,
                                                          List<String> notes) {
        if (!context.hasPublicPrayers()) {
            notes.add("NO_ACTIVE_PRAYERS");
            return downgrade(intent, "NO_ACTIVE_PRAYERS", notes);
        }

        if (!context.hasSacredPrayers()) {
            notes.add("PUBLIC_PRAYERS_NOT_SACRED");
            notes.add("COMMONS_PRAYER_NO_MIRACLE");
            return downgrade(intent, "NO_SACRED_PRAYER_FOR_MIRACLE", notes);
        }

        PrayerSeed matchingPrayer = findMatchingSacredPrayer(context, intent);
        if (matchingPrayer == null) {
            notes.add("NO_PRAYER_MATCH_DOMAIN");
            notes.add("BLESS_REQUIRES_MATCHING_SACRED_PRAYER");
            return downgrade(intent, "NO_MATCHING_SACRED_PRAYER", notes);
        }

        DivineIntent corrected = new DivineIntent(
            intent.schemaVersion(), intent.colonyId(), intent.colonyName(),
            intent.intentType(), intent.domain(), intent.confidence(),
            true, matchingPrayer.sourceKey(),
            intent.oracleText(), intent.reasonCodes(), intent.source()
        );

        notes.add("BLESS_COLONY_VALIDATED");
        notes.add("PRAYER_INTENSITY_" + matchingPrayer.intensity());
        notes.add("MATCHED_PRAYER_" + matchingPrayer.sourceKey());
        return new ValidatedIntent(corrected, ValidationResult.EFFECT_ELIGIBLE, notes);
    }

    private static ValidatedIntent downgrade(DivineIntent intent, String reason, List<String> notes) {
        notes.add(reason);
        notes.add("DOWNGRADED_TO_ORACLE_ONLY");

        DivineIntent downgraded = new DivineIntent(
            intent.schemaVersion(),
            intent.colonyId(),
            intent.colonyName(),
            IntentType.ORACLE_ONLY,
            intent.domain(),
            intent.confidence() * 0.5,
            false,
            "",
            "The Spark hears, but the colony's need is not yet gathered. Only an oracle may speak.",
            intent.reasonCodes(),
            intent.source()
        );

        return new ValidatedIntent(downgraded, ValidationResult.DOWNGRADED, notes);
    }

    private static DivineIntent ensureDomainIfInferable(DivineIntent intent, DivineAnswerContext context,
                                                         List<String> notes) {
        if (intent.hasDomain()) {
            return intent;
        }

        PressureType inferred = inferDomainFromPublicPrayers(context);
        if (inferred == null) {
            notes.add("NO_DOMAIN_INFERRED");
            return intent;
        }

        notes.add("DOMAIN_INFERRED_FROM_PUBLIC_PRAYER");

        return new DivineIntent(
            intent.schemaVersion(),
            intent.colonyId(),
            intent.colonyName(),
            intent.intentType(),
            inferred,
            Math.min(1.0, intent.confidence() * 0.8),
            false,
            "",
            intent.oracleText(),
            intent.reasonCodes(),
            intent.source()
        );
    }

    private static PressureType inferDomainFromPublicPrayers(DivineAnswerContext context) {
        if (context == null || !context.hasPublicPrayers()) {
            return null;
        }

        PrayerSeed strongest = null;
        boolean tie = false;

        for (PrayerSeed seed : context.publicPrayers()) {
            if (strongest == null || seed.intensity() > strongest.intensity()) {
                strongest = seed;
                tie = false;
            } else if (seed.intensity() == strongest.intensity()) {
                tie = true;
            }
        }

        if (strongest == null || tie) {
            return null;
        }

        return strongest.pressureType();
    }

    private static DivineIntent recalculatePrayerMatch(DivineIntent intent, DivineAnswerContext context) {
        if (context == null || !intent.hasDomain() || !context.hasPublicPrayers()) {
            if (intent.matchedPublicPrayer() || !intent.matchedPrayerSourceKey().isEmpty()) {
                return new DivineIntent(
                    intent.schemaVersion(), intent.colonyId(), intent.colonyName(),
                    intent.intentType(), intent.domain(), intent.confidence(),
                    false, "",
                    intent.oracleText(), intent.reasonCodes(), intent.source()
                );
            }
            return intent;
        }

        PrayerSeed prayer = findMatchingPublicPrayer(context, intent);
        boolean matched = prayer != null;
        String sourceKey = prayer != null ? prayer.sourceKey() : "";

        if (intent.matchedPublicPrayer() != matched || !intent.matchedPrayerSourceKey().equals(sourceKey)) {
            return new DivineIntent(
                intent.schemaVersion(), intent.colonyId(), intent.colonyName(),
                intent.intentType(), intent.domain(), intent.confidence(),
                matched, sourceKey,
                intent.oracleText(), intent.reasonCodes(), intent.source()
            );
        }

        return intent;
    }

    private static boolean hasCrisisEvent(DivineAnswerContext context, DivineIntent intent) {
        if (context.activeEvents().isEmpty()) return false;
        if (!intent.hasDomain()) return !context.activeEvents().isEmpty();

        return context.activeEvents().stream()
            .anyMatch(r -> r.event().pressureType() == intent.domain());
    }

    private static PrayerSeed findMatchingPublicPrayer(DivineAnswerContext context, DivineIntent intent) {
        if (!context.hasPublicPrayers()) return null;
        if (!intent.hasDomain()) return null;

        for (PrayerSeed seed : context.publicPrayers()) {
            if (seed.pressureType() == intent.domain()) {
                return seed;
            }
        }
        return null;
    }

    private static PrayerSeed findMatchingSacredPrayer(DivineAnswerContext context, DivineIntent intent) {
        if (!context.hasSacredPrayers()) return null;
        if (!intent.hasDomain()) return null;

        for (PrayerSeed seed : context.publicPrayers()) {
            if (seed.isMiracleEligible() && seed.pressureType() == intent.domain()) {
                return seed;
            }
        }
        return null;
    }

    private static boolean hasTriumphMemory(DivineAnswerContext context) {
        return context.memories().stream()
            .anyMatch(m -> m.memoryType() == MemoryType.TRIUMPH);
    }
}