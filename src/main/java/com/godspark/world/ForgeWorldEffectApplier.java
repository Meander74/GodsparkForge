package com.godspark.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.util.RandomSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public final class ForgeWorldEffectApplier implements WorldEffectApplier {

    private static final Logger LOGGER = LogManager.getLogger(ForgeWorldEffectApplier.class);

    private static final Set<Block> VANILLA_CROPS = Set.of(
        Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES, Blocks.BEETROOTS,
        Blocks.MELON_STEM, Blocks.PUMPKIN_STEM, Blocks.NETHER_WART,
        Blocks.SWEET_BERRY_BUSH, Blocks.COCOA
    );

    private final GuardTargetResolver guardResolver;

    public ForgeWorldEffectApplier(GuardTargetResolver guardResolver) {
        this.guardResolver = guardResolver;
    }

    @Override
    public WorldEffectApplyResult applyGuardiansVigil(ServerLevel level, BlockPos center, int durationTicks, int radius) {
        List<LivingEntity> guards;
        try {
            guards = guardResolver.resolveGuards(level, center, radius);
        } catch (Exception e) {
            LOGGER.warn("[Godspark WorldEffect] GuardResolver failed: {}", e.getMessage());
            return new WorldEffectApplyResult(LightWorldEffect.GUARDIANS_VIGIL, false, 0, "guard_resolver_exception");
        }

        if (guards.isEmpty()) {
            LOGGER.info("[Godspark WorldEffect] [SECURITY] Guardian's Vigil found no loaded MineColonies guards; pressure modifier remains active.");
            return new WorldEffectApplyResult(LightWorldEffect.GUARDIANS_VIGIL, false, 0, "no_loaded_guards");
        }

        MobEffectInstance candidate = new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, durationTicks, 0, false, true, true);
        int affected = 0;

        for (LivingEntity guard : guards) {
            MobEffectInstance existing = guard.getEffect(MobEffects.DAMAGE_RESISTANCE);
            if (shouldReplaceEffect(existing, candidate)) {
                guard.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, durationTicks, 0, false, true, true));
                affected++;
            }
        }

        LOGGER.info("[Godspark WorldEffect] [SECURITY] Guardian's Vigil applied Resistance I to {} guards.", affected);
        return new WorldEffectApplyResult(LightWorldEffect.GUARDIANS_VIGIL, true, affected, "resistance_applied_to_" + affected);
    }

    @Override
    public WorldEffectApplyResult applyGreensMercy(ServerLevel level, BlockPos center, int maxAttempts, int successCap, int radius) {
        RandomSource random = level.getRandom();
        int affected = 0;
        int attempts = 0;

        while (attempts < maxAttempts && affected < successCap) {
            attempts++;
            int dx = random.nextInt(radius * 2) - radius;
            int dz = random.nextInt(radius * 2) - radius;
            BlockPos probePos = center.offset(dx, 0, dz);

            if (!level.isLoaded(probePos)) continue;

            int minY = Math.max(level.getMinBuildHeight(), center.getY() - 4);
            int maxY = Math.min(level.getMaxBuildHeight(), center.getY() + 4);

            for (int y = minY; y <= maxY; y++) {
                if (affected >= successCap) break;
                BlockPos cropPos = new BlockPos(probePos.getX(), y, probePos.getZ());
                if (!level.isLoaded(cropPos)) continue;

                BlockState state = level.getBlockState(cropPos);
                Block block = state.getBlock();

                if (!VANILLA_CROPS.contains(block)) continue;
                if (!(block instanceof BonemealableBlock growable)) continue;
                if (!growable.isValidBonemealTarget(level, cropPos, state, false)) continue;

                if (growable.isBonemealSuccess(level, random, cropPos, state)) {
                    growable.performBonemeal((ServerLevel) level, random, cropPos, state);
                    affected++;
                }
            }
        }

        if (affected == 0) {
            LOGGER.info("[Godspark WorldEffect] [FOOD] Green Mercy found no loadable crops; pressure modifier remains active.");
            return new WorldEffectApplyResult(LightWorldEffect.GREENS_MERCY, false, 0, "no_loadable_crops");
        }

        LOGGER.info("[Godspark WorldEffect] [FOOD] Green Mercy advanced {} crops ({} attempts).", affected, attempts);
        return new WorldEffectApplyResult(LightWorldEffect.GREENS_MERCY, true, affected, "crops_advanced_" + affected);
    }

    private static boolean shouldReplaceEffect(@Nullable MobEffectInstance existing, MobEffectInstance candidate) {
        if (existing == null) return true;
        if (existing.getAmplifier() < candidate.getAmplifier()) return true;
        return existing.getAmplifier() == candidate.getAmplifier()
            && existing.getDuration() < candidate.getDuration();
    }
}
