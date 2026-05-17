# Current Architecture

## Overview

Godspark is a Forge 1.20.1 MineColonies companion mod that observes colony state, computes societal pressures, generates story events with lifecycle tracking, maintains persistent colony memories, generates prayer seeds influenced by colony personality and sacred site access, reflects via optional AI, interprets divine answers from players, validates them through a Java gate, and applies bounded pressure modifier miracles.

## Data Flow

```
MineColonies API
      ↓
ColonyObserver.scan() [every 600 ticks]
      ↓
ColonySnapshot (23 fields: citizens, buildings, happiness, guards, sacredBuildingCount, gatheringBuildingCount, etc.)
      ↓
ObservedColony (history of 20 snapshots per colony)
      ↓
PressureEngine.compute() [every 1200 ticks, applies PressureModifierManager deltas]
      ↓
PressureSnapshot (5 values: FOOD, SECURITY, HOUSING, COMFORT, INDUSTRY, 0-100)
      ↓
EventGenerator.generate() [stateless, memory-adjusted thresholds]
      ↓
EventStateManager.processEvents() [ACTIVE → PERSISTENT → RESOLVED]
      ↓
MemoryEngine.generateMemories() [from transitions]
      ↓
ColonyMemory (SIGNIFICANT_EVENT, TRAUMA, PATTERN, TRIUMPH, CULTURAL)
      ↓
MemoryBank (per-colony, dedup, reinforce, trim to 50)
      ↓
MemoryInfluence → EventGenerator (threshold adjustments)
      ↓
PersonalityEngine (deterministic traits from colony history, cached per cycle)
      ↓
PersonalityInfluence (traits → threshold adjustments, prayer tone)
      ↓
PrayerSeedGenerator (pressures + events + memories + personality + sacred sites → prayer seeds)
      ↓
PrayerSeedBank (per-colony, dedup, expire, max 20)
      ↓
SacredSiteManager (dimension+position keyed, colony-bound Prayer Stone anchors)
      ↓
Divine Answer Pipeline:
  /godspark answer → DivineAnswerInterpreter → DivineIntent
      ↓
  DivineIntentValidator (ORACLE_APPROVED / EFFECT_ELIGIBLE / REJECTED / DOWNGRADED)
      ↓
  PressureModifierManager.tryApplyFromValidatedIntent() [if EFFECT_ELIGIBLE]
      ↓
Logs + /godspark commands
```

**Divine loop (player-mediated only, no auto-activation):**
```
Colony prays → Player answers → Template/AI interprets → Java validates →
  if EFFECT_ELIGIBLE: apply bounded pressure modifier (floor 20)
  if ORACLE_APPROVED/DOWNGRADED: narrative only, no world effect
```

## Package Structure

```
com.godspark
├── GodsparkMod.java                    — Mod entry point, static service instances
├── GodsparkConstants.java              — MOD_ID, VERSION, tick intervals
├── ai/
│   ├── AiClient.java                   — HTTP client for LLM API
│   ├── AiConfig.java                   — API key, endpoint, model config
│   ├── AiPromptBuilder.java            — Prompt assembly for reflection
│   ├── AiReflection.java               — Reflection result record
│   ├── AiReflectionService.java        — Async AI reflection service
│   ├── AiResponseParser.java           — JSON response parsing
│   └── TemplateReflectionService.java  — Keyword-based template fallback
├── command/
│   └── GodsparkCommands.java          — All /godspark commands
├── divine/
│   ├── DivineAnswer.java               — Record: playerId, colonyId, rawText, tick
│   ├── DivineAnswerContext.java        — Immutable context snapshot, List.copyOf
│   ├── DivineAnswerInterpreter.java    — Orchestrator: template + async AI + cooldowns
│   ├── DivineAnswerPromptBuilder.java  — LLM prompt from answer + colony state
│   ├── DivineIntent.java               — Record with compact constructor, sanitization
│   ├── DivineIntentParser.java         — AI JSON parser, nullable domain, safe fallbacks
│   ├── DivineIntentValidator.java      — Static validation, domain inference, prayer recalc
│   ├── IntentSource.java               — Enum: TEMPLATE, AI, AI_FALLBACK, AI_COOLDOWN, PARSER_FALLBACK
│   ├── IntentType.java                 — Enum: ORACLE_ONLY, ANSWER_QUESTION, CREATE_TRIAL, BLESS_COLONY
│   ├── TemplateDivineInterpreter.java  — Keyword matching with 3-tier prayer gate
│   ├── ValidatedIntent.java            — Record: intent + ValidationResult + notes
│   └── ValidationResult.java           — Enum: ORACLE_APPROVED, EFFECT_ELIGIBLE, REJECTED, DOWNGRADED
├── event/
│   └── GodsparkServerEvents.java       — Server tick heartbeat, modifier tick, lifecycle
├── memory/
│   ├── ColonyMemory.java               — Immutable memory record (12 fields)
│   ├── MemoryBank.java                — Per-colony memory storage, dedup, reinforce
│   ├── MemoryEngine.java              — Generates memories from event transitions
│   ├── MemoryInfluence.java            — Computes threshold adjustments from memories
│   └── MemoryType.java                — Enum: SIGNIFICANT_EVENT, TRAUMA, PATTERN, TRIUMPH, CULTURAL
├── observer/
│   ├── BuildingCategory.java          — Enum: FOOD, SECURITY, HOUSING, COMFORT, INDUSTRY, WAREHOUSE, SACRED, OTHER
│   ├── BuildingClassifier.java        — Maps building names to categories; sacred keywords: altar/sanctuary/oracle/ritual/rune/totem/spirit/divine/sacred/reliquary/obelisk; graveyard is gathering-only (COMMONS)
│   ├── ColonyObserver.java            — Reflection-based MineColonies API scanner
│   ├── ColonySnapshot.java            — Immutable colony state record (23 fields incl. sacred/gathering/raid)
│   └── ObservedColony.java            — Tracks snapshot history per colony
├── persistence/
│   └── GodsparkSavedData.java         — NBT serialization (v2), backward compat
├── prayer/
│   ├── PrayerChannel.java             — Enum: PRIVATE, COMMONS, CHURCH, SHRINE, TEMPLE (isPublic, isSacred, isMiracleEligible)
│   ├── PrayerSeed.java                — Immutable prayer seed record (13 fields, isPublicPrayer, isMiracleEligible)
│   ├── PrayerSeedBank.java            — Per-colony prayer storage, dedup, expire, max 20
│   ├── PrayerSeedGenerator.java       — Generates seeds with channel/personality/tone/sacred scoring
│   ├── PrayerTone.java                — Enum: 8 tones with adverbial phrases, fromTrait() mapping
│   └── PrayerType.java               — Enum: PLEA, VIGIL, THANKS, LAMENT, HOPE
├── pressure/
│   ├── PressureEngine.java            — Computes 5 pressure types, applies modifier deltas
│   ├── PressureModifier.java          — Record: colonyId, pressureType, amount, expiresAtTick, source
│   ├── PressureModifierManager.java  — Server-thread-only, miracle application + expiry
│   ├── PressureSnapshot.java          — Immutable pressure values record
│   └── PressureType.java              — Enum: FOOD, SECURITY, HOUSING, COMFORT, INDUSTRY
├── personality/
│   ├── ColonyPersonality.java        — Record: primaryTrait, secondaryTrait, scores, evidence
│   ├── PersonalityEngine.java        — Computes traits from history, cached updateFromObservations()
│   ├── PersonalityInfluence.java     — traits → threshold adjustments (±5 per trait, combined clamp -20/+10)
│   └── PersonalityTrait.java         — Enum: 11 traits with descriptions
├── sacred/
│   ├── SacredSiteManager.java        — Singleton, dimension+position keyed, colony-bound sacred anchors
│   ├── SacredSiteType.java           — Enum: PRAYER_STONE(anchorScore=1), MINECOLONIES_SACRED
│   └── SacredSiteRecord.java         — Record: type, pos, dimension, colonyId, registeredAtTick
├── block/
│   └── PrayerStoneBlock.java         — BaseEntityBlock, lightLevel=7, right-click=actionbar message
├── block/entity/
│   └── PrayerStoneBlockEntity.java   — Auto-registers with SacredSiteManager on server load; chunk unload does not unregister
├── registry/
│   ├── GodsparkBlocks.java           — DeferredRegister: PRAYER_STONE
│   ├── GodsparkBlockEntities.java    — DeferredRegister: PRAYER_STONE BlockEntityType
│   └── GodsparkItems.java            — DeferredRegister: PRAYER_STONE BlockItem
└── story/
    ├── EventGenerator.java             — Stateless: pressures → event candidates (memory-adjusted)
    ├── EventQueue.java                 — Dedup (6000 ticks), history (100 max)
    ├── EventRecord.java                — Wraps StoryEvent with lifecycle state
    ├── EventSeverity.java              — Enum: LOW(1), MEDIUM(2), HIGH(3)
    ├── EventState.java                 — Enum: ACTIVE(1), PERSISTENT(2), RESOLVED(3)
    ├── EventStateManager.java          — Lifecycle tracking, keyed by colonyId+PressureType
    ├── StoryEvent.java                 — Immutable event data record
    └── StoryEventType.java             — 15 event types with thresholds/descriptions
```

## Static Services (GodsparkMod)

| Service | Purpose |
|---|---|
| COLONY_OBSERVER | Scans MineColonies colonies via reflection |
| PRESSURE_ENGINE | Computes pressure values, applies modifier deltas |
| PRESSURE_MODIFIER_MANAGER | Manages temporary pressure bonuses/penalties from miracles |
| EVENT_GENERATOR | Generates event candidates from pressures |
| EVENT_QUEUE | Display/log deduplication |
| EVENT_STATE_MANAGER | Lifecycle state tracking |
| MEMORY_BANK | Per-colony memory storage |
| MEMORY_ENGINE | Memory generation from transitions |
| MEMORY_INFLUENCE | Threshold adjustments from memories |
| PRAYER_SEED_GENERATOR | Generates prayer seeds from separate true-sacred and Prayer Stone anchor inputs |
| PRAYER_SEED_BANK | Per-colony prayer seed storage |
| PERSONALITY_ENGINE | Computes colony personality from history, cached per cycle |
| PERSONALITY_INFLUENCE | Threshold adjustments from personality traits |
| AI_REFLECTION_SERVICE | Async AI/template reflection |
| DIVINE_ANSWER_INTERPRETER | Interprets player divine answers |

## Divine Answer Architecture

```
/godspark answer <colonyId> <message>
    ↓
GodsparkCommands (server thread)
    ↓ sanitize input (500 char cap, control chars stripped)
    ↓ build DivineAnswerContext (immutable snapshot on server thread)
    ↓ construct DivineAnswer
    ↓ DivineAnswerInterpreter.interpretAsync()
        ↓ if AI disabled or no public prayers: template path (sync)
        ↓ if on cooldown: template with AI_COOLDOWN source
        ↓ otherwise: async AI call, fallback to template on failure
    ↓ parse → DivineIntent
    ↓ DivineIntentValidator.validate()
        ↓ if ORACLE_ONLY/ANSWER_QUESTION: ORACLE_APPROVED (no effect)
        ↓ if BLESS_COLONY with matching prayer: EFFECT_ELIGIBLE
        ↓ if BLESS_COLONY without matching prayer: DOWNGRADED
        ↓ if CREATE_TRIAL: ORACLE_APPROVED (narrative-only)
        ↓ domain inference from strongest public prayer if null
        ↓ prayer match recalculated by Java (not AI)
    ↓ if EFFECT_ELIGIBLE: PressureModifierManager.tryApplyFromValidatedIntent()
        ↓ BLESS_COLONY + SECURITY: Guardian's Vigil (if crisis present)
        ↓ BLESS_COLONY + FOOD: Green Mercy (if crisis present)
        ↓ pressure floor of 20, 24000-tick shared cooldown
    ↓ return result to server thread via server.execute()
```

## Pressure Modifier Details

| Miracle | Domain | Amount | Duration | Floor | Cooldown | Precondition |
|---|---|---|---|---|---|---|
| Guardian's Vigil | SECURITY | -20 | 6000 ticks | 20 | 24000 ticks (shared) | raid OR pressure≥70 OR event≥MEDIUM |
| Green Mercy | FOOD | -20 | 6000 ticks | 20 | 24000 ticks (shared) | pressure≥70 OR event≥MEDIUM |

**No automatic activation.** Miracles only fire from `/godspark answer` after validation.

## ValidationResult Semantics

| Result | Meaning | World Effect |
|---|---|---|
| ORACLE_APPROVED | Narrative-only answer | None |
| EFFECT_ELIGIBLE | Approved for miracle | Pressure modifier applied |
| REJECTED | Invalid/null intent | None |
| DOWNGRADED | Intent downgraded to ORACLE_ONLY | None |

## Tick Intervals

| Interval | Ticks | Seconds | Purpose |
|---|---|---|---|
| Observer scan | 600 | 30 | Scan MineColonies colonies |
| Pressure compute | 1200 | 60 | Compute pressures, generate events |
| Modifier expiry | 1 | 0.05 | Check and expire pressure modifiers |
| Event dedup cooldown | 6000 | 300 | Prevent event spam |
| AI success cooldown | 6000 | 300 | Per-colony AI call throttling |
| AI failure backoff | 1200 | 60 | Per-colony failure cooldown |
| Miracle cooldown | 24000 | 1200 | Per-colony miracle throttling (shared) |
| Miracle duration | 6000 | 300 | How long a pressure modifier lasts |

## Key Design Principles

1. **Java owns truth** — LLM narrates only; Java validates all intent, recalculates prayer match, controls world effects
2. **No auto-miracles** — pressure modifiers only from `/godspark answer` after validation
3. **Immutable context** — `DivineAnswerContext` uses `List.copyOf`, built on server thread before async work
4. **Domain inference** — if AI returns null domain and one strongest public prayer exists, infer domain with ×0.8 confidence
5. **Pressure floor 20** — miracles relieve suffering, don't erase consequences
6. **CREATE_TRIAL is narrative-only** — no pressure modifier, no world effect, until future phase
7. **Server-thread-only modifier manager** — `PressureModifierManager` uses `ArrayList`/`HashMap`, no concurrent access
8. **Sacred prayer gate** — no public prayer → ORACLE_ONLY only; no matching prayer → DOWNGRADED; COMMONS not miracle-eligible
9. **Reflection for MineColonies API** — compile-time types incomplete, runtime access via `getMethod()`/`invoke()`
10. **All reflection methods defensive** — safe defaults on failure
11. **Personality from previous cycle** — cached, never affects PressureEngine baselines, only event thresholds
12. **SacredSiteManager server-thread-only** — no world scanning, block entities self-register on load
13. **Prayer Stone is player-placed** — citizens do not autonomously build it; future Phase 6B may add colony intentions
14. **COMMONS separates "heard" from "miracle-eligible"** — gathering places give voice, sacred sites unlock miracles
