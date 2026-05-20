package com.godsparkneo.export.dto;

import java.util.List;

public class BuildingExport {
    private final BlockPosExport id;
    private final String type;
    private final String displayName;
    private final int level;
    private final int maxLevel;
    private final boolean isBuilt;
    private final boolean isPendingConstruction;
    private final List<Integer> assignedCitizens;
    private final String structurePack;

    public BuildingExport(BlockPosExport id, String type, String displayName,
                          int level, int maxLevel, boolean isBuilt,
                          boolean isPendingConstruction, List<Integer> assignedCitizens,
                          String structurePack) {
        this.id = id;
        this.type = type;
        this.displayName = displayName;
        this.level = level;
        this.maxLevel = maxLevel;
        this.isBuilt = isBuilt;
        this.isPendingConstruction = isPendingConstruction;
        this.assignedCitizens = assignedCitizens;
        this.structurePack = structurePack;
    }

    public BlockPosExport getId() { return id; }
    public String getType() { return type; }
    public String getDisplayName() { return displayName; }
    public int getLevel() { return level; }
    public int getMaxLevel() { return maxLevel; }
    public boolean isBuilt() { return isBuilt; }
    public boolean isPendingConstruction() { return isPendingConstruction; }
    public List<Integer> getAssignedCitizens() { return assignedCitizens; }
    public String getStructurePack() { return structurePack; }
}
