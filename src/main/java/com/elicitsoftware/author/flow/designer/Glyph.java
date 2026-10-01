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

import java.util.Locale;
import java.util.Map;

/**
 * The designer's visual vocabulary (UC-029 BR-001): one small tile per kind of element. The same
 * glyph identifies a step, section, question or rule action on the guide page, in the palette,
 * in the structure tree and on the board, where it is also the element's drag handle.
 */
public class Glyph extends Span {

    public enum Kind {
        STEP("step"), SECTION("section"), QUESTION("question"),
        RULE_SHOW("rule-show"), RULE_REPEAT("rule-repeat"), RULE_TEXT("rule-text");

        final String cssSuffix;

        Kind(String cssSuffix) {
            this.cssSuffix = cssSuffix;
        }
    }

    /** Icon per {@code survey.question_types.name}; anything unknown falls back to a question mark. */
    static final Map<String, VaadinIcon> QUESTION_ICONS = Map.ofEntries(
            Map.entry("TEXT", VaadinIcon.INPUT),
            Map.entry("TEXTAREA", VaadinIcon.PARAGRAPH),
            Map.entry("INTEGER", VaadinIcon.HASH),
            Map.entry("DOUBLE", VaadinIcon.CALC),
            Map.entry("CHECKBOX", VaadinIcon.CHECK_SQUARE),
            Map.entry("CHECKBOX_GROUP", VaadinIcon.TASKS),
            Map.entry("RADIO", VaadinIcon.DOT_CIRCLE),
            Map.entry("COMBOBOX", VaadinIcon.COMBOBOX),
            Map.entry("MULTI_SELECT", VaadinIcon.LIST_SELECT),
            Map.entry("DATE_PICKER", VaadinIcon.CALENDAR),
            Map.entry("DATE_TIME_PICKER", VaadinIcon.CALENDAR_CLOCK),
            Map.entry("TIME_PICKER", VaadinIcon.CLOCK),
            Map.entry("EMAIL", VaadinIcon.ENVELOPE),
            Map.entry("PASSWORD", VaadinIcon.PASSWORD),
            Map.entry("HTML", VaadinIcon.CODE),
            Map.entry("MODAL", VaadinIcon.MODAL));

    private final Kind kind;

    private Glyph(Kind kind, String accessibleName) {
        this.kind = kind;
        addClassNames("elicit-glyph", "elicit-glyph-" + kind.cssSuffix);
        getElement().setAttribute("role", "img");
        getElement().setAttribute("aria-label", accessibleName);
        getElement().setAttribute("title", accessibleName);
    }

    /** A step tile showing its display order, or a layout icon when the order is not known yet. */
    public static Glyph step(Integer order) {
        Glyph g = new Glyph(Kind.STEP, order == null ? Translations.get("glyph.step") : Translations.get("glyph.stepOrder", order));
        if (order == null) {
            g.add(VaadinIcon.LAYOUT.create());
        } else {
            g.setText(String.valueOf(order));
        }
        return g;
    }

    public static Glyph section() {
        Glyph g = new Glyph(Kind.SECTION, Translations.get("glyph.section"));
        g.add(VaadinIcon.FOLDER.create());
        return g;
    }

    /** A question tile keyed by its type name ({@code survey.question_types.name}). */
    public static Glyph question(String questionTypeName) {
        String type = questionTypeName == null ? "" : questionTypeName.toUpperCase(Locale.ROOT);
        Glyph g = new Glyph(Kind.QUESTION, type.isEmpty() ? Translations.get("glyph.question")
                : Translations.get("glyph.questionOfType", type.replace('_', ' ').toLowerCase(Locale.ROOT)));
        g.add(QUESTION_ICONS.getOrDefault(type, VaadinIcon.QUESTION_CIRCLE).create());
        return g;
    }

    /** A rule tile for an action name ({@code survey.action_types.name}: SHOW, REPEAT or TEXT). */
    public static Glyph rule(String actionName) {
        String action = actionName == null ? "" : actionName.toUpperCase(Locale.ROOT);
        return switch (action) {
            case "SHOW" -> withIcon(new Glyph(Kind.RULE_SHOW, Translations.get("glyph.rule.show")), VaadinIcon.EYE);
            case "REPEAT" -> withIcon(new Glyph(Kind.RULE_REPEAT, Translations.get("glyph.rule.repeat")), VaadinIcon.REFRESH);
            case "TEXT" -> {
                Glyph g = new Glyph(Kind.RULE_TEXT, Translations.get("glyph.rule.text"));
                g.setText("{T}");
                yield g;
            }
            default -> withIcon(new Glyph(Kind.RULE_SHOW, Translations.get("glyph.rule")), VaadinIcon.CONNECT);
        };
    }

    private static Glyph withIcon(Glyph g, VaadinIcon icon) {
        Icon i = icon.create();
        g.add(i);
        return g;
    }

    public Kind kind() {
        return kind;
    }

    public String accessibleName() {
        return getElement().getAttribute("aria-label");
    }
}
