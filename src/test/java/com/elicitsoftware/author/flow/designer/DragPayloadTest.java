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

import com.elicitsoftware.author.survey.StructureNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What a drag carries and which drop spots take it (UC-006 BR-007). */
class DragPayloadTest {

    private static StructureNode node(StructureNode.Kind kind, boolean retired, StructureNode parent) {
        return new StructureNode(kind, 1, 1, "n", null, null, null, retired, parent);
    }

    @Test
    void everyDragSourceNamesItsPayload() {
        assertEquals(DragPayload.STEP, DragPayload.ofData(PaletteItem.step()));
        assertEquals(DragPayload.SECTION, DragPayload.ofData(PaletteItem.section()));
        assertEquals(DragPayload.QUESTION, DragPayload.ofData(PaletteItem.question("TEXT")));
        assertEquals(DragPayload.RULE, DragPayload.ofData(PaletteItem.rule("SHOW")));
        assertEquals(DragPayload.SECTION, DragPayload.ofData(new BankSection(1, "s")));
        assertEquals(DragPayload.QUESTION, DragPayload.ofData(new BankQuestion(1, "q", "TEXT")));
        assertEquals(DragPayload.LIST, DragPayload.ofData(new PaletteList(1, "l")));
        assertEquals(DragPayload.STEP, DragPayload.ofData(node(StructureNode.Kind.STEP, false, null)));
        assertNull(DragPayload.ofData("a file from the desktop"));
        assertNull(DragPayload.ofData(null));
    }

    @Test
    void aSpotTakesOnlyItsOwnKindAndNothingInARemovedContainer() {
        StructureNode step = node(StructureNode.Kind.STEP, false, null);
        DropSpot inStep = new DropSpot(step, 0, StructureNode.Kind.SECTION);
        assertTrue(inStep.takes(DragPayload.SECTION));
        assertFalse(inStep.takes(DragPayload.QUESTION), "#130: a question does not go in a step");
        assertFalse(inStep.takes(DragPayload.RULE), "#133: a rule goes on a question row");
        assertFalse(inStep.takes(DragPayload.LIST));
        assertFalse(inStep.takes(null));
        assertTrue(new DropSpot(null, 0, StructureNode.Kind.STEP).takes(DragPayload.STEP));
        assertFalse(new DropSpot(node(StructureNode.Kind.STEP, true, null), 0, StructureNode.Kind.SECTION).takes(DragPayload.SECTION),
                "a removed step takes nothing");
    }
}
