package com.godspark.observer;

import com.godspark.GodsparkMod;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ColonyObserver {
    private static final boolean REFLECTION_DIAGNOSTICS = false;

    private final Map<Integer, ObservedColony> observedColonies = new HashMap<>();
    private final Set<String> dumpedReflectionClasses = new HashSet<>();

    public void scan(MinecraftServer server) {
        if (!ModList.get().isLoaded("minecolonies")) {
            return;
        }

        try {
            IColonyManager colonyManager = com.minecolonies.api.IMinecoloniesAPI.getInstance().getColonyManager();
            if (colonyManager == null) {
                GodsparkMod.LOGGER.warn("[Godspark Observer] ColonyManager is null");
                return;
            }

            long gameTick = server.getTickCount();
            Set<Integer> seenThisTick = new HashSet<>();

            for (Level level : server.getAllLevels()) {
                List<IColony> colonies = colonyManager.getColonies(level);
                if (colonies == null) continue;

                for (IColony colony : colonies) {
                    try {
                        int colonyId = colony.getID();
                        seenThisTick.add(colonyId);
                        String name = colony.getName();
                        if (name == null || name.isBlank()) {
                            name = "Colony #" + colonyId;
                        }

                        if (REFLECTION_DIAGNOSTICS) {
                            dumpMethodsOnce("Colony", colony, "citizen", "building", "raid", "attack", "happy");
                        }

                        int citizenCount = getSafeCitizenCount(colony);
                        int buildingCount = getSafeBuildingCount(colony);
                        int warehouseCount = getSafeWarehouseCount(colony);

                        ColonySnapshot snapshot = new ColonySnapshot(
                            colonyId,
                            name,
                            colony.getCenter(),
                            citizenCount,
                            buildingCount,
                            warehouseCount,
                            getSafeHappiness(colony),
                            getSafeGuardCount(colony),
                            getSafeFoodBuildingCount(colony),
                            getSafeHousingCapacity(colony),
                            getSafeIndustryBuildingCount(colony),
                            getSafeSacredBuildingCount(colony),
                            getSafeGatheringBuildingCount(colony),
                            getSafeHasActiveRaid(colony),
                            colony.getDimension(),
                            gameTick
                        );

                        observedColonies
                            .computeIfAbsent(colonyId, id -> new ObservedColony(id))
                            .addSnapshot(snapshot);

                        GodsparkMod.LOGGER.info("[Godspark Observer] Scanned colony #{}: {} | citizens={} | buildings={}",
                            colonyId, name, citizenCount, buildingCount);

                    } catch (Exception e) {
                        GodsparkMod.LOGGER.warn("Failed to snapshot colony: {}", e.getMessage(), e);
                    }
                }
            }

            observedColonies.keySet().removeIf(id -> !seenThisTick.contains(id));

            if (!observedColonies.isEmpty()) {
                GodsparkMod.LOGGER.debug("ColonyObserver scanned {} colonies", observedColonies.size());
            }
        } catch (Throwable t) {
            GodsparkMod.LOGGER.warn("Failed to scan MineColonies colonies", t);
        }
    }

    /* ==================== REFLECTION HELPERS ==================== */

    private Object invokeNoArg(Object target, String methodName) {
        if (target == null) return null;

        Method method = findNoArgMethod(target.getClass(), methodName);
        if (method == null) return null;

        try {
            method.setAccessible(true);
            return method.invoke(target);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            GodsparkMod.LOGGER.debug(
                "[Godspark Observer] Invocation failed: {}.{}(): {}",
                target.getClass().getName(), methodName, cause.toString()
            );
        } catch (Exception e) {
            GodsparkMod.LOGGER.debug(
                "[Godspark Observer] Reflection failed: {}.{}(): {}",
                target.getClass().getName(), methodName, e.toString()
            );
        }
        return null;
    }

    private Object invokeAnyNoArg(Object target, String... methodNames) {
        if (target == null) return null;

        for (String methodName : methodNames) {
            Object result = invokeNoArg(target, methodName);
            if (result != null) return result;
        }
        return null;
    }

    private Method findNoArgMethod(Class<?> type, String methodName) {
        for (Method method : type.getMethods()) {
            if (method.getParameterCount() == 0 && method.getName().equals(methodName)) {
                return method;
            }
        }

        Class<?> current = type;
        while (current != null) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getParameterCount() == 0 && method.getName().equals(methodName)) {
                    return method;
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private Integer asNonNegativeInt(Object value) {
        if (value instanceof Number number) {
            return Math.max(0, number.intValue());
        }
        return null;
    }

    private Double asFiniteDouble(Object value) {
        if (value instanceof Number number) {
            double result = number.doubleValue();
            if (!Double.isNaN(result) && !Double.isInfinite(result)) {
                return result;
            }
        }
        return null;
    }

    private Boolean asBoolean(Object value) {
        if (value instanceof Boolean bool) return bool;
        return null;
    }

    private int countLike(Object value) {
        if (value == null) return -1;

        if (value instanceof Map<?, ?> map) return map.size();
        if (value instanceof Collection<?> collection) return collection.size();

        if (value instanceof Iterable<?> iterable) {
            int count = 0;
            for (Object ignored : iterable) count++;
            return count;
        }

        if (value.getClass().isArray()) return Array.getLength(value);

        Object size = invokeAnyNoArg(value, "size", "getSize", "count", "getCount");
        Integer parsed = asNonNegativeInt(size);
        if (parsed != null) return parsed;

        return -1;
    }

    private void dumpMethodsOnce(String label, Object target, String... keywords) {
        if (!REFLECTION_DIAGNOSTICS || target == null) return;

        String className = target.getClass().getName();
        String dumpKey = label + ":" + className;

        if (!dumpedReflectionClasses.add(dumpKey)) return;

        GodsparkMod.LOGGER.info("[Godspark Observer] {} runtime class: {}", label, className);

        for (Method method : target.getClass().getMethods()) {
            if (method.getParameterCount() != 0) continue;

            boolean matches = keywords.length == 0;
            String methodName = method.getName().toLowerCase(Locale.ROOT);
            for (String keyword : keywords) {
                if (methodName.contains(keyword.toLowerCase(Locale.ROOT))) {
                    matches = true;
                    break;
                }
            }

            if (matches) {
                GodsparkMod.LOGGER.info(
                    "[Godspark Observer]   {}() -> {}",
                    method.getName(), method.getReturnType().getName()
                );
            }
        }
    }

    /* ==================== CORE METRICS ==================== */

    private int getSafeCitizenCount(IColony colony) {
        try {
            Object citizenManager = invokeAnyNoArg(colony, "getCitizenManager");
            if (citizenManager == null) {
                GodsparkMod.LOGGER.warn("[Godspark Observer] Could not get citizen manager for colony {}", colony.getID());
                return 0;
            }

            dumpMethodsOnce("CitizenManager", citizenManager, "citizen", "count", "max");

            Object directCount = invokeAnyNoArg(citizenManager, "getCurrentCitizenCount");
            Integer parsed = asNonNegativeInt(directCount);
            if (parsed != null) return parsed;

            Object citizens = invokeAnyNoArg(citizenManager, "getCitizens");
            int count = countLike(citizens);
            if (count >= 0) return count;

            GodsparkMod.LOGGER.warn(
                "[Godspark Observer] Could not count citizens for colony {}. CitizenManager class={}",
                colony.getID(), citizenManager.getClass().getName()
            );
            return 0;
        } catch (Exception e) {
            GodsparkMod.LOGGER.warn("[Godspark Observer] Could not get citizen count for colony {}", colony.getID(), e);
            return 0;
        }
    }

    private int getSafeHousingCapacity(IColony colony) {
        try {
            Object citizenManager = invokeAnyNoArg(colony, "getCitizenManager");
            if (citizenManager == null) return 0;

            Object maxCitizens = invokeAnyNoArg(citizenManager, "getMaxCitizens");
            Integer parsed = asNonNegativeInt(maxCitizens);
            if (parsed != null) return parsed;

            Object potentialMax = invokeAnyNoArg(citizenManager, "getPotentialMaxCitizens");
            Integer parsedPotential = asNonNegativeInt(potentialMax);
            if (parsedPotential != null) return parsedPotential;

            return 0;
        } catch (Exception e) {
            GodsparkMod.LOGGER.warn("[Godspark Observer] Could not get housing capacity for colony {}", colony.getID(), e);
            return 0;
        }
    }

    private int getSafeBuildingCount(IColony colony) {
        try {
            Object buildingManager = getBuildingManager(colony);
            if (buildingManager == null) {
                GodsparkMod.LOGGER.warn("[Godspark Observer] Could not get building manager for colony {}", colony.getID());
                return 0;
            }

            dumpMethodsOnce("BuildingManager", buildingManager, "building", "structure", "count", "size");

            Object buildings = invokeAnyNoArg(buildingManager, "getBuildings");
            int count = countLike(buildings);
            if (count >= 0) return count;

            GodsparkMod.LOGGER.warn(
                "[Godspark Observer] Could not count buildings for colony {}. BuildingManager class={}",
                colony.getID(), buildingManager.getClass().getName()
            );
            return 0;
        } catch (Exception e) {
            GodsparkMod.LOGGER.warn("[Godspark Observer] Could not get building count for colony {}", colony.getID(), e);
            return 0;
        }
    }

    private Object getBuildingManager(IColony colony) {
        return invokeAnyNoArg(
            colony,
            "getServerBuildingManager",
            "getCommonBuildingManager",
            "getBuildingManager"
        );
    }

    private double getSafeHappiness(IColony colony) {
        try {
            Object happiness = invokeAnyNoArg(colony, "getOverallHappiness");
            Double parsed = asFiniteDouble(happiness);
            if (parsed != null) {
                return normalizeHappiness(parsed);
            }

            return 7.0;
        } catch (Exception e) {
            GodsparkMod.LOGGER.debug("[Godspark Observer] Could not get happiness for colony {}", colony.getID(), e);
            return 7.0;
        }
    }

    private static double normalizeHappiness(double happiness) {
        if (Double.isNaN(happiness) || Double.isInfinite(happiness)) {
            return 7.0;
        }

        if (happiness >= 0.0 && happiness <= 1.0) {
            return happiness * 10.0;
        }

        if (happiness < 0.0) {
            return 0.0;
        }

        if (happiness > 10.0) {
            return 10.0;
        }

        return happiness;
    }

    private boolean getSafeHasActiveRaid(IColony colony) {
        try {
            Object underAttack = invokeAnyNoArg(colony, "isColonyUnderAttack");
            Boolean parsed = asBoolean(underAttack);
            if (parsed != null) return parsed;

            Object raiderManager = invokeAnyNoArg(colony, "getRaiderManager");
            if (raiderManager != null) {
                Object isRaided = invokeAnyNoArg(raiderManager, "isRaided");
                Boolean parsedRaided = asBoolean(isRaided);
                if (parsedRaided != null) return parsedRaided;
            }

            return false;
        } catch (Exception e) {
            GodsparkMod.LOGGER.debug("[Godspark Observer] Could not check active raid for colony {}", colony.getID(), e);
            return false;
        }
    }

    /* ==================== BUILDING CLASSIFICATION ==================== */

    private Iterable<?> getBuildingValues(IColony colony) {
        Object buildingManager = getBuildingManager(colony);
        if (buildingManager == null) return List.of();

        Object buildings = invokeAnyNoArg(buildingManager, "getBuildings");
        if (buildings == null) return List.of();

        if (buildings instanceof Map<?, ?> map) return map.values();
        if (buildings instanceof Iterable<?> iterable) return iterable;

        if (buildings.getClass().isArray()) {
            List<Object> result = new ArrayList<>();
            int length = Array.getLength(buildings);
            for (int i = 0; i < length; i++) result.add(Array.get(buildings, i));
            return result;
        }

        return List.of();
    }

    private String getBuildingTypeKey(Object building) {
        if (building == null) return "";

        Object buildingType = invokeAnyNoArg(building, "getBuildingType");
        if (buildingType != null) {
            Object registryName = invokeAnyNoArg(buildingType, "getRegistryName");
            if (registryName != null) return registryName.toString().toLowerCase(Locale.ROOT);

            Object translationKey = invokeAnyNoArg(buildingType, "getTranslationKey");
            if (translationKey != null) return translationKey.toString().toLowerCase(Locale.ROOT);
        }

        Object displayName = invokeAnyNoArg(building, "getBuildingDisplayName");
        if (displayName != null) return displayName.toString().toLowerCase(Locale.ROOT);

        Object customName = invokeAnyNoArg(building, "getCustomName");
        if (customName != null) return customName.toString().toLowerCase(Locale.ROOT);

        return building.getClass().getName().toLowerCase(Locale.ROOT);
    }

    private int countByCategory(IColony colony, BuildingCategory... categories) {
        int count = 0;
        for (Object building : getBuildingValues(colony)) {
            String key = getBuildingTypeKey(building);
            if (BuildingClassifier.matches(key, categories)) {
                count++;
            }
        }
        return count;
    }

    private int getSafeWarehouseCount(IColony colony) {
        try {
            return countByCategory(colony, BuildingCategory.WAREHOUSE);
        } catch (Exception e) {
            GodsparkMod.LOGGER.warn("[Godspark Observer] Could not count warehouses for colony {}: {}", colony.getID(), e.getMessage());
            return 0;
        }
    }

    private int getSafeGuardCount(IColony colony) {
        try {
            return countByCategory(colony, BuildingCategory.SECURITY);
        } catch (Exception e) {
            GodsparkMod.LOGGER.warn("[Godspark Observer] Could not get guard count for colony {}", colony.getID(), e);
            return 0;
        }
    }

    private int getSafeFoodBuildingCount(IColony colony) {
        try {
            return countByCategory(colony, BuildingCategory.FOOD);
        } catch (Exception e) {
            GodsparkMod.LOGGER.warn("[Godspark Observer] Could not get food building count for colony {}", colony.getID(), e);
            return 0;
        }
    }

    private int getSafeIndustryBuildingCount(IColony colony) {
        try {
            return countByCategory(colony, BuildingCategory.INDUSTRY);
        } catch (Exception e) {
            GodsparkMod.LOGGER.warn("[Godspark Observer] Could not get industry building count for colony {}", colony.getID(), e);
            return 0;
        }
    }

    private int getSafeSacredBuildingCount(IColony colony) {
        try {
            return countByCategory(colony, BuildingCategory.SACRED);
        } catch (Exception e) {
            GodsparkMod.LOGGER.warn("[Godspark Observer] Could not get sacred building count for colony {}", colony.getID(), e);
            return 0;
        }
    }

    private int getSafeGatheringBuildingCount(IColony colony) {
        try {
            int count = 0;
            for (Object building : getBuildingValues(colony)) {
                String key = getBuildingTypeKey(building);
                if (BuildingClassifier.isGatheringPlace(key)) {
                    count++;
                }
            }
            return count;
        } catch (Exception e) {
            GodsparkMod.LOGGER.warn("[Godspark Observer] Could not get gathering building count for colony {}", colony.getID(), e);
            return 0;
        }
    }

    /* ==================== ACCESSORS ==================== */

    public Map<Integer, ObservedColony> getObservedColonies() {
        return Map.copyOf(observedColonies);
    }

    public void clear() {
        observedColonies.clear();
    }

    @Nullable
    public ObservedColony getColony(int id) {
        return observedColonies.get(id);
    }
}
