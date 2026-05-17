package com.godspark.prayer;

import com.godspark.memory.ColonyMemory;
import com.godspark.memory.MemoryBank;
import com.godspark.memory.MemoryInfluence;
import com.godspark.memory.MemoryType;
import com.godspark.observer.ColonySnapshot;
import com.godspark.observer.ObservedColony;
import com.godspark.personality.ColonyPersonality;
import com.godspark.pressure.PressureSnapshot;
import com.godspark.pressure.PressureType;
import com.godspark.story.EventRecord;
import com.godspark.story.EventSeverity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PrayerSeedGenerator {

    private static final int MAX_PRAYERS_PER_CYCLE = 3;
    private static final long PRAYER_TTL_TICKS = 12000L;

    private static final int PERSISTENT_BONUS = 10;
    private static final int TRAUMA_BONUS = 10;
    private static final int RAID_BONUS = 10;
    private static final int THANKS_PERSISTENCE_BONUS = 5;

    public List<PrayerSeed> generate(
        Map<Integer, PressureSnapshot> pressureSnapshots,
        Map<Integer, ObservedColony> observedColonies,
        List<EventRecord> activeEvents,
        List<EventRecord> transitions,
        MemoryBank memoryBank,
        MemoryInfluence memoryInfluence,
        Map<Integer, ColonyPersonality> personalities,
        Map<Integer, Boolean> prayerStoneAnchors,
        long gameTick
    ) {
        if (pressureSnapshots == null || pressureSnapshots.isEmpty()) {
            return List.of();
        }

        List<PrayerSeed> seeds = new ArrayList<>();

        for (Map.Entry<Integer, PressureSnapshot> entry : pressureSnapshots.entrySet()) {
            int colonyId = entry.getKey();
            PressureSnapshot pressureSnapshot = entry.getValue();
            ObservedColony observedColony = observedColonies.get(colonyId);

            if (observedColony == null || observedColony.getLatest() == null) {
                continue;
            }

            ColonySnapshot colonySnapshot = observedColony.getLatest();
            ColonyPersonality personality = personalities != null ? personalities.get(colonyId) : null;
            boolean hasPrayerStoneAnchor = prayerStoneAnchors != null
                && prayerStoneAnchors.getOrDefault(colonyId, false);

            List<EventRecord> colonyActiveEvents = activeEvents.stream()
                .filter(r -> r.event().colonyId() == colonyId)
                .toList();

            List<EventRecord> colonyTransitions = transitions.stream()
                .filter(r -> r.event().colonyId() == colonyId)
                .toList();

            List<PrayerSeed> colonySeeds = generateForColony(
                colonyId, colonySnapshot, pressureSnapshot,
                colonyActiveEvents, colonyTransitions, memoryBank,
                memoryInfluence, personality, hasPrayerStoneAnchor, gameTick
            );

            seeds.addAll(colonySeeds);
        }

        return seeds;
    }

    private List<PrayerSeed> generateForColony(
        int colonyId,
        ColonySnapshot colonySnapshot,
        PressureSnapshot pressureSnapshot,
        List<EventRecord> activeEvents,
        List<EventRecord> transitions,
        MemoryBank memoryBank,
        MemoryInfluence memoryInfluence,
        ColonyPersonality personality,
        boolean hasPrayerStoneAnchor,
        long gameTick
    ) {
        Map<PressureType, Integer> thresholdAdjustments = memoryBank.isEmpty()
            ? Map.of()
            : memoryInfluence.computeAdjustments(colonyId, memoryBank);

        PrayerTone tone = personality != null
            ? PrayerTone.fromTrait(personality.primaryTrait())
            : PrayerTone.STOIC;
        PrayerChannel channel = selectPrayerChannel(colonySnapshot, personality, hasPrayerStoneAnchor);

        List<PrayerSeed> seeds = new ArrayList<>();
        String colonyName = colonySnapshot.name();

        List<PrayerSeed> thanksSeeds = generateThanks(colonyId, colonyName, channel, tone, personality, transitions, memoryBank, gameTick);
        seeds.addAll(thanksSeeds);

        List<PrayerSeed> vigilSeeds = generateVigil(colonyId, colonyName, channel, tone, personality, colonySnapshot, pressureSnapshot, activeEvents, thresholdAdjustments, gameTick);
        seeds.addAll(vigilSeeds);

        List<PrayerSeed> lamentSeeds = generateLament(colonyId, colonyName, channel, tone, personality, pressureSnapshot, activeEvents, memoryBank, thresholdAdjustments, gameTick);
        seeds.addAll(lamentSeeds);

        List<PrayerSeed> pleaSeeds = generatePlea(colonyId, colonyName, channel, tone, personality, pressureSnapshot, activeEvents, memoryBank, thresholdAdjustments, gameTick);
        seeds.addAll(pleaSeeds);

        if (seeds.size() < MAX_PRAYERS_PER_CYCLE) {
            List<PrayerSeed> hopeSeeds = generateHope(colonyId, colonyName, channel, tone, personality, pressureSnapshot, thresholdAdjustments, gameTick);
            seeds.addAll(hopeSeeds);
        }

        seeds.sort((a, b) -> Integer.compare(b.intensity(), a.intensity()));

        return seeds.subList(0, Math.min(seeds.size(), MAX_PRAYERS_PER_CYCLE));
    }

    public static PrayerChannel selectPrayerChannel(ColonySnapshot colonySnapshot, ColonyPersonality personality,
                                                    boolean hasPrayerStoneAnchor) {
        return selectPrayerChannel(
            colonySnapshot.sacredBuildingCount(),
            colonySnapshot.gatheringBuildingCount(),
            hasPrayerStoneAnchor
        );
    }

    public static PrayerChannel selectPrayerChannel(int trueSacredBuildings, int gatheringBuildings,
                                                    boolean hasPrayerStoneAnchor) {

        if (trueSacredBuildings >= 3) return PrayerChannel.TEMPLE;
        if (trueSacredBuildings >= 2) return PrayerChannel.SHRINE;
        if (trueSacredBuildings >= 1) return PrayerChannel.CHURCH;
        if (hasPrayerStoneAnchor) return PrayerChannel.CHURCH;
        if (gatheringBuildings > 0) return PrayerChannel.COMMONS;
        return PrayerChannel.PRIVATE;
    }

    // ── Channel helpers ──

    private static int applyChannelIntensity(int intensity, PrayerChannel channel) {
        if (channel == PrayerChannel.PRIVATE) {
            return Math.max(1, intensity / 2);
        }
        return intensity;
    }

    private static int clampIntensity(int value) {
        return Math.max(1, Math.min(100, value));
    }

    private static void addChannelReasonCodes(List<String> reasonCodes, PrayerChannel channel) {
        switch (channel) {
            case PRIVATE -> {
                reasonCodes.add("NO_SACRED_SITE");
                reasonCodes.add("PRIVATE_MURMUR");
            }
            case COMMONS -> {
                reasonCodes.add("GATHERING_PLACE_PRESENT");
                reasonCodes.add("PUBLIC_COMMON_PRAYER");
                reasonCodes.add("NOT_SACRED_MIRACLE_ELIGIBLE");
            }
            case CHURCH, SHRINE, TEMPLE -> {
                reasonCodes.add("SACRED_SITE_PRESENT");
                reasonCodes.add("PUBLIC_PRAYER");
                reasonCodes.add("PRAYER_CHANNEL_" + channel.name());
            }
        }
    }

    private static void addPersonalityReasonCodes(List<String> reasonCodes, ColonyPersonality personality, PrayerTone tone) {
        if (personality == null) {
            reasonCodes.add("PERSONALITY_DEFAULT_STOIC");
            return;
        }
        reasonCodes.add("PERSONALITY_TRAIT_" + personality.primaryTrait().name());
        reasonCodes.add("PERSONALITY_TONE_" + tone.name());
    }

    // ── Prayer generators ──

    private List<PrayerSeed> generateThanks(
        int colonyId, String colonyName, PrayerChannel channel, PrayerTone tone, ColonyPersonality personality,
        List<EventRecord> transitions, MemoryBank memoryBank,
        long gameTick
    ) {
        List<PrayerSeed> seeds = new ArrayList<>();

        for (EventRecord record : transitions) {
            if (!record.isResolved() || record.persistenceCount() < 3) {
                continue;
            }

            PressureType pressureType = record.event().pressureType();
            int intensity = clampIntensity(50 + record.persistenceCount() * THANKS_PERSISTENCE_BONUS + tone.getIntensityMod());
            intensity = applyChannelIntensity(intensity, channel);

            List<String> reasonCodes = new ArrayList<>();
            reasonCodes.add("EVENT_RESOLVED");
            if (record.persistenceCount() >= 3) {
                reasonCodes.add("EVENT_WAS_PERSISTENT");
            }
            addChannelReasonCodes(reasonCodes, channel);
            addPersonalityReasonCodes(reasonCodes, personality, tone);

            String content = formatThanks(colonyName, pressureType, channel, tone);
            String sourceKey = "resolved:" + colonyId + ":" + pressureType.name() + ":THANKS";

            seeds.add(new PrayerSeed(
                colonyId, colonyName, PrayerType.THANKS, channel,
                pressureType, record.event().severity(),
                record.event().pressureValue(), intensity, 0,
                content, reasonCodes,
                gameTick, gameTick + PRAYER_TTL_TICKS, sourceKey
            ));
        }

        return seeds;
    }

    private List<PrayerSeed> generateVigil(
        int colonyId, String colonyName, PrayerChannel channel, PrayerTone tone, ColonyPersonality personality,
        ColonySnapshot colonySnapshot,
        PressureSnapshot pressureSnapshot,
        List<EventRecord> activeEvents,
        Map<PressureType, Integer> thresholdAdjustments,
        long gameTick
    ) {
        List<PrayerSeed> seeds = new ArrayList<>();

        if (!colonySnapshot.hasActiveRaid()) {
            int securityPressure = pressureSnapshot.values().getOrDefault(PressureType.SECURITY, 0);
            boolean hasSecurityEvent = activeEvents.stream()
                .anyMatch(e -> e.event().pressureType() == PressureType.SECURITY && !e.isResolved());

            if (securityPressure < 80 && !hasSecurityEvent) {
                return seeds;
            }
        }

        int securityPressure = pressureSnapshot.values().getOrDefault(PressureType.SECURITY, 0);
        int intensity = clampIntensity(securityPressure + RAID_BONUS + tone.getIntensityMod());

        if (colonySnapshot.hasActiveRaid()) {
            intensity = clampIntensity(intensity + RAID_BONUS);
        }

        boolean persistentSecurity = activeEvents.stream()
            .anyMatch(e -> e.event().pressureType() == PressureType.SECURITY && e.isPersistent());
        if (persistentSecurity) {
            intensity = clampIntensity(intensity + PERSISTENT_BONUS);
        }

        List<String> reasonCodes = new ArrayList<>();
        reasonCodes.add("SECURITY_PRESSURE_HIGH");

        if (colonySnapshot.hasActiveRaid()) {
            reasonCodes.add("ACTIVE_RAID");
        }
        if (persistentSecurity) {
            reasonCodes.add("EVENT_PERSISTENT");
        }

        int memoryInfluence = thresholdAdjustments.getOrDefault(PressureType.SECURITY, 0);
        if (memoryInfluence != 0) {
            reasonCodes.add("MEMORY_INFLUENCE_ACTIVE");
        }
        intensity = applyMemorySensitivity(intensity, memoryInfluence);
        intensity = applyChannelIntensity(intensity, channel);
        addChannelReasonCodes(reasonCodes, channel);
        addPersonalityReasonCodes(reasonCodes, personality, tone);

        String content = formatVigil(colonyName, channel, tone);
        String sourceKey = "event:" + colonyId + ":SECURITY:VIGIL";

        seeds.add(new PrayerSeed(
            colonyId, colonyName, PrayerType.VIGIL, channel,
            PressureType.SECURITY, EventSeverity.HIGH,
            securityPressure, intensity, memoryInfluence,
            content, reasonCodes,
            gameTick, gameTick + PRAYER_TTL_TICKS, sourceKey
        ));

        return seeds;
    }

    private List<PrayerSeed> generateLament(
        int colonyId, String colonyName, PrayerChannel channel, PrayerTone tone, ColonyPersonality personality,
        PressureSnapshot pressureSnapshot,
        List<EventRecord> activeEvents,
        MemoryBank memoryBank,
        Map<PressureType, Integer> thresholdAdjustments,
        long gameTick
    ) {
        List<PrayerSeed> seeds = new ArrayList<>();

        for (PressureType pressureType : PressureType.values()) {
            List<ColonyMemory> traumaMemories = memoryBank.getMemories(colonyId).stream()
                .filter(m -> m.memoryType() == MemoryType.TRAUMA && m.pressureType() == pressureType)
                .toList();

            if (traumaMemories.isEmpty()) {
                continue;
            }

            int pressureValue = pressureSnapshot.values().getOrDefault(pressureType, 0);
            if (pressureValue < 40) {
                continue;
            }

            int intensity = clampIntensity(pressureValue + TRAUMA_BONUS + tone.getIntensityMod());

            boolean persistentEvent = activeEvents.stream()
                .anyMatch(e -> e.event().pressureType() == pressureType && e.isPersistent());
            if (persistentEvent) {
                intensity = clampIntensity(intensity + PERSISTENT_BONUS);
            }

            int memoryInfluence = thresholdAdjustments.getOrDefault(pressureType, 0);

            List<String> reasonCodes = new ArrayList<>();
            reasonCodes.add("TRAUMA_MEMORY");
            if (persistentEvent) {
                reasonCodes.add("EVENT_PERSISTENT");
            }
            if (memoryInfluence != 0) {
                reasonCodes.add("MEMORY_INFLUENCE_ACTIVE");
            }
            intensity = applyMemorySensitivity(intensity, memoryInfluence);
            intensity = applyChannelIntensity(intensity, channel);
            addChannelReasonCodes(reasonCodes, channel);
            addPersonalityReasonCodes(reasonCodes, personality, tone);

            EventSeverity severity = pressureValue >= 70 ? EventSeverity.HIGH : EventSeverity.MEDIUM;
            String content = formatLament(colonyName, pressureType, channel, tone);
            String sourceKey = "memory:" + colonyId + ":" + pressureType.name() + ":LAMENT";

            seeds.add(new PrayerSeed(
                colonyId, colonyName, PrayerType.LAMENT, channel,
                pressureType, severity,
                pressureValue, intensity, memoryInfluence,
                content, reasonCodes,
                gameTick, gameTick + PRAYER_TTL_TICKS, sourceKey
            ));
        }

        return seeds;
    }

    private List<PrayerSeed> generatePlea(
        int colonyId, String colonyName, PrayerChannel channel, PrayerTone tone, ColonyPersonality personality,
        PressureSnapshot pressureSnapshot,
        List<EventRecord> activeEvents,
        MemoryBank memoryBank,
        Map<PressureType, Integer> thresholdAdjustments,
        long gameTick
    ) {
        List<PrayerSeed> seeds = new ArrayList<>();

        for (EventRecord record : activeEvents) {
            if (record.isResolved()) {
                continue;
            }

            PressureType pressureType = record.event().pressureType();
            int pressureValue = pressureSnapshot.values().getOrDefault(pressureType, 0);

            boolean hasTrauma = memoryBank.getMemories(colonyId).stream()
                .anyMatch(m -> m.memoryType() == MemoryType.TRAUMA && m.pressureType() == pressureType);

            int intensity = pressureValue + tone.getIntensityMod();
            if (record.isPersistent()) {
                intensity = clampIntensity(intensity + PERSISTENT_BONUS);
            }
            if (hasTrauma) {
                intensity = clampIntensity(intensity + TRAUMA_BONUS);
            }

            int memoryInfluence = thresholdAdjustments.getOrDefault(pressureType, 0);
            intensity = applyMemorySensitivity(intensity, memoryInfluence);
            intensity = applyChannelIntensity(intensity, channel);

            List<String> reasonCodes = new ArrayList<>();
            String prefix = record.isPersistent() ? pressureType.getDisplayName().toUpperCase(Locale.ROOT) + "_PRESSURE_HIGH" : pressureType.getDisplayName().toUpperCase(Locale.ROOT) + "_PRESSURE_" + record.event().severity().name();
            reasonCodes.add(prefix);
            if (record.isPersistent()) {
                reasonCodes.add("EVENT_PERSISTENT");
            } else {
                reasonCodes.add("EVENT_ACTIVE");
            }
            if (hasTrauma) {
                reasonCodes.add("TRAUMA_MEMORY");
            }
            if (memoryInfluence != 0) {
                reasonCodes.add("MEMORY_INFLUENCE_ACTIVE");
            }
            addChannelReasonCodes(reasonCodes, channel);
            addPersonalityReasonCodes(reasonCodes, personality, tone);

            String content = formatPlea(colonyName, pressureType, channel, tone);
            String sourceKey = "event:" + colonyId + ":" + pressureType.name() + ":" + (record.isPersistent() ? "PERSISTENT" : "ACTIVE");

            seeds.add(new PrayerSeed(
                colonyId, colonyName, PrayerType.PLEA, channel,
                pressureType, record.event().severity(),
                pressureValue, intensity, memoryInfluence,
                content, reasonCodes,
                gameTick, gameTick + PRAYER_TTL_TICKS, sourceKey
            ));
        }

        return seeds;
    }

    private List<PrayerSeed> generateHope(
        int colonyId, String colonyName, PrayerChannel channel, PrayerTone tone, ColonyPersonality personality,
        PressureSnapshot pressureSnapshot,
        Map<PressureType, Integer> thresholdAdjustments,
        long gameTick
    ) {
        List<PrayerSeed> seeds = new ArrayList<>();

        for (PressureType pressureType : PressureType.values()) {
            int pressureValue = pressureSnapshot.values().getOrDefault(pressureType, 0);
            if (pressureValue < 25 || pressureValue > 60) {
                continue;
            }

            int intensity = pressureValue + tone.getIntensityMod();
            int memoryInfluence = thresholdAdjustments.getOrDefault(pressureType, 0);
            intensity = applyMemorySensitivity(intensity, memoryInfluence);
            intensity = applyChannelIntensity(intensity, channel);

            List<String> reasonCodes = new ArrayList<>();
            reasonCodes.add(pressureType.getDisplayName().toUpperCase(Locale.ROOT) + "_PRESSURE_LOW");
            addChannelReasonCodes(reasonCodes, channel);
            addPersonalityReasonCodes(reasonCodes, personality, tone);

            String content = formatHope(colonyName, pressureType, channel, tone);
            String sourceKey = "pressure:" + colonyId + ":" + pressureType.name() + ":HOPE";

            seeds.add(new PrayerSeed(
                colonyId, colonyName, PrayerType.HOPE, channel,
                pressureType, EventSeverity.LOW,
                pressureValue, intensity, memoryInfluence,
                content, reasonCodes,
                gameTick, gameTick + PRAYER_TTL_TICKS, sourceKey
            ));
        }

        return seeds;
    }

    // ── Channel-aware text formatters ──

    String formatPlea(String colonyName, PressureType pressureType, PrayerChannel channel, PrayerTone tone) {
        String phrase = tone.pickPhrase(colonyName.hashCode() + pressureType.ordinal());
        if (channel == PrayerChannel.PRIVATE) {
            return switch (pressureType) {
                case FOOD -> phrase + ", whispers pass through " + colonyName + " of empty bowls.";
                case SECURITY -> phrase + ", fears move through " + colonyName + " when night falls.";
                case HOUSING -> "Those without shelter in " + colonyName + " murmur, " + phrase + ", of roofs they do not yet have.";
                case COMFORT -> "In " + colonyName + ", sorrow is carried " + phrase + " from door to door.";
                case INDUSTRY -> "Workers in " + colonyName + " " + phrase + " hope for tools and hands enough for the work.";
            };
        }
        // COMMONS is public civic voice, not a sacred channel.
        // Do not use chapel/shrine/altar/sacred-location wording here.
        if (channel == PrayerChannel.COMMONS) {
            return switch (pressureType) {
                case FOOD -> "At the town square, " + colonyName + " voices concern " + phrase + " for full granaries and steady hands.";
                case SECURITY -> "By the colony gate, " + colonyName + " calls " + phrase + " for shields and watchful eyes against the darkness.";
                case HOUSING -> "In the meeting hall, " + colonyName + " seeks shelter " + phrase + " for all who dwell within its walls.";
                case COMFORT -> "Around the common hearth, " + colonyName + " longs " + phrase + " for peace and contentment among its people.";
                case INDUSTRY -> "Near the market stalls, " + colonyName + " voices need " + phrase + " for forge and farm to meet the demands of all.";
            };
        }
        return switch (pressureType) {
            case FOOD -> "At the chapel, " + colonyName + " offers prayers " + phrase + " for full granaries and steady hands.";
            case SECURITY -> "At the chapel, " + colonyName + " calls " + phrase + " for shields and watchful eyes against the darkness.";
            case HOUSING -> "At the sacred hall, " + colonyName + " seeks shelter " + phrase + " for all who dwell within its walls.";
            case COMFORT -> "At the chapel, " + colonyName + " longs " + phrase + " for peace and contentment among its people.";
            case INDUSTRY -> "At the chapel, " + colonyName + " offers prayers " + phrase + " for forge and farm to meet the needs of all.";
        };
    }

    String formatVigil(String colonyName, PrayerChannel channel, PrayerTone tone) {
        String phrase = tone.pickPhrase(colonyName.hashCode() + 100);
        if (channel == PrayerChannel.PRIVATE) {
            return phrase + ", fears move through " + colonyName + " when night falls.";
        }
        // COMMONS is public civic voice, not a sacred channel.
        // Do not use chapel/shrine/altar/sacred-location wording here.
        if (channel == PrayerChannel.COMMONS) {
            return "Torches burn late at the watch post. " + colonyName + " keeps vigil " + phrase + " against the dark.";
        }
        return "Torches burn late around the chapel. " + colonyName + " keeps vigil " + phrase + " against the dark.";
    }

    String formatLament(String colonyName, PressureType pressureType, PrayerChannel channel, PrayerTone tone) {
        String phrase = tone.pickPhrase(colonyName.hashCode() + pressureType.ordinal() + 200);
        if (channel == PrayerChannel.PRIVATE) {
            return switch (pressureType) {
                case FOOD -> "Some families in " + colonyName + " whisper, " + phrase + ", of empty bowls and old hunger.";
                case SECURITY -> phrase + ", fears move through " + colonyName + " — old wounds remembered in silence.";
                case HOUSING -> "Those without shelter in " + colonyName + " murmur, " + phrase + ", of roofs still owed.";
                case COMFORT -> "In " + colonyName + ", discontent is spoken of " + phrase + " at the door.";
                case INDUSTRY -> "Workers in " + colonyName + " " + phrase + " remember when the forge stood cold.";
            };
        }
        // COMMONS is public civic voice, not a sacred channel.
        // Do not use chapel/shrine/altar/sacred-location wording here.
        if (channel == PrayerChannel.COMMONS) {
            return switch (pressureType) {
                case FOOD -> "At the well, " + colonyName + " remembers hunger " + phrase + ". The old fear returns with empty bowls.";
                case SECURITY -> "At the guard post, " + colonyName + " remembers the raids. Wounds heal slowly " + phrase + " in the militia.";
                case HOUSING -> "In the meeting hall, " + colonyName + " remembers the homeless. Roofs promised still wait " + phrase + ".";
                case COMFORT -> "Around the common hearth, " + colonyName + " remembers harder days. The weight of discontent lingers " + phrase + ".";
                case INDUSTRY -> "At the warehouse yard, " + colonyName + " remembers lean production. The forge grows cold too often " + phrase + ".";
            };
        }
        return switch (pressureType) {
            case FOOD -> "At the chapel, " + colonyName + " remembers hunger " + phrase + ". The old fear returns with empty bowls.";
            case SECURITY -> "At the chapel, " + colonyName + " remembers the raids. Wounds heal slowly " + phrase + " in the militia.";
            case HOUSING -> "At the sacred hall, " + colonyName + " remembers the homeless. Roofs promised still wait " + phrase + ".";
            case COMFORT -> "At the chapel, " + colonyName + " remembers harder days. The weight of discontent lingers " + phrase + ".";
            case INDUSTRY -> "At the chapel, " + colonyName + " remembers lean production. The forge grows cold too often " + phrase + ".";
        };
    }

    String formatThanks(String colonyName, PressureType pressureType, PrayerChannel channel, PrayerTone tone) {
        String phrase = tone.pickPhrase(colonyName.hashCode() + pressureType.ordinal() + 300);
        if (channel == PrayerChannel.PRIVATE) {
            return switch (pressureType) {
                case FOOD -> "Some in " + colonyName + " give thanks " + phrase + " for meals after a season of want.";
                case SECURITY -> "In " + colonyName + ", " + phrase + ", relief for the guard that stood through the night.";
                case HOUSING -> "Some in " + colonyName + " whisper thanks " + phrase + " for the roofs that now shelter every family.";
                case COMFORT -> "In " + colonyName + ", gratitude " + phrase + " for the peace that has returned is spoken quietly.";
                case INDUSTRY -> "Workers in " + colonyName + " give thanks " + phrase + " for the forge that burns steady once more.";
            };
        }
        // COMMONS is public civic voice, not a sacred channel.
        // Do not use chapel/shrine/altar/sacred-location wording here.
        if (channel == PrayerChannel.COMMONS) {
            return switch (pressureType) {
                case FOOD -> "At the town square, " + colonyName + " gives thanks " + phrase + " for meals shared after a season of want.";
                case SECURITY -> "At the guard post, " + colonyName + " gives thanks " + phrase + " for the guard that stands through the night.";
                case HOUSING -> "In the meeting hall, " + colonyName + " gives thanks " + phrase + " for the roofs that now shelter every family.";
                case COMFORT -> "Around the common hearth, " + colonyName + " gives thanks " + phrase + " for the peace that has returned to its streets.";
                case INDUSTRY -> "At the warehouse yard, " + colonyName + " gives thanks " + phrase + " for the forge that burns steady once more.";
            };
        }
        return switch (pressureType) {
            case FOOD -> "At the chapel, " + colonyName + " gives thanks " + phrase + " for meals shared after a season of want.";
            case SECURITY -> "At the chapel, " + colonyName + " gives thanks " + phrase + " for the guard that stands through the night.";
            case HOUSING -> "At the sacred hall, " + colonyName + " gives thanks " + phrase + " for the roofs that now shelter every family.";
            case COMFORT -> "At the chapel, " + colonyName + " gives thanks " + phrase + " for the peace that has returned to its streets.";
            case INDUSTRY -> "At the chapel, " + colonyName + " gives thanks " + phrase + " for the forge that burns steady once more.";
        };
    }

    String formatHope(String colonyName, PressureType pressureType, PrayerChannel channel, PrayerTone tone) {
        String phrase = tone.pickPhrase(colonyName.hashCode() + pressureType.ordinal() + 400);
        if (channel == PrayerChannel.PRIVATE) {
            return switch (pressureType) {
                case FOOD -> "Some families in " + colonyName + " speak " + phrase + " of harvests yet to come.";
                case SECURITY -> "The people of " + colonyName + " " + phrase + " trust their watch will hold.";
                case HOUSING -> "Families in " + colonyName + " whisper, " + phrase + ", of homes they hope to build.";
                case COMFORT -> "Citizens of " + colonyName + " look " + phrase + " toward brighter days ahead.";
                case INDUSTRY -> "Workers in " + colonyName + " dream " + phrase + " of workshops running at full stride.";
            };
        }
        // COMMONS is public civic voice, not a sacred channel.
        // Do not use chapel/shrine/altar/sacred-location wording here.
        if (channel == PrayerChannel.COMMONS) {
            return switch (pressureType) {
                case FOOD -> "Around the common hearth, families in " + colonyName + " speak " + phrase + " of harvests yet to come.";
                case SECURITY -> "At the watch post, the people of " + colonyName + " gather " + phrase + ", trusting their watch will hold.";
                case HOUSING -> "In the meeting hall, families in " + colonyName + " speak " + phrase + " of homes yet to be built.";
                case COMFORT -> "At the town square, citizens of " + colonyName + " look " + phrase + " toward brighter days ahead.";
                case INDUSTRY -> "At the builders' yard, workers in " + colonyName + " dream " + phrase + " of workshops running at full stride.";
            };
        }
        return switch (pressureType) {
            case FOOD -> "At the chapel, families in " + colonyName + " speak " + phrase + " of harvests yet to come.";
            case SECURITY -> "At the chapel, the people of " + colonyName + " gather in prayer " + phrase + ", trusting their watch will hold.";
            case HOUSING -> "At the sacred hall, families in " + colonyName + " speak " + phrase + " of homes yet to be built.";
            case COMFORT -> "At the chapel, citizens of " + colonyName + " look " + phrase + " toward brighter days ahead.";
            case INDUSTRY -> "At the chapel, workers in " + colonyName + " dream " + phrase + " of workshops running at full stride.";
        };
    }

    private static int applyMemorySensitivity(int intensity, int thresholdAdjustment) {
        int traumaSensitivity = Math.max(0, -thresholdAdjustment);
        int triumphCalming = Math.max(0, thresholdAdjustment);
        return clampIntensity(intensity + traumaSensitivity - triumphCalming);
    }
}
