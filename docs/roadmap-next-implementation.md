# Godspark — Next Implementation Plan

> This is the immediate build plan. See `docs/roadmap-living-world-long-term.md` for the full vision.

---

## Core Loop

```
Colony prays → Player answers → LLM/template interprets → Java validates → Miracle applies pressure modifier (if eligible) → Colony remembers
```

The colony generates prayer seeds from its state (Phase 4C). The player answers via `/godspark answer`. The template interpreter (or optional LLM) produces a `DivineIntent`. Java validates through `DivineIntentValidator` and applies bounded pressure modifiers only for `EFFECT_ELIGIBLE` intents.

---

## Current State

Phases 1–6A.3 complete. Full pipeline (observer → pressure → event → memory → influence → personality → prayer → reflection → divine answer → validation → miracles) is implemented and deployed, including Prayer Stone block, SacredSiteManager, and channel progression.

---

## Phase Stabilization — In-Game Testing

Before adding new features, validate the full divine loop in a real Forge/MineColonies instance.

### Startup / Status

```
/godspark status
/godspark ai status
```

Expected: MineColonies detected, AI disabled, no startup exceptions.

### Core Pipeline

```
/godspark colonies
/godspark pressures
/godspark events
/godspark memories
/godspark influences
/godspark prayers
```

### Divine Answer Pipeline

```
/godspark answer 1 stand firm through the raid
/godspark answer 1 we need food
/godspark answer 1 what is happening
```

Expected: `IntentType`, `ValidationResult`, `Domain`, `Source`, validation notes.

### Pressure Modifiers

```
/godspark miracles
/godspark miracles 1
```

Before triggering: "No active miracle modifiers."
After `BLESS_COLONY` + `EFFECT_ELIGIBLE`: modifier shown with `base=X → effective=Y`.

### Edge Cases

```
/godspark answer              → Brigadier error (missing args)
/godspark answer 1            → Brigadier error (missing message)
/godspark answer 0 test       → "Colony #0 not found"
/godspark answer 999 test     → "Colony #999 not found"
/godspark answer 1 <500+chars> → truncated at 500
```

---

## Completed Phases

### Phase 5A — AI Reflection

`/godspark reflect <colonyId>` — async, template + optional LLM. AI cooldowns, JSON fallback.

### Phase 5B — Divine Answer Interpreter

`/godspark answer <colonyId> <message>` — player answers colony prayers. Template keyword matching (always available) + optional LLM. Produces `DivineIntent` with `IntentType`, domain, confidence, oracle text.

**New package:** `com.godspark.divine`

**New files:**
| File | Purpose |
|---|---|
| `DivineAnswer.java` | Record: playerId, playerName, colonyId, rawText, submittedAtTick |
| `DivineAnswerContext.java` | Immutable colony snapshot for async work, List.copyOf |
| `DivineIntent.java` | Record with compact constructor: schemaVersion, colonyId, intentType, domain (nullable), confidence, oracleText, reasonCodes, source |
| `IntentType.java` | Enum: ORACLE_ONLY, ANSWER_QUESTION, CREATE_TRIAL, BLESS_COLONY |
| `IntentSource.java` | Enum: TEMPLATE, AI, AI_FALLBACK, AI_COOLDOWN, PARSER_FALLBACK |
| `DivineAnswerInterpreter.java` | Orchestrator: template sync, async AI, cooldowns, buildContext() |
| `DivineAnswerPromptBuilder.java` | Builds LLM system+user messages from context |
| `DivineIntentParser.java` | Validates AI JSON, nullable domain, safe fallbacks |
| `TemplateDivineInterpreter.java` | Keyword matching with 3-tier sacred prayer gate |

**3-tier sacred prayer gate:** (1) no public prayers → ORACLE_ONLY only, (2) public prayers but no domain match → ORACLE_ONLY only, (3) public prayers with domain match → all intent types available.

### Phase 5C — DivineIntent Validator

**ValidationResult** enum: `ORACLE_APPROVED`, `EFFECT_ELIGIBLE`, `REJECTED`, `DOWNGRADED`. Only `EFFECT_ELIGIBLE` permits pressure modifiers.

**New files:**
| File | Purpose |
|---|---|
| `ValidationResult.java` | Enum with `permitsPressureModifier()` |
| `DivineIntentValidator.java` | Static validation: domain inference, prayer-match recalculation, downgrade logic |
| `ValidatedIntent.java` | Record: intent + validation result + notes, helper methods |

**Key validation rules:**
- ORACLE_ONLY / ANSWER_QUESTION → `ORACLE_APPROVED` (no recalculation, no world effect)
- BLESS_COLONY with matching public prayer → `EFFECT_ELIGIBLE` (prayer match computed by Java, not AI)
- BLESS_COLONY without matching prayer → `DOWNGRADED` to ORACLE_ONLY
- CREATE_TRIAL → `ORACLE_APPROVED` with `TRIAL_EFFECTS_NOT_IMPLEMENTED` (narrative-only)
- NULL intent → `REJECTED`
- NULL context → `DOWNGRADED`

**Domain inference:** If AI returns null domain and exactly one strongest public prayer exists, infer domain from that prayer with `DOMAIN_INFERRED_FROM_PUBLIC_PRAYER` note and ×0.8 confidence.

### Phase 5D.1 — Internal Minor Miracles (Pressure Modifiers)

Miracles apply **only** through `/godspark answer` after validation. No automatic server-tick activation.

**New files:**
| File | Purpose |
|---|---|
| `PressureModifier.java` | Record: colonyId, pressureType, amount, createdAtTick, expiresAtTick, source |
| `PressureModifierManager.java` | Server-thread-only manager, modifiers, cooldowns, floor=20 |

**Guardian's Vigil** (SECURITY domain, BLESS_COLONY):
- Precondition: active raid OR SECURITY pressure >= 70 OR SECURITY event >= MEDIUM
- Matching public SECURITY prayer required (via validator)
- Effect: -20 SECURITY pressure for 6000 ticks, floor 20
- Cooldown: 24000 ticks per colony (shared with Green Mercy)

**Green Mercy** (FOOD domain, BLESS_COLONY):
- Precondition: FOOD pressure >= 70 OR FOOD event >= MEDIUM
- Matching public FOOD prayer required
- Effect: -20 FOOD pressure for 6000 ticks, floor 20
- Same shared cooldown

**Key design decisions:**
- `PressureModifierManager` is server-thread-only (no concurrent access)
- Pressure floor of 20: miracles relieve suffering, don't erase consequences
- No TRIUMPH memory on activation (future: create memory when crisis resolves)
- CREATE_TRIAL is narrative-only (no pressure modifier, no world effect)
- AI cannot claim `matchedPublicPrayer` — Java recalculates from `DivineAnswerContext`
- `interpretAsync()` returns template immediately when context is null or no public prayers

---

## Step 4 — Phase 5D.2: Light World Effects

**Prerequisite:** Phase 5D.1 stable and proven in-game. Config toggle defaults to OFF.

### Guardian's Vigil World Effect
- Apply Resistance I to guard-like citizens during active raid
- Duration capped (6000 ticks), only loaded entities
- No AI/pathfinding control

### Green Mercy World Effect
- Bounded crop growth pulses near colony
- Loaded chunks only, existing crops only
- Limited attempts per pulse, duration capped

---

### Phase 6A — Colony Personality

PersonalityTrait enum (11 traits), PersonalityEngine (deterministic traits from history), `/godspark personality` command, ColonyPersonality record.

### Phase 6A.1 — PersonalityInfluence + PrayerTone Integration

PersonalityInfluence computes ±5 threshold adjustments per trait (combined clamp -20/+10, memory ±20). PrayerTone enum (8 tones with adverbial phrases). EventGenerator and PrayerSeedGenerator accept personality maps. GodsparkServerEvents wires personality into tick cycle.

### Phase 6A.2 — Sacred Access Score + COMMONS Channel

PrayerChannel expanded to PRIVATE/COMMONS/CHURCH/SHRINE/TEMPLE. BuildingClassifier.isGatheringPlace(). ColonySnapshot.gatheringBuildingCount. selectPrayerChannel() uses sacred access scoring. DivineIntentValidator requires sacred-eligible prayers for miracles.

### Phase 6A.2.1 — Sacred Keyword Compatibility Patch

- Graveyard moved from SACRED to gathering-only (COMMONS, not miracle-eligible)
- 11 new sacred keywords: altar, sanctuary, oracle, ritual, rune, totem, spirit, divine, sacred, reliquary, obelisk
- CHURCH threshold fix: score >= 4 for SHRINE (1 sacred building = CHURCH, not SHRINE)

### Phase 6A.3 — Prayer Stone Block Stabilization

| File | Purpose |
|---|---|
| `sacred/SacredSiteType.java` | Enum: PRAYER_STONE(anchorScore=1), MINECOLONIES_SACRED |
| `sacred/SacredSiteRecord.java` | Record: type, pos, dimension, colonyId, registeredAtTick |
| `sacred/SacredSiteManager.java` | Dimension+position keyed, binds Prayer Stones to nearest valid colony within 128 blocks |
| `block/PrayerStoneBlock.java` | BaseEntityBlock, lightLevel=7, right-click=actionbar message |
| `block/entity/PrayerStoneBlockEntity.java` | Auto-registers on server load; chunk unload does not unregister |
| `registry/GodsparkBlocks.java` | DeferredRegister: PRAYER_STONE |
| `registry/GodsparkBlockEntities.java` | DeferredRegister: PRAYER_STONE BlockEntityType |
| `registry/GodsparkItems.java` | DeferredRegister: PRAYER_STONE BlockItem |

Recipe: torch + dandelion + smooth_stone -> 1 Prayer Stone. Player-placed only. Block break unregisters; chunk unload does not.

Channel progression separates true sacred buildings from Prayer Stone anchors: no sacred/no stone -> PRIVATE, gathering only -> COMMONS, one local Prayer Stone -> CHURCH, one true sacred building -> CHURCH, two true sacred buildings -> SHRINE, three or more true sacred buildings -> TEMPLE. Multiple Prayer Stones stay capped at CHURCH.

---

## Phase UI-A — Debug Dashboard (Complete)

6-tab read-only GUI, SimpleChannel network, immutable payloads, keybind + `/godspark ui` command. OP level 2 gated, 1 req/sec throttle. `@OnlyIn(Dist.CLIENT)` client classes.

---

## In-Game Commands

```
/godspark status              — Mod status, colony count, AI status
/godspark colonies             — List colonies with citizens/buildings
/godspark pressures            — Show pressure values per colony
/godspark events [N]          — Show recent events (default 10)
/godspark memories [colonyId] — Show colony memories
/godspark influences           — Show memory threshold adjustments
/godspark prayers [colonyId]  — Show active prayer seeds
/godspark personality [colonyId] — Show personality traits, threshold influence, prayer tone
/godspark reflect <colonyId>  — AI/template reflection
/godspark answer <colonyId> <message> — Player answers colony prayer (permission 2)
/godspark miracles [colonyId] — Show active pressure modifiers
```

---

## Build Then Test

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\gradlew build --no-daemon
.\deploy.ps1
```

### Core Test

```
/godspark answer 1 stand firm through the raid
```

Expected: structured response with IntentType, Domain, ValidationResult, validation notes, no crash, no server freeze.

---

*Next step: In-game stabilization testing, then Phase 5D.2 (world effects) or Phase 6B (colony intentions).*
