# Godspark — Project Status Report

> **For:** AI assistant handoff / project context transfer
> **Date:** 2026-05-20
> **Version:** 0.1.0
> **GitHub:** https://github.com/Meander74/GodsparkForge (public)

---

## What This Project Is

Godspark is a **Forge 1.20.1 MineColonies companion mod** that observes colony state, computes societal pressures (0-100 scale across 5 domains), generates deterministic story events, maintains persistent colony memories, generates prayer seeds from colony personality and sacred sites, and allows players to answer colony prayers via `/godspark answer` — with validated divine answers applying bounded pressure modifiers and optional light world effects.

**What it is NOT:** A MineColonies replacement, standalone simulation, LLM-driven NPC controller, or auto-activating miracle system.

---

## Tech Stack

| Component | Version |
|---|---|
| Minecraft | 1.20.1 |
| Forge | 47.x |
| Java | 17 |
| MineColonies | 1.20.1-1.1.x (required, reflection-based API) |
| Create | Optional, detected at runtime |
| Gson | Bundled with Forge |

**Build:**
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"; .\gradlew build --no-daemon
.\deploy.ps1  # -> PrismLauncher instance
```

---

## Architecture Overview

```
MineColonies API
      ↓
ColonyObserver.scan() [every 600 ticks]
      ↓
ColonySnapshot (23+ fields: citizens, buildings, happiness, guards, sacredBuildingCount, etc.)
      ↓
PressureEngine.compute() [every 1200 ticks]
      ↓
PressureSnapshot (5 values: FOOD, SECURITY, HOUSING, COMFORT, INDUSTRY)
      ↓
EventGenerator.generate() [stateless, memory-adjusted thresholds]
      ↓
EventStateManager.processEvents() [ACTIVE → PERSISTENT → RESOLVED]
      ↓
MemoryEngine.generateMemories() [from transitions]
      ↓
MemoryBank (per-colony, dedup, reinforce, trim to 50)
      ↓
MemoryInfluence → EventGenerator (threshold adjustments)
      ↓
PersonalityEngine (deterministic traits from history, cached)
      ↓
PersonalityInfluence → PrayerSeedGenerator
      ↓
PrayerSeedBank (per-colony, dedup, expire, max 20)
      ↓
SacredSiteManager (Prayer Stone blocks + MineColonies sacred buildings)
      ↓
Divine Answer Pipeline:
  /godspark answer → DivineAnswerInterpreter → DivineIntent
      ↓
  DivineIntentValidator (ORACLE_APPROVED / EFFECT_ELIGIBLE / REJECTED / DOWNGRADED)
      ↓
  [if EFFECT_ELIGIBLE]
      ├── PressureModifierManager (temporary -20 pressure, floor 20, 6000 ticks)
      └── [if worldEffects.enabled] WorldEffectEngine → ForgeWorldEffectApplier
              ├── SECURITY → Guardian's Vigil (Resistance I on guards)
              └── FOOD → Green Mercy (bonemeal-like crop growth)
```

---

## Completed Phases

| Phase | Status | Description | Key Files |
|---|---|---|---|
| 1 | ✅ DONE | Forge scaffold, MineColonies detection, commands | `GodsparkMod.java`, `GodsparkCommands.java` |
| 2 | ✅ DONE | Pressure Engine — 5 types, 0-100 scale | `pressure/` package |
| 3 | ✅ DONE | Story Events — 15 event types, EventQueue dedup | `story/` package |
| 3.5A | ✅ DONE | Event Lifecycle — ACTIVE/PERSISTENT/RESOLVED | `EventStateManager.java` |
| 3.5B | ✅ DONE | SavedData Persistence — NBT v5, events survive restarts | `GodsparkSavedData.java` |
| 4A | ✅ DONE | Memory System — colony memories, pattern detection | `memory/` package |
| 4B | ✅ DONE | Memory Influence — thresholds adjust based on history | `MemoryInfluence.java` |
| 4C | ✅ DONE | Prayer Seed Generation — channels, sacred site gate | `prayer/` package |
| 5A | ✅ DONE | AI Reflection — async LLM/template fallback | `ai/` package |
| 5B | ✅ DONE | Divine Answer Interpreter — `/godspark answer` | `divine/` package |
| 5C | ✅ DONE | DivineIntent Validator — EFFECT_ELIGIBLE gate | `DivineIntentValidator.java` |
| 5D.1 | ✅ DONE | Pressure Modifier Miracles — Guardian's Vigil, Green Mercy | `PressureModifierManager.java` |
| 5D.2 | ✅ DONE | Light World Effects — Resistance I on guards, crop growth | `world/` package (12 files) |
| 6A | ✅ DONE | Colony Personality — deterministic traits from history | `personality/` package |
| 6A.1 | ✅ DONE | PersonalityInfluence + PrayerTone integration | `PersonalityInfluence.java` |
| 6A.2 | ✅ DONE | Sacred Access Score + COMMONS channel | `SacredSiteManager.java` |
| 6A.2.1 | ✅ DONE | Sacred keyword compatibility patch | `BuildingClassifier.java` |
| 6A.3 | ✅ DONE | Prayer Stone block + SacredSiteManager | `block/`, `sacred/` packages |
| UI-A | ✅ DONE | Debug Dashboard (6-tab GUI) | `network/`, `client/` packages |

---

## Current Package Structure (61 Java files)

| Package | Files | Purpose |
|---|---|---|
| `com.godspark` | 2 | Mod entry point, constants, config |
| `com.godspark.ai` | 6 | AI reflection (async LLM + template fallback) |
| `com.godspark.block` | 2 | PrayerStoneBlock, PrayerStoneBlockEntity |
| `com.godspark.client` | 4 | Dashboard GUI, client state, screen |
| `com.godspark.command` | 1 | All `/godspark` commands (Brigadier) |
| `com.godspark.divine` | 12 | Divine answer: interpreter, validator, parser, intent |
| `com.godspark.event` | 1 | Server tick lifecycle |
| `com.godspark.memory` | 4 | Memory system |
| `com.godspark.network` | 6 | Packet system for dashboard |
| `com.godspark.observer` | 5 | MineColonies reflection scanner |
| `com.godspark.persistence` | 1 | NBT SavedData persistence |
| `com.godspark.personality` | 4 | Colony personality, traits, influence |
| `com.godspark.prayer` | 5 | Prayer seed generation with channels |
| `com.godspark.pressure` | 5 | Pressure computation + modifier manager |
| `com.godspark.registry` | 3 | Block/item/BlockEntity deferred registers |
| `com.godspark.sacred` | 3 | Sacred site tracking |
| `com.godspark.story` | 8 | Event lifecycle system |
| `com.godspark.world` | 12 | Light world effects engine + appliers |

---

## In-Game Commands

```
/godspark status              — Mod status, colony count, AI status
/godspark colonies             — List colonies with citizens/buildings
/godspark pressures            — Show pressure values per colony
/godspark events [N]          — Show recent events (default 10)
/godspark memories             — Show colony memories
/godspark influences           — Show memory threshold adjustments
/godspark prayers              — Show active prayer seeds
/godspark personality          — Show colony personality traits + prayer tone
/godspark reflect <colonyId>  — AI/template reflection
/godspark answer <colonyId> <message> — Player answers colony prayer (permission 2)
/godspark miracles [colonyId]  — Show active pressure modifiers + world effect status
/godspark ui                  — Open debug dashboard
```

---

## Active Documents (Current)

| Document | Purpose | Status |
|---|---|---|
| `AGENTS.md` | Agent context, build instructions, tool rules | ✅ Current |
| `docs/godspark-reference.md` | Main reference: architecture, formulas, thresholds, package structure | ✅ Current |
| `docs/current-architecture.md` | Detailed architecture with data flow | ✅ Current |
| `docs/roadmap-next-implementation.md` | Immediate build plan + completed phase details | ✅ Current |
| `docs/testing-and-deploy.md` | Build, test, deploy instructions | ✅ Current |
| `docs/minecolonies-api-quickref.md` | MineColonies API quick reference | ✅ Current |
| `docs/current-codebase-summary.md` | Package breakdown, key files, dependencies | ✅ Current (needs 5D.2 update) |
| `docs/roadmap-living-world-long-term.md` | Long-term vision document | ✅ Current |
| `EXPERIMENT.md` | UI-A colony grouping experiment notes | ✅ Current (active experiment) |

---

## Archived Documents (Deprecated/Outdated)

Moved to `docs/archive/`:

| Document | Why Archived |
|---|---|
| `PLAN.md` | Last commit reference is Phase 4A/4B — superseded by roadmap-next-implementation.md |
| `phase-1-observer.md` through `phase-4-memory-system-plan.md` | Historical phase documentation, phases complete |
| `phase-3-5-event-state-persistence.md` | Historical, persistence evolved to schema v5 |
| `phase-5b-divine-answer-proposal.md` | Proposal document, now fully implemented |
| `review-package-phases-5b-5c-5d1.md` | Review package for completed phases |
| `review-response-package.md` | Review response for completed phases |
| `gpt-review-package.md` | GPT review, historical |
| `godspark-colony-observer-review.md` | ColonyObserver review, fixes applied |
| `test-phase-6a1-polish.md` | Test plan for completed polish pass |

---

## Known TODOs / Technical Debt

| Item | Priority | Notes |
|---|---|---|
| Building classification uses heuristic string matching | Medium | Need proper MineColonies building type checks |
| Pressure formulas need tuning | Medium | Needs real gameplay data |
| Memory decay not implemented | Low | decayRate stored but unused |
| Phase 6B — Colony Intentions | Future | Not started |
| Phase 6C — Civilization Evolution | Future | Not started |
| Phase 5D.3 — Full domain coverage (COMFORT/HOUSING/INDUSTRY effects) | Future | Deferred after FOOD/SECURITY proven stable |

---

## Recent Changes (Latest Commit)

**Commit `8d2f031` on `master`:**
- Phase 5D.2: Light World Effects (12 new `world/` package files)
- Guardian's Vigil: Resistance I on MineColonies guards (reflection-based)
- Green Mercy: Bonemeal-like crop growth near colony center
- Config-gated (default OFF), server-thread-only, loaded-chunks-only
- Schema v5 persistence for world effect cooldowns
- Patch A: Weighted keyword scoring in `TemplateDivineInterpreter`
  - Fixes: `"heal the guard"` now routes to SECURITY (was COMFORT)
  - Action verbs W3, target nouns W2, generic overlap W1
  - Tokenization prevents false positives ("warehouse" ≠ "war")
- 20 new tests in `TemplateDivineInterpreterTest.java`

---

## How to Onboard

1. Read `AGENTS.md` for agent-specific instructions and build commands
2. Read `docs/godspark-reference.md` for architecture and design decisions
3. Read `docs/roadmap-next-implementation.md` for current build plan
4. Check `docs/current-codebase-summary.md` for package/file map
5. Run `gradlew test` to verify build
6. See `docs/testing-and-deploy.md` for deploy instructions

---

## Git Branches

| Branch | Purpose | Status |
|---|---|---|
| `master` | Stable, all phases 1-6A.3 + 5D.2 merged | ✅ Default |
| `phase-5d2-light-world-effects` | Feature branch (merged) | Can delete |
| `experiment/ui-a-colony-grouping` | Dashboard colony filter experiment | Active |

---

## Key Design Principles

1. **Deterministic:** No RNG for core logic. Colony state → pressure → events is reproducible.
2. **Bounded:** Miracles relieve pressure, don't erase consequences (floor 20).
3. **Player-mediated:** Divine answers require player action (`/godspark answer`). No auto-activation.
4. **Config-gated:** World effects default OFF. Server operator opts in.
5. **Server-thread-only:** All mutation happens on server tick. No concurrent access.
6. **Reflection-defensive:** MineColonies API accessed via reflection with graceful fallbacks.
