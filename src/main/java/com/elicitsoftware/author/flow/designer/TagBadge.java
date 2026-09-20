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

import com.elicitsoftware.author.i18n.Translations;
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
        Span names = new Span(text);
        names.getElement().setAttribute("data-i18n-content", ""); // tag names are authored content
        badge.add(icon, names);
        badge.setTitle(Translations.get("tagBadge.title", text));
        badge.getElement().setAttribute("aria-label", Translations.get("tagBadge.title", text));
    }

    /** The badge's visible text, for tests. */
    static String text(Span badge) {
        return badge.getChildren().filter(Span.class::isInstance).map(c -> ((Span) c).getText()).findFirst().orElse("");
    }
}
