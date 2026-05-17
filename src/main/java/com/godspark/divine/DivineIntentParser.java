package com.godspark.divine;

import com.godspark.pressure.PressureType;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class DivineIntentParser {

    private static final int MAX_ORACLE_LEN = 300;
    private static final int MAX_REASON_CODES = 8;
    private static final int MAX_REASON_CODE_LEN = 48;

    private DivineIntentParser() {}

    static DivineIntent parse(String json, int colonyId, String colonyName) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            IntentType intentType = parseIntentType(root, "intentType");
            PressureType domain = parseDomain(root, "domain");
            double confidence = getDouble(root, "confidence", 0.5, 0.0, 1.0);
            String oracleText = limit(getString(root, "oracleText", ""), MAX_ORACLE_LEN);
            List<String> reasonCodes = getStringList(root, "reasonCodes", MAX_REASON_CODES, MAX_REASON_CODE_LEN);

            if (oracleText.isBlank()) {
                oracleText = "The Spark hears, but nothing resolves.";
            }

            return new DivineIntent(
                1,
                colonyId,
                colonyName,
                intentType,
                domain,
                confidence,
                false,
                "",
                oracleText,
                reasonCodes,
                IntentSource.AI
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse AI divine intent JSON: " + e.getMessage(), e);
        }
    }

    private static IntentType parseIntentType(JsonObject obj, String key) {
        String raw = getString(obj, key, "ORACLE_ONLY").toUpperCase(Locale.ROOT).trim();
        try {
            return IntentType.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return IntentType.ORACLE_ONLY;
        }
    }

    private static PressureType parseDomain(JsonObject obj, String key) {
        String raw = getString(obj, key, "").toUpperCase(Locale.ROOT).trim();
        if (raw.isBlank()) {
            return null;
        }
        for (PressureType pt : PressureType.values()) {
            if (pt.name().equals(raw) || pt.getDisplayName().equalsIgnoreCase(raw)) {
                return pt;
            }
        }
        return null;
    }

    private static String getString(JsonObject obj, String key, String fallback) {
        JsonElement elem = obj.get(key);
        if (elem == null || elem.isJsonNull()) return fallback;
        try {
            return elem.getAsString();
        } catch (Exception e) {
            return fallback;
        }
    }

    private static double getDouble(JsonObject obj, String key, double fallback, double min, double max) {
        JsonElement elem = obj.get(key);
        if (elem == null || elem.isJsonNull()) return fallback;
        try {
            double val = elem.getAsDouble();
            return Math.max(min, Math.min(max, val));
        } catch (Exception e) {
            return fallback;
        }
    }

    private static List<String> getStringList(JsonObject obj, String key, int maxItems, int maxLen) {
        List<String> result = new ArrayList<>();
        JsonElement elem = obj.get(key);
        if (elem == null || !elem.isJsonArray()) return result;
        JsonArray arr = elem.getAsJsonArray();
        for (JsonElement item : arr) {
            if (result.size() >= maxItems) break;
            try {
                String s = limit(item.getAsString(), maxLen);
                if (!s.isBlank() && !result.contains(s)) result.add(s);
            } catch (Exception ignored) {}
        }
        return result;
    }

    private static String limit(String text, int maxLen) {
        if (text == null) return "";
        String trimmed = text.trim();
        return trimmed.length() <= maxLen ? trimmed : trimmed.substring(0, maxLen);
    }
}