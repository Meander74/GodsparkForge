package com.godspark.event;

import com.godspark.GodsparkMod;
import com.godspark.GodsparkConstants;
import com.godspark.story.StoryEvent;
import com.godspark.story.EventRecord;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

public final class GodsparkServerEvents {
    private static long tickCounter = 0;

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
                server.getTickCount()
            );

            List<EventRecord> transitions = GodsparkMod.EVENT_STATE_MANAGER.processEvents(
                candidates,
                server.getTickCount()
            );

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
