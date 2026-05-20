package com.godspark.persistence;

import com.godspark.memory.ColonyMemory;
import com.godspark.memory.MemoryBank;
import com.godspark.memory.MemoryType;
import com.godspark.pressure.PressureModifier;
import com.godspark.pressure.PressureModifierManager;
import com.godspark.pressure.PressureType;
import com.godspark.sacred.SacredSiteManager;
import com.godspark.sacred.SacredSiteRecord;
import com.godspark.sacred.SacredSiteType;
import com.godspark.story.EventStateManager;
import com.godspark.story.EventState;
import com.godspark.story.EventSeverity;
import com.godspark.story.EventRecord;
import com.godspark.story.StoryEvent;
import com.godspark.story.StoryEventType;
import com.godspark.world.WorldEffectCooldownKey;
import com.godspark.world.WorldEffectCooldownRecord;
import com.godspark.world.WorldEffectEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.saveddata.SavedData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GodsparkSavedData extends SavedData {

    private static final Logger LOGGER = LogManager.getLogger(GodsparkSavedData.class);
    private static final int DATA_VERSION = 2;
    private static final int CURRENT_SCHEMA_VERSION = 5;
    // Schema compatibility:
    //   v2/no schema: events + memories only
    //   v3: adds sacred sites
    //   v4: adds miracle cooldowns + active pressure modifiers
    //   v5: adds world effect cooldowns (Phase 5D.2)
    //   future: load known safe sections, skip unknown
    private static final int SACRED_SECTION_VERSION = 1;
    private static final int MAX_RESOLVED_EVENTS = 500;
    private static final int MAX_MEMORIES_PER_COLONY = 50;

    private static final String TAG_SCHEMA_VERSION = "schemaVersion";
    private static final String TAG_SACRED = "sacred";
    private static final String TAG_SACRED_VERSION = "version";
    private static final String TAG_SACRED_SITES = "sites";
    private static final String TAG_MIRACLE_COOLDOWNS = "miracleCooldowns";
    private static final String TAG_ACTIVE_MODIFIERS = "activePressureModifiers";
    private static final String TAG_WORLD_EFFECTS = "worldEffects";
    private static final String TAG_WORLD_COOLDOWNS = "cooldowns";

    public static final String DATA_KEY = "godspark_data";

    public record SavedModifierRecord(
        int colonyId,
        PressureType pressureType,
        int amount,
        long createdAtTick,
        long remainingTicks,
        String source
    ) {}

    public record SavedEventRecord(
        StoryEventType eventType,
        int colonyId,
        String colonyName,
        PressureType pressureType,
        EventSeverity severity,
        int pressureValue,
        int threshold,
        String description,
        long gameTick,
        EventState state,
        int persistenceCount,
        int missingCycles,
        long firstSeenTick,
        long lastSeenTick,
        long resolvedTick
    ) {}

    private final Map<String, SavedEventRecord> activeEvents = new HashMap<>();
    private final Deque<SavedEventRecord> resolvedEvents = new ArrayDeque<>();
    private final Map<Integer, Deque<ColonyMemory>> memoriesByColony = new HashMap<>();
    private final Map<Integer, Long> miracleCooldowns = new HashMap<>();
    private final List<SavedModifierRecord> activeModifiers = new ArrayList<>();
    private final Map<WorldEffectCooldownKey, WorldEffectCooldownRecord> worldEffectCooldowns = new HashMap<>();
    private int loadedSchemaVersion;
    private boolean unsafeToOverwrite;

    public static GodsparkSavedData load(CompoundTag tag) {
        GodsparkSavedData data = new GodsparkSavedData();

        int schemaVersion = 0;
        if (tag.contains(TAG_SCHEMA_VERSION, Tag.TAG_ANY_NUMERIC)) {
            schemaVersion = tag.getInt(TAG_SCHEMA_VERSION);
        }

        if (schemaVersion > CURRENT_SCHEMA_VERSION) {
            LOGGER.warn("[Godspark SavedData] Save file schemaVersion={} > current={}. Loading defensively.",
                schemaVersion, CURRENT_SCHEMA_VERSION);
            data.unsafeToOverwrite = true;
        }

        data.loadedSchemaVersion = schemaVersion;

        if (tag.contains("activeEvents")) {
            ListTag activeList = tag.getList("activeEvents", Tag.TAG_COMPOUND);
            for (int i = 0; i < activeList.size(); i++) {
                CompoundTag eventTag = activeList.getCompound(i);
                SavedEventRecord record = readEventRecord(eventTag);
                if (record == null) continue;
                String key = record.colonyId() + ":" + record.pressureType().name();
                data.activeEvents.put(key, record);
            }
        }

        if (tag.contains("resolvedEvents")) {
            ListTag resolvedList = tag.getList("resolvedEvents", Tag.TAG_COMPOUND);
            for (int i = 0; i < resolvedList.size(); i++) {
                CompoundTag eventTag = resolvedList.getCompound(i);
                SavedEventRecord record = readEventRecord(eventTag);
                if (record == null) continue;
                data.resolvedEvents.addLast(record);
            }
        }

        data.trimResolvedEvents();

        if (tag.contains("memories")) {
            ListTag memoriesList = tag.getList("memories", Tag.TAG_COMPOUND);
            for (int i = 0; i < memoriesList.size(); i++) {
                CompoundTag memoryTag = memoriesList.getCompound(i);
                ColonyMemory memory = readMemoryRecord(memoryTag);
                if (memory == null) continue;
                data.memoriesByColony.computeIfAbsent(memory.colonyId(), k -> new ArrayDeque<>())
                    .addLast(memory);
            }
            data.trimMemories();
        }

        if (schemaVersion >= 3 && schemaVersion <= CURRENT_SCHEMA_VERSION
            && tag.contains(TAG_SACRED, Tag.TAG_COMPOUND)) {
            loadSacredSection(tag.getCompound(TAG_SACRED), data);
        }

        if (schemaVersion >= 4 && schemaVersion <= CURRENT_SCHEMA_VERSION) {
            if (tag.contains(TAG_MIRACLE_COOLDOWNS, Tag.TAG_LIST)) {
                loadMiracleCooldowns(tag.getList(TAG_MIRACLE_COOLDOWNS, Tag.TAG_COMPOUND), data);
            }
            if (tag.contains(TAG_ACTIVE_MODIFIERS, Tag.TAG_LIST)) {
                loadActiveModifiers(tag.getList(TAG_ACTIVE_MODIFIERS, Tag.TAG_COMPOUND), data);
            }
        }

        if (schemaVersion >= 5 && schemaVersion <= CURRENT_SCHEMA_VERSION) {
            if (tag.contains(TAG_WORLD_EFFECTS, Tag.TAG_COMPOUND)) {
                loadWorldEffectCooldowns(tag.getCompound(TAG_WORLD_EFFECTS), data);
            }
        }

        return data;
    }

    private static void loadSacredSection(CompoundTag sacredTag, GodsparkSavedData data) {
        int sacredVersion = 0;
        if (sacredTag.contains(TAG_SACRED_VERSION, Tag.TAG_ANY_NUMERIC)) {
            sacredVersion = sacredTag.getInt(TAG_SACRED_VERSION);
        }

        if (sacredVersion < 1 || sacredVersion > SACRED_SECTION_VERSION) {
            LOGGER.warn("[Godspark SavedData] Sacred section version={} not supported (expected {}). Skipping.",
                sacredVersion, SACRED_SECTION_VERSION);
            return;
        }

        if (!sacredTag.contains(TAG_SACRED_SITES, Tag.TAG_LIST)) return;

        ListTag sitesList = sacredTag.getList(TAG_SACRED_SITES, Tag.TAG_COMPOUND);
        int loaded = 0;
        int dropped = 0;

        for (int i = 0; i < sitesList.size(); i++) {
            CompoundTag siteTag = sitesList.getCompound(i);
            SacredSiteRecord record = readSacredSiteRecord(siteTag);
            if (record != null) {
                data.pendingSacredSites.add(record);
                loaded++;
            } else {
                dropped++;
            }
        }

        LOGGER.info("[Godspark SavedData] Loaded {} sacred sites ({} dropped)", loaded, dropped);
    }

    private static void loadMiracleCooldowns(ListTag list, GodsparkSavedData data) {
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            int colonyId = entry.getInt("colonyId");
            long remainingTicks = entry.getLong("remainingTicks");
            if (colonyId < 0 || remainingTicks <= 0) continue;
            data.miracleCooldowns.put(colonyId, remainingTicks);
        }
        LOGGER.info("[Godspark SavedData] Loaded {} miracle cooldowns", data.miracleCooldowns.size());
    }

    private static void loadActiveModifiers(ListTag list, GodsparkSavedData data) {
        int loaded = 0;
        int dropped = 0;
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            try {
                int colonyId = entry.getInt("colonyId");
                String typeName = entry.getString("pressureType");
                PressureType pressureType = parseEnumOrNull(PressureType.class, typeName);
                if (pressureType == null || !PressureModifierManager.isMiracleWhitelisted(pressureType)) {
                    dropped++;
                    continue;
                }
                int amount = entry.getInt("amount");
                long createdAtTick = entry.getLong("createdAtTick");
                long remainingTicks = entry.getLong("remainingTicks");
                String source = entry.getString("source");
                if (remainingTicks <= 0) { dropped++; continue; }
                data.activeModifiers.add(new SavedModifierRecord(
                    colonyId, pressureType, amount, createdAtTick, remainingTicks, source));
                loaded++;
            } catch (Exception e) {
                LOGGER.warn("[Godspark SavedData] Skipping invalid modifier record: {}", e.getMessage());
                dropped++;
            }
        }
        LOGGER.info("[Godspark SavedData] Loaded {} active modifiers ({} dropped)", loaded, dropped);
    }

    @Nullable
    private static SacredSiteRecord readSacredSiteRecord(CompoundTag tag) {
        try {
            String typeName = tag.getString("type");
            SacredSiteType type = parseEnumOrNull(SacredSiteType.class, typeName);
            if (type == null) {
                LOGGER.warn("[Godspark SavedData] Skipping sacred site with unknown type: {}", typeName);
                return null;
            }

            String dimStr = tag.getString("dimension");
            ResourceLocation dimension = ResourceLocation.tryParse(dimStr);
            if (dimension == null) {
                LOGGER.warn("[Godspark SavedData] Skipping sacred site with invalid dimension: {}", dimStr);
                return null;
            }

            int x = tag.getInt("x");
            int y = tag.getInt("y");
            int z = tag.getInt("z");
            BlockPos pos = new BlockPos(x, y, z);

            int colonyId = tag.getInt("colonyId");
            long registeredAtTick = tag.getLong("registeredAtTick");
            long lastSeenTick = tag.contains("lastSeenTick", Tag.TAG_ANY_NUMERIC)
                ? tag.getLong("lastSeenTick")
                : registeredAtTick;

            return new SacredSiteRecord(type, pos, dimension, colonyId, registeredAtTick, lastSeenTick);
        } catch (Exception e) {
            LOGGER.warn("[Godspark SavedData] Skipping invalid sacred site record: {}", e.getMessage());
            return null;
        }
    }

    private static CompoundTag writeSacredSiteRecord(SacredSiteRecord record) {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", record.type().name());
        tag.putString("dimension", record.dimension().toString());
        tag.putInt("x", record.pos().getX());
        tag.putInt("y", record.pos().getY());
        tag.putInt("z", record.pos().getZ());
        tag.putInt("colonyId", record.colonyId());
        tag.putLong("registeredAtTick", record.registeredAtTick());
        tag.putLong("lastSeenTick", record.lastSeenTick());
        return tag;
    }

    private final List<SacredSiteRecord> pendingSacredSites = new ArrayList<>();

    public static GodsparkSavedData createDefault() {
        return new GodsparkSavedData();
    }

    @Nullable
    private static SavedEventRecord readEventRecord(CompoundTag tag) {
        try {
            StoryEventType eventType = parseEnumOrNull(StoryEventType.class, tag.getString("eventType"));
            if (eventType == null) {
                LOGGER.warn("[Godspark SavedData] Skipping record with unknown eventType");
                return null;
            }

            PressureType pressureType = parseEnum(PressureType.class, tag.getString("pressureType"), eventType.pressureType());
            EventSeverity severity = parseEnum(EventSeverity.class, tag.getString("severity"), eventType.severity());
            EventState state = parseEnum(EventState.class, tag.getString("state"), EventState.ACTIVE);

            int threshold = tag.contains("threshold") ? tag.getInt("threshold") : eventType.threshold();
            String description = tag.contains("description") ? tag.getString("description") : eventType.description();
            long resolvedTick = tag.contains("resolvedTick") ? tag.getLong("resolvedTick") : -1L;

            return new SavedEventRecord(
                eventType,
                tag.getInt("colonyId"),
                tag.getString("colonyName"),
                pressureType,
                severity,
                tag.getInt("pressureValue"),
                threshold,
                description,
                tag.getLong("gameTick"),
                state,
                tag.getInt("persistenceCount"),
                tag.getInt("missingCycles"),
                tag.getLong("firstSeenTick"),
                tag.getLong("lastSeenTick"),
                resolvedTick
            );
        } catch (Exception e) {
            LOGGER.warn("[Godspark SavedData] Skipping invalid event record: {}", e.getMessage());
            return null;
        }
    }

    @Nullable
    private static <E extends Enum<E>> E parseEnumOrNull(Class<E> enumClass, String value) {
        try {
            return Enum.valueOf(enumClass, value);
        } catch (Exception e) {
            return null;
        }
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value, E fallback) {
        try {
            return Enum.valueOf(enumClass, value);
        } catch (Exception e) {
            return fallback;
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putInt(TAG_SCHEMA_VERSION, CURRENT_SCHEMA_VERSION);
        tag.putInt("version", DATA_VERSION);

        ListTag activeList = new ListTag();
        for (SavedEventRecord record : activeEvents.values()) {
            activeList.add(writeEventRecord(record));
        }
        tag.put("activeEvents", activeList);

        ListTag resolvedList = new ListTag();
        for (SavedEventRecord record : resolvedEvents) {
            resolvedList.add(writeEventRecord(record));
        }
        tag.put("resolvedEvents", resolvedList);

        ListTag memoriesList = new ListTag();
        for (Deque<ColonyMemory> colonyMemories : memoriesByColony.values()) {
            for (ColonyMemory memory : colonyMemories) {
                memoriesList.add(writeMemoryRecord(memory));
            }
        }
        tag.put("memories", memoriesList);

        if (sacredSites != null && !sacredSites.isEmpty()) {
            CompoundTag sacredTag = new CompoundTag();
            sacredTag.putInt(TAG_SACRED_VERSION, SACRED_SECTION_VERSION);
            ListTag sitesList = new ListTag();
            for (SacredSiteRecord record : sacredSites) {
                sitesList.add(writeSacredSiteRecord(record));
            }
            sacredTag.put(TAG_SACRED_SITES, sitesList);
            tag.put(TAG_SACRED, sacredTag);
        }

        if (!miracleCooldowns.isEmpty()) {
            ListTag cooldownList = new ListTag();
            for (Map.Entry<Integer, Long> entry : miracleCooldowns.entrySet()) {
                CompoundTag entryTag = new CompoundTag();
                entryTag.putInt("colonyId", entry.getKey());
                entryTag.putLong("remainingTicks", entry.getValue());
                cooldownList.add(entryTag);
            }
            tag.put(TAG_MIRACLE_COOLDOWNS, cooldownList);
        }

        if (!activeModifiers.isEmpty()) {
            ListTag modifiersList = new ListTag();
            for (SavedModifierRecord mod : activeModifiers) {
                CompoundTag modTag = new CompoundTag();
                modTag.putInt("colonyId", mod.colonyId());
                modTag.putString("pressureType", mod.pressureType().name());
                modTag.putInt("amount", mod.amount());
                modTag.putLong("createdAtTick", mod.createdAtTick());
                modTag.putLong("remainingTicks", mod.remainingTicks());
                modTag.putString("source", mod.source());
                modifiersList.add(modTag);
            }
            tag.put(TAG_ACTIVE_MODIFIERS, modifiersList);
        }

        if (!worldEffectCooldowns.isEmpty()) {
            CompoundTag worldEffectsTag = new CompoundTag();
            worldEffectsTag.putInt("version", 1);
            ListTag cooldownList = new ListTag();
            for (Map.Entry<WorldEffectCooldownKey, WorldEffectCooldownRecord> entry : worldEffectCooldowns.entrySet()) {
                CompoundTag entryTag = new CompoundTag();
                entryTag.putString("dimension", entry.getKey().dimension().toString());
                entryTag.putInt("colonyId", entry.getKey().colonyId());
                entryTag.putLong("lastFireTick", entry.getValue().lastFireTick());
                entryTag.putString("lastEffect", entry.getValue().lastEffect());
                entryTag.putInt("lastAffectedCount", entry.getValue().lastAffectedCount());
                cooldownList.add(entryTag);
            }
            worldEffectsTag.put(TAG_WORLD_COOLDOWNS, cooldownList);
            tag.put(TAG_WORLD_EFFECTS, worldEffectsTag);
        }

        return tag;
    }

    private static CompoundTag writeEventRecord(SavedEventRecord record) {
        CompoundTag tag = new CompoundTag();
        tag.putString("eventType", record.eventType().name());
        tag.putInt("colonyId", record.colonyId());
        tag.putString("colonyName", record.colonyName());
        tag.putString("pressureType", record.pressureType().name());
        tag.putString("severity", record.severity().name());
        tag.putInt("pressureValue", record.pressureValue());
        tag.putInt("threshold", record.threshold());
        tag.putString("description", record.description());
        tag.putLong("gameTick", record.gameTick());
        tag.putString("state", record.state().name());
        tag.putInt("persistenceCount", record.persistenceCount());
        tag.putInt("missingCycles", record.missingCycles());
        tag.putLong("firstSeenTick", record.firstSeenTick());
        tag.putLong("lastSeenTick", record.lastSeenTick());
        tag.putLong("resolvedTick", record.resolvedTick());
        return tag;
    }

    @Nullable
    private static ColonyMemory readMemoryRecord(CompoundTag tag) {
        try {
            MemoryType memoryType = parseEnum(MemoryType.class, tag.getString("memoryType"), MemoryType.SIGNIFICANT_EVENT);
            PressureType pressureType = parseEnum(PressureType.class, tag.getString("pressureType"), PressureType.FOOD);
            EventSeverity severity = parseEnum(EventSeverity.class, tag.getString("severity"), EventSeverity.LOW);

            return new ColonyMemory(
                tag.getInt("colonyId"),
                tag.getString("colonyName"),
                memoryType,
                pressureType,
                severity,
                tag.getString("content"),
                tag.getInt("intensity"),
                tag.getInt("reinforcementCount"),
                tag.getLong("createdAtTick"),
                tag.getLong("lastRecalledTick"),
                tag.getLong("lastReinforcedTick"),
                tag.getInt("decayRate")
            );
        } catch (Exception e) {
            LOGGER.warn("[Godspark SavedData] Skipping invalid memory record: {}", e.getMessage());
            return null;
        }
    }

    private static CompoundTag writeMemoryRecord(ColonyMemory memory) {
        CompoundTag tag = new CompoundTag();
        tag.putString("memoryType", memory.memoryType().name());
        tag.putInt("colonyId", memory.colonyId());
        tag.putString("colonyName", memory.colonyName());
        tag.putString("pressureType", memory.pressureType().name());
        tag.putString("severity", memory.severity().name());
        tag.putString("content", memory.content());
        tag.putInt("intensity", memory.intensity());
        tag.putInt("reinforcementCount", memory.reinforcementCount());
        tag.putLong("createdAtTick", memory.createdAtTick());
        tag.putLong("lastRecalledTick", memory.lastRecalledTick());
        tag.putLong("lastReinforcedTick", memory.lastReinforcedTick());
        tag.putInt("decayRate", memory.decayRate());
        return tag;
    }

    private void trimMemories() {
        for (Map.Entry<Integer, Deque<ColonyMemory>> entry : memoriesByColony.entrySet()) {
            while (entry.getValue().size() > MAX_MEMORIES_PER_COLONY) {
                entry.getValue().removeLast();
            }
        }
    }

    public void restoreTo(EventStateManager manager) {
        manager.clear();

        for (SavedEventRecord saved : activeEvents.values()) {
            StoryEvent event = new StoryEvent(
                saved.eventType(),
                saved.colonyId(),
                saved.colonyName(),
                saved.pressureType(),
                saved.severity(),
                saved.pressureValue(),
                saved.threshold(),
                saved.description(),
                saved.gameTick()
            );
            EventRecord record = new EventRecord(
                event,
                saved.state(),
                saved.persistenceCount(),
                saved.missingCycles(),
                saved.firstSeenTick(),
                saved.lastSeenTick(),
                saved.resolvedTick()
            );
            if (record.isResolved()) {
                manager.restoreResolved(record);
            } else {
                manager.restoreActive(record);
            }
        }

        for (SavedEventRecord saved : resolvedEvents) {
            StoryEvent event = new StoryEvent(
                saved.eventType(),
                saved.colonyId(),
                saved.colonyName(),
                saved.pressureType(),
                saved.severity(),
                saved.pressureValue(),
                saved.threshold(),
                saved.description(),
                saved.gameTick()
            );
            EventRecord record = new EventRecord(
                event,
                saved.state(),
                saved.persistenceCount(),
                saved.missingCycles(),
                saved.firstSeenTick(),
                saved.lastSeenTick(),
                saved.resolvedTick()
            );
            if (record.isResolved()) {
                manager.restoreResolved(record);
            } else {
                manager.restoreActive(record);
            }
        }

        LOGGER.info(
            "[Godspark SavedData] Restored {} active, {} resolved events",
            activeEvents.size(),
            resolvedEvents.size()
        );
    }

    public void captureFrom(EventStateManager manager) {
        activeEvents.clear();
        resolvedEvents.clear();

        for (EventRecord record : manager.getAllActive()) {
            StoryEvent event = record.event();
            SavedEventRecord saved = new SavedEventRecord(
                event.eventType(),
                event.colonyId(),
                event.colonyName(),
                event.pressureType(),
                event.severity(),
                event.pressureValue(),
                event.threshold(),
                event.description(),
                event.gameTick(),
                record.state(),
                record.persistenceCount(),
                record.missingCycles(),
                record.firstSeenTick(),
                record.lastSeenTick(),
                record.resolvedTick()
            );
            String key = event.colonyId() + ":" + event.pressureType().name();
            activeEvents.put(key, saved);
        }

        for (EventRecord record : manager.getAllResolved()) {
            StoryEvent event = record.event();
            SavedEventRecord saved = new SavedEventRecord(
                event.eventType(),
                event.colonyId(),
                event.colonyName(),
                event.pressureType(),
                event.severity(),
                event.pressureValue(),
                event.threshold(),
                event.description(),
                event.gameTick(),
                record.state(),
                record.persistenceCount(),
                record.missingCycles(),
                record.firstSeenTick(),
                record.lastSeenTick(),
                record.resolvedTick()
            );
            resolvedEvents.addLast(saved);
        }

        trimResolvedEvents();
        setDirty();
    }

    public void captureMemoriesFrom(MemoryBank memoryBank) {
        memoriesByColony.clear();
        Map<Integer, Deque<ColonyMemory>> allMemories = memoryBank.getAllMemoriesByColony();
        for (Map.Entry<Integer, Deque<ColonyMemory>> entry : allMemories.entrySet()) {
            Deque<ColonyMemory> trimmed = new ArrayDeque<>();
            for (ColonyMemory memory : entry.getValue()) {
                trimmed.addLast(memory);
            }
            while (trimmed.size() > MAX_MEMORIES_PER_COLONY) {
                trimmed.removeFirst();
            }
            memoriesByColony.put(entry.getKey(), trimmed);
        }
        setDirty();
    }

    public void restoreMemoriesTo(MemoryBank memoryBank) {
        memoryBank.clear();
        Map<Integer, List<ColonyMemory>> memoriesToLoad = new HashMap<>();
        for (Map.Entry<Integer, Deque<ColonyMemory>> entry : memoriesByColony.entrySet()) {
            memoriesToLoad.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        memoryBank.loadMemories(memoriesToLoad);
        LOGGER.info(
            "[Godspark SavedData] Restored {} colony memories",
            memoriesByColony.values().stream().mapToInt(Deque::size).sum()
        );
    }

    private List<SacredSiteRecord> sacredSites;

    public void captureSacredSites(SacredSiteManager manager) {
        sacredSites = new ArrayList<>(manager.snapshotSites());
        setDirty();
        LOGGER.info("[Godspark SavedData] Captured {} sacred sites", sacredSites.size());
    }

    public void restoreSacredSitesTo(SacredSiteManager manager) {
        if (!pendingSacredSites.isEmpty()) {
            manager.restoreFrom(pendingSacredSites);
            pendingSacredSites.clear();
        } else if (sacredSites != null && !sacredSites.isEmpty()) {
            manager.restoreFrom(sacredSites);
        }
    }

    public void captureModifiersFrom(PressureModifierManager manager, long currentTick) {
        miracleCooldowns.clear();
        activeModifiers.clear();

        for (Map.Entry<Integer, Long> entry : manager.getCooldownEntries().entrySet()) {
            long remainingTicks = Math.max(0,
                PressureModifierManager.MIRACLE_COOLDOWN_TICKS - (currentTick - entry.getValue()));
            miracleCooldowns.put(entry.getKey(), remainingTicks);
        }
        for (PressureModifier mod : manager.getAllModifiers()) {
            long remainingTicks = Math.max(0, mod.expiresAtTick() - currentTick);
            activeModifiers.add(new SavedModifierRecord(
                mod.colonyId(), mod.pressureType(), mod.amount(),
                mod.createdAtTick(), remainingTicks, mod.source()));
        }

        setDirty();
        LOGGER.info("[Godspark SavedData] Captured {} cooldowns, {} active modifiers",
            miracleCooldowns.size(), activeModifiers.size());
    }

    public void restoreModifiersTo(PressureModifierManager manager, long currentTick) {
        manager.restoreCooldownRemaining(miracleCooldowns, currentTick);
        List<PressureModifier> mods = new ArrayList<>();
        for (SavedModifierRecord rec : activeModifiers) {
            long expiresAtTick = currentTick + rec.remainingTicks();
            mods.add(new PressureModifier(
                rec.colonyId(), rec.pressureType(), rec.amount(),
                rec.createdAtTick(), expiresAtTick, rec.source()));
        }
        manager.restoreModifiers(mods, currentTick);
    }

    private static void loadWorldEffectCooldowns(CompoundTag worldEffectsTag, GodsparkSavedData data) {
        int version = worldEffectsTag.contains("version", Tag.TAG_ANY_NUMERIC)
            ? worldEffectsTag.getInt("version") : 0;
        if (version < 1) return;

        if (!worldEffectsTag.contains(TAG_WORLD_COOLDOWNS, Tag.TAG_LIST)) return;

        ListTag list = worldEffectsTag.getList(TAG_WORLD_COOLDOWNS, Tag.TAG_COMPOUND);
        int loaded = 0;
        int skipped = 0;

        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            try {
                String dimStr = entry.getString("dimension");
                ResourceLocation dimension = ResourceLocation.tryParse(dimStr);
                if (dimension == null) { skipped++; continue; }

                int colonyId = entry.getInt("colonyId");
                long lastFireTick = entry.getLong("lastFireTick");
                String lastEffect = entry.getString("lastEffect");
                int lastAffectedCount = entry.getInt("lastAffectedCount");

                WorldEffectCooldownKey key = new WorldEffectCooldownKey(dimension, colonyId);
                data.worldEffectCooldowns.put(key, new WorldEffectCooldownRecord(key, lastFireTick, lastEffect, lastAffectedCount));
                loaded++;
            } catch (Exception e) {
                LOGGER.warn("[Godspark SavedData] Skipping invalid world effect cooldown: {}", e.getMessage());
                skipped++;
            }
        }

        LOGGER.info("[Godspark SavedData] Loaded {} world effect cooldowns ({} skipped)", loaded, skipped);
    }

    public void captureWorldEffectCooldownsFrom(WorldEffectEngine engine) {
        worldEffectCooldowns.clear();
        worldEffectCooldowns.putAll(engine.snapshotCooldowns());
        setDirty();
        LOGGER.info("[Godspark SavedData] Captured {} world effect cooldowns", worldEffectCooldowns.size());
    }

    public void restoreWorldEffectCooldownsTo(WorldEffectEngine engine) {
        engine.restoreCooldowns(new HashMap<>(worldEffectCooldowns));
    }

    private void trimResolvedEvents() {
        while (resolvedEvents.size() > MAX_RESOLVED_EVENTS) {
            resolvedEvents.removeFirst();
        }
    }
}