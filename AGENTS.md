# AGENTS.md — GodsparkNeo

## Project Mission

GodsparkNeo is a direct source-code fork of MineColonies (MC 1.21.1, NeoForge) that integrates Godspark systems directly into the colony simulation. The goal is a God Game / Autonomous Colony Simulation where:

- An **external LLM Director** reads exported colony JSON and proposes structured actions.
- **Java validates and executes** all world mutations.
- The **player** acts as a deity, answering prayers through blessings, miracles, and guidance.

## Licensing

- MineColonies is **GPL-3.0**.
- This fork is a GPL-3.0 derivative work.
- **Do not** add proprietary or GPL-incompatible libraries directly into the fork.
- **Do not** commit API keys, private LLM credentials, model keys, or paid service secrets.
- External LLM services must remain external processes or HTTP services.

## Critical Rules

1. **Do NOT rename the MineColonies runtime mod id** (`minecolonies`).
   - Changing it would break registries, resource locations, data packs, recipes, tags, loot tables, advancements, translations, structures, research data, save data, and upstream mergeability.
   - Repository/project name: `GodsparkNeo`
   - Runtime mod ID: `minecolonies`

2. **Do NOT mass-rename** MineColonies packages, registry namespaces, or resource directories.

3. **Do NOT** change core gameplay behavior unless explicitly scoped.

4. **Do NOT** allow the LLM to:
   - Run arbitrary commands
   - Execute raw code
   - Mutate the world directly
   - Bypass Java validators
   - Control pathfinding directly
   - Run every tick
   - Bypass MineColonies work-order systems
   - Override player/admin permissions
   - Create unbounded action loops

5. **Java must own** all validation, permission checks, cooldowns, persistence, locks, world mutation, and final execution.

## Verified Build Environment

| Item | Value |
|------|-------|
| Upstream branch | `version/1.21` |
| Upstream commit | `b1e469f56645b37483f16714a0ddab6db4138bf4` |
| Minecraft | 1.21.1 |
| Loader | NeoForge 21.1.80 |
| Java | 21 (LTS) |
| Gradle | 8.13 (wrapper) |
| Mod ID | `minecolonies` |
| Mod Group | `com.ldtteam` |
| Mod Version | `0.0.11` |

## Build Commands

```powershell
# Windows
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.5.11-hotspot"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\gradlew build --no-daemon

# Dev client
.\gradlew runClient --no-daemon
```

## Package Layout for New Systems

New Godspark code must be placed under:

```
com.godsparkneo
├── api
├── export
├── pressure
├── event
├── memory
├── prayer
├── personality
├── divine
├── llm
├── lock
├── action
├── validation
├── persistence
├── command
└── debug
```

## Implementation Phases

| Phase | Goal | Gate |
|-------|------|------|
| **A** | Import upstream, preserve legacy, build unmodified | `godsparkneo-phase-a-upstream-build-pass` |
| **B** | Minimal branding/docs, no gameplay changes | README, AGENTS.md, docs/ |
| **C** | Read-only colony JSON state exporter | `feature/state-exporter` |
| **D** | Port deterministic pressure/memory/prayer | `feature/pressure-port` |
| **E** | Lock/token reservation system | `feature/lock-state` |
| **F** | First Java-validated autopilot action | `feature/llm-action-validator` |
| **G** | Deity UI and blessing systems | `feature/deity-ui` |

## Upstream Sync Policy

- Keep upstream remote: `git remote add upstream https://github.com/ldtteam/minecolonies.git`
- Keep Godspark changes isolated where possible.
- Avoid mass formatting upstream files.
- Prefer additive classes and small integration points.
- Document every upstream file modified for Godspark.

When syncing:
```bash
git fetch upstream
git checkout main
git merge upstream/version/1.21
```

## Branches

| Branch | Purpose |
|--------|---------|
| `main` | GodsparkNeo fork main |
| `legacy/forge-1.20.1-companion` | Old companion mod |
| `upstream/version-main-sync` | Optional tracking branch |
| `feature/*` | Feature branches |

## Performance Guardrails

- Do not scan huge world areas frequently.
- Do not run AI on every tick.
- Do not serialize massive JSON every tick.
- Do not block the server thread with HTTP calls.
- Use throttled exporters, cached snapshots, async HTTP, and bounded data structures.

## Persistence Guardrails

Any long-lived system must persist:
- memories
- active pressure modifiers
- miracle cooldowns
- locks
- LLM action audit log
- colony-level Godspark state

All load paths must be defensive. Bad data must not crash the world.
