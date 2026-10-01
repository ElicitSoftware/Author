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

    public static DesignerAddress of(StructureNode node) {
        return new DesignerAddress(node.routeKind(), node.durableId());
    }

    public static DesignerAddress mount(int stepsSectionsId) {
        return new DesignerAddress(MOUNT, stepsSectionsId);
    }

    public static DesignerAddress assignment(int sectionsQuestionId) {
        return new DesignerAddress(ASSIGNMENT, sectionsQuestionId);
    }

    public static DesignerAddress rule(int relationshipId) {
        return new DesignerAddress(RULE, relationshipId);
    }

    /** Parses {@code kind:id}; anything else names nothing, which selects nothing (UC-006 A2). */
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
