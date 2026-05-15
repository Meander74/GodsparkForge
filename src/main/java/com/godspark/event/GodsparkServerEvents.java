package com.godspark.event;

import com.godspark.GodsparkMod;
import com.godspark.GodsparkConstants;
import com.godspark.memory.ColonyMemory;
import com.godspark.persistence.GodsparkSavedData;
import com.godspark.story.StoryEvent;
import com.godspark.story.EventRecord;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class GodsparkServerEvents {
    private static long tickCounter = 0;
    @Nullable
    private static GodsparkSavedData savedData = null;

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        savedData = server.overworld().getDataStorage().computeIfAbsent(
            GodsparkSavedData::load,
            GodsparkSavedData::createDefault,
            GodsparkSavedData.DATA_KEY
        );
        savedData.restoreTo(GodsparkMod.EVENT_STATE_MANAGER);
        savedData.restoreMemoriesTo(GodsparkMod.MEMORY_BANK);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        if (savedData != null) {
            savedData.captureFrom(GodsparkMod.EVENT_STATE_MANAGER);
            savedData.captureMemoriesFrom(GodsparkMod.MEMORY_BANK);
            GodsparkMod.LOGGER.info("[Godspark SavedData] Captured final event state and memories");
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        GodsparkMod.COLONY_OBSERVER.clear();
        GodsparkMod.PRESSURE_ENGINE.clear();
        GodsparkMod.EVENT_QUEUE.clear();
        GodsparkMod.EVENT_STATE_MANAGER.clear();
        GodsparkMod.MEMORY_BANK.clear();
        tickCounter = 0;
        savedData = null;
        GodsparkMod.LOGGER.info("[Godspark] Static services cleared");
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        tickCounter++;

        MinecraftServer server = event.getServer();
        if (server == null) {
            return;
        }

        if (tickCounter % GodsparkConstants.OBSERVER_INTERVAL_TICKS == 0) {
            GodsparkMod.COLONY_OBSERVER.scan(server);
        }

        if (tickCounter % GodsparkConstants.PRESSURE_INTERVAL_TICKS == 0) {
            GodsparkMod.PRESSURE_ENGINE.compute(GodsparkMod.COLONY_OBSERVER.getObservedColonies());

            List<StoryEvent> candidates = GodsparkMod.EVENT_GENERATOR.generate(
                GodsparkMod.PRESSURE_ENGINE.getSnapshots(),
                GodsparkMod.COLONY_OBSERVER.getObservedColonies(),
                GodsparkMod.MEMORY_BANK,
                server.getTickCount()
            );

            List<EventRecord> transitions = GodsparkMod.EVENT_STATE_MANAGER.processEvents(
                candidates,
                server.getTickCount()
            );

            List<ColonyMemory> newMemories = GodsparkMod.MEMORY_ENGINE.generateMemories(
                transitions,
                GodsparkMod.MEMORY_BANK,
                server.getTickCount()
            );

            for (ColonyMemory memory : newMemories) {
                ColonyMemory stored = GodsparkMod.MEMORY_BANK.addOrReinforce(memory, server.getTickCount());
                GodsparkMod.LOGGER.info(
                    "[Godspark Memory] [{}][{}] {}: {} (intensity={})",
                    stored.memoryType().getDisplayName(),
                    stored.pressureType().getDisplayName(),
                    stored.colonyName(),
                    stored.content(),
                    stored.intensity()
                );
            }

            if (!transitions.isEmpty() && savedData != null) {
                savedData.captureFrom(GodsparkMod.EVENT_STATE_MANAGER);
                savedData.captureMemoriesFrom(GodsparkMod.MEMORY_BANK);
            }

            for (EventRecord record : transitions) {
                GodsparkMod.LOGGER.info(
                    "[Godspark Event] [{} → {}][{}] {}: {} (pressure={}, persistence={})",
                    record.isResolved() ? "ACTIVE/PERSISTENT" : "NEW/UPGRADE",
                    record.state(),
                    record.event().pressureType().getDisplayName(),
                    record.event().colonyName(),
                    record.event().description(),
                    record.event().pressureValue(),
                    record.persistenceCount()
                );
            }

            for (StoryEvent storyEvent : candidates) {
                if (GodsparkMod.EVENT_QUEUE.offer(storyEvent)) {
                    GodsparkMod.LOGGER.info(
                        "[Godspark Event] [{}][{}] {}: {} (pressure={}, threshold={})",
                        storyEvent.severity(),
                        storyEvent.pressureType().getDisplayName(),
                        storyEvent.colonyName(),
                        storyEvent.description(),
                        storyEvent.pressureValue(),
                        storyEvent.threshold()
                    );
                }
            }
        }
    }
}
