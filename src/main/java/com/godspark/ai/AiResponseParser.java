package com.godspark.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class AiResponseParser {

    private static final int MAX_REFLECTION_LEN = 500;
    private static final int MAX_ORACLE_LEN = 200;
    private static final int MAX_REASON_CODES = 8;
    private static final int MAX_REASON_CODE_LEN = 48;
    private static final int MAX_TAGS = 8;
    private static final int MAX_TAG_LEN = 32;

    private AiResponseParser() {}

    static AiReflection parse(String json, int colonyId, String colonyName) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            String dominantPressure = getString(root, "dominantPressure", "Unknown");
            String mood = getString(root, "mood", "Stable");
            int intensity = getInt(root, "intensity", 0, 0, 100);
            String reflection = limit(getString(root, "reflection", ""), MAX_REFLECTION_LEN);
            String oracleText = limit(getString(root, "oracleText", ""), MAX_ORACLE_LEN);
            double confidence = getDouble(root, "confidence", 0.5, 0.0, 1.0);
            List<String> reasonCodes = getStringList(root, "reasonCodes", MAX_REASON_CODES, MAX_REASON_CODE_LEN);
            List<String> tags = getStringList(root, "tags", MAX_TAGS, MAX_TAG_LEN);

            if (reflection.isBlank()) {
                throw new IllegalArgumentException("AI response reflection is empty");
            }

            dominantPressure = normalizePressure(dominantPressure);
            mood = normalizeMood(mood);

            return new AiReflection(
                1,
                colonyId,
                colonyName,
                dominantPressure,
                mood,
                intensity,
                reflection.trim(),
                oracleText.trim(),
                reasonCodes,
                tags,
                confidence,
                "AI"
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse AI JSON reflection: " + e.getMessage(), e);
        }
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

    private static int getInt(JsonObject obj, String key, int fallback, int min, int max) {
        JsonElement elem = obj.get(key);
        if (elem == null || elem.isJsonNull()) return fallback;
        try {
            int val = elem.getAsInt();
            return Math.max(min, Math.min(max, val));
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

    private static String normalizePressure(String raw) {
        String trimmed = raw.trim();
        String upper = trimmed.toUpperCase(Locale.ROOT);
        for (com.godspark.pressure.PressureType pt : com.godspark.pressure.PressureType.values()) {
            if (pt.name().equals(upper) || pt.getDisplayName().equalsIgnoreCase(trimmed)) {
                return pt.getDisplayName();
            }
        }
        return "Unknown";
    }

    private static String normalizeMood(String raw) {
        String[] valid = {"Crisis", "Uneasy", "Anxious", "Concerned", "Stable",
            "Hopeful", "Relieved", "Desperate", "Fearful", "Wary", "Cautious", "Despairing"};
        for (String v : valid) {
            if (v.equalsIgnoreCase(raw)) return v;
        }
        String lower = raw.toLowerCase(Locale.ROOT);
        if (lower.contains("crisis")) return "Crisis";
        if (lower.contains("anxious") || lower.contains("anxiety")) return "Anxious";
        if (lower.contains("uneasy") || lower.contains("worried")) return "Uneasy";
        if (lower.contains("concern")) return "Concerned";
        if (lower.contains("stable") || lower.contains("calm") || lower.contains("peace")) return "Stable";
        if (lower.contains("hope") || lower.contains("optimis")) return "Hopeful";
        if (lower.contains("relie")) return "Relieved";
        if (lower.contains("desper") || lower.contains("despair")) return "Desperate";
        if (lower.contains("fear")) return "Fearful";
        if (lower.contains("wary") || lower.contains("watchful") || lower.contains("guarded")) return "Wary";
        if (lower.contains("cautious") || lower.contains("caution")) return "Cautious";
        return "Stable";
    }

    private static String limit(String text, int maxLen) {
        if (text == null) return "";
        String trimmed = text.trim();
        return trimmed.length() <= maxLen ? trimmed : trimmed.substring(0, maxLen);
    }
}
