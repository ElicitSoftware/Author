package com.elicitsoftware.author.survey;

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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** UC-006: a row of the structure tree; UC-007 BR-011: an element with no name is named by its durable id. */
class StructureNodeTest {

    private static StructureNode node(StructureNode.Kind kind, UUID key) {
        return new StructureNode(kind, 5, null, "label", null, null, key, false, null);
    }

    @Test
    void nameOr_fallsBackToTheDurableId() {
        assertEquals("Family", StructureNode.nameOr("Family", 5));
        assertEquals("#5", StructureNode.nameOr(null, 5));
        assertEquals("#5", StructureNode.nameOr("", 5));
        assertEquals("#5", StructureNode.nameOr("   ", 5));
    }

    @Test
    void shortKey_isTheFirstEightCharacters() {
        UUID key = UUID.fromString("0123abcd-4567-89ef-0123-456789abcdef");
        assertEquals("0123abcd", node(StructureNode.Kind.STEP, key).shortKey());
        assertEquals("", node(StructureNode.Kind.STEP, null).shortKey());
    }

    @Test
    void routeKind_isTheLowerCaseKind() {
        assertEquals("step", node(StructureNode.Kind.STEP, null).routeKind());
        assertEquals("section", node(StructureNode.Kind.SECTION, null).routeKind());
        assertEquals("question", node(StructureNode.Kind.QUESTION, null).routeKind());
    }

    @Test
    void kind_namesItsTable() {
        assertEquals("steps", StructureNode.Kind.STEP.table);
        assertEquals("sections", StructureNode.Kind.SECTION.table);
        assertEquals("questions", StructureNode.Kind.QUESTION.table);
    }
}
