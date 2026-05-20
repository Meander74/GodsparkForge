package com.godsparkneo.export.dto;

public class ResourceSummaryExport {
    private final int warehouseCount;
    private final FoodSummary food;
    private final String note;

    public ResourceSummaryExport(int warehouseCount, int totalFoodItems, int foodDiversity) {
        this.warehouseCount = warehouseCount;
        this.food = new FoodSummary(totalFoodItems, foodDiversity);
        this.note = "Detailed warehouse inventory deferred to future phases";
    }

    public int getWarehouseCount() { return warehouseCount; }
    public FoodSummary getFood() { return food; }
    public String getNote() { return note; }

    public static class FoodSummary {
        private final int totalFoodItems;
        private final int foodDiversity;
        public FoodSummary(int totalFoodItems, int foodDiversity) {
            this.totalFoodItems = totalFoodItems;
            this.foodDiversity = foodDiversity;
        }
        public int getTotalFoodItems() { return totalFoodItems; }
        public int getFoodDiversity() { return foodDiversity; }
    }
}
