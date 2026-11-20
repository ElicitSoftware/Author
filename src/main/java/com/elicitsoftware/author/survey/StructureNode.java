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
 *
 * @param kind         which kind of element the node is
 * @param durableId    the element's durable key
 * @param assignmentId the durable key of the mount or assignment that placed it, or null for a
 *                     step and for a section or question that nothing places
 * @param label        the name shown for the node
 * @param detail       secondary text: a step's description, a mounted section's display key or an
 *                     unplaced section's description, a question's type name ({@code ""} when it
 *                     has none)
 * @param order        the display order where the node sits, or null when it has none
 * @param elementKey   the element key
 * @param retired      whether the element, or the mount or assignment that places it, is retired
 * @param parent       the node it sits under, or null for a step and an unplaced element
 */
public record StructureNode(Kind kind, Integer durableId, Integer assignmentId, String label, String detail,
                            BigDecimal order, UUID elementKey, boolean retired, StructureNode parent) {

    /** The kind of element a node is, with the table its durable id belongs to. */
    public enum Kind {
        STEP("steps"), SECTION("sections"), QUESTION("questions");

        public final String table;

        Kind(String table) {
            this.table = table;
        }
    }

    /**
     * Route segment used by the editor: {@code survey/:id/:kind/:durableId}.
     *
     * @return the kind's name in lower case
     */
    public String routeKind() {
        return kind.name().toLowerCase();
    }

    /**
     * What names an element that has no name of its own — its durable id (UC-007 BR-011). An import
     * can carry in a step, a section or a question with a blank name, and the panels, the tree and
     * the validation findings must still be able to point at it.
     *
     * @param name      the element's name, may be null or blank
     * @param durableId the element's durable key
     * @return the name, or {@code #} followed by the durable key when the name is null or blank
     */
    public static String nameOr(String name, Integer durableId) {
        return name == null || name.isBlank() ? "#" + durableId : name;
    }

    /**
     * The first eight characters of the element key, for display.
     *
     * @return the key's prefix, or {@code ""} when the node has no element key
     */
    public String shortKey() {
        return elementKey == null ? "" : elementKey.toString().substring(0, 8);
    }
}
