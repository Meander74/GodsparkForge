package com.godsparkneo.export;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.views.IBuildingView;
import com.minecolonies.api.colony.workorders.IWorkOrder;
import com.minecolonies.api.entity.citizen.Skill;
import com.minecolonies.api.entity.citizen.VisibleCitizenStatus;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public final class ColonyStateExporter {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private ColonyStateExporter() {}

    public static ExportResult export(IColony colony, Path outputDir) {
        try {
            Files.createDirectories(outputDir);
            ColonySnapshot snapshot = buildSnapshot(colony);
            String json = GSON.toJson(snapshot);
            String filename = String.format("colony-%d-%s.json",
                    colony.getID(),
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneOffset.UTC)
                            .format(Instant.now())
                            .replace(":", "-"));
            Path filePath = outputDir.resolve(filename);
            Files.writeString(filePath, json);
            int citizenCount = snapshot.getCitizens() != null ? snapshot.getCitizens().size() : 0;
            int buildingCount = snapshot.getBuildings() != null ? snapshot.getBuildings().size() : 0;
            int workOrderCount = snapshot.getWorkOrders() != null ? snapshot.getWorkOrders().size() : 0;
            return new ExportResult(true, filePath.toString(), String.format(
                    "Colony '%s' exported: %d citizens, %d buildings, %d work orders",
                    colony.getName(), citizenCount, buildingCount, workOrderCount));
        } catch (Exception e) {
            return new ExportResult(false, null, "Export failed: " + e.getMessage());
        }
    }

    private static ColonySnapshot buildSnapshot(IColony colony) {
        ColonyData colonyData = buildColonyData(colony);
        List<CitizenData> citizens = buildCitizenList(colony);
        List<BuildingData> buildings = buildBuildingList(colony);
        List<WorkOrderData> workOrders = buildWorkOrderList(colony);
        ThreatData threats = buildThreatData(colony);
        ResourceSummaryData resources = buildResourceSummary(colony);
        return new ColonySnapshot(colonyData, citizens, buildings, workOrders, threats, resources);
    }

    private static ColonyData buildColonyData(IColony colony) {
        String dimension = Optional.ofNullable(colony.getDimension())
                .map(Object::toString)
                .orElse("unknown");
        String state = Optional.ofNullable(colony.getState())
                .map(Object::toString)
                .orElse("UNKNOWN");
        return new ColonyData(
                colony.getID(),
                colony.getName(),
                dimension,
                BlockPosData.from(colony.getCenter()),
                state,
                colony.getOverallHappiness(),
                colony.isColonyUnderAttack(),
                colony.getLastContactInHours(),
                colony.getCitizenManager().getCurrentCitizenCount(),
                colony.getCitizenManager().getMaxCitizens(),
                colony.getServerBuildingManager().getBuildings().size(),
                colony.getWorkManager().getWorkOrders().size());
    }

    private static List<CitizenData> buildCitizenList(IColony colony) {
        List<CitizenData> result = new ArrayList<>();
        for (ICitizenData citizen : colony.getCitizenManager().getCitizens()) {
            if (citizen == null) continue;
            try {
                result.add(buildCitizenData(citizen));
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    private static CitizenData buildCitizenData(ICitizenData citizen) {
        String jobName = "unemployed";
        if (citizen.getJob() != null && citizen.getJob().getJobRegistryEntry() != null) {
            jobName = citizen.getJob().getJobRegistryEntry().getKey().getPath();
        }

        double health = 20.0;
        double maxHealth = 20.0;
        try {
            if (citizen.getEntity().isPresent()) {
                health = citizen.getEntity().get().getHealth();
                maxHealth = citizen.getEntity().get().getMaxHealth();
            }
        } catch (Exception ignored) {}

        double happiness = 5.5;
        try {
            happiness = citizen.getCitizenHappinessHandler().getHappiness(citizen.getColony(), citizen);
        } catch (Exception ignored) {}

        String status = "";
        try {
            VisibleCitizenStatus vis = citizen.getStatus();
            if (vis != null) status = vis.toString();
        } catch (Exception ignored) {}

        Map<String, Integer> skills = new LinkedHashMap<>();
        try {
            for (Skill s : Skill.values()) {
                int level = citizen.getCitizenSkillHandler().getLevel(s);
                skills.put(s.name(), level);
            }
        } catch (Exception ignored) {}

        int foodDiversity = 0;
        int foodQuality = 0;
        try {
            var foodStats = citizen.getCitizenFoodHandler().getFoodHappinessStats();
            if (foodStats != null) {
                foodDiversity = foodStats.diversity();
                foodQuality = foodStats.quality();
            }
        } catch (Exception ignored) {}

        BlockPosData homePos = citizen.getHomeBuilding() != null
                ? BlockPosData.from(citizen.getHomeBuilding().getPosition())
                : null;
        BlockPosData workPos = citizen.getWorkBuilding() != null
                ? BlockPosData.from(citizen.getWorkBuilding().getPosition())
                : null;

        return new CitizenData(
                citizen.getId(), citizen.getName(), citizen.isFemale(), citizen.isChild(),
                citizen.getSaturation(), health, maxHealth,
                happiness, jobName, citizen.isIdleAtJob(),
                status, citizen.isAsleep(),
                homePos, workPos,
                skills, foodDiversity, foodQuality);
    }

    private static List<BuildingData> buildBuildingList(IColony colony) {
        List<BuildingData> result = new ArrayList<>();
        for (Map.Entry<net.minecraft.core.BlockPos, IBuilding> entry :
                colony.getServerBuildingManager().getBuildings().entrySet()) {
            IBuilding building = entry.getValue();
            if (building == null) continue;
            try {
                result.add(buildBuildingData(building));
            } catch (Exception ignored) {}
        }
        return result;
    }

    private static BuildingData buildBuildingData(IBuilding building) {
        String type = "unknown";
        if (building.getBuildingType() != null && building.getBuildingType().getRegistryName() != null) {
            type = building.getBuildingType().getRegistryName().toString();
        }

        List<Integer> assignedCitizens = building.getAllAssignedCitizen().stream()
                .filter(Objects::nonNull)
                .map(ICitizenData::getId)
                .collect(Collectors.toList());

        return new BuildingData(
                BlockPosData.from(building.getPosition()),
                type,
                building.getBuildingDisplayName(),
                building.getBuildingLevel(),
                building.getMaxBuildingLevel(),
                building.isBuilt(),
                building.isPendingConstruction(),
                assignedCitizens,
                building.getStructurePack());
    }

    private static List<WorkOrderData> buildWorkOrderList(IColony colony) {
        List<WorkOrderData> result = new ArrayList<>();
        for (IWorkOrder wo : colony.getWorkManager().getWorkOrders().values()) {
            if (wo == null) continue;
            try {
                result.add(buildWorkOrderData(wo));
            } catch (Exception ignored) {}
        }
        return result;
    }

    private static WorkOrderData buildWorkOrderData(IWorkOrder wo) {
        String type = wo.getWorkOrderType() != null ? wo.getWorkOrderType().name() : "UNKNOWN";
        boolean isClaimed = false;
        BlockPosData claimedBy = null;
        try {
            isClaimed = wo.isClaimed();
            claimedBy = BlockPosData.from(wo.getClaimedBy());
        } catch (Exception ignored) {}

        return new WorkOrderData(
                wo.getID(), type, wo.getPriority(),
                BlockPosData.from(wo.getLocation()),
                wo.getCurrentLevel(), wo.getTargetLevel(),
                isClaimed, claimedBy,
                wo.getStructurePath(), wo.getStructurePack());
    }

    private static ThreatData buildThreatData(IColony colony) {
        boolean isRaided = false;
        boolean willRaidTonight = false;
        int nightsSinceLastRaid = -1;
        int colonyRaidLevel = 0;
        List<Integer> activeEventIds = new ArrayList<>();

        try {
            var raiderManager = colony.getRaiderManager();
            if (raiderManager != null) {
                isRaided = raiderManager.isRaided();
                willRaidTonight = raiderManager.willRaidTonight();
                nightsSinceLastRaid = raiderManager.getNightsSinceLastRaid();
                colonyRaidLevel = raiderManager.getColonyRaidLevel();
            }
        } catch (Exception ignored) {}

        try {
            var eventManager = colony.getEventManager();
            if (eventManager != null) {
                activeEventIds = eventManager.getEvents().values().stream()
                        .map(e -> { try { return e.getID(); } catch (Exception ex) { return -1; } })
                        .filter(id -> id >= 0)
                        .collect(Collectors.toList());
            }
        } catch (Exception ignored) {}

        return new ThreatData(isRaided, willRaidTonight, nightsSinceLastRaid, colonyRaidLevel, activeEventIds);
    }

    private static ResourceSummaryData buildResourceSummary(IColony colony) {
        int warehouseCount = 0;
        try {
            var warehouses = colony.getServerBuildingManager().getWareHouses();
            warehouseCount = warehouses != null ? warehouses.size() : 0;
        } catch (Exception ignored) {}

        return new ResourceSummaryData(warehouseCount, 0, 0);
    }

    public record ExportResult(boolean success, String filePath, String message) {}
}
