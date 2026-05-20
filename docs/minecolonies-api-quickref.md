# MineColonies API Quick Reference

> Auto-extracted from minecolonies 1.20.1-1.1.x source via graphify.
> For full API details, search context-mode sources or read source files directly.

## API Entry Point

```
IMinecoloniesAPI (com.minecolonies.api.IMinecoloniesAPI)
├── .getColonyManager()        → IColonyManager
├── .getColonyEventRegistry() → IColonyEventRegistry
├── .getCitizenDataManager()  → ICitizenDataManager
└── .getColonyEventDescriptionRegistry() → IColonyEventDescriptionRegistry
```

Access pattern: `IMinecoloniesAPI.getInstance()` (available after `MineColoniesAPIProxy` loads)

## Core API Path: Colony Access

```
IMinecoloniesAPI.getInstance()
  → IColonyManager
    → getColony(World, BlockPos)     → IColony
    → getColony(int)                  → IColony (by ID)
    → getAllColonies()                 → Collection<IColony>
    → createColony(ServerLevel, BlockPos) → IColony
    → deleteColonyByWorld(World, BlockPos)
```

## IColony Interface

Key sub-managers accessible from `IColony`:

| Manager | Getter | Purpose |
|---------|--------|---------|
| Buildings | `getBuildingManager()` | `IBuildingManager` → all buildings |
| Citizens | `getCitizenManager()` | `ICitizenManager` → all citizens |
| Requests | `getRequestManager()` | `IRequestManager` → request system |
| Work | `getWorkManager()` | `IWorkManager` → work orders |
| Permissions | `getPermissions()` | `IPermissions` → rank/access |
| Research | `getResearchManager()` | `IResearchManager` |
| Events | `getEventManager()` | `IEventManager` → colony events |
| Visitors | `getVisitorManager()` | `IVisitorManager` |
| Package | `getPackage()` | `IColonyPackage` → network sync |

## IBuilding Interface

```
IBuilding
├── .getBuildingLevel()              → int (current level)
├── .getMaxBuildingLevel()            → int
├── .getSchematicName()               → String (e.g., "baker", "guardtower")
├── .getLocation()                    → ILocation
├── .getModules()                     → Collection<IBuildingModule>
├── .getAssignedCitizens()            → Collection<ICitizenData>
├── .getSetting(String)               → ISetting
├── .isBuilt()                        → boolean
├── .isDeconstructed()                 → boolean
└── .getBuildingType()                → IBuildingType (or null)
```

## ICitizenData Interface

```
ICitizenData
├── .getId()                          → int
├── .getName()                        → String
├── .getJob()                         → IJob (may be null)
├── .getHomeBuilding()                → IBuilding (residence)
├── .getWorkBuilding()                → IBuilding (workplace)
├── .getSaturation()                  → double
├── .getHappiness()                   → double
├── .getCitizenColonyHandler()        → ICitizenColonyHandler
├── .getEntity()                      → AbstractEntityCitizen (may be null if unloaded)
├── .isFemale()                       → boolean
└── .getSkills()                      → ICitizenSkillHandler
```

## Event System

```
IColonyEventRegistry (accessible via IMinecoloniesAPI)
IEventManager (accessible via IColony.getEventManager())
  ├── ColonyEventManager          → handles raid events, visitor events, etc.
  ├── Event hooks: subscribe to colony events
  └── IColonyEvent               → base event interface

Key event types registrar accessible from API:
  - IMinecoloniesAPI.getInstance().getColonyEventRegistry()
  - IMinecoloniesAPI.getInstance().getColonyEventDescriptionRegistry()
```

## Request System

```
IRequestManager (accessible via IColony.getRequestManager())
  ├── .createRequest(IRequester, IRequestToken) → creates a request
  ├── .updateRequest(IRequest)                  → updates existing
  ├── .assignRequest(IRequest, ICitizenData)     → assigns citizen
  └── IBuildingBasedRequester                    → building as requester
```

## GodsparkForge Integration Points

When writing code that interacts with MineColonies, use these exact paths:

| Need | Search Term | Source File Pattern |
|------|-------------|---------------------|
| Get colony data | `IColony` | `api/colony/IColony.java` |
| Get building data | `IBuilding` | `api/colony/buildings/IBuilding.java` |
| Get citizen data | `ICitizenData` | `api/colony/ICitizenData.java` |
| Get citizen manager | `ICitizenManager` | `api/colony/ICitizenManager.java` |
| Get building manager | `IBuildingManager` | `api/colony/buildings/IBuildingManager.java` |
| Get request system | `IRequestManager` | `api/colony/requests/IRequestManager.java` |
| Get events | `IEventManager` | `api/colony/events/IEventManager.java` |
| Get API instance | `IMinecoloniesAPI` | `api/IMinecoloniesAPI.java` |
| Colony lifecycle | `IColonyManager` | `api/colony/IColonyManager.java` |

## Building Schematic Names (Holy Site Detection)

GodsparkForge classifies buildings by schematic name. Common MineColonies schematic names:

| Type | Schematic Names |
|------|-----------------|
| Town Hall | `townhall` |
| Residence | `citizen` |
| Guard Tower | `guardtower` |
| Builder's Hut | `builder` |
| Warehouse | `warehouse` |
| Bakery | `baker` |
| Farm | `farmer` |
| Fisher | `fisherman` |
| Lumberjack | `lumberjack` |
| Miner | `miner` |
| Smeltery | `smeltery` |
| University | `university` |
| Library | `library` |
| Tavern | `tavern` |
| Hospital | `hospital` |
| Church | `church` |
| Cemetery | `cemetery` |
| Tavern | `tavern` |
| Barracks | `barracks` |
| Archery | `archery` |
| Combat Academy | `combatacademy` |
| Cowboy | `cowboy` |
| Shepherd | `shepherd` |
| Swine Herder | `swineherder` |
| Chicken Herder | `chickenherder` |
| Florist | `florist` |
| Glassblower | `glassblower` |
| Dyer | `dyer` |
| Mechanic | `mechanic` |

## Update Procedure

When MineColonies updates:

```powershell
git -C "C:\Users\Suttawat\minecolonies-ref" pull
# Then re-run graphify:
cd "C:\Users\Suttawat\minecolonies-ref"
python -c "from graphify.detect import detect; from pathlib import Path; import json; result = detect(Path('.')); print(f'{result[\"total_files\"]} files detected')"
```

Then re-index the graph into context-mode (see AGENTS.md for procedure).