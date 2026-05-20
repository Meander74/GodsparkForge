package com.godspark.block;

import com.godspark.GodsparkMod;
import com.godspark.block.entity.PrayerStoneBlockEntity;
import com.godspark.observer.ObservedColony;
import com.godspark.sacred.SacredSiteManager;
import com.godspark.sacred.SacredSiteRecord;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PrayerStoneBlock extends BaseEntityBlock {

    private static final long USE_COOLDOWN_TICKS = 40L;
    private static final Map<UUID, Long> LAST_USE_TICK = new ConcurrentHashMap<>();

    public PrayerStoneBlock() {
        super(BlockBehaviour.Properties.of()
            .strength(3.0f, 4.0f)
            .lightLevel(PrayerStoneBlock::lightLevel)
        );
    }

    private static int lightLevel(BlockState state) {
        return 7;
    }

    private static final VoxelShape SHAPE = box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0);

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PrayerStoneBlockEntity(pos, state);
    }

    @Override
    public void onRemove(BlockState oldState, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (oldState.getBlock() != newState.getBlock() && level instanceof ServerLevel) {
            SacredSiteManager.getInstance().unregister(level, pos);
        }
        super.onRemove(oldState, level, pos, newState, movedByPiston);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                  Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.CONSUME;
        }

        long gameTick = serverLevel.getServer().getTickCount();
        Long lastUse = LAST_USE_TICK.get(player.getUUID());
        if (lastUse != null && gameTick - lastUse < USE_COOLDOWN_TICKS) {
            player.displayClientMessage(Component.literal("The stone rests. Wait a while before speaking again."), true);
            return InteractionResult.CONSUME;
        }
        LAST_USE_TICK.put(player.getUUID(), gameTick);

        boolean bound = SacredSiteManager.getInstance().registerPrayerStone(
            serverLevel,
            pos,
            GodsparkMod.COLONY_OBSERVER.getObservedColonies(),
            gameTick
        );

        SacredSiteRecord record = SacredSiteManager.getInstance().getSite(level.dimension(), pos);
        if (bound && record != null) {
            ObservedColony colony = GodsparkMod.COLONY_OBSERVER.getColony(record.colonyId());
            String colonyName = colony != null && colony.getLatest() != null
                ? colony.getLatest().name()
                : "Colony #" + record.colonyId();
            player.displayClientMessage(
                Component.literal("The stone hums softly. The colony of " + colonyName + " hears your voice."),
                true
            );
        } else {
            player.displayClientMessage(
                Component.literal("The stone is silent. No colony is near enough to hear."),
                true
            );
        }

        return InteractionResult.CONSUME;
    }
}
