package com.godspark.divine;

import com.godspark.GodsparkMod;
import com.godspark.pressure.PressureType;
import com.godspark.prayer.PrayerChannel;
import com.godspark.prayer.PrayerSeed;
import com.godspark.prayer.PrayerType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class TemplateDivineInterpreter {

    private TemplateDivineInterpreter() {}

    static DivineIntent interpret(DivineAnswer answer, DivineAnswerContext context) {
        if (context == null || context.colonySnapshot() == null) {
            return noDataContext(answer);
        }

        String rawText = answer.rawText();
        PressureType domain = detectDomain(rawText, context);
        IntentType intentType = detectIntentType(rawText);

        List<PrayerSeed> publicPrayers = context.publicPrayers();
        boolean hasPublicPrayers = context.hasPublicPrayers();

        if (!hasPublicPrayers) {
            return noPublicPrayerResult(answer, context, domain, rawText);
        }

        PrayerSeed matchingPrayer = findMatchingPublicPrayer(publicPrayers, domain);
        boolean matchedPublicPrayer = matchingPrayer != null;
        String matchedPrayerSourceKey = matchingPrayer != null ? matchingPrayer.sourceKey() : "";

        if (!matchedPublicPrayer) {
            return noMatchingPrayerResult(answer, context, domain, intentType, rawText);
        }

        List<String> reasonCodes = buildReasonCodes(answer, context, domain, intentType, matchedPublicPrayer);
        reasonCodes.add("PUBLIC_PRAYER_MATCHED");
        reasonCodes.add("SACRED_SITE_PRESENT");

        String oracleText = buildOracleText(answer, context, domain, intentType, matchingPrayer);

        return new DivineIntent(
            1,
            answer.colonyId(),
            answer.colonyName(),
            intentType,
            domain,
            1.0,
            matchedPublicPrayer,
            matchedPrayerSourceKey,
            oracleText,
            reasonCodes,
            IntentSource.TEMPLATE
        );
    }

    private static DivineIntent noDataContext(DivineAnswer answer) {
        return new DivineIntent(
            1, answer.colonyId(), answer.colonyName(),
            IntentType.ORACLE_ONLY, null, 0.3,
            false, "",
            "The Spark reaches out, but finds no colony to answer.",
            List.of("NO_COLONY_DATA"),
            IntentSource.TEMPLATE
        );
    }

    private static DivineIntent noPublicPrayerResult(DivineAnswer answer, DivineAnswerContext context,
                                                      PressureType domain, String rawText) {
        IntentType intentType = detectIntentType(rawText);
        if (intentType == IntentType.BLESS_COLONY || intentType == IntentType.CREATE_TRIAL) {
            intentType = IntentType.ORACLE_ONLY;
        }

        List<String> reasonCodes = new ArrayList<>();
        reasonCodes.add("NO_PUBLIC_PRAYER");
        reasonCodes.add("PRIVATE_MURMURS_ONLY");
        reasonCodes.add("SACRED_SITE_REQUIRED");
        reasonCodes.add("DIVINE_ANSWER_ORACLE");

        String oracleText = "The colony's fears remain private whispers. Build a shrine, chapel, or sacred site so their voice may gather.";

        PressureType effectiveDomain = domain;
        if (effectiveDomain == null) {
            effectiveDomain = detectDomainFromPrayers(answer.colonyId());
        }

        return new DivineIntent(
            1, answer.colonyId(), answer.colonyName(),
            intentType, effectiveDomain, 0.4,
            false, "",
            oracleText, reasonCodes,
            IntentSource.TEMPLATE
        );
    }

    private static DivineIntent noMatchingPrayerResult(DivineAnswer answer, DivineAnswerContext context,
                                                         PressureType domain, IntentType intentType, String rawText) {
        if (intentType == IntentType.BLESS_COLONY || intentType == IntentType.CREATE_TRIAL) {
            intentType = IntentType.ORACLE_ONLY;
        }

        List<String> reasonCodes = new ArrayList<>();
        reasonCodes.add("NO_MATCHING_PUBLIC_PRAYER");
        reasonCodes.add("SACRED_SITE_PRESENT");
        reasonCodes.add("DIVINE_ANSWER_ORACLE");

        PressureType effectiveDomain = domain;
        if (effectiveDomain == null) {
            effectiveDomain = detectDomainFromPrayers(answer.colonyId());
        }

        String domainLabel = effectiveDomain != null ? effectiveDomain.getDisplayName() : "Unknown";
        String oracleText = "The Spark hears your words on " + domainLabel.toLowerCase(Locale.ROOT)
            + ", but no public prayer gathers that need yet.";

        return new DivineIntent(
            1, answer.colonyId(), answer.colonyName(),
            intentType, effectiveDomain, 0.5,
            false, "",
            oracleText, reasonCodes,
            IntentSource.TEMPLATE
        );
    }

    // ── Domain Detection ──

    private static PressureType detectDomain(String text, DivineAnswerContext context) {
        PressureType keywordDomain = detectDomainFromKeywords(text);
        if (keywordDomain != null) return keywordDomain;

        return detectDomainFromPrayers(context);
    }

    private static PressureType detectDomainFromKeywords(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("food") || lower.contains("hunger") || lower.contains("bread")
            || lower.contains("crop") || lower.contains("field") || lower.contains("farm")
            || lower.contains("eat") || lower.contains("starv") || lower.contains("granar")) {
            return PressureType.FOOD;
        }
        if (lower.contains("guard") || lower.contains("raid") || lower.contains("protect")
            || lower.contains("dark") || lower.contains("wall") || lower.contains("shield")
            || lower.contains("watch") || lower.contains("defend") || lower.contains("sword")
            || lower.contains("militia")) {
            return PressureType.SECURITY;
        }
        if (lower.contains("home") || lower.contains("roof") || lower.contains("bed")
            || lower.contains("shelter") || lower.contains("house") || lower.contains("residence")
            || lower.contains("dwell")) {
            return PressureType.HOUSING;
        }
        if (lower.contains("sad") || lower.contains("peace") || lower.contains("joy")
            || lower.contains("happy") || lower.contains("rest") || lower.contains("comfort")
            || lower.contains("heal") || lower.contains("hope")) {
            return PressureType.COMFORT;
        }
        if (lower.contains("tool") || lower.contains("work") || lower.contains("build")
            || lower.contains("mine") || lower.contains("industry") || lower.contains("forge")
            || lower.contains("craft") || lower.contains("prod")) {
            return PressureType.INDUSTRY;
        }
        return null;
    }

    private static PressureType detectDomainFromPrayers(DivineAnswerContext context) {
        List<PrayerSeed> publicPrayers = context.publicPrayers();
        if (publicPrayers.isEmpty()) return null;
        return publicPrayers.get(0).pressureType();
    }

    private static PressureType detectDomainFromPrayers(int colonyId) {
        List<PrayerSeed> allPrayers = GodsparkMod.PRAYER_SEED_BANK.getPrayers(colonyId);
        List<PrayerSeed> publicPrayers = allPrayers.stream()
            .filter(PrayerSeed::isPublicPrayer)
            .toList();
        if (publicPrayers.isEmpty()) return null;
        return publicPrayers.get(0).pressureType();
    }

    // ── Intent Detection ──

    private static IntentType detectIntentType(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("what") || lower.contains("why") || lower.contains("how")
            || lower.contains("when") || lower.contains("where") || lower.contains("?")
            || lower.contains("tell me") || lower.contains("explain")) {
            return IntentType.ANSWER_QUESTION;
        }
        if (lower.contains("bless") || lower.contains("protect") || lower.contains("help")
            || lower.contains("feed") || lower.contains("save") || lower.contains("strength")
            || lower.contains("shelter") || lower.contains("watch over") || lower.contains("grant")) {
            return IntentType.BLESS_COLONY;
        }
        if (lower.contains("vow") || lower.contains("promise") || lower.contains("trial")
            || lower.contains("prove") || lower.contains("test") || lower.contains("challenge")
            || lower.contains("endure") || lower.contains("worthy")) {
            return IntentType.CREATE_TRIAL;
        }
        return IntentType.ORACLE_ONLY;
    }

    // ── Reason Codes ──

    private static List<String> buildReasonCodes(DivineAnswer answer, DivineAnswerContext context,
                                                  PressureType domain, IntentType intentType,
                                                  boolean matchedPublicPrayer) {
        List<String> codes = new ArrayList<>();
        codes.add("DIVINE_ANSWER_" + intentType.name());

        if (domain != null) {
            codes.add(domain.name() + "_DOMAIN");
        }

        if (context.pressureSnapshot() != null) {
            int pressureValue = context.pressureSnapshot().values().getOrDefault(domain, 0);
            if (pressureValue >= 70) {
                codes.add(domain.name() + "_PRESSURE_HIGH");
            } else if (pressureValue >= 40) {
                codes.add(domain.name() + "_PRESSURE_MEDIUM");
            }
        }

        if (context.colonySnapshot() != null && context.colonySnapshot().hasActiveRaid()) {
            codes.add("ACTIVE_RAID");
        }

        if (!context.activeEvents().isEmpty()) {
            codes.add("HAS_ACTIVE_EVENTS");
        }

        if (!context.memories().isEmpty()) {
            codes.add("HAS_MEMORIES");
        }

        return codes;
    }

    // ── Oracle Text ──

    private static String buildOracleText(DivineAnswer answer, DivineAnswerContext context,
                                          PressureType domain, IntentType intentType,
                                          PrayerSeed matchingPrayer) {
        String colonyName = answer.colonyName();

        if (intentType == IntentType.ANSWER_QUESTION) {
            return buildQuestionOracle(colonyName, domain, matchingPrayer);
        }
        if (intentType == IntentType.CREATE_TRIAL) {
            return buildTrialOracle(colonyName, domain, matchingPrayer);
        }
        if (intentType == IntentType.BLESS_COLONY) {
            return buildBlessingOracle(colonyName, domain, matchingPrayer);
        }
        return buildOracleOnlyText(colonyName, domain, matchingPrayer);
    }

    private static String buildOracleOnlyText(String colonyName, PressureType domain, PrayerSeed prayer) {
        if (domain == null) {
            return "The Spark stirs, but the colony's need is unclear. Wait and watch.";
        }
        return switch (domain) {
            case FOOD -> "The land listens to " + colonyName + ". Full granaries are not yet promised, but the watchfires see the need.";
            case SECURITY -> "The shadows around " + colonyName + " are known. No shield is woven yet, but the guard stands.";
            case HOUSING -> colonyName + " seeks shelter. The walls are not yet raised, but the stones are watched.";
            case COMFORT -> "Peace waits beyond the horizon for " + colonyName + ". The hearth is not yet lit, but the spark remembers.";
            case INDUSTRY -> "The forge of " + colonyName + " holds its fire. No tool is forged yet, but the anvil is ready.";
        };
    }

    private static String buildQuestionOracle(String colonyName, PressureType domain, PrayerSeed prayer) {
        if (domain == null) {
            return "The Spark listens, but cannot yet speak clearly to " + colonyName + ". The answers gather like dust.";
        }
        return switch (domain) {
            case FOOD -> "The granaries of " + colonyName + " tell their own story. The harvest knows whether it was enough.";
            case SECURITY -> "The walls of " + colonyName + " remember every shadow that passed. The guard knows what watches.";
            case HOUSING -> "Every roof in " + colonyName + " has a tale. The question is whether the walls remember.";
            case COMFORT -> "The streets of " + colonyName + " carry whispers of plenty and want. Listen carefully.";
            case INDUSTRY -> "The forge of " + colonyName + " rings with answers. The work speaks for itself.";
        };
    }

    private static String buildTrialOracle(String colonyName, PressureType domain, PrayerSeed prayer) {
        if (domain == null) {
            return "A trial requires a focus. " + colonyName + " must name what it seeks to prove.";
        }
        return switch (domain) {
            case FOOD -> colonyName + " vows to earn its harvest. The trial of empty hands begins. (Trials are not yet implemented.)";
            case SECURITY -> colonyName + " vows to stand against the dark. The trial of the watchful begins. (Trials are not yet implemented.)";
            case HOUSING -> colonyName + " vows to raise its walls. The trial of the builder begins. (Trials are not yet implemented.)";
            case COMFORT -> colonyName + " vows to find its peace. The trial of patience begins. (Trials are not yet implemented.)";
            case INDUSTRY -> colonyName + " vows to forge its tools. The trial of the worker begins. (Trials are not yet implemented.)";
        };
    }

    private static String buildBlessingOracle(String colonyName, PressureType domain, PrayerSeed prayer) {
        if (domain == null) {
            return "The Spark hears " + colonyName + ", but no miracle is woven yet. The watchfires burn onward.";
        }
        return switch (domain) {
            case FOOD -> "The earth hears " + colonyName + ". Fields may yet yield, but no miracle is woven yet.";
            case SECURITY -> "Steel and courage flow through the watchfires of " + colonyName + ". Stand firm. Miracles are not yet implemented.";
            case HOUSING -> "The walls of " + colonyName + " gather strength. No roof appears yet, but the intention is woven.";
            case COMFORT -> "Peace settles gently over " + colonyName + ". The hearth is not yet lit, but the warmth is promised.";
            case INDUSTRY -> "The forge of " + colonyName + " burns brighter. No tool appears yet, but the fire is stoked.";
        };
    }

    // ── Matching Logic ──

    private static PrayerSeed findMatchingPublicPrayer(List<PrayerSeed> publicPrayers, PressureType domain) {
        if (domain == null) return null;
        for (PrayerSeed seed : publicPrayers) {
            if (seed.pressureType() == domain) {
                return seed;
            }
        }
        return null;
    }
}