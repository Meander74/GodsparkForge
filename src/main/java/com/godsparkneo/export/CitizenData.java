package com.godsparkneo.export;

import java.util.Map;

public class CitizenData {
    private final int id;
    private final String name;
    private final boolean isFemale;
    private final boolean isChild;
    private final double saturation;
    private final double health;
    private final double maxHealth;
    private final double happiness;
    private final String job;
    private final boolean isIdle;
    private final String status;
    private final boolean isAsleep;
    private final BlockPosData homeBuilding;
    private final BlockPosData workBuilding;
    private final Map<String, Integer> skills;
    private final FoodData food;

    public CitizenData(int id, String name, boolean isFemale, boolean isChild,
                       double saturation, double health, double maxHealth,
                       double happiness, String job, boolean isIdle,
                       String status, boolean isAsleep,
                       BlockPosData homeBuilding, BlockPosData workBuilding,
                       Map<String, Integer> skills, int foodDiversity, int foodQuality) {
        this.id = id;
        this.name = name;
        this.isFemale = isFemale;
        this.isChild = isChild;
        this.saturation = saturation;
        this.health = health;
        this.maxHealth = maxHealth;
        this.happiness = happiness;
        this.job = job;
        this.isIdle = isIdle;
        this.status = status;
        this.isAsleep = isAsleep;
        this.homeBuilding = homeBuilding;
        this.workBuilding = workBuilding;
        this.skills = skills;
        this.food = new FoodData(foodDiversity, foodQuality);
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public boolean isFemale() { return isFemale; }
    public boolean isChild() { return isChild; }
    public double getSaturation() { return saturation; }
    public double getHealth() { return health; }
    public double getMaxHealth() { return maxHealth; }
    public double getHappiness() { return happiness; }
    public String getJob() { return job; }
    public boolean isIdle() { return isIdle; }
    public String getStatus() { return status; }
    public boolean isAsleep() { return isAsleep; }
    public BlockPosData getHomeBuilding() { return homeBuilding; }
    public BlockPosData getWorkBuilding() { return workBuilding; }
    public Map<String, Integer> getSkills() { return skills; }
    public FoodData getFood() { return food; }

    public static class FoodData {
        private final int diversity;
        private final int quality;
        public FoodData(int diversity, int quality) {
            this.diversity = diversity;
            this.quality = quality;
        }
        public int getDiversity() { return diversity; }
        public int getQuality() { return quality; }
    }
}
