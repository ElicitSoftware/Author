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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** UC-043 BR-002: which base columns a content translation may target. */
class TranslatableFieldsTest {

    @Test
    void respondentFacingText_isTranslatable() {
        assertTrue(TranslatableFields.isTranslatable("questions", "text"));
        assertTrue(TranslatableFields.isTranslatable("questions", "validation_text"));
        assertTrue(TranslatableFields.isTranslatable("select_items", "display_text"));
        assertTrue(TranslatableFields.isTranslatable("surveys", "title"));
        assertTrue(TranslatableFields.isTranslatable("report_items", "label"));
    }

    @Test
    void identifiersAndMachineReadValues_areNot() {
        assertFalse(TranslatableFields.isTranslatable("surveys", "name"), "a survey's name is an identifier");
        assertFalse(TranslatableFields.isTranslatable("questions", "default_value"), "stored as answer data");
        assertFalse(TranslatableFields.isTranslatable("questions", "mask"));
        assertFalse(TranslatableFields.isTranslatable("ontology", "tag"), "an element type with no translatable field");
    }
}
