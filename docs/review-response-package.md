# Godspark Review Response Package — P1/P2 Fixes Applied

## What Changed

All P1 blockers and P2 recommended fixes from the previous review have been implemented.

### P1-1: Automatic Miracle Activation Removed
Miracle triggering was removed from `GodsparkServerEvents`. Miracles now **only** fire from the `/godspark answer` command flow after validation.

**GodsparkServerEvents.java** — server tick now only:
```java
if (tickCounter % GodsparkConstants.PRESSURE_INTERVAL_TICKS == 0) {
    GodsparkMod.COLONY_OBSERVER.scan(server);
    GodsparkMod.PRESSURE_ENGINE.compute(...);
    // candidates, events, memories, prayers...
    logColonySummary();
}
```
No miracle loop. Only `tick()` for expiry.

---

### P1-2 + P1-3: Validator Now Produces Downgraded Intent + Recalculates Prayer Match

**DivineIntentValidator.java** — key changes:

```java
// BLESS_COLONY validation — downgrade actually creates ORACLE_ONLY intent
private static ValidatedIntent validateBlessColony(DivineIntent intent, DivineAnswerContext context,
                                                 List<String> notes) {
    if (!context.hasPublicPrayers()) {
        notes.add("NO_ACTIVE_PRAYERS");
        return downgrade(intent, "NO_ACTIVE_PRAYERS", notes);
    }

    PrayerSeed matchingPrayer = findMatchingPrayer(context, intent);
    if (matchingPrayer == null) {
        notes.add("NO_PRAYER_MATCH_DOMAIN");
        notes.add("BLESS_REQUIRES_MATCHING_PRAYER");
        return downgrade(intent, "NO_MATCHING_PUBLIC_PRAYER", notes);
    }

    // Java calculates matchedPublicPrayer from context — not from AI
    DivineIntent corrected = new DivineIntent(
        intent.schemaVersion(), intent.colonyId(), intent.colonyName(),
        intent.intentType(), intent.domain(), intent.confidence(),
        true, matchingPrayer.sourceKey(),
        intent.oracleText(), intent.reasonCodes(), intent.source()
    );

    notes.add("BLESS_COLONY_VALIDATED");
    notes.add("MATCHED_PRAYER_" + matchingPrayer.sourceKey());
    return new ValidatedIntent(corrected, ValidationResult.EFFECT_ELIGIBLE, notes);
}

// Downgrade helper — actually changes intent type
private static ValidatedIntent downgrade(DivineIntent intent, String reason, List<String> notes) {
    notes.add(reason);
    notes.add("DOWNGRADED_TO_ORACLE_ONLY");

    DivineIntent downgraded = new DivineIntent(
        intent.schemaVersion(),
        intent.colonyId(),
        intent.colonyName(),
        IntentType.ORACLE_ONLY,          // Changed from whatever the AI said
        intent.domain(),
        intent.confidence() * 0.5,       // Confidence halved
        false,                           // matchedPublicPrayer cleared
        "",
        "The Spark hears, but the colony's need is not yet gathered. Only an oracle may speak.",
        intent.reasonCodes(),
        intent.source()
    );

    return new ValidatedIntent(downgraded, ValidationResult.DOWNGRADED, notes);
}

// Java recalculates prayer match — AI cannot claim it matched
private static DivineIntent recalculatePrayerMatch(DivineIntent intent, DivineAnswerContext context) {
    if (context == null || !intent.hasDomain() || !context.hasPublicPrayers()) {
        if (intent.matchedPublicPrayer() || !intent.matchedPrayerSourceKey().isEmpty()) {
            return new DivineIntent(
                intent.schemaVersion(), intent.colonyId(), intent.colonyName(),
                intent.intentType(), intent.domain(), intent.confidence(),
                false, "",                                   // Override AI's claim
                intent.oracleText(), intent.reasonCodes(), intent.source()
            );
        }
        return intent;
    }

    PrayerSeed prayer = findMatchingPrayer(context, intent);
    boolean matched = prayer != null;
    String sourceKey = prayer != null ? prayer.sourceKey() : "";

    if (intent.matchedPublicPrayer() != matched || !intent.matchedPrayerSourceKey().equals(sourceKey)) {
        return new DivineIntent(
            intent.schemaVersion(), intent.colonyId(), intent.colonyName(),
            intent.intentType(), intent.domain(), intent.confidence(),
            matched, sourceKey,                           // Java overrides AI
            intent.oracleText(), intent.reasonCodes(), intent.source()
        );
    }
    return intent;
}
```

---

### P1-4: ValidationResult Split

```java
public enum ValidationResult {
    ORACLE_APPROVED("Oracle Approved"),   // No world effect
    EFFECT_ELIGIBLE("Effect Eligible"),  // Miracle eligible
    REJECTED("Rejected"),
    DOWNGRADED("Downgraded");

    public boolean permitsPressureModifier() {
        return this == EFFECT_ELIGIBLE;
    }
}
```

Only `EFFECT_ELIGIBLE` permits pressure modifiers. `ORACLE_APPROVED` means harmless narrative output.

---

### P2-1: DivineAnswerContext Null-Safe Immutable Constructor

```java
public record DivineAnswerContext(
    ColonySnapshot colonySnapshot,
    PressureSnapshot pressureSnapshot,
    List<PrayerSeed> publicPrayers,
    List<EventRecord> activeEvents,
    List<ColonyMemory> memories
) {
    public DivineAnswerContext {
        publicPrayers = publicPrayers == null ? List.of() : List.copyOf(publicPrayers);
        activeEvents = activeEvents == null ? List.of() : List.copyOf(activeEvents);
        memories = memories == null ? List.of() : List.copyOf(memories);
    }

    public boolean hasPublicPrayers() {
        return !publicPrayers.isEmpty();
    }
}
```

---

### P2-2 + P2-3: interpretAsync() Early Return + interpretAndValidate() Null Guard

```java
public CompletableFuture<DivineIntent> interpretAsync(DivineAnswer answer, DivineAnswerContext context, long gameTick) {
    // Early return if no public prayers — don't waste AI call
    if (context == null || !context.hasPublicPrayers()) {
        DivineIntent template = TemplateDivineInterpreter.interpret(answer, context);
        ValidatedIntent validated = DivineIntentValidator.validate(template, context);
        return CompletableFuture.completedFuture(validated.intent());
    }

    // ... rest of AI path
}

public ValidatedIntent interpretAndValidate(DivineAnswer answer, DivineAnswerContext context, long gameTick) {
    if (context == null) {
        DivineIntent fallback = new DivineIntent(
            1, answer.colonyId(), answer.colonyName(),
            IntentType.ORACLE_ONLY, null, 0.2,
            false, "",
            "The Spark cannot find the colony's gathered voice.",
            List.of("NO_CONTEXT_AVAILABLE"),
            IntentSource.TEMPLATE
        );
        return ValidatedIntent.rejected(fallback, List.of("NO_CONTEXT_AVAILABLE"));
    }

    DivineIntent intent = TemplateDivineInterpreter.interpret(answer, context);
    return DivineIntentValidator.validate(intent, context);
}
```

---

### P2-4: PressureModifierManager — Miracles Only Via Validated Intent

Old API (removed):
```java
tryGuardiansVigil(int colonyId, long currentTick, List<EventRecord>, Map<PressureType,Integer>, List<PrayerSeed>)
tryGreenMercy(...)
```

New API:
```java
public boolean tryApplyFromValidatedIntent(ValidatedIntent validated, DivineAnswerContext context, long currentTick) {
    if (validated == null || validated.intent() == null) return false;

    // Only EFFECT_ELIGIBLE gets through
    if (!validated.permitsPressureModifier()) return false;

    // Cooldown, domain, miracle type all checked here
    DivineIntent intent = validated.intent();
    int colonyId = intent.colonyId();

    if (isOnCooldown(colonyId, currentTick)) return false;
    if (!intent.hasDomain()) return false;

    PressureType domain = intent.domain();

    if (intent.intentType() == IntentType.BLESS_COLONY) {
        return tryBlessing(colonyId, domain, context, currentTick);
    }

    if (intent.intentType() == IntentType.CREATE_TRIAL) {
        return tryTrial(colonyId, domain, context, currentTick);
    }

    return false;
}
```

Miracle application from `/godspark answer`:
```java
ValidatedIntent validated = DivineIntentValidator.validate(intent, context);

if (validated.permitsPressureModifier()) {
    boolean miracleApplied = GodsparkMod.PRESSURE_MODIFIER_MANAGER
        .tryApplyFromValidatedIntent(validated, context, gameTick);
}

source.sendSuccess(() -> Component.literal(formatValidatedIntent(validated)), false);
```

---

### P2-5: No TRIUMPH Memory on Miracle Activation

Removed. Memories are only created through the existing `MemoryEngine` event-transition flow, not on miracle activation.

---

### P2-6: /godspark miracles Shows Base → Effective

```
/godspark miracles 1

Miracle modifiers for colony #1:
  Security -20 (expires in 4200 ticks) [GUARDIANS_VIGIL]
    base=82 → effective=62
  Next miracle available in: 18000 ticks
```

---

### Guardian's Vigil Relaxed Preconditions

```java
private boolean canApplyGuardiansVigil(int colonyId, DivineAnswerContext context) {
    // Either: active raid (enough for a real crisis)
    if (context.colonySnapshot() != null && context.colonySnapshot().hasActiveRaid()) {
        return true;
    }

    // OR: high security pressure (colony is struggling)
    boolean hasSecurityPressure = context.pressureSnapshot() != null
        && context.pressureSnapshot().values().getOrDefault(PressureType.SECURITY, 0) >= 70;

    // OR: active security event at MEDIUM+
    boolean hasSecurityEvent = context.activeEvents().stream()
        .anyMatch(r -> r.event().colonyId() == colonyId
            && r.event().pressureType() == PressureType.SECURITY
            && r.event().severity().rank() >= EventSeverity.MEDIUM.rank());

    return hasSecurityPressure || hasSecurityEvent;
}
```

Green Mercy uses same pattern for FOOD domain.

---

### DivineIntent Compact Constructor Sanitization

```java
public DivineIntent {
    if (schemaVersion <= 0) {
        schemaVersion = 1;
    }
    if (intentType == null) {
        intentType = IntentType.ORACLE_ONLY;
    }
    if (colonyName == null) {
        colonyName = "";
    }
    // ... rest unchanged
}
```

---

## Questions for GPT (Round 2)

### Q1: Domain Null Handling in tryApplyFromValidatedIntent

The validator's `recalculatePrayerMatch()` only corrects `matchedPublicPrayer`/`matchedPrayerSourceKey` — it does NOT set a null domain to a real value. If the LLM returns a BLESS_COLONY with `domain=null`, the validator will try to call `canApplyGuardiansVigil(domain=null)`, which will return false for the `hasSecurityPressure` and `hasSecurityEvent` checks (since they both check `domain == SECURITY` in the stream).

Is this acceptable? Or should the validator also try to infer a domain from the matching prayer when the AI returns null?

### Q2: CREATE_TRIAL Miracle Effect

The current `tryTrial()` applies a half-strength pressure reduction (-10 instead of -20) as a "trial" — the colony is being tested, not blessed. But there's no world effect yet to represent the trial itself. Should CREATE_TRIAL actually create a memory event rather than a pressure modifier? Or is the half-strength modifier a reasonable placeholder?

### Q3: ConcurrentHashMap vs HashMap in PressureModifierManager

The `cooldowns` map in `PressureModifierManager` is now `HashMap` (was `ConcurrentHashMap`). All access is on the server thread, so it's fine. But should we add a comment documenting this thread-safety assumption, or is the absence of `synchronized` or `Concurrent` enough to signal "server-thread-only"?

### Q4: ValidationResult.ORACLE_APPROVED vs APPROVED

The old name was `APPROVED` which was ambiguous. The new name `ORACLE_APPROVED` makes it clear this is a narrative-only result. Any concerns with this naming? Is there a better name like `NARRATIVE_ONLY` or `HARMLESS`?

### Q5: Guardian's Vigil / Green Mercy Relaxed Rules

Previously: required raid AND event>=MEDIUM AND prayer>=70 (too strict)
Now: raid OR pressure>=70 OR event>=MEDIUM (with prayer always required through `matchedPublicPrayer`)

Is the OR logic too permissive? Should there be a minimum threshold — e.g., at least 2 of 3 conditions must be met, rather than any 1?

### Q6: Pressure Modifier Amounts

Current:
- BLESS_COLONY: -20 for 6000 ticks
- CREATE_TRIAL: -10 for 3000 ticks

Is -20 on a 0-100 scale too strong? Should it be capped at a percentage of base pressure (e.g., "cannot reduce below 20")?

### Q7: No Memory on Miracle Activation

We removed TRIUMPH memory creation on activation. But this means the miracle has no persistent trace unless the player watches the logs. Should we add a CULTURAL memory later, or is the modifier + log sufficient for Phase 5D.1?

### Q8: TemplateDivineInterpreter Already Has Prayer Gate + Validator Has Prayer Gate

The template interpreter's 3-tier gate already downgrades BLESS_COLONY/CREATE_TRIAL when no matching prayer exists. The validator also checks this. So the AI path goes:
1. Template interpreter: detects no matching prayer → ORACLE_ONLY
2. Validator: sees ORACLE_ONLY → ORACLE_APPROVED immediately

Is the double-gate redundant? Should the validator skip if `intentType == ORACLE_ONLY` before even calling `recalculatePrayerMatch`?