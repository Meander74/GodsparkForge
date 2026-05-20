package com.godsparkneo.export;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.godsparkneo.export.dto.*;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.workorders.IWorkOrder;
import com.minecolonies.api.entity.citizen.Skill;
import com.minecolonies.api.entity.citizen.VisibleCitizenStatus;
import net.minecraft.core.BlockPos;

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

    private static final DateTimeFormatter FILENAME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS").withZone(ZoneOffset.UTC);

    private ColonyStateExporter() {}

    public static ExportResult export(IColony colony, Path outputDir) {
        try {
            Files.createDirectories(outputDir);
            ColonySnapshot snapshot = buildSnapshot(colony);
            String json = GSON.toJson(snapshot);
            String filename = String.format("colony-%d-%s.json",
                    colony.getID(), FILENAME_FORMATTER.format(Instant.now()));
            Path filePath = outputDir.resolve(filename);
            Files.writeString(filePath, json);
            int citizenCount = snapshot.getCitizens() != null ? snapshot.getCitizens().size() : 0;
            int buildingCount = snapshot.getBuildings() != null ? snapshot.getBuildings().size() : 0;
            int workOrderCount = snapshot.getWorkOrders() != null ? snapshot.getWorkOrders().size() : 0;
            return new ExportResult(true, filePath.toString(), String.format(
                    "Colony '%s' exported: %d citizens, %d buildings, %d work orders -> %s",
                    colony.getName(), citizenCount, buildingCount, workOrderCount, filePath.getFileName()));
        } catch (Exception e) {
            return new ExportResult(false, null, "Export failed: " + e.getMessage());
        }
    }

    private static ColonySnapshot buildSnapshot(IColony colony) {
        ColonyExport colonyData = buildColonyExport(colony);
        List<CitizenExport> citizens = buildCitizenList(colony);
        List<BuildingExport> buildings = buildBuildingList(colony);
        List<WorkOrderExport> workOrders = buildWorkOrderList(colony);
        ThreatExport threats = buildThreatExport(colony);
        ResourceSummaryExport resources = buildResourceSummary(colony);
        return new ColonySnapshot(colonyData, citizens, buildings, workOrders, threats, resources);
    }

    private static ColonyExport buildColonyExport(IColony colony) {
        String dimension = Optional.ofNullable(colony.getDimension())
                .map(Object::toString)
                .orElse("unknown");
        String state = Optional.ofNullable(colony.getState())
                .map(Object::toString)
                .orElse("UNKNOWN");
        return new ColonyExport(
                colony.getID(),
                colony.getName(),
                dimension,
                BlockPosExport.from(colony.getCenter()),
                state,
                colony.getOverallHappiness(),
                colony.isColonyUnderAttack(),
                colony.getLastContactInHours(),
                colony.getCitizenManager().getCurrentCitizenCount(),
                colony.getCitizenManager().getMaxCitizens(),
                colony.getServerBuildingManager().getBuildings().size(),
                colony.getWorkManager().getWorkOrders().size());
    }

    private static List<CitizenExport> buildCitizenList(IColony colony) {
        List<CitizenExport> result = new ArrayList<>();
        for (ICitizenData citizen : colony.getCitizenManager().getCitizens()) {
            if (citizen == null) continue;
            try {
                result.add(buildCitizenExport(citizen));
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    private static CitizenExport buildCitizenExport(ICitizenData citizen) {
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

        BlockPosExport homePos = citizen.getHomeBuilding() != null
                ? BlockPosExport.from(citizen.getHomeBuilding().getPosition())
                : null;
        BlockPosExport workPos = citizen.getWorkBuilding() != null
                ? BlockPosExport.from(citizen.getWorkBuilding().getPosition())
                : null;

        return new CitizenExport(
                citizen.getId(), citizen.getName(), citizen.isFemale(), citizen.isChild(),
                citizen.getSaturation(), health, maxHealth,
                happiness, jobName, citizen.isIdleAtJob(),
                status, citizen.isAsleep(),
                homePos, workPos,
                skills, foodDiversity, foodQuality);
    }

    private static List<BuildingExport> buildBuildingList(IColony colony) {
        List<BuildingExport> result = new ArrayList<>();
        for (Map.Entry<BlockPos, IBuilding> entry :
                colony.getServerBuildingManager().getBuildings().entrySet()) {
            IBuilding building = entry.getValue();
            if (building == null) continue;
            try {
                result.add(buildBuildingExport(building));
            } catch (Exception ignored) {}
        }
        return result;
    }

    private static BuildingExport buildBuildingExport(IBuilding building) {
        String type = "unknown";
        if (building.getBuildingType() != null && building.getBuildingType().getRegistryName() != null) {
            type = building.getBuildingType().getRegistryName().toString();
        }

        List<Integer> assignedCitizens = building.getAllAssignedCitizen().stream()
                .filter(Objects::nonNull)
                .map(ICitizenData::getId)
                .collect(Collectors.toList());

        return new BuildingExport(
                BlockPosExport.from(building.getPosition()),
                type,
                building.getBuildingDisplayName(),
                building.getBuildingLevel(),
                building.getMaxBuildingLevel(),
                building.isBuilt(),
                building.isPendingConstruction(),
                assignedCitizens,
                building.getStructurePack());
    }

    private static List<WorkOrderExport> buildWorkOrderList(IColony colony) {
        List<WorkOrderExport> result = new ArrayList<>();
        for (IWorkOrder wo : colony.getWorkManager().getWorkOrders().values()) {
            if (wo == null) continue;
            try {
                result.add(buildWorkOrderExport(wo));
            } catch (Exception ignored) {}
        }
        return result;
    }

    private static WorkOrderExport buildWorkOrderExport(IWorkOrder wo) {
        String type = wo.getWorkOrderType() != null ? wo.getWorkOrderType().name() : "UNKNOWN";
        boolean isClaimed = false;
        BlockPosExport claimedBy = null;
        try {
            isClaimed = wo.isClaimed();
            claimedBy = BlockPosExport.from(wo.getClaimedBy());
        } catch (Exception ignored) {}

        return new WorkOrderExport(
                wo.getID(), type, wo.getPriority(),
                BlockPosExport.from(wo.getLocation()),
                wo.getCurrentLevel(), wo.getTargetLevel(),
                isClaimed, claimedBy,
                wo.getStructurePath(), wo.getStructurePack());
    }

    private static ThreatExport buildThreatExport(IColony colony) {
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

        return new ThreatExport(isRaided, willRaidTonight, nightsSinceLastRaid, colonyRaidLevel, activeEventIds);
    }

    private static ResourceSummaryExport buildResourceSummary(IColony colony) {
        int warehouseCount = 0;
        try {
            var warehouses = colony.getServerBuildingManager().getWareHouses();
            warehouseCount = warehouses != null ? warehouses.size() : 0;
        } catch (Exception ignored) {}

        return new ResourceSummaryExport(warehouseCount, 0, 0);
    }

    public record ExportResult(boolean success, String filePath, String message) {}
}
