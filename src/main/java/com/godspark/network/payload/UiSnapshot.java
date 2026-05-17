package com.godspark.network.payload;

import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

public record UiSnapshot(
    UiStatusInfo status,
    List<UiColonyEntry> colonies,
    List<UiPressureEntry> pressures,
    List<UiEventEntry> events,
    List<UiMemoryEntry> memories,
    List<UiPrayerEntry> prayers,
    long serverTick
) {
    private static final int MAX_COLONIES = 256;
    private static final int MAX_PRESSURES = 256 * 5;
    private static final int MAX_EVENTS = 256;
    private static final int MAX_MEMORIES = 2048;
    private static final int MAX_PRAYERS = 512;

    public UiSnapshot {
        colonies = colonies == null ? List.of() : List.copyOf(colonies);
        pressures = pressures == null ? List.of() : List.copyOf(pressures);
        events = events == null ? List.of() : List.copyOf(events);
        memories = memories == null ? List.of() : List.copyOf(memories);
        prayers = prayers == null ? List.of() : List.copyOf(prayers);
    }

    public static void encode(UiSnapshot snap, FriendlyByteBuf buf) {
        UiStatusInfo.encode(snap.status(), buf);
        buf.writeVarInt(snap.colonies().size());
        for (UiColonyEntry c : snap.colonies()) UiColonyEntry.encode(c, buf);
        buf.writeVarInt(snap.pressures().size());
        for (UiPressureEntry p : snap.pressures()) UiPressureEntry.encode(p, buf);
        buf.writeVarInt(snap.events().size());
        for (UiEventEntry e : snap.events()) UiEventEntry.encode(e, buf);
        buf.writeVarInt(snap.memories().size());
        for (UiMemoryEntry m : snap.memories()) UiMemoryEntry.encode(m, buf);
        buf.writeVarInt(snap.prayers().size());
        for (UiPrayerEntry p : snap.prayers()) UiPrayerEntry.encode(p, buf);
        buf.writeVarLong(snap.serverTick());
    }

    public static UiSnapshot decode(FriendlyByteBuf buf) {
        UiStatusInfo status = UiStatusInfo.decode(buf);

        int cn = readBoundedCount(buf, MAX_COLONIES, "colonies");
        List<UiColonyEntry> colonies = new ArrayList<>(cn);
        for (int i = 0; i < cn; i++) colonies.add(UiColonyEntry.decode(buf));

        int pn = readBoundedCount(buf, MAX_PRESSURES, "pressures");
        List<UiPressureEntry> pressures = new ArrayList<>(pn);
        for (int i = 0; i < pn; i++) pressures.add(UiPressureEntry.decode(buf));

        int en = readBoundedCount(buf, MAX_EVENTS, "events");
        List<UiEventEntry> events = new ArrayList<>(en);
        for (int i = 0; i < en; i++) events.add(UiEventEntry.decode(buf));

        int mn = readBoundedCount(buf, MAX_MEMORIES, "memories");
        List<UiMemoryEntry> memories = new ArrayList<>(mn);
        for (int i = 0; i < mn; i++) memories.add(UiMemoryEntry.decode(buf));

        int prn = readBoundedCount(buf, MAX_PRAYERS, "prayers");
        List<UiPrayerEntry> prayers = new ArrayList<>(prn);
        for (int i = 0; i < prn; i++) prayers.add(UiPrayerEntry.decode(buf));

        long tick = buf.readVarLong();
        return new UiSnapshot(status, colonies, pressures, events, memories, prayers, tick);
    }

    private static int readBoundedCount(FriendlyByteBuf buf, int max, String label) {
        int count = buf.readVarInt();
        if (count < 0 || count > max) {
            throw new IllegalArgumentException("Invalid " + label + " count: " + count + " (max: " + max + ")");
        }
        return count;
    }
}