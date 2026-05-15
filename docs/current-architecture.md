# Current Architecture

## Overview

Godspark is a Forge 1.20.1 MineColonies companion mod that observes colony state, computes societal pressures, generates story events with lifecycle tracking, and maintains persistent colony memories.

## Data Flow

```
MineColonies API
      ↓
ColonyObserver.scan() [every 600 ticks]
      ↓
ColonySnapshot (14 fields: citizens, buildings, happiness, guards, etc.)
      ↓
ObservedColony (history of 20 snapshots per colony)
      ↓
PressureEngine.compute() [every 1200 ticks]
      ↓
PressureSnapshot (5 values: FOOD, SECURITY, HOUSING, COMFORT, INDUSTRY, 0-100)
      ↓
EventGenerator.generate() [stateless pure function]
      ↓
List<StoryEvent> (one per pressure type per colony, highest matching severity)
      ↓
EventStateManager.processEvents() [lifecycle tracking]
      ↓
EventRecord (ACTIVE → PERSISTENT → RESOLVED)
      ↓
MemoryEngine.generateMemories() [from transitions]
      ↓
ColonyMemory (SIGNIFICANT_EVENT, TRAUMA, PATTERN)
      ↓
MemoryBank (per-colony storage, dedup, reinforce, trim to 50)
      ↓
GodsparkSavedData.captureFrom() [on transitions]
      ↓
NBT persistence (survives restarts, DATA_VERSION = 2)
      ↓
EventQueue.offer() [6000-tick dedup, 100 max display history]
      ↓
Logs + /godspark commands
```

## Package Structure

```
com.godspark
├── GodsparkMod.java              — Mod entry point, static service instances
── GodsparkConstants.java        — MOD_ID, VERSION, tick intervals
├── command/
│   └── GodsparkCommands.java     — /godspark status|colonies|pressures|events|memories
├── event/
│   └── GodsparkServerEvents.java — Server tick heartbeat, lifecycle management
├── memory/
│   ├── ColonyMemory.java         — Immutable memory record (12 fields)
│   ├── MemoryBank.java           — Per-colony memory storage, dedup, reinforce
│   ├── MemoryEngine.java         — Generates memories from event transitions
│   └── MemoryType.java           — Enum: SIGNIFICANT_EVENT, TRAUMA, PATTERN, TRIUMPH, CULTURAL
├── observer/
│   ├── ColonyObserver.java       — Reflection-based MineColonies API scanner
│   ├── ColonySnapshot.java       — Immutable colony state record (14 fields)
│   └── ObservedColony.java       — Tracks snapshot history per colony
├── persistence/
│   └── GodsparkSavedData.java    — NBT serialization/deserialization (v2)
├── pressure/
│   ├── PressureEngine.java       — Computes 5 pressure types from snapshots
│   ├── PressureSnapshot.java     — Immutable pressure values record
│   └── PressureType.java         — Enum: FOOD, SECURITY, HOUSING, COMFORT, INDUSTRY
└── story/
    ├── EventGenerator.java       — Stateless: pressures → event candidates
    ├── EventQueue.java           — Dedup (6000 ticks), history (100 max)
    ├── EventRecord.java          — Wraps StoryEvent with lifecycle state
    ├── EventSeverity.java        — Enum: LOW(1), MEDIUM(2), HIGH(3)
    ├── EventState.java           — Enum: ACTIVE(1), PERSISTENT(2), RESOLVED(3)
    ├── EventStateManager.java    — Lifecycle tracking, keyed by colonyId+PressureType
    ├── StoryEvent.java           — Immutable event data record
    ── StoryEventType.java       — 15 event types with thresholds/descriptions
```

## Static Services (GodsparkMod)

| Service | Purpose |
|---|---|
| COLONY_OBSERVER | Scans MineColonies colonies via reflection |
| PRESSURE_ENGINE | Computes pressure values from snapshots |
| EVENT_GENERATOR | Generates event candidates from pressures |
| EVENT_QUEUE | Display/log deduplication |
| EVENT_STATE_MANAGER | Lifecycle state tracking |
| MEMORY_BANK | Per-colony memory storage |
| MEMORY_ENGINE | Memory generation from transitions |

## Tick Intervals

| Interval | Ticks | Seconds | Purpose |
|---|---|---|---|
| Observer scan | 600 | 30 | Scan MineColonies colonies |
| Pressure compute | 1200 | 60 | Compute pressures, generate events |
| Event dedup cooldown | 6000 | 300 | Prevent event spam |
| Max snapshot history | 20 | — | Per-colony snapshot limit |
| Max resolved events | 500 | — | SavedData resolved event limit |
| Max memories per colony | 50 | — | MemoryBank per-colony limit |

## Pressure Formulas

```
FOOD      = clamp(100 - foodBuildingCount * 20)
SECURITY  = clamp(100 - guardCount * 25 + (hasActiveRaid ? 30 : 0))
HOUSING   = citizenCount > housingCapacity ? clamp((citizenCount - housingCapacity) * 30) : 0
COMFORT   = clamp((10.0 - happiness) * 10.0)
INDUSTRY  = clamp(100 - industryBuildingCount * 15)
```

## Event Thresholds

| Pressure | LOW (>N) | MEDIUM (>N) | HIGH (>N) |
|---|---|---|---|
| FOOD | 40 | 70 | 90 |
| SECURITY | 40 | 70 | 90 + activeRaid |
| HOUSING | 0 | 50 | 80 |
| COMFORT | 40 | 70 | 85 |
| INDUSTRY | 40 | 70 | 90 |

## Lifecycle State Machine

```
ACTIVE (new event)
  → PERSISTENT (after 3 consecutive cycles)
  → RESOLVED (after 2 cycles without event)
```

## Memory Generation Rules

| Trigger | Memory Type |
|---|---|
| ACTIVE → PERSISTENT | SIGNIFICANT_EVENT |
| HIGH severity + persistent | TRAUMA |
| 3+ memories for same pressure | PATTERN |
| RESOLVED with persistence >= 3 | SIGNIFICANT_EVENT |

## Persistence

- **File**: `<world>/data/godspark_data.dat`
- **Format**: Minecraft NBT (CompoundTag)
- **Version**: 2 (backward compat with v1)
- **Contents**: activeEvents, resolvedEvents, memories
- **Save trigger**: ServerStoppingEvent + on transitions during tick

## Key Design Principles

1. **Reflection for MineColonies API** — compile-time types incomplete
2. **All reflection methods defensive** — safe defaults on failure
3. **EventGenerator is stateless** — pure function, testable
4. **EventStateManager keyed by colonyId + PressureType** — not StoryEventType
5. **EventQueue owns display dedup** — EventStateManager owns lifecycle truth
6. **MemoryEngine consumes transitions** — not all active events every cycle
7. **Locale.ROOT for all toLowerCase()** — avoids locale-sensitive bugs
8. **clampPressure handles NaN/infinite** — robust edge cases
9. **getSnapshots() returns Map.copyOf()** — prevents external mutation
10. **Records for immutable data** — ColonySnapshot, PressureSnapshot, StoryEvent, EventRecord, ColonyMemory
