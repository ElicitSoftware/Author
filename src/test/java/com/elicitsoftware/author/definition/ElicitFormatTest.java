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
}
