# Phase 3.5 — Event State Persistence

## Status
✅ COMPLETE (3.5A + 3.5B)

## Goal
Add lifecycle state tracking (ACTIVE → PERSISTENT → RESOLVED) and NBT-based persistence so events survive game restarts.

## Phase 3.5A — Lifecycle State

### Event States
| State | Rank | Description |
|---|---|---|
| ACTIVE | 1 | Newly detected event |
| PERSISTENT | 2 | Event persisted for 3+ consecutive cycles |
| RESOLVED | 3 | Event no longer detected for 2+ cycles |

### Transition Rules
- **ACTIVE → PERSISTENT**: After `PERSISTENT_THRESHOLD = 3` consecutive cycles
- **→ RESOLVED**: After `RESOLVE_AFTER_MISSING_CYCLES = 2` cycles where event is no longer generated
- **Keyed by `colonyId + PressureType`** — not StoryEventType (severity changes are one continuing condition)

### EventRecord Record
Wraps `StoryEvent` with lifecycle metadata: `state`, `persistenceCount`, `missingCycles`, `firstSeenTick`, `lastSeenTick`, `resolvedTick`.

### Files
- `src/main/java/com/godspark/story/EventStateManager.java`
- `src/main/java/com/godspark/story/EventRecord.java`
- `src/main/java/com/godspark/story/EventState.java`

## Phase 3.5B — NBT Persistence

### Architecture
```
ServerStartedEvent → load SavedData → restoreTo(EventStateManager)
ServerStoppingEvent → captureFrom(EventStateManager) → setDirty()
ServerStoppedEvent → clear static services
```

### GodsparkSavedData
- Extends Minecraft `SavedData` for NBT-based persistence
- `DATA_VERSION = 1` (upgraded to 2 in Phase 4A)
- `MAX_RESOLVED_EVENTS = 500`
- `SavedEventRecord` inner record mirrors `EventRecord` + `StoryEvent` combined (15 fields)
- Defensive enum parsing with fallbacks for unknown enum values (forward-compat)
- Trims resolved events to 500 max on load and capture

### Files
- `src/main/java/com/godspark/persistence/GodsparkSavedData.java`
- `src/main/java/com/godspark/event/GodsparkServerEvents.java` (modified)

### Persistence Flow
1. **ServerStartedEvent**: `computeIfAbsent` loads or creates `GodsparkSavedData`, then `restoreTo()` repopulates `EventStateManager`
2. **ServerStoppingEvent**: `captureFrom()` snapshots `EventStateManager` state to NBT, calls `setDirty()`
3. **ServerStoppedEvent**: Clears all static services including `EventStateManager`
4. **ServerTick**: After transitions, `captureFrom()` saves updated state

### Testing
- Events persist across game restarts
- Active and resolved events correctly restored
- No data loss during shutdown
- Forward-compat: unknown enum values fall back to safe defaults

## Key Design Decisions
- **Split save logic** into `ServerStoppingEvent` (data capture) and `ServerStoppedEvent` (state clearing) to prevent data loss
- **Decoupled `GodsparkSavedData`** from static `GodsparkMod` fields by accepting `EventStateManager` as method parameter
- **Used `computeIfAbsent`** on `ServerStartedEvent` for lazy/initial data loading
- **EventStateManager keyed by colonyId + PressureType** — not StoryEventType

## SavedData File Location
`<world>/data/godspark_data.dat`
