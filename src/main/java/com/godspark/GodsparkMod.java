package com.godspark;

import com.godspark.ai.AiReflectionService;
import com.godspark.ai.AiConfig;
import com.godspark.command.GodsparkCommands;
import com.godspark.divine.DivineAnswerInterpreter;
import com.godspark.event.GodsparkServerEvents;
import com.godspark.memory.MemoryBank;
import com.godspark.memory.MemoryEngine;
import com.godspark.memory.MemoryInfluence;
import com.godspark.network.GodsparkNetwork;
import com.godspark.observer.ColonyObserver;
import com.godspark.personality.PersonalityEngine;
import com.godspark.personality.PersonalityInfluence;
import com.godspark.prayer.PrayerSeedBank;
import com.godspark.prayer.PrayerSeedGenerator;
import com.godspark.pressure.PressureEngine;
import com.godspark.pressure.PressureModifierManager;
import com.godspark.registry.GodsparkBlockEntities;
import com.godspark.registry.GodsparkBlocks;
import com.godspark.registry.GodsparkCreativeTab;
import com.godspark.registry.GodsparkItems;
import com.godspark.story.EventGenerator;
import com.godspark.story.EventQueue;
import com.godspark.story.EventStateManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(GodsparkConstants.MOD_ID)
public final class GodsparkMod {
    public static final Logger LOGGER = LogManager.getLogger(GodsparkConstants.MOD_ID);

    public static final ColonyObserver COLONY_OBSERVER = new ColonyObserver();
    public static final PressureEngine PRESSURE_ENGINE = new PressureEngine();
    public static final PressureModifierManager PRESSURE_MODIFIER_MANAGER = new PressureModifierManager();
    public static final EventGenerator EVENT_GENERATOR = new EventGenerator();
    public static final EventQueue EVENT_QUEUE = new EventQueue();
    public static final EventStateManager EVENT_STATE_MANAGER = new EventStateManager();
    public static final MemoryBank MEMORY_BANK = new MemoryBank();
    public static final MemoryEngine MEMORY_ENGINE = new MemoryEngine();
    public static final MemoryInfluence MEMORY_INFLUENCE = new MemoryInfluence();
    public static final PrayerSeedGenerator PRAYER_SEED_GENERATOR = new PrayerSeedGenerator();
    public static final PrayerSeedBank PRAYER_SEED_BANK = new PrayerSeedBank();
    public static final PersonalityInfluence PERSONALITY_INFLUENCE = new PersonalityInfluence();

    private static AiConfig aiConfig;
    public static final AiReflectionService AI_REFLECTION_SERVICE;
    public static final DivineAnswerInterpreter DIVINE_ANSWER_INTERPRETER;
    public static final PersonalityEngine PERSONALITY_ENGINE = new PersonalityEngine();

    static {
        aiConfig = buildAiConfig();
        AI_REFLECTION_SERVICE = new AiReflectionService(aiConfig);
        DIVINE_ANSWER_INTERPRETER = new DivineAnswerInterpreter(aiConfig);
    }

    @SuppressWarnings("deprecation")
    public GodsparkMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, GodsparkConfig.SPEC);

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        GodsparkBlocks.BLOCKS.register(modEventBus);
        GodsparkBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        GodsparkItems.ITEMS.register(modEventBus);

        GodsparkCreativeTab.TABS.register(modEventBus);

        PRESSURE_ENGINE.setModifierManager(PRESSURE_MODIFIER_MANAGER);

        LOGGER.info("Godspark initialized");
        LOGGER.info("Version: {}", GodsparkConstants.VERSION);
        LOGGER.info("Minecraft: 1.20.1 | Forge: 47.x");
        LOGGER.info("AI: enabled={}, endpoint={}, model={}",
            aiConfig.enabled(), aiConfig.endpoint(), aiConfig.model());

        boolean minecoloniesLoaded = ModList.get().isLoaded("minecolonies");
        boolean createLoaded = ModList.get().isLoaded("create");

        LOGGER.info("MineColonies detected: {}", minecoloniesLoaded);
        LOGGER.info("Create detected: {}", createLoaded);

        MinecraftForge.EVENT_BUS.register(GodsparkServerEvents.class);
        MinecraftForge.EVENT_BUS.register(GodsparkCommands.class);

        GodsparkNetwork.register();
    }

    private static AiConfig buildAiConfig() {
        return new AiConfig(
            GodsparkConfig.AI_ENABLED.get(),
            GodsparkConfig.AI_ENDPOINT.get(),
            GodsparkConfig.AI_MODEL.get(),
            GodsparkConfig.AI_TIMEOUT_MS.get(),
            GodsparkConfig.AI_COOLDOWN_TICKS.get(),
            GodsparkConfig.AI_MAX_TOKENS.get(),
            GodsparkConfig.AI_TEMPERATURE.get()
        );
    }

    public static AiConfig getCurrentAiConfig() {
        return buildAiConfig();
    }

    public static void reloadAiConfig() {
        AiConfig newConfig = buildAiConfig();
        AI_REFLECTION_SERVICE.reloadConfig(newConfig);
        DIVINE_ANSWER_INTERPRETER.reloadConfig(newConfig);
        LOGGER.info("[Godspark] AI config reloaded: enabled={}, endpoint={}, model={}",
            newConfig.enabled(), newConfig.endpoint(), newConfig.model());
    }
}
