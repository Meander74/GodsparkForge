package com.godspark.pressure;

import com.godspark.divine.DivineAnswerContext;
import com.godspark.divine.DivineIntent;
import com.godspark.divine.IntentSource;
import com.godspark.divine.IntentType;
import com.godspark.divine.ValidatedIntent;
import com.godspark.divine.ValidationResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PressureModifierWhitelistTest {

    private static final long BASE_TICK = 100L;
    private static final int COLONY_ID = 1;

    private static DivineIntent createBlessIntent(PressureType domain) {
        return new DivineIntent(
            1, COLONY_ID, "Test Colony",
            IntentType.BLESS_COLONY, domain, 1.0, true,
            "test", "test", List.of(), IntentSource.TEMPLATE
        );
    }

    private static DivineIntent createBlessIntent() {
        return createBlessIntent(null);
    }

    private static ValidatedIntent effectEligible(DivineIntent intent) {
        return new ValidatedIntent(intent, ValidationResult.EFFECT_ELIGIBLE, List.of("TEST"));
    }

    private static DivineAnswerContext emptyContext() {
        return new DivineAnswerContext(null, null, List.of(), List.of(), List.of());
    }

    @Test
    void isMiracleWhitelistedDirectly() {
        assertTrue(PressureModifierManager.isMiracleWhitelisted(PressureType.SECURITY));
        assertTrue(PressureModifierManager.isMiracleWhitelisted(PressureType.FOOD));
        assertFalse(PressureModifierManager.isMiracleWhitelisted(PressureType.HOUSING));
        assertFalse(PressureModifierManager.isMiracleWhitelisted(PressureType.COMFORT));
        assertFalse(PressureModifierManager.isMiracleWhitelisted(PressureType.INDUSTRY));
        assertFalse(PressureModifierManager.isMiracleWhitelisted(null));
    }

    @Test
    void getWhitelistedDomainsReturnsOnlySecFood() {
        assertEquals(2, PressureModifierManager.getWhitelistedDomains().size());
        assertTrue(PressureModifierManager.getWhitelistedDomains().contains(PressureType.SECURITY));
        assertTrue(PressureModifierManager.getWhitelistedDomains().contains(PressureType.FOOD));
    }

    @Test
    void nonWhitelistedDomainsRejectedWithEffectEligible() {
        PressureModifierManager manager = new PressureModifierManager();

        for (PressureType type : List.of(PressureType.HOUSING, PressureType.COMFORT, PressureType.INDUSTRY)) {
            DivineIntent intent = createBlessIntent(type);
            ValidatedIntent validated = effectEligible(intent);
            assertFalse(manager.tryApplyFromValidatedIntent(validated, emptyContext(), BASE_TICK),
                "Domain " + type + " should be rejected by whitelist");
        }

        assertEquals(0, manager.getAllModifiers().size(),
            "No modifiers should be registered for rejected domains");
    }

    @Test
    void nullDomainRejected() {
        PressureModifierManager manager = new PressureModifierManager();
        DivineIntent intent = createBlessIntent();
        ValidatedIntent validated = effectEligible(intent);
        assertFalse(manager.tryApplyFromValidatedIntent(validated, emptyContext(), BASE_TICK));
        assertEquals(0, manager.getAllModifiers().size());
    }

    @Test
    void downgradedIntentsDoNotReachWhitelist() {
        PressureModifierManager manager = new PressureModifierManager();
        for (PressureType type : PressureType.values()) {
            DivineIntent intent = createBlessIntent(type);
            ValidatedIntent validated = new ValidatedIntent(intent, ValidationResult.DOWNGRADED,
                List.of("DOMAIN_NOT_WHITELISTED"));
            assertFalse(manager.tryApplyFromValidatedIntent(validated, emptyContext(), BASE_TICK));
        }
        assertEquals(0, manager.getAllModifiers().size());
    }

    @Test
    void securityMiracleRequiresPrecondition() {
        PressureModifierManager manager = new PressureModifierManager();
        DivineIntent intent = createBlessIntent(PressureType.SECURITY);
        ValidatedIntent validated = effectEligible(intent);

        assertFalse(manager.tryApplyFromValidatedIntent(validated, emptyContext(), BASE_TICK),
            "Guardian's Vigil without raid/high security should fail");
        assertEquals(0, manager.getAllModifiers().size());
    }

    @Test
    void securityMiracleSucceedsWithHighSecurityPressure() {
        PressureModifierManager manager = new PressureModifierManager();
        DivineIntent intent = createBlessIntent(PressureType.SECURITY);
        ValidatedIntent validated = effectEligible(intent);

        Map<PressureType, Integer> pressures = new HashMap<>();
        pressures.put(PressureType.SECURITY, 70);
        PressureSnapshot snapshot = new PressureSnapshot(COLONY_ID, pressures, BASE_TICK);
        DivineAnswerContext context = new DivineAnswerContext(null, snapshot, List.of(), List.of(), List.of());

        assertTrue(manager.tryApplyFromValidatedIntent(validated, context, BASE_TICK),
            "Guardian's Vigil with security pressure >= 70 should succeed");
        assertEquals(1, manager.getAllModifiers().size());
    }

    @Test
    void foodMiracleRequiresPrecondition() {
        PressureModifierManager manager = new PressureModifierManager();
        DivineIntent intent = createBlessIntent(PressureType.FOOD);
        ValidatedIntent validated = effectEligible(intent);

        assertFalse(manager.tryApplyFromValidatedIntent(validated, emptyContext(), BASE_TICK),
            "Green Mercy without high food pressure should fail");
        assertEquals(0, manager.getAllModifiers().size());
    }

    @Test
    void foodMiracleSucceedsWithHighFoodPressure() {
        PressureModifierManager manager = new PressureModifierManager();
        DivineIntent intent = createBlessIntent(PressureType.FOOD);
        ValidatedIntent validated = effectEligible(intent);

        Map<PressureType, Integer> pressures = new HashMap<>();
        pressures.put(PressureType.FOOD, 80);
        PressureSnapshot snapshot = new PressureSnapshot(COLONY_ID, pressures, BASE_TICK);
        DivineAnswerContext context = new DivineAnswerContext(null, snapshot, List.of(), List.of(), List.of());

        assertTrue(manager.tryApplyFromValidatedIntent(validated, context, BASE_TICK),
            "Green Mercy with food pressure >= 70 should succeed");
        assertEquals(1, manager.getAllModifiers().size());
    }

    @Test
    void rejectedDomainsLeaveNoModifierInManager() {
        PressureModifierManager manager = new PressureModifierManager();

        DivineIntent secIntent = createBlessIntent(PressureType.SECURITY);
        assertFalse(manager.tryApplyFromValidatedIntent(
            effectEligible(secIntent), emptyContext(), BASE_TICK));

        DivineIntent foodIntent = createBlessIntent(PressureType.FOOD);
        assertFalse(manager.tryApplyFromValidatedIntent(
            effectEligible(foodIntent), emptyContext(), BASE_TICK));

        assertEquals(0, manager.getAllModifiers().size());
    }

    @Test
    void downgradedDownstreamPreservesWhitelistGate() {
        PressureModifierManager manager = new PressureModifierManager();

        for (PressureType type : PressureType.values()) {
            DivineIntent intent = createBlessIntent(type);
            ValidatedIntent validated = new ValidatedIntent(intent, ValidationResult.DOWNGRADED,
                List.of("DOMAIN_NOT_WHITELISTED"));
            assertFalse(manager.tryApplyFromValidatedIntent(validated, emptyContext(), BASE_TICK),
                "DOWNGRADED should never apply modifier for " + type);
        }

        assertEquals(0, manager.getAllModifiers().size());
    }
}