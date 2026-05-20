package com.godspark.world;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.Mob;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class ReflectiveMineColoniesGuardResolver implements GuardTargetResolver {

    private static final Logger LOGGER = LogManager.getLogger(ReflectiveMineColoniesGuardResolver.class);

    private static final Set<String> HOSTILE_PACKAGE_PREFIXES = Set.of(
        "net.minecraft.world.entity.monster",
        "net.minecraft.world.entity.boss",
        "com.minecolonies.core.entity.mobs"
    );

    private static final Set<String> HOSTILE_CLASS_SIMPLE_NAMES = Set.of(
        "Raider", "RaiderEntity", "Pillager", "Vindicator", "Evoker", "Illusioner",
        "Zombie", "Skeleton", "Creeper", "Spider", "Enderman", "Witch",
        "Hoglin", "PiglinBrute", "Blaze", "Ghast", "MagmaCube", "Slime",
        "Shulker", "Vex", "Warden", "Guardian", "ElderGuardian"
    );

    @Override
    public List<LivingEntity> resolveGuards(ServerLevel level, BlockPos center, int radius) {
        AABB searchBox = AABB.ofSize(center.getCenter(), radius * 2.0, radius * 2.0, radius * 2.0);

        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, searchBox,
            e -> !e.isDeadOrDying() && !(e instanceof Player));

        List<LivingEntity> guards = new ArrayList<>();
        for (LivingEntity entity : candidates) {
            if (isHostile(entity)) continue;
            if (isDefinitelyMineColoniesGuard(entity)) {
                guards.add(entity);
            }
        }
        return guards;
    }

    private static boolean isHostile(LivingEntity entity) {
        Class<?> clazz = entity.getClass();
        Package pkg = clazz.getPackage();
        if (pkg != null) {
            String pkgName = pkg.getName();
            for (String prefix : HOSTILE_PACKAGE_PREFIXES) {
                if (pkgName.startsWith(prefix)) return true;
            }
        }
        String simpleName = clazz.getSimpleName();
        for (String name : HOSTILE_CLASS_SIMPLE_NAMES) {
            if (simpleName.contains(name)) return true;
        }
        return false;
    }

    private static boolean isDefinitelyMineColoniesGuard(LivingEntity entity) {
        Class<?> clazz = entity.getClass();
        Package pkg = clazz.getPackage();
        if (pkg == null) return false;
        String pkgName = pkg.getName();

        if (!pkgName.startsWith("com.minecolonies")) return false;

        String simpleName = clazz.getSimpleName();
        boolean looksLikeGuard = simpleName.contains("Citizen")
            || simpleName.contains("EntityCitizen")
            || simpleName.contains("Visitor")
            || simpleName.contains("Guard");

        if (!looksLikeGuard) return false;

        try {
            if (entity instanceof Mob mob) {
                var target = mob.getTarget();
                return target == null || isHostile(target);
            }
        } catch (Exception e) {
            LOGGER.warn("[Godspark WorldEffect] Guard check failed for {}: {}", simpleName, e.getMessage());
        }

        return true;
    }
}
