# Current Codebase Summary

## Project
**Godspark** — Forge 1.20.1 MineColonies companion mod

## Version
0.1.0

## Total Files
48 Java files

## Package Breakdown

| Package | Files | Purpose |
|---|---|---|
| `com.godspark` | 2 | Mod entry point, constants |
| `com.godspark.ai` | 6 | AI reflection (async LLM + template fallback) |
| `com.godspark.command` | 1 | Brigadier commands (all /godspark commands) |
| `com.godspark.divine` | 11 | Divine answer interpretation, validation, miracles |
| `com.godspark.event` | 1 | Server tick lifecycle, modifier tick |
| `com.godspark.memory` | 4 | Memory system (Phase 4A) |
| `com.godspark.observer` | 5 | MineColonies reflection scanner + building classifier |
| `com.godspark.persistence` | 1 | NBT SavedData persistence |
| `com.godspark.prayer` | 5 | Prayer seed generation with channels |
| `com.godspark.pressure` | 5 | Pressure computation + modifier manager |
| `com.godspark.story` | 8 | Event lifecycle system |

## Completed Phases

| Phase | Status | Description |
|---|---|---|
| 1 | ✅ DONE | Forge scaffold, MineColonies detection, commands |
| 2 | ✅ DONE | Pressure Engine — 5 pressure types, 0-100 scale |
| 3 | ✅ DONE | Story Events — 15 event types, EventQueue dedup |
| 3.5A | ✅ DONE | Event Lifecycle State — ACTIVE/PERSISTENT/RESOLVED |
| 3.5B | ✅ DONE | SavedData Persistence — events survive restarts |
| 4A | ✅ DONE | Memory System — colony memories, pattern detection |
| 4B | ✅ DONE | Memory Influence — memories affect event thresholds |
| 4C | ✅ DONE | Prayer Seed Generation — channels, sacred site gate |
| 5A | ✅ DONE | AI Reflection — async LLM/template reflection |
| 5B | ✅ DONE | Divine Answer Interpreter — `/godspark answer` |
| 5C | ✅ DONE | DivineIntent Validator — ORACLE_APPROVED / EFFECT_ELIGIBLE / REJECTED / DOWNGRADED |
| 5D.1 | ✅ DONE | Pressure Modifier Miracles — Guardian's Vigil, Green Mercy |

## Next Candidate Phases

| Phase | Risk | Description |
|---|---|---|
| 5 Stabilization | Low | In-game smoke testing of 5B-5D.1 pipeline |
| 6A | Low | Colony Personality — deterministic traits from history |
| 5D.2 | Medium | Light World Effects — entity effects, crop growth |

## Key Files (divine package)

| File | Purpose |
|---|---|
| `DivineAnswer.java` | Player answer record |
| `DivineAnswerContext.java` | Immutable colony context for async work |
| `DivineAnswerInterpreter.java` | Orchestrator: template + async AI + cooldowns + context builder |
| `DivineAnswerPromptBuilder.java` | LLM prompt construction from answer + state |
| `DivineIntent.java` | Result record with compact constructor, sanitization |
| `DivineIntentParser.java` | AI JSON parser, nullable domain, safe fallbacks |
| `DivineIntentValidator.java` | Domain inference, prayer-match recalculation, downgrade |
| `IntentType.java` | ORACLE_ONLY, ANSWER_QUESTION, CREATE_TRIAL, BLESS_COLONY |
| `IntentSource.java` | TEMPLATE, AI, AI_FALLBACK, AI_COOLDOWN, PARSER_FALLBACK |
| `TemplateDivineInterpreter.java` | Keyword matching with 3-tier sacred prayer gate |
| `ValidatedIntent.java` | Intent + ValidationResult + validation notes |
| `ValidationResult.java` | ORACLE_APPROVED, EFFECT_ELIGIBLE, REJECTED, DOWNGRADED |

## Dependencies
- Forge 47.x (1.20.1)
- MineColonies 1.20.1-1.1.x (required, reflection)
- Create (optional, detected at runtime)
- Gson (bundled with Forge)
- No other external libraries

## Build
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\gradlew build --no-daemon
.\deploy.ps1
```

## Test Instance
```
C:\Users\Suttawat\AppData\Roaming\PrismLauncher\instances\Create'a Colony
```

## Known Issues / TODOs
- Building classification uses heuristic string matching — replace with proper MineColonies building type checks
- Pressure formulas need tuning with real data
- Memory decay not implemented (decayRate stored but unused)
- CREATE_TRIAL is narrative-only — no world effect yet
- No persistence for pressure modifiers (runtime only — cleared on restart)
- Phase 5D.2 (light world effects) and Phase 6A (colony personality) not yet started