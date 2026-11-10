package com.elicitsoftware.author.definition;

/*-
 * ***LICENSE_START***
 * Elicit Author
 * %%
 * Copyright (C) 2026 Elicit Software LLC
 * %%
 * PolyForm Noncommercial License 1.0.0
 * <https://polyformproject.org/licenses/noncommercial/1.0.0>
 * ***LICENSE_END***
 */

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.elicitsoftware.author.i18n.Translations;
import java.math.BigDecimal;
import java.util.Map;

/** UC-005 / UC-008: the file grammar shared with Admin. */
class ElicitFormatTest {

    @Test
    void escapeAndParseRoundTrip() {
        String[] in = {"a|b", "back\\slash", "line\nbreak", "cr\rhere", "", "plain"};
        String joined = ElicitFormat.joinFields((Object[]) in);
        assertArrayEquals(in, ElicitFormat.parseFields(joined));
    }

    @Test
    void nullBecomesEmptyField() {
        assertEquals("x||y", ElicitFormat.joinFields("x", null, "y"));
        assertNull(ElicitFormat.nullIfEmpty(""));
    }

    @Test
    void rebaseDisplayKey_rewritesLeadingSurveyComponent() {
        assertEquals("0042-0001-0000-0001-0000-0000-0000", ElicitFormat.rebaseDisplayKey("0001-0001-0000-0001-0000-0000-0000", 42));
        assertEquals("plain", ElicitFormat.rebaseDisplayKey("plain", 42));
        assertNull(ElicitFormat.rebaseDisplayKey("", 42));
        assertEquals("0001-0002", ElicitFormat.rebaseDisplayKey("0001-0002", null));
    }

    @Test
    void revisionHeader_parsedOrRejected() {
        assertEquals(OffsetDateTime.parse("2026-09-15T19:00:00-04:00"),
                ElicitFormat.parseRevisionHeader("# survey_revision: 2026-09-15T19:00:00-04:00"));
        assertNull(ElicitFormat.parseRevisionHeader("# generated: 2026-09-15T19:00:00-04:00"));
        assertThrows(IllegalArgumentException.class, () -> ElicitFormat.parseRevisionHeader("# survey_revision: "));
        assertThrows(IllegalArgumentException.class, () -> ElicitFormat.parseRevisionHeader("# survey_revision: yesterday"));
    }

    @Test
    void retiredRows_recognizedByClosedEffectiveTo() {
        String[] current = ElicitFormat.parseFields("1|k|n|d|Text|0|1970-01-01 00:00:00+00|9999-12-31 23:59:59+00|by|note");
        String[] retired = ElicitFormat.parseFields("1|k|n|d|Text|0|1970-01-01 00:00:00+00|2026-09-17 12:00:00+00|by|note");
        String[] blank = ElicitFormat.parseFields("1|k|n|d|Text|0|||by|note");
        assertFalse(ElicitFormat.isRetired("select_groups", current));
        assertTrue(ElicitFormat.isRetired("select_groups", retired));
        assertFalse(ElicitFormat.isRetired("select_groups", blank));
    }

    @Test
    void requiredFields_acceptsPostV015AndPreV015Layouts() {
        String[] post = new String[20];
        String[] pre = new String[21];
        java.util.Arrays.fill(post, "");
        java.util.Arrays.fill(pre, "");
        ElicitFormat.requireFields("questions", post);
        ElicitFormat.requireFields("questions", pre);
        assertThrows(IllegalArgumentException.class, () -> ElicitFormat.requireFields("questions", new String[19]));
    }

    @Test
    void keysAreCarriedOrMintedAndMalformedRejected() {
        assertEquals("b3a7c2e4-5d18-4f92-8a60-1c9e3f75d2b4",
                ElicitFormat.parseKeyOrMint("b3a7c2e4-5d18-4f92-8a60-1c9e3f75d2b4", "survey_key").toString());
        assertTrue(ElicitFormat.parseKeyOrMint("", "survey_key") != null);
        assertThrows(IllegalArgumentException.class, () -> ElicitFormat.parseKeyOrMint("not-a-uuid", "survey_key"));
    }

    @Test
    void safeFileName() {
        assertEquals("Elicit_Designer", SurveyDefinitionExporter.safeName(" Elicit Designer! "));
        assertEquals("survey", SurveyDefinitionExporter.safeName("///"));
    }

    @Test
    void rebaseDisplayKey_aLeadingSeparatorIsStillAStructuredKey() {
        assertEquals("0007-01", ElicitFormat.rebaseDisplayKey("-01", 7));
    }

    @Test
    void parseNumbers_trimAndReturnNullForBlankOrMalformed() {
        assertEquals(42, ElicitFormat.parseIntOrNull(" 42 "));
        assertNull(ElicitFormat.parseIntOrNull(null));
        assertNull(ElicitFormat.parseIntOrNull(""));
        assertNull(ElicitFormat.parseIntOrNull("4.2"));

        assertEquals(9_000_000_000L, ElicitFormat.parseLongOrNull(" 9000000000 "));
        assertNull(ElicitFormat.parseLongOrNull(null));
        assertNull(ElicitFormat.parseLongOrNull(""));
        assertNull(ElicitFormat.parseLongOrNull("nine"));

        assertEquals(new BigDecimal("1.50"), ElicitFormat.parseDecimalOrNull(" 1.50 "));
        assertNull(ElicitFormat.parseDecimalOrNull(null));
        assertNull(ElicitFormat.parseDecimalOrNull(""));
        assertNull(ElicitFormat.parseDecimalOrNull("1,5"));
    }

    @Test
    void parseBooleanOrFalse_acceptsTrueAndPostgresT() {
        assertTrue(ElicitFormat.parseBooleanOrFalse("true"));
        assertTrue(ElicitFormat.parseBooleanOrFalse("TRUE"));
        assertTrue(ElicitFormat.parseBooleanOrFalse("t"));
        assertTrue(ElicitFormat.parseBooleanOrFalse("T"));
        assertFalse(ElicitFormat.parseBooleanOrFalse("f"));
        assertFalse(ElicitFormat.parseBooleanOrFalse("yes"));
        assertFalse(ElicitFormat.parseBooleanOrFalse(""));
        assertFalse(ElicitFormat.parseBooleanOrFalse(null));
    }

    private static String[] stepsRecord(String effectiveTo) {
        String[] fields = new String[9];
        java.util.Arrays.fill(fields, "x");
        fields[8] = effectiveTo;
        return fields;
    }

    @Test
    void isRetired_readsTheEffectiveToField() {
        assertTrue(ElicitFormat.isRetired("steps", stepsRecord("2026-01-01 00:00:00+00")));
        assertFalse(ElicitFormat.isRetired("steps", stepsRecord(ElicitFormat.SENTINEL_LITERAL)));
        assertFalse(ElicitFormat.isRetired("steps", stepsRecord("")), "no effective_to is current");
        assertFalse(ElicitFormat.isRetired("steps", stepsRecord("9999-12-31")), "an unparsable sentinel is current");
        assertTrue(ElicitFormat.isRetired("steps", stepsRecord("2026-01-01")), "an unparsable past date is retired");
    }

    @Test
    void isRetired_falseWhenTheTableIsUnversionedOrTheRecordIsShort() {
        assertFalse(ElicitFormat.isRetired("surveys", stepsRecord("2026-01-01 00:00:00+00")));
        String[] shortRecord = new String[8];
        java.util.Arrays.fill(shortRecord, "2026-01-01 00:00:00+00");
        assertFalse(ElicitFormat.isRetired("steps", shortRecord), "steps' effective_to is field 8");
    }

    @Test
    void resolveIds_mapOldIdsToNew() {
        Map<Long, Long> ids = Map.of(10L, 110L);
        assertEquals(110L, ElicitFormat.resolveRequired(10L, ids, "step"));
        assertEquals(110L, ElicitFormat.resolveNullable(10L, ids, "step"));
        assertNull(ElicitFormat.resolveNullable(null, ids, "step"));

        IllegalStateException noSource = assertThrows(IllegalStateException.class,
                () -> ElicitFormat.resolveRequired(null, ids, "step"));
        assertEquals(Translations.get("error.import.noSourceId", "step"), noSource.getMessage());

        IllegalStateException noMapping = assertThrows(IllegalStateException.class,
                () -> ElicitFormat.resolveNullable(11L, ids, "step"));
        assertEquals(Translations.get("error.import.noIdMapping", "step", "11"), noMapping.getMessage());
    }
}
