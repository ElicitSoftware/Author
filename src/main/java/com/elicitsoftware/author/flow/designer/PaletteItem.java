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

/**
 * What a palette glyph carries while dragged: the kind of element to create, or, for a rule glyph,
 * the action name ({@code survey.action_types.name}) with no kind. A question carries no type; the
 * dialog the drop opens asks for it (UC-011 BR-006).
 */
public record PaletteItem(StructureNode.Kind kind, String action) {

    public static PaletteItem step() {
        return new PaletteItem(StructureNode.Kind.STEP, null);
    }

    public static PaletteItem section() {
        return new PaletteItem(StructureNode.Kind.SECTION, null);
    }

    public static PaletteItem question() {
        return new PaletteItem(StructureNode.Kind.QUESTION, null);
    }

    public static PaletteItem rule(String action) {
        return new PaletteItem(null, action);
    }

    public boolean isRule() {
        return action != null;
    }
}
