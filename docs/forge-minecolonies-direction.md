# Forge + MineColonies Direction

## Overview

Godspark is now a **Forge 1.20.1 MineColonies companion mod**. It observes colony state, computes societal pressures, generates high-level story/prayer/evolution systems, and later uses local AI only for reflection and narration.

## What Godspark Is

- An **observer** of MineColonies colony state
- A **pressure engine** that converts colony metrics into societal pressures
- A **storyteller** that generates events from simulation state
- A **god/prayer system** for civilization evolution
- An **optional local AI reflection layer** (future)

## What Godspark Is Not

- A replacement for MineColonies
- A standalone colony simulation
- An LLM-driven NPC controller
- A Fabric mod
- A mod that runs AI every tick

## Architecture

```
MineColonies Simulation
      ↓
Godspark Observer Layer
      ↓
Structured Colony Metrics
      ↓
Pressure System
      ↓
Storyteller / Event System
      ↓
Optional AI Reflection
      ↓
Player-Facing Prayers / Civilization Evolution
      ↓
World Consequences
```

## Technical Stack

| Component | Version |
|-----------|---------|
| Minecraft | 1.20.1 |
| Forge | 47.x |
| Java | 17 |
| MineColonies | 1.20.1-1.1.x (mandatory dependency) |
| Create | Optional, detected at runtime |

## Deprecated Direction

The previous **Fabric standalone colony simulation** direction is **deprecated**. This included:

- Fabric mod loader
- Fabulously Optimized integration
- Custom NPC simulation
- Custom colony systems built from scratch
- Custom logistics from scratch

These are no longer the project direction. MineColonies is the core colony simulation substrate, and it is Forge/NeoForge-oriented.

## Development Phases

### Phase 1 — Forge/MineColonies Observer (Current)
- Forge 1.20.1 mod scaffold
- MineColonies detection and colony observation
- Debug commands (`/godspark status`, `/godspark colonies`, `/godspark pressures`)
- Server tick heartbeat with interval throttling

### Phase 2 — Pressure Engine
- Convert observed colony state into pressure metrics
- Food, security, housing, comfort, industry pressures

### Phase 3 — Story Events
- Pressure creates deterministic event candidates
- Logging-first approach before physical events

### Phase 4 — Memory System
- Record significant colony-level events persistently
- Forge/vanilla saved data

### Phase 5 — AI Reflection
- Send structured metrics to local AI (llama.cpp)
- Async HTTP for narrative interpretation

### Phase 6 — Civilization Evolution
- Societal pressures unlock cultural/technological directions

## Integration Test Instance

The primary integration test instance is:

```
C:\Users\Suttawat\AppData\Roaming\PrismLauncher\instances\Create'a Colony
```

See `docs/createa-colony-test-instance.md` for details.

## First Milestone

The first milestone is **not AI**. It is:

> Can Godspark reliably observe and interpret MineColonies state?

Minimum viable first result:
- Game launches with Godspark + MineColonies
- Godspark logs initialization
- Godspark detects MineColonies
- `/godspark status` works
- `/godspark colonies` works safely
