package com.godsparkneo.export;

import java.time.Instant;
import java.util.List;

public class ColonySnapshot {
    private final int schemaVersion;
    private final String exportTimestamp;
    private final ColonyData colony;
    private final List<CitizenData> citizens;
    private final List<BuildingData> buildings;
    private final List<WorkOrderData> workOrders;
    private final ThreatData threats;
    private final ResourceSummaryData resources;

    public ColonySnapshot(
            ColonyData colony,
            List<CitizenData> citizens,
            List<BuildingData> buildings,
            List<WorkOrderData> workOrders,
            ThreatData threats,
            ResourceSummaryData resources) {
        this.schemaVersion = 1;
        this.exportTimestamp = Instant.now().toString();
        this.colony = colony;
        this.citizens = citizens;
        this.buildings = buildings;
        this.workOrders = workOrders;
        this.threats = threats;
        this.resources = resources;
    }

    public int getSchemaVersion() { return schemaVersion; }
    public String getExportTimestamp() { return exportTimestamp; }
    public ColonyData getColony() { return colony; }
    public List<CitizenData> getCitizens() { return citizens; }
    public List<BuildingData> getBuildings() { return buildings; }
    public List<WorkOrderData> getWorkOrders() { return workOrders; }
    public ThreatData getThreats() { return threats; }
    public ResourceSummaryData getResources() { return resources; }
}
