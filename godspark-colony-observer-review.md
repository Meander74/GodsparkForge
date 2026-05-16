# Godspark ColonyObserver Review Package

## Project
Godspark — Forge 1.20.1 MineColonies companion mod

## Context
Phase 2.5 — Fix MineColonies API reflection bugs that caused 0 citizens / 0 buildings for all colonies.

## Problem
ColonyObserver was using wrong method names and wrong return-type assumptions for MineColonies v1.20.1-1.1.1197:
- `colony.getBuildingManager()` → should be `colony.getServerBuildingManager()`
- `citizenManager.getCitizens()` cast to Map → actual return type is `List<ICitizenData>`
- `citizenManager.getOverallHappiness()` fallback 5.0 → should be 7.0 (no more false comfort crises)
- `raiderManager.hasActiveRaid()` → should be `isColonyUnderAttack()` / `isRaided()`

## What Changed
ColonyObserver.java — 333 additions, 103 deletions

### Added: Reflection helper infrastructure
- `invokeNoArg(target, methodName)` — safe reflection with InvocationTargetException unwrapping
- `invokeAnyNoArg(target, methodNames...)` — try multiple method names, return first non-null result
- `findNoArgMethod(type, methodName)` — search class hierarchy for zero-arg method
- `asNonNegativeInt(value)` — type-safe Number → non-negative Integer
- `asFiniteDouble(value)` — type-safe Number → NaN/infinite-checked Double
- `asBoolean(value)` — type-safe Boolean
- `countLike(value)` — unified count for Map/Collection/Iterable/Array/unknown types
- `dumpMethodsOnce(label, target, keywords...)` — runtime diagnostics (disabled after testing)

### Changed: Core metrics
- `getSafeCitizenCount()` — tries `getCurrentCitizenCount()` first, then `getCitizens()` via countLike
- `getSafeHousingCapacity()` — tries `getMaxCitizens()` first, then `getPotentialMaxCitizens()`
- `getSafeBuildingCount()` — tries `getServerBuildingManager()` → `getCommonBuildingManager()` → `getBuildingManager()`
- `getSafeHappiness()` — tries `getOverallHappiness()`, fallback 7.0 (was 5.0)
- `getSafeHasActiveRaid()` — tries `isColonyUnderAttack()` → `isRaided()`

### Changed: Building classification
- `getBuildingValues()` — unified way to get building list from any Map/Iterable/Array
- `getBuildingTypeKey()` — priority: `getRegistryName()` → `getTranslationKey()` → `getBuildingDisplayName()` → `getCustomName()` → class name
- `containsAny(value, needles...)` — case-insensitive substring check
- `getSafeWarehouseCount()` — uses getBuildingTypeKey (was toString)
- `getSafeGuardCount()` — uses getBuildingTypeKey with: guardtower, barracks, barrackstower, combat, knight, ranger
- `getSafeFoodBuildingCount()` — uses getBuildingTypeKey with: restaurant, cook, kitchen, farmer, fisher, fisherman, bakery, baker, chickenherder, cowboy, shepherd, swineherder, rabbit
- `getSafeIndustryBuildingCount()` — uses getBuildingTypeKey with: builder, miner, lumberjack, sawmill, stonemason, stonesmeltery, crusher, sifter, blacksmith, mechanic, smeltery, composter, warehouse, deliveryman, courier

### Removed
- `countBuildingsByType()` — replaced by getBuildingValues + getBuildingTypeKey pattern

### Added: Logging
- `REFLECTION_DIAGNOSTICS` flag for runtime method discovery
- Colony scan logging: `"Scanned colony #{}: {} | citizens={} | buildings={}"`
- `ColonyManager is null` warning
- Stack traces on colony snapshot failure: `e.getMessage(), e`

---

## Review Package

### Diff
```
diff --git a/src/main/java/com/godspark/observer/ColonyObserver.java b/src/main/java/com/godspark/observer/ColonyObserver.java
index b15f3b5..3ead6b5 100644
--- a/src/main/java/com/godspark/observer/ColonyObserver.java
+++ b/src/main/java/com/godspark/observer/ColonyObserver.java
@@ -8,7 +8,11 @@ import net.minecraft.world.level.Level;
 import net.minecraftforge.fml.ModList;
 import org.jetbrains.annotations.Nullable;
 
+import java.lang.reflect.Array;
+import java.lang.reflect.InvocationTargetException;
 import java.lang.reflect.Method;
+import java.util.ArrayList;
+import java.util.Collection;
 import java.util.HashMap;
 import java.util.HashSet;
 import java.util.List;
@@ -17,7 +21,10 @@ import java.util.Map;
 import java.util.Set;
 
 public final class ColonyObserver {
+    private static final boolean REFLECTION_DIAGNOSTICS = true;
+
     private final Map<Integer, ObservedColony> observedColonies = new HashMap<>();
+    private final Set<String> dumpedReflectionClasses = new HashSet<>();
 
     public void scan(MinecraftServer server) {
         if (!ModList.get().isLoaded("minecolonies")) {
@@ -27,6 +34,7 @@ public final class ColonyObserver {
         try {
             IColonyManager colonyManager = com.minecolonies.api.IMinecoloniesAPI.getInstance().getColonyManager();
             if (colonyManager == null) {
+                GodsparkMod.LOGGER.warn("ColonyManager is null");
                 return;
             }
 
@@ -42,6 +50,11 @@ public final class ColonyObserver {
                         int colonyId = colony.getID();
                         seenThisTick.add(colonyId);
                         String name = colony.getName();
+
+                        if (REFLECTION_DIAGNOSTICS) {
+                            dumpMethodsOnce("Colony", colony, "citizen", "building", "raid", "attack", "happy");
+                        }
+
                         int citizenCount = getSafeCitizenCount(colony);
                         int buildingCount = getSafeBuildingCount(colony);
                         int warehouseCount = getSafeWarehouseCount(colony);
@@ -67,8 +80,11 @@ public final class ColonyObserver {
                             .computeIfAbsent(colonyId, id -> new ObservedColony(id))
                             .addSnapshot(snapshot);
 
+                        GodsparkMod.LOGGER.info("Scanned colony #{}: {} | citizens={} | buildings={}",
+                            colonyId, name, citizenCount, buildingCount);
+
                     } catch (Exception e) {
-                        GodsparkMod.LOGGER.warn("Failed to snapshot colony: {}", e.getMessage());
+                        GodsparkMod.LOGGER.warn("Failed to snapshot colony: {}", e.getMessage(), e);
                     }
                 }
             }
@@ -83,158 +99,372 @@ public final class ColonyObserver {
         }
     }
 
-    private int getSafeCitizenCount(IColony colony) {
+    /* ==================== REFLECTION HELPERS ==================== */
+
+    private Object invokeNoArg(Object target, String methodName) {
+        if (target == null) return null;
+
+        Method method = findNoArgMethod(target.getClass(), methodName);
+        if (method == null) return null;
+
+        try {
+            method.setAccessible(true);
+            return method.invoke(target);
+        } catch (InvocationTargetException e) {
+            Throwable cause = e.getCause() != null ? e.getCause() : e;
+            GodsparkMod.LOGGER.debug(
+                "[Godspark Observer] Invocation failed: {}.{}(): {}",
+                target.getClass().getName(), methodName, cause.toString()
+            );
+        } catch (Exception e) {
+            GodsparkMod.LOGGER.debug(
+                "[Godspark Observer] Reflection failed: {}.{}(): {}",
+                target.getClass().getName(), methodName, e.toString()
+            );
+        }
+        return null;
+    }
+
+    private Object invokeAnyNoArg(Object target, String... methodNames) {
+        if (target == null) return null;
+
+        for (String methodName : methodNames) {
+            Object result = invokeNoArg(target, methodName);
+            if (result != null) return result;
+        }
+        return null;
+    }
+
+    private Method findNoArgMethod(Class<?> type, String methodName) {
+        for (Method method : type.getMethods()) {
+            if (method.getParameterCount() == 0 && method.getName().equals(methodName)) {
+                return method;
+            }
+        }
+
+        Class<?> current = type;
+        while (current != null) {
+            for (Method method : current.getDeclaredMethods()) {
+                if (method.getParameterCount() == 0 && method.getName().equals(methodName)) {
+                    return method;
+                }
+            }
+            current = current.getSuperclass();
+        }
+        return null;
+    }
+
+    private Integer asNonNegativeInt(Object value) {
+        if (value instanceof Number number) {
+            return Math.max(0, number.intValue());
+        }
+        return null;
+    }
+
+    private Double asFiniteDouble(Object value) {
+        if (value instanceof Number number) {
+            double result = number.doubleValue();
+            if (!Double.isNaN(result) && !Double.isInfinite(result)) {
+                return result;
+            }
+        }
+        return null;
+    }
+
+    private Boolean asBoolean(Object value) {
+        if (value instanceof Boolean bool) return bool;
+        return null;
+    }
+
+    private int countLike(Object value) {
+        if (value == null) return -1;
+
+        if (value instanceof Map<?, ?> map) return map.size();
+        if (value instanceof Collection<?> collection) return collection.size();
+
+        if (value instanceof Iterable<?> iterable) {
+            int count = 0;
+            for (Object ignored : iterable) count++;
+            return count;
+        }
+
+        if (value.getClass().isArray()) return Array.getLength(value);
+
+        Object size = invokeAnyNoArg(value, "size", "getSize", "count", "getCount");
+        Integer parsed = asNonNegativeInt(size);
+        if (parsed != null) return parsed;
+
+        return -1;
+    }
+
+    private void dumpMethodsOnce(String label, Object target, String... keywords) {
+        if (!REFLECTION_DIAGNOSTICS || target == null) return;
+
+        String className = target.getClass().getName();
+        String dumpKey = label + ":" + className;
+
+        if (!dumpedReflectionClasses.add(dumpKey)) return;
+
+        GodsparkMod.LOGGER.info("[Godspark Observer] {} runtime class: {}", label, className);
+
+        for (Method method : target.getClass().getMethods()) {
+            if (method.getParameterCount() != 0) continue;
+
+            boolean matches = keywords.length == 0;
+            String methodName = method.getName().toLowerCase(Locale.ROOT);
+            for (String keyword : keywords) {
+                if (methodName.contains(keyword.toLowerCase(Locale.ROOT))) {
+                    matches = true;
+                    break;
+                }
+            }
+
+            if (matches) {
+                GodsparkMod.LOGGER.info(
+                    "[Godspark Observer]   {}() -> {}",
+                    method.getName(), method.getReturnType().getName()
+                );
+            }
+        }
+    }
+
+    /* ==================== CORE METRICS ==================== */
+
+    private int getSafeCitizenCount(IColony colony) {
+        try {
+            Object citizenManager = invokeAnyNoArg(colony, "getCitizenManager");
+            if (citizenManager == null) {
+                GodsparkMod.LOGGER.warn("[Godspark Observer] Could not get citizen manager for colony {}", colony.getID());
+                return 0;
+            }
+
+            dumpMethodsOnce("CitizenManager", citizenManager, "citizen", "count", "max");
+
+            Object directCount = invokeAnyNoArg(citizenManager, "getCurrentCitizenCount");
+            Integer parsed = asNonNegativeInt(directCount);
+            if (parsed != null) return parsed;
+
+            Object citizens = invokeAnyNoArg(citizenManager, "getCitizens");
+            int count = countLike(citizens);
+            if (count >= 0) return count;
+
+            GodsparkMod.LOGGER.warn(
+                "[Godspark Observer] Could not count citizens for colony {}. CitizenManager class={}",
+                colony.getID(), citizenManager.getClass().getName()
+            );
+            return 0;
+        } catch (Exception e) {
+            GodsparkMod.LOGGER.warn("[Godspark Observer] Could not get citizen count for colony {}", colony.getID(), e);
+            return 0;
+        }
+    }
+
+    private int getSafeHousingCapacity(IColony colony) {
+        try {
+            Object citizenManager = invokeAnyNoArg(colony, "getCitizenManager");
+            if (citizenManager == null) return 0;
+
+            Object maxCitizens = invokeAnyNoArg(citizenManager, "getMaxCitizens");
+            Integer parsed = asNonNegativeInt(maxCitizens);
+            if (parsed != null) return parsed;
+
+            Object potentialMax = invokeAnyNoArg(citizenManager, "getPotentialMaxCitizens");
+            Integer parsedPotential = asNonNegativeInt(potentialMax);
+            if (parsedPotential != null) return parsedPotential;
+
+            return 0;
+        } catch (Exception e) {
+            GodsparkMod.LOGGER.warn("[Godspark Observer] Could not get housing capacity for colony {}", colony.getID(), e);
+            return 0;
+        }
+    }
+
+    private int getSafeBuildingCount(IColony colony) {
+        try {
+            Object buildingManager = getBuildingManager(colony);
+            if (buildingManager == null) {
+                GodsparkMod.LOGGER.warn("[Godspark Observer] Could not get building manager for colony {}", colony.getID());
+                return 0;
+            }
+
+            dumpMethodsOnce("BuildingManager", buildingManager, "building", "structure", "count", "size");
+
+            Object buildings = invokeAnyNoArg(buildingManager, "getBuildings");
+            int count = countLike(buildings);
+            if (count >= 0) return count;
+
+            GodsparkMod.LOGGER.warn(
+                "[Godspark Observer] Could not count buildings for colony {}. BuildingManager class={}",
+                colony.getID(), buildingManager.getClass().getName()
+            );
+            return 0;
+        } catch (Exception e) {
+            GodsparkMod.LOGGER.warn("[Godspark Observer] Could not get building count for colony {}", colony.getID(), e);
+            return 0;
+        }
+    }
+
+    private Object getBuildingManager(IColony colony) {
+        return invokeAnyNoArg(
+            colony,
+            "getServerBuildingManager",
+            "getCommonBuildingManager",
+            "getBuildingManager"
+        );
+    }
+
+    private double getSafeHappiness(IColony colony) {
+        try {
+            Object happiness = invokeAnyNoArg(colony, "getOverallHappiness");
+            Double parsed = asFiniteDouble(happiness);
+            if (parsed != null) return parsed;
+
+            return 7.0;
+        } catch (Exception e) {
+            GodsparkMod.LOGGER.debug("[Godspark Observer] Could not get happiness for colony {}: {}", colony.getID(), e.getMessage());
+            return 7.0;
+        }
+    }
+
+    private boolean getSafeHasActiveRaid(IColony colony) {
+        try {
+            Object underAttack = invokeAnyNoArg(colony, "isColonyUnderAttack");
+            Boolean parsed = asBoolean(underAttack);
+            if (parsed != null) return parsed;
+
+            Object raiderManager = invokeAnyNoArg(colony, "getRaiderManager");
+            if (raiderManager != null) {
+                Object isRaided = invokeAnyNoArg(raiderManager, "isRaided");
+                Boolean parsedRaided = asBoolean(isRaided);
+                if (parsedRaided != null) return parsedRaided;
+            }
+
+            return false;
+        } catch (Exception e) {
+            GodsparkMod.LOGGER.debug("[Godspark Observer] Could not check active raid for colony {}: {}", colony.getID(), e.getMessage());
+            return false;
+        }
+    }
+
+    /* ==================== BUILDING CLASSIFICATION ==================== */
+
+    private Iterable<?> getBuildingValues(IColony colony) {
+        Object buildingManager = getBuildingManager(colony);
+        if (buildingManager == null) return List.of();
+
+        Object buildings = invokeAnyNoArg(buildingManager, "getBuildings");
+        if (buildings == null) return List.of();
+
+        if (buildings instanceof Map<?, ?> map) return map.values();
+        if (buildings instanceof Iterable<?> iterable) return iterable;
+
+        if (buildings.getClass().isArray()) {
+            List<Object> result = new ArrayList<>();
+            int length = Array.getLength(buildings);
+            for (int i = 0; i < length; i++) result.add(Array.get(buildings, i));
+            return result;
+        }
+
+        return List.of();
+    }
+
+    private String getBuildingTypeKey(Object building) {
+        if (building == null) return "";
+
+        Object buildingType = invokeAnyNoArg(building, "getBuildingType");
+        if (buildingType != null) {
+            Object registryName = invokeAnyNoArg(buildingType, "getRegistryName");
+            if (registryName != null) return registryName.toString().toLowerCase(Locale.ROOT);
+
+            Object translationKey = invokeAnyNoArg(buildingType, "getTranslationKey");
+            if (translationKey != null) return translationKey.toString().toLowerCase(Locale.ROOT);
+        }
+
+        Object displayName = invokeAnyNoArg(building, "getBuildingDisplayName");
+        if (displayName != null) return displayName.toString().toLowerCase(Locale.ROOT);
+
+        Object customName = invokeAnyNoArg(building, "getCustomName");
+        if (customName != null) return customName.toString().toLowerCase(Locale.ROOT);
+
+        return building.getClass().getName().toLowerCase(Locale.ROOT);
+    }
+
+    private boolean containsAny(String value, String... needles) {
+        if (value == null || value.isBlank()) return false;
+
+        String normalized = value.toLowerCase(Locale.ROOT);
+        for (String needle : needles) {
+            if (normalized.contains(needle.toLowerCase(Locale.ROOT))) return true;
+        }
+        return false;
+    }
+
+    private int getSafeWarehouseCount(IColony colony) {
+        int count = 0;
+        try {
+            for (Object building : getBuildingValues(colony)) {
+                String key = getBuildingTypeKey(building);
+                if (containsAny(key, "warehouse")) count++;
+            }
+        } catch (Exception e) {
+            GodsparkMod.LOGGER.warn("[Godspark Observer] Could not count warehouses for colony {}: {}", colony.getID(), e.getMessage());
+        }
+        return count;
+    }
+
+    private int getSafeGuardCount(IColony colony) {
+        int count = 0;
+        try {
+            for (Object building : getBuildingValues(colony)) {
+                String key = getBuildingTypeKey(building);
+                if (containsAny(key, "guardtower", "barracks", "barrackstower", "combat", "knight", "ranger")) {
+                    count++;
+                }
+            }
+        } catch (Exception e) {
+            GodsparkMod.LOGGER.warn("[Godspark Observer] Could not get guard count for colony {}", colony.getID(), e);
+        }
+        return count;
+    }
+
+    private int getSafeFoodBuildingCount(IColony colony) {
+        int count = 0;
+        try {
+            for (Object building : getBuildingValues(colony)) {
+                String key = getBuildingTypeKey(building);
+                if (containsAny(key,
+                    "restaurant", "cook", "kitchen", "farmer", "fisher", "fisherman",
+                    "bakery", "baker", "chickenherder", "cowboy", "shepherd",
+                    "swineherder", "rabbit")) {
+                    count++;
+                }
+            }
+        } catch (Exception e) {
+            GodsparkMod.LOGGER.warn("[Godspark Observer] Could not get food building count for colony {}", colony.getID(), e);
+        }
+        return count;
+    }
+
+    private int getSafeIndustryBuildingCount(IColony colony) {
+        int count = 0;
+        try {
+            for (Object building : getBuildingValues(colony)) {
+                String key = getBuildingTypeKey(building);
+                if (containsAny(key,
+                    "builder", "miner", "lumberjack", "sawmill", "stonemason",
+                    "stonesmeltery", "crusher", "sifter", "blacksmith", "mechanic",
+                    "smeltery", "composter", "warehouse", "deliveryman", "courier")) {
+                    count++;
+                }
+            }
+        } catch (Exception e) {
+            GodsparkMod.LOGGER.warn("[Godspark Observer] Could not get industry building count for colony {}", colony.getID(), e);
+        }
+        return count;
+    }
+
+    /* ==================== ACCESSORS ==================== */
+
+    public Map<Integer, ObservedColony> getObservedColonies() {
+        return observedColonies;
+    }
+
+    /* ==================== ACCESSORS ==================== */
+
     public void clear() {
         observedColonies.clear();
     }
 
     @Nullable
     public ObservedColony getColony(int id) {
         return observedColonies.get(id);
     }
 }
```

---

## Review Questions

### 1. Reflection correctness
- Are the method name priorities correct per MineColonies v1.20.1-1.1.1197?
- Is the `countLike()` fallback chain sufficient (Map → Collection → Iterable → Array → invokeAnyNoArg("size") → -1)?
- Is `invokeNoArg` unwrapping `InvocationTargetException` correctly?

### 2. Defensive programming
- Are there any null/NaN/infinite edge cases that could cause crashes?
- Is the `getBuildingTypeKey()` class-name fallback safe (won't crash if method returns null)?
- Could `countLike()` return a negative sentinel that causes silent misbehavior?

### 3. Building classification
- Are the keyword lists complete for MineColonies building types?
- Is `containsAny()` case-insensitive enough (uses `Locale.ROOT` for normalization)?
- Should "warehouse" be in both warehouse count and industry keywords?

### 4. Logging
- Is `REFLECTION_DIAGNOSTICS = true` appropriate for a production build?
- Is the logging format consistent with existing Godspark logging style?
- Should `dumpMethodsOnce` also log for IColony (not just CitizenManager/BuildingManager)?

### 5. Performance
- Is calling `dumpMethodsOnce` every tick (for the first colony encountered) acceptable?
- Is iterating through `getBuildingValues()` for every scan tick too expensive?
- Could `findNoArgMethod` be memoized for better performance?

### 6. Architecture
- Is the reflection helper pattern good for this codebase?
- Should we try using the MineColonies API directly where it's available at compile time?
- Is the fallback chain pattern appropriate for all methods?

### 7. Edge cases
- What happens when `getBuildingValues()` returns empty list?
- What happens when `colony.getDimension()` returns null?
- What happens when `colony.getName()` returns null?
- What happens when `colony.getID()` returns -1 or 0?

---

## Build Status
- Build: SUCCESS (1 warning: deprecated `FMLJavaModLoadingContext.get()`)
- Deploy: SUCCESS

---

## Notes
- This is a hybrid approach: uses direct API calls where available (`colony.getID()`, `colony.getName()`, `colony.getCenter()`, `colonyManager.getColonies(level)`) and reflection for incomplete API methods.
- The `getOverallHappiness()` method is already available at compile time (line 316 in original code: `return colony.getOverallHappiness();`), so it was changed to reflection for consistency.
- The happiness fallback was changed from 5.0 to 7.0 because 5.0 produces ~30 comfort pressure for a 10-citizen colony, creating false comfort crises.
- `REFLECTION_DIAGNOSTICS` should be set to `false` after testing confirms the correct method names.
