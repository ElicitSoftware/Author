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

import com.elicitsoftware.author.survey.StructureNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What a designer palette glyph carries when dragged: an element to create (UC-011, UC-015) or a rule (UC-019). */
class PaletteItemTest {

    @Test
    void elementGlyphs_carryTheirKind() {
        assertEquals(new PaletteItem(StructureNode.Kind.STEP, null), PaletteItem.step());
        assertEquals(new PaletteItem(StructureNode.Kind.SECTION, null), PaletteItem.section());
        assertEquals(new PaletteItem(StructureNode.Kind.QUESTION, null), PaletteItem.question());
        assertFalse(PaletteItem.step().isRule());
        assertFalse(PaletteItem.question().isRule());
    }

    @Test
    void ruleGlyph_carriesOnlyItsAction() {
        PaletteItem rule = PaletteItem.rule("SHOW");
        assertEquals(new PaletteItem(null, "SHOW"), rule);
        assertTrue(rule.isRule());
    }
}
