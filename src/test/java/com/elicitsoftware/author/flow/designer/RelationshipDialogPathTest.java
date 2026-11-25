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

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * UC-019 step 4: the rule's question pickers name the path to a question and the type of answer it
 * collects, so the author can tell which operator and value the rule can use.
 */
class RelationshipDialogPathTest {

    private static StructureNode step(String name) {
        return new StructureNode(StructureNode.Kind.STEP, 1, null, name, null, BigDecimal.ONE, UUID.randomUUID(), false, null);
    }

    private static StructureNode section(String name, StructureNode parent) {
        return new StructureNode(StructureNode.Kind.SECTION, 2, 20, name, null, BigDecimal.ONE, UUID.randomUUID(), false, parent);
    }

    private static StructureNode question(String name, String type, StructureNode parent) {
        return new StructureNode(StructureNode.Kind.QUESTION, 3, 30, name, type, BigDecimal.ONE, UUID.randomUUID(), false, parent);
    }

    @Test
    void aQuestionNamesItsTypeAfterThePath() {
        StructureNode section = section("Demographics", step("welcome"));
        assertEquals("welcome › Demographics › married (radio)", RelationshipDialog.path(question("married", "RADIO", section)));
        assertEquals("welcome › Demographics › name (text)", RelationshipDialog.path(question("name", "TEXT", section)));
        assertEquals("welcome › Demographics › age (integer)", RelationshipDialog.path(question("age", "INTEGER", section)));
    }

    @Test
    void anUnderscoredTypeReadsAsWords() {
        StructureNode section = section("Demographics", step("welcome"));
        assertEquals("welcome › Demographics › born (date picker)", RelationshipDialog.path(question("born", "DATE_PICKER", section)));
    }

    @Test
    void stepsAndSectionsKeepThePathAlone() {
        StructureNode step = step("welcome");
        StructureNode section = section("Demographics", step);
        assertEquals("welcome", RelationshipDialog.path(step));
        assertEquals("welcome › Demographics", RelationshipDialog.path(section));
    }

    @Test
    void aQuestionWithNoTypeKeepsThePathAlone() {
        StructureNode section = section("Demographics", step("welcome"));
        assertEquals("welcome › Demographics › married", RelationshipDialog.path(question("married", "", section)));
        assertEquals("welcome › Demographics › married", RelationshipDialog.path(question("married", null, section)));
    }
}
