package com.godspark.divine;

import com.godspark.pressure.PressureType;

import java.util.List;
import java.util.Locale;

public record DivineIntent(
    int schemaVersion,
    int colonyId,
    String colonyName,
    IntentType intentType,
    PressureType domain,
    double confidence,
    boolean matchedPublicPrayer,
    String matchedPrayerSourceKey,
    String oracleText,
    List<String> reasonCodes,
    IntentSource source
) {
    private static final int MAX_ORACLE_TEXT_LENGTH = 300;
    private static final int MAX_REASON_CODES = 8;

    public DivineIntent {
        if (schemaVersion <= 0) {
            schemaVersion = 1;
        }
        if (intentType == null) {
            intentType = IntentType.ORACLE_ONLY;
        }
        if (colonyName == null) {
            colonyName = "";
        }
        confidence = Math.max(0.0, Math.min(1.0, confidence));
        if (matchedPrayerSourceKey == null) {
            matchedPrayerSourceKey = "";
        }
        oracleText = sanitizeText(oracleText, MAX_ORACLE_TEXT_LENGTH);
        reasonCodes = reasonCodes == null
            ? List.of()
            : reasonCodes.stream()
                .filter(code -> code != null && !code.isBlank())
                .map(code -> sanitizeReasonCode(code, 64))
                .distinct()
                .limit(MAX_REASON_CODES)
                .toList();
        source = source == null ? IntentSource.TEMPLATE : source;
    }

    public boolean hasDomain() {
        return domain != null;
    }

    public String domainDisplayName() {
        return domain == null ? "Unknown" : domain.getDisplayName();
    }

    public boolean eligibleForFutureAnswer() {
        return matchedPublicPrayer
            && intentType != IntentType.ORACLE_ONLY
            && intentType != IntentType.ANSWER_QUESTION;
    }

    private static String sanitizeText(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String cleaned = value
            .replace('\n', ' ')
            .replace('\r', ' ')
            .replaceAll("\\p{Cntrl}", "")
            .trim();
        if (cleaned.length() > maxLength) {
            return cleaned.substring(0, maxLength);
        }
        return cleaned;
    }

    private static String sanitizeReasonCode(String value, int maxLength) {
        String cleaned = value
            .toUpperCase(Locale.ROOT)
            .replaceAll("[^A-Z0-9_]", "_");
        if (cleaned.length() > maxLength) {
            return cleaned.substring(0, maxLength);
        }
        return cleaned;
    }
}