# GodsparkNeo

**A God Game / Autonomous Colony Simulation fork of MineColonies**

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

GodsparkNeo is a direct source-code fork of [MineColonies](https://github.com/ldtteam/minecolonies), pivoting from the previous `GodsparkForge` companion-mod architecture into an integrated simulation where an external LLM Director observes colony state, proposes structured actions, and the player acts as a deity answering citizen prayers.

## What Changed

- **Old**: `GodsparkForge` was a Forge 1.20.1 companion mod that observed MineColonies through reflection.
- **New**: `GodsparkNeo` is a direct MineColonies 1.21.1 fork with Godspark systems integrated into the core codebase.

The legacy companion-mod code is preserved on branch `legacy/forge-1.20.1-companion`.

## Architecture

```
MineColonies colony state
  ↓
GodsparkNeo State Exporter (read-only JSON)
  ↓
External LLM Director
  ↓
Structured proposed action
  ↓
GodsparkNeo Java validator
  ↓
Allowed action executor
  ↓
MineColonies work-order / building / citizen systems
  ↓
Audit log + memory update + prayer update
```

- The **LLM never directly mutates the world**.
- **Java validates and executes** all real effects.
- The **player** receives prayers and answers through bounded blessings, miracles, or guidance.

## License

This project is a derivative work of MineColonies, which is licensed under [GPL-3.0](https://www.gnu.org/licenses/gpl-3.0).

> If you distribute a built JAR, modified source, modpack, server package, or binary, GPL-3.0 source-distribution obligations may apply. This repository does not include proprietary or GPL-incompatible embedded dependencies.

## Build Requirements

| Requirement | Version |
|-------------|---------|
| Minecraft   | 1.21.1  |
| Loader      | NeoForge 21.1.80 |
| Java        | 21 (LTS) |
| Gradle      | 8.13 (via wrapper) |

## Build

On Windows (PowerShell):
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.5.11-hotspot"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\gradlew build --no-daemon
```

On Linux/macOS:
```bash
export JAVA_HOME=/path/to/jdk-21
./gradlew build --no-daemon
```

## Run Client (Dev)

```powershell
.\gradlew runClient --no-daemon
```

## Upstream

- **Upstream repository**: [ldtteam/minecolonies](https://github.com/ldtteam/minecolonies)
- **Upstream branch used**: `version/1.21`
- **Runtime mod ID**: `minecolonies` (intentionally preserved for compatibility)

## Documentation

- [AGENTS.md](AGENTS.md) — Coding agent briefing, guardrails, and build commands
- [docs/godsparkneo-architecture.md](docs/godsparkneo-architecture.md) — System architecture
- [docs/godsparkneo-roadmap.md](docs/godsparkneo-roadmap.md) — Implementation roadmap
- [docs/legacy-godspark-port-map.md](docs/legacy-godspark-port-map.md) — Legacy system porting guide
- [docs/upstream-sync-policy.md](docs/upstream-sync-policy.md) — How to stay mergeable with upstream

## Legacy

The original `GodsparkForge` Forge 1.20.1 companion mod is archived on:
- Branch: `legacy/forge-1.20.1-companion`
- Tag: `legacy-forge-1.20.1-companion-final`
