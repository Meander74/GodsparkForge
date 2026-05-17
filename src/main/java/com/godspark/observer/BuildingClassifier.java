package com.godspark.observer;

import com.godspark.GodsparkMod;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class BuildingClassifier {

    private static final Set<String> LOGGED_UNKNOWNS = new HashSet<>();

    private static final Map<String, BuildingCategory> REGISTRY_MAP = Map.ofEntries(
        // ── FOOD ──
        Map.entry("restaurant", BuildingCategory.FOOD),
        Map.entry("farmer", BuildingCategory.FOOD),
        Map.entry("fisherman", BuildingCategory.FOOD),
        Map.entry("fisher", BuildingCategory.FOOD),
        Map.entry("fishershut", BuildingCategory.FOOD),
        Map.entry("bakery", BuildingCategory.FOOD),
        Map.entry("baker", BuildingCategory.FOOD),
        Map.entry("cook", BuildingCategory.FOOD),
        Map.entry("kitchen", BuildingCategory.FOOD),
        Map.entry("chickenherder", BuildingCategory.FOOD),
        Map.entry("cowboy", BuildingCategory.FOOD),
        Map.entry("shepherd", BuildingCategory.FOOD),
        Map.entry("swineherder", BuildingCategory.FOOD),
        Map.entry("rabbithutch", BuildingCategory.FOOD),
        Map.entry("beekeeper", BuildingCategory.FOOD),
        // ── SECURITY ──
        Map.entry("guardtower", BuildingCategory.SECURITY),
        Map.entry("guard_tower", BuildingCategory.SECURITY),
        Map.entry("barracks", BuildingCategory.SECURITY),
        Map.entry("barrackstower", BuildingCategory.SECURITY),
        Map.entry("barracks_tower", BuildingCategory.SECURITY),
        Map.entry("archery", BuildingCategory.SECURITY),
        Map.entry("combatacademy", BuildingCategory.SECURITY),
        Map.entry("knight", BuildingCategory.SECURITY),
        Map.entry("ranger", BuildingCategory.SECURITY),
        // ── HOUSING ──
        Map.entry("residence", BuildingCategory.HOUSING),
        Map.entry("citizenhut", BuildingCategory.HOUSING),
        Map.entry("citizen_hut", BuildingCategory.HOUSING),
        Map.entry("tavern", BuildingCategory.HOUSING),
        // ── COMFORT ──
        Map.entry("hospital", BuildingCategory.COMFORT),
        Map.entry("library", BuildingCategory.COMFORT),
        Map.entry("university", BuildingCategory.COMFORT),
        Map.entry("school", BuildingCategory.COMFORT),
        Map.entry("nethermine", BuildingCategory.COMFORT),
        Map.entry("enchanter", BuildingCategory.COMFORT),
        Map.entry("alchemist", BuildingCategory.COMFORT),
        Map.entry("glassblower", BuildingCategory.COMFORT),
        Map.entry("postbox", BuildingCategory.COMFORT),
        Map.entry("stash", BuildingCategory.COMFORT),
        Map.entry("florist", BuildingCategory.COMFORT),
        Map.entry("plantation", BuildingCategory.COMFORT),
        // ── INDUSTRY ──
        Map.entry("builder", BuildingCategory.INDUSTRY),
        Map.entry("miner", BuildingCategory.INDUSTRY),
        Map.entry("lumberjack", BuildingCategory.INDUSTRY),
        Map.entry("sawmill", BuildingCategory.INDUSTRY),
        Map.entry("stonemason", BuildingCategory.INDUSTRY),
        Map.entry("stonesmeltery", BuildingCategory.INDUSTRY),
        Map.entry("crusher", BuildingCategory.INDUSTRY),
        Map.entry("sifter", BuildingCategory.INDUSTRY),
        Map.entry("blacksmith", BuildingCategory.INDUSTRY),
        Map.entry("mechanic", BuildingCategory.INDUSTRY),
        Map.entry("smeltery", BuildingCategory.INDUSTRY),
        Map.entry("concretemixer", BuildingCategory.INDUSTRY),
        Map.entry("dyer", BuildingCategory.INDUSTRY),
        Map.entry("fletcher", BuildingCategory.INDUSTRY),
        Map.entry("composter", BuildingCategory.INDUSTRY),
        Map.entry("courier", BuildingCategory.INDUSTRY),
        Map.entry("deliveryman", BuildingCategory.INDUSTRY),
        Map.entry("deliverymen", BuildingCategory.INDUSTRY),
        // ── WAREHOUSE ──
        Map.entry("warehouse", BuildingCategory.WAREHOUSE),
        // ── SACRED ──
        Map.entry("mysticalsite", BuildingCategory.SACRED),
        Map.entry("mystical_site", BuildingCategory.SACRED),
        Map.entry("church", BuildingCategory.SACRED),
        Map.entry("chapel", BuildingCategory.SACRED),
        Map.entry("temple", BuildingCategory.SACRED),
        Map.entry("shrine", BuildingCategory.SACRED),
        Map.entry("monastery", BuildingCategory.SACRED),
        Map.entry("altar", BuildingCategory.SACRED),
        Map.entry("sanctuary", BuildingCategory.SACRED),
        Map.entry("oracle", BuildingCategory.SACRED),
        Map.entry("ritual", BuildingCategory.SACRED),
        Map.entry("rune", BuildingCategory.SACRED),
        Map.entry("totem", BuildingCategory.SACRED),
        Map.entry("spirit", BuildingCategory.SACRED),
        Map.entry("divine", BuildingCategory.SACRED),
        Map.entry("sacred", BuildingCategory.SACRED),
        Map.entry("reliquary", BuildingCategory.SACRED),
        Map.entry("obelisk", BuildingCategory.SACRED),
        // ── OTHER ──
        Map.entry("townhall", BuildingCategory.OTHER)
    );

    private BuildingClassifier() {}

    public static BuildingCategory classify(String buildingTypeKey) {
        if (buildingTypeKey == null || buildingTypeKey.isBlank()) {
            return BuildingCategory.OTHER;
        }

        String normalized = normalizeBuildingKey(buildingTypeKey);

        BuildingCategory mapped = REGISTRY_MAP.get(normalized);
        if (mapped != null) {
            return mapped;
        }

        BuildingCategory keywordMatch = classifyByKeyword(normalized);
        if (keywordMatch != BuildingCategory.OTHER) {
            return keywordMatch;
        }

        logUnknown(normalized);
        return BuildingCategory.OTHER;
    }

    public static boolean matches(String buildingTypeKey, BuildingCategory... categories) {
        BuildingCategory cat = classify(buildingTypeKey);
        for (BuildingCategory c : categories) {
            if (cat == c) return true;
        }
        return false;
    }

    public static boolean isGatheringPlace(String buildingTypeKey) {
        String key = normalizeBuildingKey(buildingTypeKey);
        return key.contains("townhall")
            || key.contains("tavern")
            || key.contains("library")
            || key.contains("school")
            || key.contains("university")
            || key.contains("hospital")
            || key.contains("graveyard")
            || key.contains("memorial");
    }

    static String normalizeBuildingKey(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }

        String normalized = raw.toLowerCase(Locale.ROOT).trim();

        int namespaceIndex = normalized.indexOf(':');
        if (namespaceIndex >= 0 && namespaceIndex < normalized.length() - 1) {
            normalized = normalized.substring(namespaceIndex + 1);
        }

        return normalized
            .replace("-", "_")
            .replace(" ", "_");
    }

    private static BuildingCategory classifyByKeyword(String key) {
        String lower = key.toLowerCase(Locale.ROOT);

        if (lower.contains("church") || lower.contains("chapel")
            || lower.contains("temple") || lower.contains("shrine")
            || lower.contains("monastery") || lower.contains("mystical")
            || lower.contains("altar") || lower.contains("sanctuary")
            || lower.contains("oracle") || lower.contains("ritual")
            || lower.contains("rune") || lower.contains("totem")
            || lower.contains("spirit") || lower.contains("divine")
            || lower.contains("sacred") || lower.contains("reliquary")
            || lower.contains("obelisk")) {
            return BuildingCategory.SACRED;
        }
        if (lower.contains("warehouse")) return BuildingCategory.WAREHOUSE;
        if (lower.contains("guard") || lower.contains("barracks")
            || lower.contains("knight") || lower.contains("ranger")
            || lower.contains("combat") || lower.contains("archery")) {
            return BuildingCategory.SECURITY;
        }
        if (lower.contains("restaurant") || lower.contains("farmer")
            || lower.contains("fisher") || lower.contains("bakery")
            || lower.contains("baker") || lower.contains("cook")
            || lower.contains("kitchen") || lower.contains("herder")
            || lower.contains("cowboy") || lower.contains("shepherd")
            || lower.contains("swine") || lower.contains("rabbit")
            || lower.contains("beekeeper")) {
            return BuildingCategory.FOOD;
        }
        if (lower.contains("builder") || lower.contains("miner")
            || lower.contains("lumber") || lower.contains("sawmill")
            || lower.contains("mason") || lower.contains("smelter")
            || lower.contains("crusher") || lower.contains("sifter")
            || lower.contains("blacksmith") || lower.contains("mechanic")
            || lower.contains("composter") || lower.contains("courier")
            || lower.contains("delivery") || lower.contains("concrete")
            || lower.contains("dyer") || lower.contains("fletcher")) {
            return BuildingCategory.INDUSTRY;
        }
        if (lower.contains("residence") || lower.contains("citizen")
            || lower.contains("hut") || lower.contains("tavern")
            || lower.contains("housing")) {
            return BuildingCategory.HOUSING;
        }
        if (lower.contains("hospital") || lower.contains("library")
            || lower.contains("university") || lower.contains("school")
            || lower.contains("enchant") || lower.contains("alchemist")
            || lower.contains("glass") || lower.contains("postbox")
            || lower.contains("stash") || lower.contains("florist")
            || lower.contains("plantation")) {
            return BuildingCategory.COMFORT;
        }

        return BuildingCategory.OTHER;
    }

    private static void logUnknown(String key) {
        if (LOGGED_UNKNOWNS.add(key)) {
            GodsparkMod.LOGGER.warn("[Godspark Observer] Unknown MineColonies building type: {}", key);
        }
    }
}