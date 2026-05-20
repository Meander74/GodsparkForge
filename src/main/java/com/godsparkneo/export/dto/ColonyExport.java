package com.godsparkneo.export.dto;

public class ColonyExport {
    private final int id;
    private final String name;
    private final String dimension;
    private final BlockPosExport center;
    private final String state;
    private final double overallHappiness;
    private final boolean isUnderAttack;
    private final int lastContactInHours;
    private final CitizenCount citizens;
    private final int buildingCount;
    private final int workOrderCount;

    public ColonyExport(int id, String name, String dimension, BlockPosExport center,
                        String state, double overallHappiness, boolean isUnderAttack,
                        int lastContactInHours, int currentCitizens, int maxCitizens,
                        int buildingCount, int workOrderCount) {
        this.id = id;
        this.name = name;
        this.dimension = dimension;
        this.center = center;
        this.state = state;
        this.overallHappiness = overallHappiness;
        this.isUnderAttack = isUnderAttack;
        this.lastContactInHours = lastContactInHours;
        this.citizens = new CitizenCount(currentCitizens, maxCitizens);
        this.buildingCount = buildingCount;
        this.workOrderCount = workOrderCount;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getDimension() { return dimension; }
    public BlockPosExport getCenter() { return center; }
    public String getState() { return state; }
    public double getOverallHappiness() { return overallHappiness; }
    public boolean isUnderAttack() { return isUnderAttack; }
    public int getLastContactInHours() { return lastContactInHours; }
    public CitizenCount getCitizens() { return citizens; }
    public int getBuildingCount() { return buildingCount; }
    public int getWorkOrderCount() { return workOrderCount; }

    public static class CitizenCount {
        private final int current;
        private final int max;
        public CitizenCount(int current, int max) {
            this.current = current;
            this.max = max;
        }
        public int getCurrent() { return current; }
        public int getMax() { return max; }
    }
}
