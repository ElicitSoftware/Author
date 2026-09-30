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

import java.math.BigDecimal;
import java.util.UUID;

/**
 * One row of the structure tree (UC-006): a step, a section assigned to a step, or a question
 * assigned to a section. {@code kind} is the table the node's durable id belongs to; for sections
 * and questions {@code assignmentId} is the durable id of the join row that placed it there.
 */
public record StructureNode(Kind kind, Integer durableId, Integer assignmentId, String label, String detail,
                            BigDecimal order, UUID elementKey, boolean retired, StructureNode parent) {

    public enum Kind {
        STEP("steps"), SECTION("sections"), QUESTION("questions");

        public final String table;

        Kind(String table) {
            this.table = table;
        }
    }

    /** Route segment used by the editor: {@code survey/:id/:kind/:durableId}. */
    public String routeKind() {
        return kind.name().toLowerCase();
    }

    /**
     * What names an element that has no name of its own — its durable id (UC-007 BR-011). An import
     * can carry in a step, a section or a question with a blank name, and the panels, the tree and
     * the validation findings must still be able to point at it.
     */
    public static String nameOr(String name, Integer durableId) {
        return name == null || name.isBlank() ? "#" + durableId : name;
    }

    public String shortKey() {
        return elementKey == null ? "" : elementKey.toString().substring(0, 8);
    }
}
