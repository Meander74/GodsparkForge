package com.godspark.pressure;

import com.godspark.divine.DivineAnswerContext;
import com.godspark.divine.DivineIntent;
import com.godspark.divine.IntentSource;
import com.godspark.divine.IntentType;
import com.godspark.divine.ValidatedIntent;
import com.godspark.divine.ValidationResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

class PressureModifierWhitelistTest {

    @Test
    void unsupportedDomainsDoNotApplyInPatchOneBaseline() {
        PressureModifierManager manager = new PressureModifierManager();
        DivineAnswerContext context = new DivineAnswerContext(null, null, List.of(), List.of(), List.of());

        for (PressureType type : List.of(PressureType.HOUSING, PressureType.COMFORT, PressureType.INDUSTRY)) {
            DivineIntent intent = new DivineIntent(
                1,
                1,
                "Test Colony",
                IntentType.BLESS_COLONY,
                type,
                1.0,
                true,
                "test",
                "test",
                List.of(),
                IntentSource.TEMPLATE
            );
            ValidatedIntent validated = new ValidatedIntent(intent, ValidationResult.DOWNGRADED, List.of("PATCH_2_PENDING"));
            assertFalse(manager.tryApplyFromValidatedIntent(validated, context, 100L));
        }
    }
}
