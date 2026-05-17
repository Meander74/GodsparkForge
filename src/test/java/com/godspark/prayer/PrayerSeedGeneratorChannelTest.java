package com.godspark.prayer;

import com.godspark.pressure.PressureType;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class PrayerSeedGeneratorChannelTest {

    private static final PrayerSeedGenerator GENERATOR = new PrayerSeedGenerator();
    private static final String COLONY_NAME = "Oakville";
    private static final PrayerTone TONE = PrayerTone.STOIC;

    private static final String[] FORBIDDEN_TERMS = {
        "chapel", "church", "shrine", "temple", "altar",
        "sanctuary", "oracle", "reliquary", "sanctum",
        "monastery", "sacred", "holy site"
    };

    private static final Pattern[] FORBIDDEN_PATTERNS;
    static {
        FORBIDDEN_PATTERNS = new Pattern[FORBIDDEN_TERMS.length];
        for (int i = 0; i < FORBIDDEN_TERMS.length; i++) {
            FORBIDDEN_PATTERNS[i] = Pattern.compile(
                "\\b" + Pattern.quote(FORBIDDEN_TERMS[i]) + "\\b",
                Pattern.CASE_INSENSITIVE
            );
        }
    }

    @Test
    void commonsPrayerTextNoSacredWords() {
        for (PressureType pt : PressureType.values()) {
            String plea = GENERATOR.formatPlea(COLONY_NAME, pt, PrayerChannel.COMMONS, TONE);
            assertNoForbidden(plea, "formatPlea COMMONS " + pt);

            String lament = GENERATOR.formatLament(COLONY_NAME, pt, PrayerChannel.COMMONS, TONE);
            assertNoForbidden(lament, "formatLament COMMONS " + pt);

            String thanks = GENERATOR.formatThanks(COLONY_NAME, pt, PrayerChannel.COMMONS, TONE);
            assertNoForbidden(thanks, "formatThanks COMMONS " + pt);

            String hope = GENERATOR.formatHope(COLONY_NAME, pt, PrayerChannel.COMMONS, TONE);
            assertNoForbidden(hope, "formatHope COMMONS " + pt);
        }

        String vigil = GENERATOR.formatVigil(COLONY_NAME, PrayerChannel.COMMONS, TONE);
        assertNoForbidden(vigil, "formatVigil COMMONS");
    }

    @Test
    void sacredChannelTextContainsSacredWording() {
        Pattern[] sacredPatterns = {
            Pattern.compile("\\bchapel\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bsacred\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\baltar\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bprayer stone\\b", Pattern.CASE_INSENSITIVE)
        };

        for (PressureType pt : PressureType.values()) {
            String plea = GENERATOR.formatPlea(COLONY_NAME, pt, PrayerChannel.CHURCH, TONE);
            assertTrue(anyMatch(plea, sacredPatterns),
                "CHURCH formatPlea " + pt + " should contain sacred wording: " + plea);

            String lament = GENERATOR.formatLament(COLONY_NAME, pt, PrayerChannel.CHURCH, TONE);
            assertTrue(anyMatch(lament, sacredPatterns),
                "CHURCH formatLament " + pt + " should contain sacred wording: " + lament);

            String thanks = GENERATOR.formatThanks(COLONY_NAME, pt, PrayerChannel.CHURCH, TONE);
            assertTrue(anyMatch(thanks, sacredPatterns),
                "CHURCH formatThanks " + pt + " should contain sacred wording: " + thanks);

            String hope = GENERATOR.formatHope(COLONY_NAME, pt, PrayerChannel.CHURCH, TONE);
            assertTrue(anyMatch(hope, sacredPatterns),
                "CHURCH formatHope " + pt + " should contain sacred wording: " + hope);
        }

        String vigil = GENERATOR.formatVigil(COLONY_NAME, PrayerChannel.CHURCH, TONE);
        assertTrue(anyMatch(vigil, sacredPatterns),
            "CHURCH formatVigil should contain sacred wording: " + vigil);
    }

    // ── existing channel selection tests ──

    @Test
    void noSacredNoStoneIsPrivate() {
        assertEquals(PrayerChannel.PRIVATE, channel(0, 0, false));
    }

    @Test
    void stoneOnlyIsChurch() {
        assertEquals(PrayerChannel.CHURCH, channel(0, 0, true));
    }

    @Test
    void sacredBuildingTiersRemainHigherPrestige() {
        assertEquals(PrayerChannel.CHURCH, channel(1, 0, false));
        assertEquals(PrayerChannel.SHRINE, channel(2, 0, false));
        assertEquals(PrayerChannel.TEMPLE, channel(3, 0, false));
    }

    @Test
    void gatheringOnlyIsCommons() {
        assertEquals(PrayerChannel.COMMONS, channel(0, 1, false));
    }

    @Test
    void manyStonesCannotBecomeShrineOrTemple() {
        PrayerChannel channel = channel(0, 0, true);
        assertNotEquals(PrayerChannel.SHRINE, channel);
        assertNotEquals(PrayerChannel.TEMPLE, channel);
    }

    // ── helpers ──

    private static void assertNoForbidden(String text, String label) {
        for (int i = 0; i < FORBIDDEN_TERMS.length; i++) {
            assertFalse(FORBIDDEN_PATTERNS[i].matcher(text).find(),
                label + " must not contain '" + FORBIDDEN_TERMS[i] + "': " + text);
        }
    }

    private static boolean anyMatch(String text, Pattern[] patterns) {
        for (Pattern p : patterns) {
            if (p.matcher(text).find()) return true;
        }
        return false;
    }

    private static PrayerChannel channel(int sacredBuildings, int gatheringBuildings, boolean hasPrayerStoneAnchor) {
        return PrayerSeedGenerator.selectPrayerChannel(
            sacredBuildings,
            gatheringBuildings,
            hasPrayerStoneAnchor
        );
    }
}
