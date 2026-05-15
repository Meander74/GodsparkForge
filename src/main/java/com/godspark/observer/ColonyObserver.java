package com.godspark.observer;

import com.godspark.GodsparkMod;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ColonyObserver {
    private final Map<Integer, ObservedColony> observedColonies = new HashMap<>();

    public void scan(MinecraftServer server) {
        if (!ModList.get().isLoaded("minecolonies")) {
            return;
        }

        try {
            IColonyManager colonyManager = com.minecolonies.api.IMinecoloniesAPI.getInstance().getColonyManager();
            if (colonyManager == null) {
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
                            getSafeHasActiveRaid(colony),
                            colony.getDimension(),
                            gameTick
                        );

                        observedColonies
                            .computeIfAbsent(colonyId, id -> new ObservedColony(id))
                            .addSnapshot(snapshot);

                    } catch (Exception e) {
                        GodsparkMod.LOGGER.warn("Failed to snapshot colony: {}", e.getMessage());
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

    private int getSafeCitizenCount(IColony colony) {
        try {
            Method method = colony.getClass().getMethod("getCitizenManager");
            Object manager = method.invoke(colony);
            Method getCitizens = manager.getClass().getMethod("getCitizens");
            Object citizens = getCitizens.invoke(manager);
            return ((Map<?, ?>) citizens).size();
        } catch (Exception e) {
            GodsparkMod.LOGGER.debug("Could not get citizen count via reflection: {}", e.getMessage());
            return 0;
        }
    }

    private int getSafeBuildingCount(IColony colony) {
        try {
            Method method = colony.getClass().getMethod("getBuildingManager");
            Object manager = method.invoke(colony);
            Method getBuildings = manager.getClass().getMethod("getBuildings");
            Object buildings = getBuildings.invoke(manager);
            if (buildings instanceof Map) {
                return ((Map<?, ?>) buildings).size();
            }
            return 0;
        } catch (Exception e) {
            GodsparkMod.LOGGER.debug("Could not get building count via reflection: {}", e.getMessage());
            return 0;
        }
    }

    private int getSafeWarehouseCount(IColony colony) {
        try {
            Method method = colony.getClass().getMethod("getBuildingManager");
            Object manager = method.invoke(colony);
            Method getBuildings = manager.getClass().getMethod("getBuildings");
            Object buildingsObj = getBuildings.invoke(manager);
            if (!(buildingsObj instanceof Map)) return 0;

            int count = 0;
            Map<?, ?> buildings = (Map<?, ?>) buildingsObj;
            for (Object building : buildings.values()) {
                try {
                    Method getBuildingType = building.getClass().getMethod("getBuildingType");
                    Object type = getBuildingType.invoke(building);
                    if (type.toString().toLowerCase(Locale.ROOT).contains("warehouse")) {
                        count++;
                    }
                } catch (Exception ignored) {}
            }
            return count;
        } catch (Exception e) {
            GodsparkMod.LOGGER.debug("Could not count warehouses via reflection: {}", e.getMessage());
            return 0;
        }
    }

    // TODO: Replace with proper MineColonies building type/class checks
    private double getSafeHappiness(IColony colony) {
        try {
            return colony.getOverallHappiness();
        } catch (Exception e) {
            GodsparkMod.LOGGER.debug("Could not get happiness: {}", e.getMessage());
            return 5.0;
        }
    }

    // TODO: Replace with proper MineColonies building type/class checks
    private int getSafeGuardCount(IColony colony) {
        return countBuildingsByType(colony, "guard", "barracks", "tower");
    }

    // TODO: Replace with proper MineColonies building type/class checks
    private int getSafeFoodBuildingCount(IColony colony) {
        return countBuildingsByType(colony, "farm", "bakery", "fisher", "composter");
    }

    // TODO: Replace with proper MineColonies building type/class checks
    private int getSafeHousingCapacity(IColony colony) {
        try {
            Method method = colony.getClass().getMethod("getBuildingManager");
            Object manager = method.invoke(colony);
            Method getBuildings = manager.getClass().getMethod("getBuildings");
            Object buildingsObj = getBuildings.invoke(manager);
            if (!(buildingsObj instanceof Map)) return 0;

            int residenceCount = 0;
            Map<?, ?> buildings = (Map<?, ?>) buildingsObj;
            for (Object building : buildings.values()) {
                try {
                    Method getBuildingType = building.getClass().getMethod("getBuildingType");
                    Object type = getBuildingType.invoke(building);
                    String key = type.toString().toLowerCase(Locale.ROOT);
                    if (key.contains("house") || key.contains("residence")) {
                        residenceCount++;
                    }
                } catch (Exception ignored) {}
            }
            return residenceCount * 5;
        } catch (Exception e) {
            GodsparkMod.LOGGER.debug("Could not get housing capacity via reflection: {}", e.getMessage());
            return 0;
        }
    }

    // TODO: Replace with proper MineColonies building type/class checks
    private int getSafeIndustryBuildingCount(IColony colony) {
        return countBuildingsByType(colony, "smith", "sawmill", "stonemason", "builder");
    }

    // TODO: Replace with proper MineColonies building type/class checks
    private boolean getSafeHasActiveRaid(IColony colony) {
        try {
            Method method = colony.getClass().getMethod("getRaiderManager");
            Object manager = method.invoke(colony);
            Method hasActiveRaid = manager.getClass().getMethod("hasActiveRaid");
            return (boolean) hasActiveRaid.invoke(manager);
        } catch (Exception e) {
            GodsparkMod.LOGGER.debug("Could not check active raid: {}", e.getMessage());
            return false;
        }
    }

    // TODO: Replace with proper MineColonies building type/class checks
    private int countBuildingsByType(IColony colony, String... keywords) {
        try {
            Method method = colony.getClass().getMethod("getBuildingManager");
            Object manager = method.invoke(colony);
            Method getBuildings = manager.getClass().getMethod("getBuildings");
            Object buildingsObj = getBuildings.invoke(manager);
            if (!(buildingsObj instanceof Map)) return 0;

            int count = 0;
            Map<?, ?> buildings = (Map<?, ?>) buildingsObj;
            for (Object building : buildings.values()) {
                try {
                    Method getBuildingType = building.getClass().getMethod("getBuildingType");
                    Object type = getBuildingType.invoke(building);
                    String key = type.toString().toLowerCase(Locale.ROOT);
                    for (String keyword : keywords) {
                        if (key.contains(keyword)) {
                            count++;
                            break;
                        }
                    }
                } catch (Exception ignored) {}
            }
            return count;
        } catch (Exception e) {
            GodsparkMod.LOGGER.debug("Could not count buildings by type: {}", e.getMessage());
            return 0;
        }
    }

    public Map<Integer, ObservedColony> getObservedColonies() {
        return observedColonies;
    }

    public void clear() {
        observedColonies.clear();
    }

    @Nullable
    public ObservedColony getColony(int id) {
        return observedColonies.get(id);
    }
}
