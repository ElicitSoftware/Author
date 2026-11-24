package com.elicitsoftware.author.survey;

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

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * UC-045 BR-004: a translation's budget grows with the source string and never falls below the
 * base column. The 20- and 50-character tiers cannot be told apart through {@code of}: every
 * column is at least 100 wide and 50 x 1.6 is 80, so the column wins below 51 characters.
 */
class TranslationBudgetTest {

    @Test
    void noSource_isTheColumnWidth() {
        assertEquals(100, TranslationBudget.of("questions", "short_text", null));
        assertEquals(100, TranslationBudget.of("questions", "short_text", ""));
        assertEquals(255, TranslationBudget.of("unknown", "field", null), "an unknown column defaults to 255");
    }

    @Test
    void shortSource_neverGoesBelowTheColumn() {
        assertEquals(100, TranslationBudget.of("questions", "short_text", "x".repeat(20)));
        assertEquals(255, TranslationBudget.of("questions", "tool_tip", "x".repeat(50)));
        assertEquals(255, TranslationBudget.of("questions", "tool_tip", "x".repeat(180)), "180 x 1.4 = 252");
    }

    @Test
    void longSource_growsByItsTier() {
        assertEquals(280, TranslationBudget.of("questions", "tool_tip", "x".repeat(200)), "200 x 1.4");
        assertEquals(262, TranslationBudget.of("questions", "tool_tip", "x".repeat(201)), "201 x 1.3 = 261.3, rounded up");
        assertEquals(325, TranslationBudget.of("questions", "tool_tip", "x".repeat(250)), "250 x 1.3");
        assertEquals(112, TranslationBudget.of("questions", "short_text", "x".repeat(80)), "80 x 1.4 beats the 100 column");
    }

    @Test
    void wideColumn_winsOverAnyExpansion() {
        assertEquals(8000, TranslationBudget.of("questions", "text", "x".repeat(1000)));
        assertEquals(2600, TranslationBudget.of("surveys", "title", "x".repeat(2000)), "2000 x 1.3");
    }

    @Test
    void columnWidth_isTheBaseColumn() {
        assertEquals(8000, TranslationBudget.columnWidth("questions", "text"));
        assertEquals(2000, TranslationBudget.columnWidth("surveys", "description"));
        assertEquals(255, TranslationBudget.columnWidth("unknown", "field"));
    }
}
