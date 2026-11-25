package com.elicitsoftware.author.flow.designer;

/*-
 * ***LICENSE_START***
 * Elicit Author
 * %%
 * Copyright (C) 2025 - 2026 Elicit Software LLC
 * %%
 * PolyForm Noncommercial License 1.0.0
 * <https://polyformproject.org/licenses/noncommercial/1.0.0>
 * ***LICENSE_END***
 */

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * UC-011 BR-004: a display-only question's preview renders its text only after removing
 * everything that could run in the viewer's session, and keeps the formatting a survey uses.
 */
class PreviewHtmlSanitizeTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "<img src=x onerror=alert(1)>",
            "<script>alert(1)</script>",
            "<a href=\"javascript:alert(1)\">x</a>",
            "<p onclick=\"alert(1)\">x</p>",
            "<iframe src=\"https://example.org\"></iframe>",
            "<svg><script>alert(1)</script></svg>",
            "<form action=\"https://example.org\"><input name=a></form>",
    })
    void nothingThatCanRunSurvives(String hostile) {
        String clean = PreviewFields.sanitize(hostile).toLowerCase();

        assertFalse(clean.contains("<script"), clean);
        assertFalse(clean.contains("onerror"), clean);
        assertFalse(clean.contains("onclick"), clean);
        assertFalse(clean.contains("javascript:"), clean);
        assertFalse(clean.contains("<iframe"), clean);
        assertFalse(clean.contains("<form"), clean);
    }

    /** The markup the Family History and census definitions use previews as written. */
    @Test
    void theFormattingASurveyUsesIsKept() {
        String fhhs = "<h1>Family History</h1><p>Tell us about <b>your</b> family.<br>Thank you.</p>"
                + "<ul><li>Parents</li><li>Siblings</li></ul>";

        assertEquals(fhhs, PreviewFields.sanitize(fhhs));
    }

    @Test
    void classAndStyleAreKept() {
        String styled = "<p class=\"note\" style=\"color: red\">Note</p>";

        assertEquals(styled, PreviewFields.sanitize(styled));
    }

    /** A token's value arrives escaped (BR-005) and stays text, not markup. */
    @Test
    void anEscapedSampleStaysText() {
        assertEquals("<p>&lt;b&gt;Bob&lt;/b&gt;</p>", PreviewFields.sanitize("<p>&lt;b&gt;Bob&lt;/b&gt;</p>"));
    }
}
