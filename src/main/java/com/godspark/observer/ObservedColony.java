package com.godspark.observer;

import com.godspark.GodsparkConstants;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;

public final class ObservedColony {
    private final int colonyId;
    private final Deque<ColonySnapshot> history;
    private ColonySnapshot latest;

    public ObservedColony(int colonyId) {
        this.colonyId = colonyId;
        this.history = new ArrayDeque<>();
        this.latest = null;
    }

    public void addSnapshot(ColonySnapshot snapshot) {
        this.latest = snapshot;
        history.addLast(snapshot);
        while (history.size() > GodsparkConstants.MAX_SNAPSHOT_HISTORY) {
            history.removeFirst();
        }
    }

    public int getColonyId() {
        return colonyId;
    }

    @Nullable
    public ColonySnapshot getLatest() {
        return latest;
    }

    public Deque<ColonySnapshot> getHistory() {
        return history;
    }
}
