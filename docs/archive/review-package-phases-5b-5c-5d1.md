# Godspark Code Review Package — Phases 5B, 5C, 5D.1

## Project Overview

Godspark is a Minecraft 1.20.1 / Forge 47.x companion mod for MineColonies. It observes colony state, computes societal pressure scores (FOOD, SECURITY, HOUSING, COMFORT, INDUSTRY), generates story events, maintains persistent colony memories, generates prayer seeds, reflects via optional LLM, and now interprets divine answers from the player and applies minor miracles as pressure modifiers.

**Architecture flow:**
```
MineColonies → ColonyObserver → PressureEngine → EventGenerator
                                                    ↓
                                          EventStateManager → MemoryEngine → MemoryBank
                                                    ↓                        ↓
                                          MemoryInfluence → PrayerSeedGenerator → PrayerSeedBank
                                                    ↓
                                          DivineAnswerInterpreter → DivineIntentValidator
                                                    ↓
                                          PressureModifierManager (miracles: Guardian's Vigil, Green Mercy)
```

---

## Phase 5B — Divine Answer Interpreter

### Goal
Player answers colony prayers via `/godspark answer <colonyId> <message>`. The system interprets the answer into a `DivineIntent` via template keyword matching (always available) or optional LLM. No world mutation.

### New Files (9)

#### IntentType.java
```java
package com.godspark.divine;

public enum IntentType {
    ORACLE_ONLY("Oracle Only"),
    ANSWER_QUESTION("Answer Question"),
    CREATE_TRIAL("Create Trial"),
    BLESS_COLONY("Bless Colony");

    private final String displayName;

    IntentType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
```

#### IntentSource.java
```java
package com.godspark.divine;

public enum IntentSource {
    TEMPLATE, AI, AI_FALLBACK, AI_COOLDOWN, PARSER_FALLBACK
}
```

#### DivineAnswer.java
```java
package com.godspark.divine;

import java.util.UUID;

public record DivineAnswer(
    int colonyId,
    String colonyName,
    UUID playerId,
    String playerName,
    String rawText,
    long submittedAtTick
) {}
```

#### DivineAnswerContext.java
```java
package com.godspark.divine;

import com.godspark.memory.ColonyMemory;
import com.godspark.observer.ColonySnapshot;
import com.godspark.prayer.PrayerSeed;
import com.godspark.pressure.PressureSnapshot;
import com.godspark.story.EventRecord;

import java.util.List;

public record DivineAnswerContext(
    ColonySnapshot colonySnapshot,
    PressureSnapshot pressureSnapshot,
    List<PrayerSeed> publicPrayers,
    List<EventRecord> activeEvents,
    List<ColonyMemory> memories
) {
    public boolean hasPublicPrayers() {
        return publicPrayers != null && !publicPrayers.isEmpty();
    }
}
```

#### DivineIntent.java
```java
package com.godspark.divine;

import com.godspark.pressure.PressureType;

import java.util.List;
import java.util.Locale;

public record DivineIntent(
    int schemaVersion,
    int colonyId,
    String colonyName,
    IntentType intentType,
    PressureType domain,
    double confidence,
    boolean matchedPublicPrayer,
    String matchedPrayerSourceKey,
    String oracleText,
    List<String> reasonCodes,
    IntentSource source
) {
    private static final int MAX_ORACLE_TEXT_LENGTH = 300;
    private static final int MAX_REASON_CODES = 8;

    public DivineIntent {
        if (intentType == null) {
            intentType = IntentType.ORACLE_ONLY;
        }
        confidence = Math.max(0.0, Math.min(1.0, confidence));
        if (matchedPrayerSourceKey == null) {
            matchedPrayerSourceKey = "";
        }
        oracleText = sanitizeText(oracleText, MAX_ORACLE_TEXT_LENGTH);
        reasonCodes = reasonCodes == null
            ? List.of()
            : reasonCodes.stream()
                .filter(code -> code != null && !code.isBlank())
                .map(code -> sanitizeReasonCode(code, 64))
                .distinct()
                .limit(MAX_REASON_CODES)
                .toList();
        source = source == null ? IntentSource.TEMPLATE : source;
    }

    public boolean hasDomain() {
        return domain != null;
    }

    public String domainDisplayName() {
        return domain == null ? "Unknown" : domain.getDisplayName();
    }

    public boolean eligibleForFutureAnswer() {
        return matchedPublicPrayer
            && intentType != IntentType.ORACLE_ONLY
            && intentType != IntentType.ANSWER_QUESTION;
    }

    private static String sanitizeText(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String cleaned = value
            .replace('\n', ' ').replace('\r', ' ')
            .replaceAll("\\p{Cntrl}", "").trim();
        if (cleaned.length() > maxLength) {
            return cleaned.substring(0, maxLength);
        }
        return cleaned;
    }

    private static String sanitizeReasonCode(String value, int maxLength) {
        String cleaned = value
            .toUpperCase(Locale.ROOT)
            .replaceAll("[^A-Z0-9_]", "_");
        if (cleaned.length() > maxLength) {
            return cleaned.substring(0, maxLength);
        }
        return cleaned;
    }
}
```

#### DivineIntentParser.java
```java
package com.godspark.divine;

import com.godspark.pressure.PressureType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DivineIntentParser {

    private DivineIntentParser() {}

    public static DivineIntent parse(String json, int defaultColonyId, String defaultColonyName) {
        try {
            var element = com.google.gson.JsonParser.parseString(json);
            if (!element.isJsonObject()) {
                return fallback(json, defaultColonyId, defaultColonyName, "NOT_JSON_OBJECT");
            }

            var obj = element.getAsJsonObject();

            IntentType intentType = parseIntentType(getStr(obj, "intentType"));
            PressureType domain = parseDomain(getStr(obj, "domain"));
            double confidence = parseConfidence(obj, "confidence");
            boolean matchedPublicPrayer = parseBool(obj, "matchedPublicPrayer", false);
            String matchedPrayerSourceKey = getStr(obj, "matchedPrayerSourceKey");
            String oracleText = getStr(obj, "oracleText");
            List<String> reasonCodes = parseReasonCodes(obj);

            return new DivineIntent(
                1, defaultColonyId, defaultColonyName,
                intentType, domain, confidence,
                matchedPublicPrayer, matchedPrayerSourceKey,
                oracleText, reasonCodes, IntentSource.AI
            );

        } catch (Exception e) {
            return fallback(json, defaultColonyId, defaultColonyName, "PARSE_ERROR: " + e.getMessage());
        }
    }

    private static IntentType parseIntentType(String raw) {
        if (raw == null || raw.isBlank()) return IntentType.ORACLE_ONLY;
        try { return IntentType.valueOf(raw.toUpperCase(Locale.ROOT).trim()); }
        catch (IllegalArgumentException e) { return IntentType.ORACLE_ONLY; }
    }

    private static PressureType parseDomain(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try { return PressureType.valueOf(raw.toUpperCase(Locale.ROOT).trim()); }
        catch (IllegalArgumentException e) { return null; }
    }

    private static double parseConfidence(com.google.gson.JsonObject obj, String key) {
        if (!obj.has(key)) return 0.5;
        try { return Math.max(0.0, Math.min(1.0, obj.get(key).getAsDouble())); }
        catch (Exception e) { return 0.5; }
    }

    private static boolean parseBool(com.google.gson.JsonObject obj, String key, boolean defaultVal) {
        if (!obj.has(key)) return defaultVal;
        try { return obj.get(key).getAsBoolean(); }
        catch (Exception e) { return defaultVal; }
    }

    private static String getStr(com.google.gson.JsonObject obj, String key) {
        if (!obj.has(key)) return "";
        try { return obj.get(key).getAsString(); }
        catch (Exception e) { return ""; }
    }

    private static List<String> parseReasonCodes(com.google.gson.JsonObject obj) {
        if (!obj.has("reasonCodes") || !obj.get("reasonCodes").isJsonArray()) {
            return List.of("AI_PARSE");
        }
        List<String> codes = new ArrayList<>();
        for (var el : obj.getAsJsonArray("reasonCodes")) {
            try {
                String code = el.getAsString();
                if (code != null && !code.isBlank()) codes.add(code);
            } catch (Exception ignored) {}
        }
        if (codes.isEmpty()) codes.add("AI_PARSE");
        return codes;
    }

    private static DivineIntent fallback(String raw, int colonyId, String colonyName, String reason) {
        return new DivineIntent(
            1, colonyId, colonyName,
            IntentType.ORACLE_ONLY, null, 0.3,
            false, "",
            "The Spark cannot parse the divine signal.",
            List.of(reason),
            IntentSource.PARSER_FALLBACK
        );
    }
}
```

#### TemplateDivineInterpreter.java
(326 lines — see full file in repository: `src/main/java/com/godspark/divine/TemplateDivineInterpreter.java`)

Key design:
- Keyword-based domain detection (food→FOOD, guard→SECURITY, etc)
- Keyword-based intent detection (what/why/how→ANSWER_QUESTION, bless/help→BLESS_COLONY, vow/trial→CREATE_TRIAL)
- 3-tier sacred prayer gate:
  1. No public prayers → ORACLE_ONLY only + guidance text
  2. Public prayers but no domain match → ORACLE_ONLY only
  3. Public prayers with domain match → all intent types available
- Always produces a result; never null

#### DivineAnswerPromptBuilder.java
(110 lines — builds LLM prompt from DivineAnswer + DivineAnswerContext)

Key design:
- Strict system prompt: "You are the Spark… respond only with valid JSON"
- JSON schema specified inline
- Colony context included (pressures, events, prayers, memories)
- Answer text is the user message

#### DivineAnswerInterpreter.java
```java
package com.godspark.divine;

import com.godspark.GodsparkMod;
import com.godspark.ai.AiClient;
import com.godspark.ai.AiConfig;
import com.godspark.memory.ColonyMemory;
import com.godspark.observer.ColonySnapshot;
import com.godspark.observer.ObservedColony;
import com.godspark.prayer.PrayerSeed;
import com.godspark.pressure.PressureSnapshot;
import com.godspark.story.EventRecord;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class DivineAnswerInterpreter {

    private static final int FAILURE_BACKOFF_TICKS = 1200;
    private static final int SUCCESS_COOLDOWN_TICKS = 6000;

    private final AiConfig config;
    private final AiClient client;
    private final Map<Integer, Long> successCooldowns = new ConcurrentHashMap<>();
    private final Map<Integer, Long> failureBackoffs = new ConcurrentHashMap<>();

    public DivineAnswerInterpreter(AiConfig config) {
        this.config = config;
        this.client = new AiClient(config);
    }

    public DivineIntent interpretTemplate(DivineAnswer answer, DivineAnswerContext context, long gameTick) {
        return TemplateDivineInterpreter.interpret(answer, context);
    }

    public ValidatedIntent interpretAndValidate(DivineAnswer answer, DivineAnswerContext context, long gameTick) {
        DivineIntent intent = TemplateDivineInterpreter.interpret(answer, context);
        return DivineIntentValidator.validate(intent, context);
    }

    public CompletableFuture<DivineIntent> interpretAsync(DivineAnswer answer, DivineAnswerContext context, long gameTick) {
        if (!config.enabled()) {
            DivineIntent template = TemplateDivineInterpreter.interpret(answer, context);
            ValidatedIntent validated = DivineIntentValidator.validate(template, context);
            return CompletableFuture.completedFuture(validated.intent());
        }

        if (isOnCooldown(answer.colonyId(), gameTick, successCooldowns, SUCCESS_COOLDOWN_TICKS)) {
            DivineIntent fallback = TemplateDivineInterpreter.interpret(answer, context);
            ValidatedIntent validated = DivineIntentValidator.validate(fallback, context);
            return CompletableFuture.completedFuture(
                withSource(validated.intent(), IntentSource.AI_COOLDOWN)
            );
        }

        if (isOnCooldown(answer.colonyId(), gameTick, failureBackoffs, FAILURE_BACKOFF_TICKS)) {
            DivineIntent fallback = TemplateDivineInterpreter.interpret(answer, context);
            ValidatedIntent validated = DivineIntentValidator.validate(fallback, context);
            return CompletableFuture.completedFuture(
                withSource(validated.intent(), IntentSource.AI_FALLBACK)
            );
        }

        var messages = DivineAnswerPromptBuilder.buildMessages(answer, context);

        return client.complete(messages).handle((rawJson, error) -> {
            if (error != null) {
                GodsparkMod.LOGGER.warn("[Godspark Divine] Async interpretation failed for colony #{}: {}",
                    answer.colonyId(), error.getMessage());
                failureBackoffs.put(answer.colonyId(), gameTick);
                DivineIntent fallback = TemplateDivineInterpreter.interpret(answer, context);
                ValidatedIntent validated = DivineIntentValidator.validate(fallback, context);
                return withSource(validated.intent(), IntentSource.AI_FALLBACK);
            }

            if (rawJson == null || rawJson.isBlank()) {
                GodsparkMod.LOGGER.warn("[Godspark Divine] Empty async response for colony #{}", answer.colonyId());
                failureBackoffs.put(answer.colonyId(), gameTick);
                DivineIntent fallback = TemplateDivineInterpreter.interpret(answer, context);
                ValidatedIntent validated = DivineIntentValidator.validate(fallback, context);
                return withSource(validated.intent(), IntentSource.AI_FALLBACK);
            }

            String json = extractJson(rawJson);

            try {
                DivineIntent parsed = DivineIntentParser.parse(json, answer.colonyId(), answer.colonyName());
                ValidatedIntent validated = DivineIntentValidator.validate(parsed, context);
                DivineIntent result = validated.intent();
                GodsparkMod.LOGGER.info("[Godspark Divine] Intent for colony #{} (type={}, domain={}, confidence={}, validation={})",
                    answer.colonyId(), result.intentType(), result.domainDisplayName(),
                    String.format("%.0f%%", result.confidence() * 100), validated.result().getDisplayName());
                successCooldowns.put(answer.colonyId(), gameTick);
                return result;
            } catch (Exception e) {
                GodsparkMod.LOGGER.warn("[Godspark Divine] Failed to parse AI JSON for colony #{}: {}",
                    answer.colonyId(), e.getMessage());
                GodsparkMod.LOGGER.debug("[Godspark Divine] Raw response: {}", json);
                failureBackoffs.put(answer.colonyId(), gameTick);
                DivineIntent fallback = TemplateDivineInterpreter.interpret(answer, context);
                ValidatedIntent validated = DivineIntentValidator.validate(fallback, context);
                return withSource(validated.intent(), IntentSource.AI_FALLBACK);
            }
        });
    }

    // ... extractJson, isOnCooldown, withSource, buildContext methods ...
}
```

### Modified Files (2)

#### GodsparkMod.java — added:
```java
public static final PressureModifierManager PRESSURE_MODIFIER_MANAGER = new PressureModifierManager();
public static final DivineAnswerInterpreter DIVINE_ANSWER_INTERPRETER = new DivineAnswerInterpreter(AiConfig.DEFAULT);

// In constructor:
PRESSURE_ENGINE.setModifierManager(PRESSURE_MODIFIER_MANAGER);
```

#### GodsparkCommands.java — added `/godspark answer` and `/godspark miracles` commands
- `/godspark answer <colonyId> <message>` — permission(2), 500-char input cap, async via `server.execute()`
- `/godspark miracles [colonyId]` — shows active pressure modifiers, expiry, cooldowns

---

## Phase 5C — DivineIntent Validator

### New Files (3)

#### ValidationResult.java
```java
package com.godspark.divine;

public enum ValidationResult {
    APPROVED("Approved"),
    REJECTED("Rejected"),
    QUEUED("Queued"),
    DOWNGRADED("Downgraded");

    private final String displayName;

    ValidationResult(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
```

#### DivineIntentValidator.java
```java
package com.godspark.divine;

import com.godspark.memory.ColonyMemory;
import com.godspark.memory.MemoryType;
import com.godspark.prayer.PrayerSeed;
import com.godspark.story.EventRecord;
import com.godspark.story.EventSeverity;
import com.godspark.story.StoryEventType;

import java.util.ArrayList;
import java.util.List;

public final class DivineIntentValidator {

    private DivineIntentValidator() {}

    public static ValidatedIntent validate(DivineIntent intent, DivineAnswerContext context) {
        if (intent == null) {
            return ValidatedIntent.rejected(null, List.of("NULL_INTENT"));
        }

        List<String> validationNotes = new ArrayList<>();

        if (intent.intentType() == IntentType.ORACLE_ONLY || intent.intentType() == IntentType.ANSWER_QUESTION) {
            validationNotes.add(intent.intentType().name() + "_AUTO_APPROVED");
            return ValidatedIntent.approved(intent, validationNotes);
        }

        if (context == null) {
            return downgrade(intent, "NO_CONTEXT_AVAILABLE", validationNotes);
        }

        ValidationResult result = switch (intent.intentType()) {
            case BLESS_COLONY -> validateBlessColony(intent, context, validationNotes);
            case CREATE_TRIAL -> validateCreateTrial(intent, context, validationNotes);
            default -> ValidationResult.APPROVED;
        };

        return new ValidatedIntent(intent, result, validationNotes);
    }

    private static ValidationResult validateBlessColony(DivineIntent intent, DivineAnswerContext context,
                                                         List<String> notes) {
        boolean hasActivePrayers = context.hasPublicPrayers();
        if (!hasActivePrayers) {
            notes.add("NO_ACTIVE_PRAYERS");
            return ValidationResult.REJECTED;
        }

        PrayerSeed matchingPrayer = findMatchingPrayer(context, intent);
        if (matchingPrayer == null) {
            notes.add("NO_PRAYER_MATCH_DOMAIN");
            notes.add("BLESS_REQUIRES_MATCHING_PRAYER");
            return ValidationResult.DOWNGRADED;
        }

        notes.add("BLESS_COLONY_VALIDATED");
        notes.add("PRAYER_INTENSITY_" + matchingPrayer.intensity());
        return ValidationResult.APPROVED;
    }

    private static ValidationResult validateCreateTrial(DivineIntent intent, DivineAnswerContext context,
                                                        List<String> notes) {
        boolean hasActiveEvent = !context.activeEvents().isEmpty();
        boolean hasMatchingPrayer = findMatchingPrayer(context, intent) != null;
        boolean hasTriumphMemory = hasTriumphMemory(context);

        if (!hasActiveEvent && !hasMatchingPrayer && !hasTriumphMemory) {
            notes.add("NO_ACTIVE_CRISIS");
            notes.add("NO_MATCHING_PRAYER");
            notes.add("NO_TRIUMPH_MEMORY");
            return ValidationResult.DOWNGRADED;
        }

        if (hasActiveEvent) {
            boolean hasMediumEvent = context.activeEvents().stream()
                .anyMatch(r -> r.event().severity().rank() >= EventSeverity.MEDIUM.rank());
            if (hasMediumEvent) {
                notes.add("MEDIUM_PLUS_EVENT_PRESENT");
            }
            notes.add("ACTIVE_EVENT_PRESENT");
        }

        if (hasMatchingPrayer) {
            notes.add("MATCHING_PRAYER_PRESENT");
        }

        if (hasTriumphMemory) {
            notes.add("TRIUMPH_MEMORY_PRESENT");
        }

        notes.add("CREATE_TRIAL_VALIDATED");
        return ValidationResult.APPROVED;
    }

    private static boolean hasTriumphMemory(DivineAnswerContext context) {
        return context.memories().stream()
            .anyMatch(m -> m.memoryType() == MemoryType.TRIUMPH);
    }

    private static PrayerSeed findMatchingPrayer(DivineAnswerContext context, DivineIntent intent) {
        if (!context.hasPublicPrayers()) return null;
        if (!intent.hasDomain()) return null;

        for (PrayerSeed seed : context.publicPrayers()) {
            if (seed.pressureType() == intent.domain()) {
                return seed;
            }
        }
        return null;
    }

    private static ValidatedIntent downgrade(DivineIntent intent, String reason, List<String> notes) {
        notes.add(reason);
        notes.add("DOWNGRADED_TO_ORACLE_ONLY");

        DivineIntent downgraded = new DivineIntent(
            intent.schemaVersion(),
            intent.colonyId(),
            intent.colonyName(),
            IntentType.ORACLE_ONLY,
            intent.domain(),
            intent.confidence() * 0.5,
            false,
            "",
            "The Spark hears, but the colony's need is not yet gathered. Only an oracle may speak.",
            intent.reasonCodes(),
            intent.source()
        );

        return new ValidatedIntent(downgraded, ValidationResult.DOWNGRADED, notes);
    }
}
```

#### ValidatedIntent.java
```java
package com.godspark.divine;

import java.util.List;

public record ValidatedIntent(
    DivineIntent intent,
    ValidationResult result,
    List<String> validationNotes
) {
    public static ValidatedIntent approved(DivineIntent intent, List<String> notes) {
        return new ValidatedIntent(intent, ValidationResult.APPROVED, notes);
    }

    public static ValidatedIntent rejected(DivineIntent intent, List<String> notes) {
        return new ValidatedIntent(intent, ValidationResult.REJECTED, notes);
    }

    public boolean isApproved() {
        return result == ValidationResult.APPROVED;
    }

    public boolean isDowngraded() {
        return result == ValidationResult.DOWNGRADED;
    }

    public boolean isRejected() {
        return result == ValidationResult.REJECTED;
    }

    public String validationSummary() {
        return String.format("[%s] %s → %s (%s)",
            result.getDisplayName(),
            intent != null ? intent.intentType().getDisplayName() : "NULL",
            intent != null ? intent.domainDisplayName() : "N/A",
            String.join(", ", validationNotes()));
    }
}
```

---

## Phase 5D.1 — Internal Minor Miracles (Pressure Modifiers)

### New Files (2)

#### PressureModifier.java
```java
package com.godspark.pressure;

public record PressureModifier(
    int colonyId,
    PressureType pressureType,
    int amount,
    long createdAtTick,
    long expiresAtTick,
    String source
) {
    public boolean isExpired(long currentTick) {
        return currentTick >= expiresAtTick;
    }

    public long remainingTicks(long currentTick) {
        return Math.max(0, expiresAtTick - currentTick);
    }
}
```

#### PressureModifierManager.java
```java
package com.godspark.pressure;

import com.godspark.GodsparkMod;
import com.godspark.memory.ColonyMemory;
import com.godspark.memory.MemoryType;
import com.godspark.prayer.PrayerSeed;
import com.godspark.story.EventRecord;
import com.godspark.story.EventSeverity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PressureModifierManager {

    private static final int MIRACLE_COOLDOWN_TICKS = 24000;
    private static final int MIRACLE_DURATION_TICKS = 6000;
    private static final int MIRACLE_AMOUNT = 20;

    private final List<PressureModifier> modifiers = new ArrayList<>();
    private final Map<Integer, Long> cooldowns = new ConcurrentHashMap<>();

    public int getModifiedPressure(int colonyId, PressureType pressureType, int baseValue) {
        int totalDelta = 0;
        for (PressureModifier mod : modifiers) {
            if (mod.colonyId() == colonyId && mod.pressureType() == pressureType) {
                totalDelta += mod.amount();
            }
        }
        return Math.max(0, Math.min(100, baseValue + totalDelta));
    }

    public List<PressureModifier> getModifiersForColony(int colonyId) { ... }
    public List<PressureModifier> getAllModifiers() { ... }

    public void tick(long currentTick) {
        Iterator<PressureModifier> it = modifiers.iterator();
        while (it.hasNext()) {
            PressureModifier mod = it.next();
            if (mod.isExpired(currentTick)) {
                GodsparkMod.LOGGER.info("[Godspark Miracle] Modifier expired: colony #{}, {} {}",
                    mod.colonyId(), mod.pressureType().getDisplayName(), mod.amount());
                it.remove();
            }
        }
    }

    // Guardian's Vigil: SECURITY -20 for 6000 ticks
    // Preconditions: active raid + SECURITY event >= MEDIUM + SECURITY prayer >= 70
    // Cooldown: 24000 ticks per colony
    public boolean tryGuardiansVigil(int colonyId, long currentTick,
                                      List<EventRecord> activeEvents,
                                      Map<PressureType, Integer> pressures,
                                      List<PrayerSeed> prayers) { ... }

    // Green Mercy: FOOD -20 for 6000 ticks
    // Preconditions: FOOD event >= MEDIUM + FOOD pressure >= 70 + FOOD prayer >= 70
    // Cooldown: 24000 ticks per colony
    public boolean tryGreenMercy(int colonyId, long currentTick,
                                  List<EventRecord> activeEvents,
                                  Map<PressureType, Integer> pressures,
                                  List<PrayerSeed> prayers) { ... }

    public boolean isOnCooldown(int colonyId, long currentTick) { ... }
    public long getCooldownRemaining(int colonyId, long currentTick) { ... }
    public void clear() { ... }
    public void clearCooldowns() { ... }
}
```

### Modified Files (3)

#### PressureEngine.java — added:
- `PressureModifierManager modifierManager` field
- `setModifierManager()` setter
- In `compute()`: after calculating base pressures, applies modifiers via `getModifiedPressure()`

#### GodsparkMod.java — added:
- `public static final PressureModifierManager PRESSURE_MODIFIER_MANAGER = new PressureModifierManager();`
- Constructor: `PRESSURE_ENGINE.setModifierManager(PRESSURE_MODIFIER_MANAGER);`

#### GodsparkServerEvents.java — added:
- `PRESSURE_MODIFIER_MANAGER.tick(tickCounter)` every server tick
- Miracle check loop after prayer seed generation (per colony)
- Creates TRIUMPH memories on miracle activation
- `PRESSURE_MODIFIER_MANAGER.clear()` on server stop

---

## Review Questions for GPT

1. **Thread Safety**: `PressureModifierManager.modifiers` is a plain `ArrayList` accessed from the server thread (both `tick()` and `tryGuardiansVigil()` etc). Since all access is on the server tick thread, is this safe, or should it be a `CopyOnWriteArrayList` / synchronized collection? The `cooldowns` map uses `ConcurrentHashMap` — is that necessary given everything runs on the server thread?

2. **DivineIntentValidator downgrade consistency**: The validator can downgrade BLESS_COLONY and CREATE_TRIAL to ORACLE_ONLY, but the `TemplateDivineInterpreter` already enforces the 3-tier prayer gate (downgrades BLESS/TRIAL to ORACLE_ONLY when no matching prayer). This means validation may redundantly downgrade an already-downgraded intent. Should the validator skip validation if `intent.intentType()` is already ORACLE_ONLY?

3. **AI result validation**: When the LLM returns a BLESS_COLONY intent but the validator downgrades it, the final intent has `source = AI` but `intentType = ORACLE_ONLY`. Is this misleading? Should the source be updated to something like `AI_DOWNGRADED` or should validation notes be stored on the intent itself?

4. **PressureModifier expiry timing**: Modifiers are expired in `tick()` which runs every server tick, but pressure computation only runs every `PRESSURE_INTERVAL_TICKS`. A modifier could be created, expired, and yet never actually affect a pressure computation if the creation and expiry both happen within one pressure interval. Is this a real concern given the 6000-tick duration vs the pressure interval?

5. **Guardian's Vigil precondition**: The tryGuardiansVigil() checks `hasActiveRaid` from ColonySnapshot AND `hasSecurityEventMedium` from EventRecords. But `hasSecurityEventMedium` is set to `false` initially and only set to true inside the loop — if the loop finds no matching events, `hasActiveRaid` is never checked either (short-circuit). Actually looking more carefully, it returns false if EITHER condition is false — but the spec says "active raid AND SECURITY event MEDIUM+". Should both be required or just one? Currently both are required.

6. **Miracle cooldown is per-colony not per-miracle-type**: After Guardian's Vigil fires, Green Mercy can't fire for the same colony for 24000 ticks because cooldown is `Map<Integer colonyId, Long>`, not per-type. Is this intentional? The spec lists both miracles with their own 24000-tick cooldown, which could mean they share it (one miracle per colony per day).

7. **`DivineAnswerInterpreter.buildContext()` returns null** for unknown colonies — the command handles this, but `interpretAndValidate()` doesn't guard for null context. Should it?

8. **PressureModifier.applyModifiers()**: This method returns a `PressureModifier` as a summary, which seems odd. The return value isn't used anywhere. Is this dead code, or was it intended for logging? Should it be removed?