package com.godspark.divine;

import com.godspark.pressure.PressureType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TemplateDivineInterpreterTest {

    @Test
    void healTheGuard_security() {
        assertEquals(PressureType.SECURITY,
            TemplateDivineInterpreter.detectDomainFromKeywords("heal the guard"));
    }

    @Test
    void healTheGuard_uppercaseAndPunctuation_security() {
        assertEquals(PressureType.SECURITY,
            TemplateDivineInterpreter.detectDomainFromKeywords("HEAL, THE GUARD!"));
    }

    @Test
    void guardsPrefixMatch_security() {
        assertEquals(PressureType.SECURITY,
            TemplateDivineInterpreter.detectDomainFromKeywords("guards"));
    }

    @Test
    void guardingPrefixMatch_security() {
        assertEquals(PressureType.SECURITY,
            TemplateDivineInterpreter.detectDomainFromKeywords("guarding"));
    }

    @Test
    void healOurPeople_comfort() {
        assertEquals(PressureType.COMFORT,
            TemplateDivineInterpreter.detectDomainFromKeywords("heal our people"));
    }

    @Test
    void feedTheGuards_food() {
        assertEquals(PressureType.FOOD,
            TemplateDivineInterpreter.detectDomainFromKeywords("feed the guards"));
    }

    @Test
    void protectTheFarmers_security() {
        assertEquals(PressureType.SECURITY,
            TemplateDivineInterpreter.detectDomainFromKeywords("protect the farmers"));
    }

    @Test
    void farmersPrefixMatch_food() {
        assertEquals(PressureType.FOOD,
            TemplateDivineInterpreter.detectDomainFromKeywords("farmers"));
    }

    @Test
    void buildAHome_housing() {
        assertEquals(PressureType.HOUSING,
            TemplateDivineInterpreter.detectDomainFromKeywords("build a home"));
    }

    @Test
    void buildTools_industry() {
        assertEquals(PressureType.INDUSTRY,
            TemplateDivineInterpreter.detectDomainFromKeywords("build tools"));
    }

    @Test
    void buildAlone_industry() {
        assertEquals(PressureType.INDUSTRY,
            TemplateDivineInterpreter.detectDomainFromKeywords("build"));
    }

    @Test
    void growWheat_food() {
        assertEquals(PressureType.FOOD,
            TemplateDivineInterpreter.detectDomainFromKeywords("grow wheat"));
    }

    @Test
    void defendTheColony_security() {
        assertEquals(PressureType.SECURITY,
            TemplateDivineInterpreter.detectDomainFromKeywords("defend the colony"));
    }

    @Test
    void bringHope_comfort() {
        assertEquals(PressureType.COMFORT,
            TemplateDivineInterpreter.detectDomainFromKeywords("bring hope"));
    }

    @Test
    void warehouse_noMatch() {
        assertNull(TemplateDivineInterpreter.detectDomainFromKeywords("warehouse"));
    }

    @Test
    void forest_noMatch() {
        assertNull(TemplateDivineInterpreter.detectDomainFromKeywords("forest"));
    }

    @Test
    void unsafe_noMatch() {
        assertNull(TemplateDivineInterpreter.detectDomainFromKeywords("unsafe"));
    }

    @Test
    void emptyString_noCrash() {
        assertNull(TemplateDivineInterpreter.detectDomainFromKeywords(""));
    }

    @Test
    void nullInput_noCrash() {
        assertNull(TemplateDivineInterpreter.detectDomainFromKeywords(null));
    }

    @Test
    void granariesPrefixMatch_food() {
        assertEquals(PressureType.FOOD,
            TemplateDivineInterpreter.detectDomainFromKeywords("granaries"));
    }

    @Test
    void shieldTheTown_security() {
        assertEquals(PressureType.SECURITY,
            TemplateDivineInterpreter.detectDomainFromKeywords("shield the town"));
    }

    @Test
    void sootheThePeople_comfort() {
        assertEquals(PressureType.COMFORT,
            TemplateDivineInterpreter.detectDomainFromKeywords("soothe the people"));
    }

    @Test
    void settleHere_housing() {
        assertEquals(PressureType.HOUSING,
            TemplateDivineInterpreter.detectDomainFromKeywords("settle here"));
    }

    @Test
    void forgeWeapons_industry() {
        assertEquals(PressureType.INDUSTRY,
            TemplateDivineInterpreter.detectDomainFromKeywords("forge weapons"));
    }
}
