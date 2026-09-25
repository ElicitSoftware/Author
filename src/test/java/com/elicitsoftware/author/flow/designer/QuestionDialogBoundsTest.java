package com.elicitsoftware.author.flow.designer;

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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC-011 BR-004: the minimum and maximum are offered for the types whose answers Survey actually bounds
 * by length, and labelled as lengths there.
 */
class QuestionDialogBoundsTest {

    @Test
    void theTextTypesSurveyValidatesByLengthAreLengthBounded() {
        assertTrue(QuestionDialog.lengthBounded("TEXT"));
        assertTrue(QuestionDialog.lengthBounded("TEXTAREA"));
        assertTrue(QuestionDialog.lengthBounded("PASSWORD"));
        assertTrue(QuestionDialog.lengthBounded("text"));
    }

    @Test
    void numericAndOtherTypesKeepTheirNumericBounds() {
        assertFalse(QuestionDialog.lengthBounded("INTEGER"));
        assertFalse(QuestionDialog.lengthBounded("DOUBLE"));
        assertFalse(QuestionDialog.lengthBounded("DATE_PICKER"));
        assertFalse(QuestionDialog.lengthBounded("RADIO"));
        assertFalse(QuestionDialog.lengthBounded("HTML"));
        assertFalse(QuestionDialog.lengthBounded(null));
    }

    /** EMAIL reads the pair only as a switch for its address-pattern check and ignores the values. */
    @Test
    void emailIsNotOffered() {
        assertFalse(QuestionDialog.lengthBounded("EMAIL"));
    }
}
