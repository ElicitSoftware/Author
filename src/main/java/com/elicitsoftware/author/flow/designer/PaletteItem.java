package com.elicitsoftware.author.flow.designer;

/*-
 * ***LICENSE_START***
 * Elicit Author
 * %%
 * Copyright (C) 2026 The Regents of the University of Michigan - Rogel Cancer Center
 * %%
 * PolyForm Noncommercial License 1.0.0
 * <https://polyformproject.org/licenses/noncommercial/1.0.0>
 * ***LICENSE_END***
 */

import com.elicitsoftware.author.survey.StructureNode;

/**
 * What a palette glyph carries while dragged: the kind of element to create and, for questions,
 * the type name ({@code survey.question_types.name}); or, for a rule glyph, the action name
 * ({@code survey.action_types.name}) with no kind.
 */
public record PaletteItem(StructureNode.Kind kind, String questionType, String action) {

    public static PaletteItem step() {
        return new PaletteItem(StructureNode.Kind.STEP, null, null);
    }

    public static PaletteItem section() {
        return new PaletteItem(StructureNode.Kind.SECTION, null, null);
    }

    public static PaletteItem question(String questionType) {
        return new PaletteItem(StructureNode.Kind.QUESTION, questionType, null);
    }

    public static PaletteItem rule(String action) {
        return new PaletteItem(null, null, action);
    }

    public boolean isRule() {
        return action != null;
    }
}
