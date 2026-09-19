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

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

import java.util.List;

/**
 * The reporting-tag badge on a question row or section card (UC-021): a tag glyph followed by the
 * tag names, "Tag = value" for a fixed value. Hidden when the element carries no tag.
 */
final class TagBadge {

    private TagBadge() {
    }

    static void fill(Span badge, List<String> labels) {
        badge.removeAll();
        badge.setClassName("designer-badge designer-tag-badge");
        badge.setVisible(!labels.isEmpty());
        if (labels.isEmpty()) {
            badge.setText("");
            return;
        }
        Icon icon = VaadinIcon.TAG.create();
        icon.addClassName("designer-tag-badge-icon");
        String text = String.join(", ", labels);
        badge.add(icon, new Span(text));
        badge.setTitle("Reporting tags: " + text);
        badge.getElement().setAttribute("aria-label", "Reporting tags: " + text);
    }

    /** The badge's visible text, for tests. */
    static String text(Span badge) {
        return badge.getChildren().filter(Span.class::isInstance).map(c -> ((Span) c).getText()).findFirst().orElse("");
    }
}
