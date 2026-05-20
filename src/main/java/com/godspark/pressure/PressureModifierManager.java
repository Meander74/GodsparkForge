package com.godspark.pressure;

import com.godspark.divine.DivineAnswerContext;
import com.godspark.divine.DivineIntent;
import com.godspark.divine.IntentType;
import com.godspark.divine.ValidatedIntent;
import com.godspark.prayer.PrayerSeed;
import com.godspark.story.EventRecord;
import com.godspark.story.EventSeverity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Server-thread-only manager for temporary pressure modifiers.
 * All methods must be called from the Minecraft server thread.
 * Async AI/interpreter code must not access this manager directly.
 */
public final class PressureModifierManager {

    public static final int MIRACLE_COOLDOWN_TICKS = 24000;
    private static final int MIRACLE_DURATION_TICKS = 6000;
    private static final int MIRACLE_AMOUNT = 20;
    private static final int MIRACLE_PRESSURE_FLOOR = 20;

    private static final Set<PressureType> WHITELISTED_DOMAINS = EnumSet.of(
        PressureType.SECURITY,
        PressureType.FOOD
    );

    private static final Logger LOGGER = LogManager.getLogger("GodsparkMiracle");

    // Server-thread-only.
    private final List<PressureModifier> modifiers = new ArrayList<>();
    // Server-thread-only. Shared per-colony miracle cooldown.
    private final Map<Integer, Long> cooldowns = new HashMap<>();
    private boolean dirty = false;

    private void markDirty() {
        dirty = true;
    }

    public boolean hasDirty() {
        return dirty;
    }

    public boolean consumeDirty() {
        boolean wasDirty = dirty;
        dirty = false;
        return wasDirty;
    }

    public int getModifiedPressure(int colonyId, PressureType pressureType, int baseValue) {
        int totalDelta = 0;
        for (PressureModifier mod : modifiers) {
            if (mod.colonyId() == colonyId && mod.pressureType() == pressureType) {
                totalDelta += mod.amount();
            }
        }
        int modified = baseValue + totalDelta;
        if (totalDelta < 0) {
            modified = Math.max(MIRACLE_PRESSURE_FLOOR, modified);
        }
        return Math.max(0, Math.min(100, modified));
    }

    public List<PressureModifier> getModifiersForColony(int colonyId) {
        return modifiers.stream()
            .filter(m -> m.colonyId() == colonyId)
            .toList();
    }

    public List<PressureModifier> getAllModifiers() {
        return List.copyOf(modifiers);
    }

    public void tick(long currentTick) {
        Iterator<PressureModifier> it = modifiers.iterator();
        while (it.hasNext()) {
            PressureModifier mod = it.next();
            if (mod.isExpired(currentTick)) {
                LOGGER.info("[Godspark Miracle] Modifier expired: colony #{}, {} {}",
                    mod.colonyId(), mod.pressureType().getDisplayName(), mod.amount());
                it.remove();
                markDirty();
            }
        }
    }

    public boolean tryApplyFromValidatedIntent(ValidatedIntent validated, DivineAnswerContext context, long currentTick) {
        if (validated == null || validated.intent() == null) {
            return false;
        }

        if (!validated.permitsPressureModifier()) {
            return false;
        }

        DivineIntent intent = validated.intent();
        int colonyId = intent.colonyId();

        if (isOnCooldown(colonyId, currentTick)) {
            LOGGER.info("[Godspark Miracle] Colony #{} on miracle cooldown ({} ticks remaining)",
                colonyId, getCooldownRemaining(colonyId, currentTick));
            return false;
        }

        if (!intent.hasDomain()) {
            return false;
        }

        PressureType domain = intent.domain();

        if (intent.intentType() == IntentType.BLESS_COLONY) {
            return tryBlessing(colonyId, domain, context, currentTick);
        }

        return false;
    }

    private boolean tryBlessing(int colonyId, PressureType domain, DivineAnswerContext context, long currentTick) {
        String miracleName;
        boolean canApply;

        if (domain == PressureType.SECURITY) {
            canApply = canApplyGuardiansVigil(colonyId, context);
            miracleName = "GUARDIANS_VIGIL";
        } else if (domain == PressureType.FOOD) {
            canApply = canApplyGreenMercy(colonyId, context);
            miracleName = "GREEN_MERCY";
        } else {
            LOGGER.info("[Godspark Miracle] DOMAIN_NOT_WHITELISTED for colony #{}: {}",
                colonyId, domain);
            return false;
        }

        if (!canApply) {
            LOGGER.info("[Godspark Miracle] {} preconditions not met for colony #{}",
                miracleName, colonyId);
            return false;
        }

        PressureModifier modifier = new PressureModifier(
            colonyId, domain, -MIRACLE_AMOUNT,
            currentTick, currentTick + MIRACLE_DURATION_TICKS,
            miracleName
        );
        modifiers.add(modifier);
        cooldowns.put(colonyId, currentTick);
        markDirty();

        LOGGER.info("[Godspark Miracle] {} activated for colony #{}: {} -{} for {} ticks",
            miracleName, colonyId, domain.getDisplayName(), MIRACLE_AMOUNT, MIRACLE_DURATION_TICKS);

        return true;
    }

    private boolean canApplyGuardiansVigil(int colonyId, DivineAnswerContext context) {
        if (context.colonySnapshot() != null && context.colonySnapshot().hasActiveRaid()) {
            return true;
        }

        boolean hasSecurityPressure = context.pressureSnapshot() != null
            && context.pressureSnapshot().values().getOrDefault(PressureType.SECURITY, 0) >= 70;

        boolean hasSecurityEvent = context.activeEvents().stream()
            .anyMatch(r -> r.event().colonyId() == colonyId
                && r.event().pressureType() == PressureType.SECURITY
                && r.event().severity().rank() >= EventSeverity.MEDIUM.rank());

        return hasSecurityPressure || hasSecurityEvent;
    }

    private boolean canApplyGreenMercy(int colonyId, DivineAnswerContext context) {
        boolean hasFoodPressure = context.pressureSnapshot() != null
            && context.pressureSnapshot().values().getOrDefault(PressureType.FOOD, 0) >= 70;

        boolean hasFoodEvent = context.activeEvents().stream()
            .anyMatch(r -> r.event().colonyId() == colonyId
                && r.event().pressureType() == PressureType.FOOD
                && r.event().severity().rank() >= EventSeverity.MEDIUM.rank());

        return hasFoodPressure || hasFoodEvent;
    }

    public boolean isOnCooldown(int colonyId, long currentTick) {
        Long lastTick = cooldowns.get(colonyId);
        if (lastTick == null) return false;
        return (currentTick - lastTick) < MIRACLE_COOLDOWN_TICKS;
    }

    public long getCooldownRemaining(int colonyId, long currentTick) {
        Long lastTick = cooldowns.get(colonyId);
        if (lastTick == null) return 0;
        long remaining = MIRACLE_COOLDOWN_TICKS - (currentTick - lastTick);
        return Math.max(0, remaining);
    }

    public void clear() {
        modifiers.clear();
        cooldowns.clear();
        dirty = false;
    }

    public Map<Integer, Long> getCooldownEntries() {
        return Map.copyOf(cooldowns);
    }

    public void restoreCooldownRemaining(Map<Integer, Long> remainingMap, long currentTick) {
        cooldowns.clear();
        for (Map.Entry<Integer, Long> entry : remainingMap.entrySet()) {
            long remaining = entry.getValue();
            if (remaining <= 0) continue;
            long lastTick = currentTick - (MIRACLE_COOLDOWN_TICKS - remaining);
            cooldowns.put(entry.getKey(), lastTick);
        }
        LOGGER.info("[Godspark Miracle] Restored {} cooldowns", cooldowns.size());
    }

    public void restoreModifiers(List<PressureModifier> savedModifiers, long currentTick) {
        int restored = 0;
        for (PressureModifier mod : savedModifiers) {
            if (mod.expiresAtTick() - currentTick <= 0 || !isMiracleWhitelisted(mod.pressureType()))
                continue;
            long remaining = mod.expiresAtTick() - currentTick;
            if (remaining <= 0) continue;
            PressureModifier restoredMod = new PressureModifier(
                mod.colonyId(), mod.pressureType(), mod.amount(),
                mod.createdAtTick(), currentTick + remaining, mod.source()
            );
            modifiers.add(restoredMod);
            restored++;
        }
        LOGGER.info("[Godspark Miracle] Restored {} active modifiers", restored);
    }

    public void clearCooldowns() {
        cooldowns.clear();
    }

    public static boolean isMiracleWhitelisted(PressureType domain) {
        return domain != null && WHITELISTED_DOMAINS.contains(domain);
    }

    public static Set<PressureType> getWhitelistedDomains() {
        return EnumSet.copyOf(WHITELISTED_DOMAINS);
    }
}