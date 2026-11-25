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
package com.elicitsoftware.author.flow.designer;

import com.elicitsoftware.author.i18n.Translations;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

import java.util.List;

/**
 * The report-mark badge on a question row or section card (UC-049): a report glyph followed by
 * the reports the element is on. Hidden when the element carries no mark.
 */
final class ReportBadge {

    private ReportBadge() {
    }

    static void fill(Span badge, List<String> labels) {
        badge.removeAll();
        badge.setClassName("designer-badge designer-report-badge");
        badge.setVisible(!labels.isEmpty());
        if (labels.isEmpty()) {
            badge.setText("");
            return;
        }
        Icon icon = VaadinIcon.CLIPBOARD_TEXT.create();
        icon.addClassName("designer-report-badge-icon");
        String text = String.join(", ", labels);
        Span names = new Span(text);
        badge.add(icon, names);
        badge.setTitle(Translations.get("reportBadge.title", text));
        badge.getElement().setAttribute("aria-label", Translations.get("reportBadge.title", text));
    }

    /** The badge's visible text, for tests. */
    static String text(Span badge) {
        return badge.getChildren().filter(Span.class::isInstance).map(c -> ((Span) c).getText()).findFirst().orElse("");
    }
}
