package com.godspark.sacred;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SacredSiteManagerTest {

    private final SacredSiteManager manager = SacredSiteManager.getInstance();

    private static final ResourceLocation OVERWORLD = new ResourceLocation("minecraft", "overworld");
    private static final ResourceLocation NETHER = new ResourceLocation("minecraft", "the_nether");

    @AfterEach
    void clearManager() {
        manager.clear();
    }

    @Test
    void prayerStoneNearColonyScoresOnlyThatColony() {
        boolean registered = manager.registerPrayerStone(
            OVERWORLD,
            new BlockPos(8, 64, 0),
            Map.of(
                1, colony(1, OVERWORLD, new BlockPos(0, 64, 0)),
                2, colony(2, OVERWORLD, new BlockPos(300, 64, 0))
            ).values(),
            10L
        );

        assertTrue(registered);
        assertEquals(1, manager.getPrayerStoneAnchorScore(1));
        assertEquals(0, manager.getPrayerStoneAnchorScore(2));
    }

    @Test
    void sameCoordsDifferentDimensionsDoNotCollide() {
        BlockPos pos = new BlockPos(4, 64, 4);

        assertTrue(manager.registerPrayerStone(
            OVERWORLD, pos,
            Map.of(1, colony(1, OVERWORLD, BlockPos.ZERO)).values(),
            10L
        ));
        assertTrue(manager.registerPrayerStone(
            NETHER, pos,
            Map.of(2, colony(2, NETHER, BlockPos.ZERO)).values(),
            10L
        ));

        assertEquals(1, manager.getPrayerStoneAnchorScore(1));
        assertEquals(1, manager.getPrayerStoneAnchorScore(2));
        assertEquals(2, manager.getAllSites().size());
    }

    @Test
    void registerThenUnregisterRemovesAnchorScore() {
        BlockPos pos = new BlockPos(4, 64, 4);
        assertTrue(manager.registerPrayerStone(
            OVERWORLD, pos,
            Map.of(1, colony(1, OVERWORLD, BlockPos.ZERO)).values(),
            10L
        ));

        manager.unregister(OVERWORLD, pos);

        assertEquals(0, manager.getPrayerStoneAnchorScore(1));
        assertTrue(manager.getAllSites().isEmpty());
    }

    @Test
    void multiplePrayerStonesCapAnchorScoreAtOne() {
        var colonies = Map.of(1, colony(1, OVERWORLD, BlockPos.ZERO)).values();
        for (int i = 0; i < 5; i++) {
            assertTrue(manager.registerPrayerStone(
                OVERWORLD,
                new BlockPos(i, 64, 0),
                colonies,
                10L + i
            ));
        }

        assertEquals(1, manager.getPrayerStoneAnchorScore(1));
    }

    @Test
    void noColonyInRadiusDoesNotRegister() {
        boolean registered = manager.registerPrayerStone(
            OVERWORLD,
            new BlockPos(500, 64, 0),
            Map.of(1, colony(1, OVERWORLD, BlockPos.ZERO)).values(),
            10L
        );

        assertFalse(registered);
        assertTrue(manager.getAllSites().isEmpty());
    }

    @Test
    void tieBreakChoosesLowestColonyId() {
        boolean registered = manager.registerPrayerStone(
            OVERWORLD,
            new BlockPos(10, 64, 0),
            Map.of(
                2, colony(2, OVERWORLD, new BlockPos(0, 64, 0)),
                1, colony(1, OVERWORLD, new BlockPos(20, 64, 0))
            ).values(),
            10L
        );

        assertTrue(registered);
        assertEquals(1, manager.getPrayerStoneAnchorScore(1));
        assertEquals(0, manager.getPrayerStoneAnchorScore(2));
    }

    private static SacredSiteManager.PrayerStoneCandidate colony(int id, ResourceLocation dimension, BlockPos center) {
        return new SacredSiteManager.PrayerStoneCandidate(
            id,
            "Colony " + id,
            dimension,
            center,
            1
        );
    }
}
