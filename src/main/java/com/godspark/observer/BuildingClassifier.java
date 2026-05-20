package com.godspark.observer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class BuildingClassifier {

    private static final Logger LOGGER = LogManager.getLogger("GodsparkBuilding");
    private static final Set<String> LOGGED_UNKNOWNS = new HashSet<>();

    private static final String[] SACRED_TOKENS = {
        "church", "chapel", "temple", "shrine", "monastery", "mystical",
        "altar", "sanctuary", "oracle", "ritual", "rune", "totem",
        "spirit", "divine", "sacred", "reliquary", "obelisk"
    };
    private static final String[] WAREHOUSE_TOKENS = {"warehouse"};
    private static final String[] SECURITY_TOKENS = {
        "guard", "barracks", "knight", "ranger", "combat", "archery"
    };
    private static final String[] FOOD_TOKENS = {
        "restaurant", "farmer", "fisher", "bakery", "baker", "cook",
        "kitchen", "herder", "cowboy", "shepherd", "swine", "rabbit", "beekeeper"
    };
    private static final String[] INDUSTRY_TOKENS = {
        "builder", "miner", "lumber", "sawmill", "mason", "smelter",
        "crusher", "sifter", "blacksmith", "mechanic", "composter",
        "courier", "delivery", "concrete", "dyer", "fletcher"
    };
    private static final String[] HOUSING_TOKENS = {
        "residence", "citizen", "hut", "tavern", "housing"
    };
    private static final String[] COMFORT_TOKENS = {
        "hospital", "library", "university", "school", "enchant",
        "alchemist", "glass", "postbox", "stash", "florist", "plantation"
    };

    private static final Map<String, BuildingCategory> REGISTRY_MAP = Map.ofEntries(
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
        Map.entry("guardtower", BuildingCategory.SECURITY),
        Map.entry("guard_tower", BuildingCategory.SECURITY),
        Map.entry("barracks", BuildingCategory.SECURITY),
        Map.entry("barrackstower", BuildingCategory.SECURITY),
        Map.entry("barracks_tower", BuildingCategory.SECURITY),
        Map.entry("archery", BuildingCategory.SECURITY),
        Map.entry("combatacademy", BuildingCategory.SECURITY),
        Map.entry("knight", BuildingCategory.SECURITY),
        Map.entry("ranger", BuildingCategory.SECURITY),
        Map.entry("residence", BuildingCategory.HOUSING),
        Map.entry("citizenhut", BuildingCategory.HOUSING),
        Map.entry("citizen_hut", BuildingCategory.HOUSING),
        Map.entry("tavern", BuildingCategory.HOUSING),
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
        Map.entry("warehouse", BuildingCategory.WAREHOUSE),
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

        if (matchesAny(lower, SACRED_TOKENS)) return BuildingCategory.SACRED;
        if (matchesAny(lower, WAREHOUSE_TOKENS)) return BuildingCategory.WAREHOUSE;
        if (matchesAny(lower, SECURITY_TOKENS)) return BuildingCategory.SECURITY;
        if (matchesAny(lower, FOOD_TOKENS)) return BuildingCategory.FOOD;
        if (matchesAny(lower, INDUSTRY_TOKENS)) return BuildingCategory.INDUSTRY;
        if (matchesAny(lower, HOUSING_TOKENS)) return BuildingCategory.HOUSING;
        if (matchesAny(lower, COMFORT_TOKENS)) return BuildingCategory.COMFORT;

        return BuildingCategory.OTHER;
    }

    private static boolean matchesAny(String text, String[] tokens) {
        for (String token : tokens) {
            if (text.contains(token)) return true;
        }
        return false;
    }

    private static void logUnknown(String key) {
        if (LOGGED_UNKNOWNS.add(key)) {
            LOGGER.warn("[Godspark Observer] Unknown MineColonies building type: {}", key);
        }
    }
}
