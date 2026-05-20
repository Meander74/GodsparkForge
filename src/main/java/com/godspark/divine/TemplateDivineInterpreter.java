package com.godspark.divine;

import com.godspark.GodsparkMod;
import com.godspark.pressure.PressureType;
import com.godspark.prayer.PrayerChannel;
import com.godspark.prayer.PrayerSeed;
import com.godspark.prayer.PrayerType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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

    private static final List<PressureType> FALLBACK_PRIORITY = List.of(
        PressureType.SECURITY, PressureType.FOOD, PressureType.HOUSING,
        PressureType.COMFORT, PressureType.INDUSTRY
    );

    private record DomainKeyword(PressureType domain, String keyword, int weight, boolean prefixAllowed) {}

    private static DomainKeyword dk(PressureType domain, String keyword, int weight) {
        return new DomainKeyword(domain, keyword, weight, keyword.length() >= 4);
    }

    private static final List<DomainKeyword> DOMAIN_KEYWORDS = List.of(
        // SECURITY - action verbs (3)
        dk(PressureType.SECURITY, "protect", 3),
        dk(PressureType.SECURITY, "defend", 3),
        dk(PressureType.SECURITY, "shield", 3),
        // SECURITY - targets (2)
        dk(PressureType.SECURITY, "guard", 2),
        dk(PressureType.SECURITY, "raid", 2),
        dk(PressureType.SECURITY, "enemy", 2),
        dk(PressureType.SECURITY, "monster", 2),
        dk(PressureType.SECURITY, "attack", 2),
        dk(PressureType.SECURITY, "threat", 2),
        dk(PressureType.SECURITY, "danger", 2),
        dk(PressureType.SECURITY, "militia", 2),
        dk(PressureType.SECURITY, "sword", 2),

        // FOOD - action verbs (3)
        dk(PressureType.FOOD, "feed", 3),
        dk(PressureType.FOOD, "grow", 3),
        dk(PressureType.FOOD, "harvest", 3),
        dk(PressureType.FOOD, "nourish", 3),
        // FOOD - targets (2)
        dk(PressureType.FOOD, "food", 2),
        dk(PressureType.FOOD, "hunger", 2),
        dk(PressureType.FOOD, "famine", 2),
        dk(PressureType.FOOD, "crop", 2),
        dk(PressureType.FOOD, "wheat", 2),
        dk(PressureType.FOOD, "farm", 2),
        dk(PressureType.FOOD, "bread", 2),
        dk(PressureType.FOOD, "granary", 2),
        dk(PressureType.FOOD, "granar", 2),

        // HOUSING - action verbs (3)
        dk(PressureType.HOUSING, "shelter", 3),
        dk(PressureType.HOUSING, "settle", 3),
        // HOUSING - targets (2)
        dk(PressureType.HOUSING, "home", 2),
        dk(PressureType.HOUSING, "house", 2),
        dk(PressureType.HOUSING, "roof", 2),
        dk(PressureType.HOUSING, "dwelling", 2),
        dk(PressureType.HOUSING, "hut", 2),
        dk(PressureType.HOUSING, "cabin", 2),
        dk(PressureType.HOUSING, "room", 2),
        // HOUSING - short words (exact match only, len < 4)
        new DomainKeyword(PressureType.HOUSING, "bed", 2, false),

        // COMFORT - action verbs (3)
        dk(PressureType.COMFORT, "comfort", 3),
        dk(PressureType.COMFORT, "soothe", 3),
        dk(PressureType.COMFORT, "calm", 3),
        // COMFORT - nouns (2)
        dk(PressureType.COMFORT, "hope", 2),
        dk(PressureType.COMFORT, "joy", 2),
        dk(PressureType.COMFORT, "peace", 2),
        dk(PressureType.COMFORT, "morale", 2),
        dk(PressureType.COMFORT, "happiness", 2),
        dk(PressureType.COMFORT, "spirit", 2),
        // COMFORT - generic (1)
        dk(PressureType.COMFORT, "heal", 1),
        new DomainKeyword(PressureType.COMFORT, "rest", 1, false),

        // INDUSTRY - action verbs (3)
        dk(PressureType.INDUSTRY, "craft", 3),
        dk(PressureType.INDUSTRY, "forge", 3),
        dk(PressureType.INDUSTRY, "smelt", 3),
        // INDUSTRY - targets (2)
        dk(PressureType.INDUSTRY, "tool", 2),
        dk(PressureType.INDUSTRY, "furnace", 2),
        dk(PressureType.INDUSTRY, "anvil", 2),
        dk(PressureType.INDUSTRY, "workshop", 2),
        dk(PressureType.INDUSTRY, "quarry", 2),
        // INDUSTRY - generic (1)
        dk(PressureType.INDUSTRY, "work", 1),
        dk(PressureType.INDUSTRY, "build", 1)
    );

    static PressureType detectDomainFromKeywords(String text) {
        if (text == null) return null;
        Set<String> tokens = tokenize(text);

        EnumMap<PressureType, Integer> scores = new EnumMap<>(PressureType.class);
        for (DomainKeyword keyword : DOMAIN_KEYWORDS) {
            if (matchesKeyword(tokens, keyword)) {
                scores.merge(keyword.domain(), keyword.weight(), Integer::sum);
            }
        }

        if (scores.isEmpty()) return null;

        int maxScore = -1;
        for (int score : scores.values()) {
            if (score > maxScore) maxScore = score;
        }

        List<PressureType> tied = new ArrayList<>();
        for (var entry : scores.entrySet()) {
            if (entry.getValue() == maxScore) {
                tied.add(entry.getKey());
            }
        }

        if (tied.size() == 1) return tied.get(0);

        for (PressureType fallback : FALLBACK_PRIORITY) {
            if (tied.contains(fallback)) return fallback;
        }

        return tied.get(0);
    }

    private static Set<String> tokenize(String text) {
        Set<String> tokens = new HashSet<>();
        for (String token : text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
            if (!token.isEmpty()) tokens.add(token);
        }
        return tokens;
    }

    private static boolean matchesKeyword(Set<String> tokens, DomainKeyword keyword) {
        String value = keyword.keyword();
        if (tokens.contains(value)) return true;
        if (!keyword.prefixAllowed()) return false;
        for (String token : tokens) {
            if (token.length() > value.length() && token.startsWith(value)) return true;
        }
        return false;
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