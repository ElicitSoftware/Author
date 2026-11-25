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

import java.util.Optional;

/**
 * What the designer's {@code select} parameter names (UC-006 BR-003): {@code kind:id}.
 *
 * <p>Three kinds name an element by its durable id ({@code step}, {@code section},
 * {@code question}); two name an assignment, so that a section mounted in two steps, or a question
 * assigned to two sections, is selected in the container meant rather than in whichever comes first
 * ({@code ss}, {@code sq}); one names a rule, which selects the element the rule targets and opens
 * the rule's editor over it (UC-007 BR-009). The vocabulary lives here because both ends need it:
 * the designer parses it, and the validation panel writes it.</p>
 *
 * @param kind what the id names: {@code step}, {@code section}, {@code question}, {@code ss},
 *             {@code sq} or {@code rule}
 * @param id   the durable id of the element, assignment or rule
 */
public record DesignerAddress(String kind, int id) {

    public static final String STEP = "step";
    public static final String SECTION = "section";
    public static final String QUESTION = "question";
    /** A section's mount in a step — a {@code steps_sections} durable id. */
    public static final String MOUNT = "ss";
    /** A question's place in a section — a {@code sections_questions} durable id. */
    public static final String ASSIGNMENT = "sq";
    public static final String RULE = "rule";

    /**
     * The address of a structure tree node's element, by its durable id.
     *
     * @param node the node
     * @return the node's address
     */
    public static DesignerAddress of(StructureNode node) {
        return new DesignerAddress(node.routeKind(), node.durableId());
    }

    /**
     * The address of a section's mount in a step.
     *
     * @param stepsSectionsId the mount's durable id
     * @return the mount's address
     */
    public static DesignerAddress mount(int stepsSectionsId) {
        return new DesignerAddress(MOUNT, stepsSectionsId);
    }

    /**
     * The address of a question's assignment to a section.
     *
     * @param sectionsQuestionId the assignment's durable id
     * @return the assignment's address
     */
    public static DesignerAddress assignment(int sectionsQuestionId) {
        return new DesignerAddress(ASSIGNMENT, sectionsQuestionId);
    }

    /**
     * The address of a rule.
     *
     * @param relationshipId the rule's durable id
     * @return the rule's address
     */
    public static DesignerAddress rule(int relationshipId) {
        return new DesignerAddress(RULE, relationshipId);
    }

    /**
     * Parses {@code kind:id}; anything else names nothing, which selects nothing (UC-006 A2). The
     * kind is lowercased but not checked against the known kinds.
     *
     * @param value the parameter's value, or {@code null}
     * @return the address, or empty when the value is {@code null}, has no colon, or its id is not
     *         an integer
     */
    public static Optional<DesignerAddress> parse(String value) {
        if (value == null) {
            return Optional.empty();
        }
        String[] parts = value.split(":");
        if (parts.length < 2) {
            return Optional.empty();
        }
        try {
            return Optional.of(new DesignerAddress(parts[0].toLowerCase(), Integer.parseInt(parts[1])));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    @Override
    public String toString() {
        return kind + ":" + id;
    }
}
