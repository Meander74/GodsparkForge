package com.godspark;

import com.godspark.command.GodsparkCommands;
import com.godspark.event.GodsparkServerEvents;
import com.godspark.memory.MemoryBank;
import com.godspark.memory.MemoryEngine;
import com.godspark.observer.ColonyObserver;
import com.godspark.pressure.PressureEngine;
import com.godspark.story.EventGenerator;
import com.godspark.story.EventQueue;
import com.godspark.story.EventStateManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(GodsparkConstants.MOD_ID)
public final class GodsparkMod {
    public static final Logger LOGGER = LogManager.getLogger(GodsparkConstants.MOD_ID);

    public static final ColonyObserver COLONY_OBSERVER = new ColonyObserver();
    public static final PressureEngine PRESSURE_ENGINE = new PressureEngine();
    public static final EventGenerator EVENT_GENERATOR = new EventGenerator();
    public static final EventQueue EVENT_QUEUE = new EventQueue();
    public static final EventStateManager EVENT_STATE_MANAGER = new EventStateManager();
    public static final MemoryBank MEMORY_BANK = new MemoryBank();
    public static final MemoryEngine MEMORY_ENGINE = new MemoryEngine();

    @SuppressWarnings("deprecation")
    public GodsparkMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        LOGGER.info("Godspark initialized");
        LOGGER.info("Version: {}", GodsparkConstants.VERSION);
        LOGGER.info("Minecraft: 1.20.1 | Forge: 47.x");

        boolean minecoloniesLoaded = ModList.get().isLoaded("minecolonies");
        boolean createLoaded = ModList.get().isLoaded("create");

        LOGGER.info("MineColonies detected: {}", minecoloniesLoaded);
        LOGGER.info("Create detected: {}", createLoaded);

        MinecraftForge.EVENT_BUS.register(GodsparkServerEvents.class);
        MinecraftForge.EVENT_BUS.register(GodsparkCommands.class);
    }
}
