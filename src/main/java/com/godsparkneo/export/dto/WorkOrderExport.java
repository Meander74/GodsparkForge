package com.godsparkneo.export.dto;

public class WorkOrderExport {
    private final int id;
    private final String type;
    private final int priority;
    private final BlockPosExport location;
    private final int currentLevel;
    private final int targetLevel;
    private final boolean isClaimed;
    private final BlockPosExport claimedByBuilding;
    private final String structurePath;
    private final String structurePack;

    public WorkOrderExport(int id, String type, int priority,
                           BlockPosExport location, int currentLevel, int targetLevel,
                           boolean isClaimed, BlockPosExport claimedByBuilding,
                           String structurePath, String structurePack) {
        this.id = id;
        this.type = type;
        this.priority = priority;
        this.location = location;
        this.currentLevel = currentLevel;
        this.targetLevel = targetLevel;
        this.isClaimed = isClaimed;
        this.claimedByBuilding = claimedByBuilding;
        this.structurePath = structurePath;
        this.structurePack = structurePack;
    }

    public int getId() { return id; }
    public String getType() { return type; }
    public int getPriority() { return priority; }
    public BlockPosExport getLocation() { return location; }
    public int getCurrentLevel() { return currentLevel; }
    public int getTargetLevel() { return targetLevel; }
    public boolean isClaimed() { return isClaimed; }
    public BlockPosExport getClaimedByBuilding() { return claimedByBuilding; }
    public String getStructurePath() { return structurePath; }
    public String getStructurePack() { return structurePack; }
}
