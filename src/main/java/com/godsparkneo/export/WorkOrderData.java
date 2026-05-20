package com.godsparkneo.export;

public class WorkOrderData {
    private final int id;
    private final String type;
    private final int priority;
    private final BlockPosData location;
    private final int currentLevel;
    private final int targetLevel;
    private final boolean isClaimed;
    private final BlockPosData claimedByBuilding;
    private final String structurePath;
    private final String structurePack;

    public WorkOrderData(int id, String type, int priority,
                         BlockPosData location, int currentLevel, int targetLevel,
                         boolean isClaimed, BlockPosData claimedByBuilding,
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
    public BlockPosData getLocation() { return location; }
    public int getCurrentLevel() { return currentLevel; }
    public int getTargetLevel() { return targetLevel; }
    public boolean isClaimed() { return isClaimed; }
    public BlockPosData getClaimedByBuilding() { return claimedByBuilding; }
    public String getStructurePath() { return structurePath; }
    public String getStructurePack() { return structurePack; }
}
