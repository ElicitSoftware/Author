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

import java.util.Locale;

/**
 * What a drag carries onto the board, which decides what may take it (UC-006 BR-007): one of the
 * three kinds of element — from the board, the palette or a drawer panel — a rule glyph, or a list.
 */
public enum DragPayload {
    STEP, SECTION, QUESTION, RULE, LIST;

    public static DragPayload of(StructureNode.Kind kind) {
        return switch (kind) {
            case STEP -> STEP;
            case SECTION -> SECTION;
            case QUESTION -> QUESTION;
        };
    }

    /** The payload of a drop's drag data; null for anything the board does not know. */
    public static DragPayload ofData(Object dragData) {
        return switch (dragData) {
            case PaletteItem item -> item.isRule() ? RULE : of(item.kind());
            case BankSection bank -> SECTION;
            case BankQuestion bank -> QUESTION;
            case PaletteList list -> LIST;
            case StructureNode node -> of(node.kind());
            case null, default -> null;
        };
    }

    /** The board's {@code dragging-<what>} class while this payload is in flight. */
    String cssClass() {
        return "dragging-" + name().toLowerCase(Locale.ROOT);
    }
}
