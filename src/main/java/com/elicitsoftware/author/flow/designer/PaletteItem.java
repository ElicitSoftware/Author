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

/**
 * What a palette glyph carries while dragged: the kind of element to create, or, for a rule glyph,
 * the action name ({@code survey.action_types.name}) with no kind. A question carries no type; the
 * dialog the drop opens asks for it (UC-011 BR-006).
 *
 * @param kind   the kind of element to create, or null for a rule glyph
 * @param action the rule's action name, or null for an element glyph
 */
public record PaletteItem(StructureNode.Kind kind, String action) {

    /**
     * Returns the glyph that creates a step.
     *
     * @return a step item with no action
     */
    public static PaletteItem step() {
        return new PaletteItem(StructureNode.Kind.STEP, null);
    }

    /**
     * Returns the glyph that creates a section.
     *
     * @return a section item with no action
     */
    public static PaletteItem section() {
        return new PaletteItem(StructureNode.Kind.SECTION, null);
    }

    /**
     * Returns the glyph that creates a question.
     *
     * @return a question item with no action
     */
    public static PaletteItem question() {
        return new PaletteItem(StructureNode.Kind.QUESTION, null);
    }

    /**
     * Returns the glyph that creates a rule with the given action.
     *
     * @param action the action name ({@code survey.action_types.name})
     * @return a rule item with no kind
     */
    public static PaletteItem rule(String action) {
        return new PaletteItem(null, action);
    }

    /**
     * Whether this glyph creates a rule.
     *
     * @return true when the item carries an action name
     */
    public boolean isRule() {
        return action != null;
    }
}
