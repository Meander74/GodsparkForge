package com.godsparkneo.export.dto;

import java.time.Instant;
import java.util.List;

public class ColonySnapshot {
    private final int schemaVersion;
    private final String exportTimestamp;
    private final ColonyExport colony;
    private final List<CitizenExport> citizens;
    private final List<BuildingExport> buildings;
    private final List<WorkOrderExport> workOrders;
    private final ThreatExport threats;
    private final ResourceSummaryExport resources;

    public ColonySnapshot(
            ColonyExport colony,
            List<CitizenExport> citizens,
            List<BuildingExport> buildings,
            List<WorkOrderExport> workOrders,
            ThreatExport threats,
            ResourceSummaryExport resources) {
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
    public ColonyExport getColony() { return colony; }
    public List<CitizenExport> getCitizens() { return citizens; }
    public List<BuildingExport> getBuildings() { return buildings; }
    public List<WorkOrderExport> getWorkOrders() { return workOrders; }
    public ThreatExport getThreats() { return threats; }
    public ResourceSummaryExport getResources() { return resources; }
}
