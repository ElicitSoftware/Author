package com.elicitsoftware.author.flow;

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

import com.elicitsoftware.author.flow.designer.Glyph;
import com.elicitsoftware.author.security.AuthorRoles;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import jakarta.annotation.security.RolesAllowed;

import java.util.ArrayList;
import java.util.List;

/**
 * UC-029: the survey model explained in the designer's own vocabulary. Eight ideas in a fixed
 * order (BR-002 rules read as a sentence), each headed by the glyph the board uses for it
 * (BR-001 one vocabulary), all stated in text so the diagram is optional (BR-003).
 * Every text comes from the translation bundle (UC-040), keyed {@code guideView.<section>.<n>}.
 */
@Route(value = "guide", layout = MainLayout.class)
@RolesAllowed(AuthorRoles.AUTHOR)
public class SurveyModelGuideView extends VerticalLayout implements HasDynamicTitle {

    /** Section headings in page order; tests assert the sequence UC-029 step 2 prescribes. */
    final List<H3> headings = new ArrayList<>();
    final Image diagram = new Image("images/survey-model.png", getTranslation("guideView.diagram.alt"));
    final RouterLink surveysLink = new RouterLink(getTranslation("guideView.surveysLink"), SurveysView.class);

    public SurveyModelGuideView() {
        addClassName("survey-guide");
        add(new H2(getTranslation("guideView.title")));
        add(new Paragraph(getTranslation("guideView.intro.p1")));

        Div figure = new Div(diagram);
        figure.addClassName("survey-guide-diagram");
        add(figure);

        section(VaadinIcon.BRANCH.create(), getTranslation("guideView.tree.h"),
                new Paragraph(getTranslation("guideView.tree.p1")));

        section(Glyph.step(1), getTranslation("guideView.containers.h"),
                new Paragraph(getTranslation("guideView.containers.p1")),
                bullets(
                        item(Glyph.step(null), "guideView.containers.step.term", "guideView.containers.step.desc"),
                        item(Glyph.section(), "guideView.containers.section.term", "guideView.containers.section.desc"),
                        item(Glyph.question("TEXT"), "guideView.containers.question.term", "guideView.containers.question.desc")));

        section(VaadinIcon.LIST_OL.create(), getTranslation("guideView.order.h"),
                new Paragraph(getTranslation("guideView.order.p1")));

        section(Glyph.rule("SHOW"), getTranslation("guideView.rules.h"),
                new Paragraph(getTranslation("guideView.rules.p1")),
                ruleSentence(),
                bullets(
                        item(Glyph.rule("SHOW"), "guideView.rules.show.term", "guideView.rules.show.desc"),
                        item(Glyph.rule("REPEAT"), "guideView.rules.repeat.term", "guideView.rules.repeat.desc"),
                        item(Glyph.rule("TEXT"), "guideView.rules.text.term", "guideView.rules.text.desc")),
                new Paragraph(getTranslation("guideView.rules.p2")),
                new Paragraph(getTranslation("guideView.rules.p3")));

        section(VaadinIcon.SITEMAP.create(), getTranslation("guideView.designer.h"),
                new Paragraph(getTranslation("guideView.designer.p1")));

        section(VaadinIcon.EDIT.create(), getTranslation("guideView.designing.h"),
                new Paragraph(getTranslation("guideView.designing.p1")),
                bullets(
                        item(VaadinIcon.SEARCH.create(), "guideView.designing.search.term", "guideView.designing.search.desc"),
                        item(VaadinIcon.TOOLS.create(), "guideView.designing.controls.term", "guideView.designing.controls.desc"),
                        item(VaadinIcon.LIST.create(), "guideView.designing.lists.term", "guideView.designing.lists.desc"),
                        item(Glyph.section(), "guideView.designing.sections.term", "guideView.designing.sections.desc"),
                        item(Glyph.question("TEXT"), "guideView.designing.questions.term", "guideView.designing.questions.desc"),
                        item(VaadinIcon.TAG.create(), "guideView.designing.reportingTags.term", "guideView.designing.reportingTags.desc"),
                        item(VaadinIcon.LIST_OL.create(), "guideView.designing.structure.term", "guideView.designing.structure.desc")),
                new Paragraph(getTranslation("guideView.designing.p2")));

        section(VaadinIcon.TABLE.create(), getTranslation("guideView.reportingTags.h"),
                new Paragraph(getTranslation("guideView.reportingTags.p1")));

        section(VaadinIcon.LINK.create(), getTranslation("guideView.identity.h"),
                new Paragraph(getTranslation("guideView.identity.p1")));

        add(new H3(getTranslation("guideView.legend.h")));
        Div legend = new Div();
        legend.addClassName("survey-guide-legend");
        legendRow(legend, Glyph.step(1), getTranslation("guideView.legend.step"));
        legendRow(legend, Glyph.section(), getTranslation("guideView.legend.section"));
        legendRow(legend, Glyph.question("TEXT"), getTranslation("guideView.legend.question"));
        legendRow(legend, Glyph.rule("SHOW"), getTranslation("guideView.legend.showRule"));
        legendRow(legend, Glyph.rule("REPEAT"), getTranslation("guideView.legend.repeatRule"));
        legendRow(legend, Glyph.rule("TEXT"), getTranslation("guideView.legend.textRule"));
        add(legend);

        Div actions = new Div(surveysLink);
        actions.addClassName("survey-guide-actions");
        add(actions);
    }

    @Override
    public String getPageTitle() {
        return getTranslation("guideView.pageTitle");
    }

    private void section(Component glyph, String title, Component... body) {
        H3 heading = new H3(glyph, new Span(title));
        headings.add(heading);
        add(heading);
        add(body);
    }

    private static UnorderedList bullets(ListItem... items) {
        return new UnorderedList(items);
    }

    /** A bullet: the glyph, a bold term, and its description (both translation keys). */
    private ListItem item(Component glyph, String termKey, String descKey) {
        Span name = new Span(getTranslation(termKey));
        name.getStyle().set("font-weight", "600");
        return new ListItem(glyph, new Span(" "), name, new Span(" " + getTranslation(descKey)));
    }

    /** "Question (operator) value → (Action) target", the order the runtime evaluates and the arrow draws. */
    private Div ruleSentence() {
        Div sentence = new Div(
                Glyph.question("INTEGER"), new Span(getTranslation("guideView.rules.example.question")),
                new Span("( > )"), new Span("0"),
                arrow(),
                Glyph.rule("REPEAT"), new Span(getTranslation("guideView.rules.example.action")),
                Glyph.section(), new Span(getTranslation("guideView.rules.example.section")),
                new Span(getTranslation("guideView.rules.example.thatManyTimes")));
        sentence.addClassName("survey-guide-rule");
        sentence.getElement().setAttribute("aria-label", getTranslation("guideView.rules.example.ariaLabel"));
        return sentence;
    }

    private static Span arrow() {
        Span arrow = new Span(VaadinIcon.ARROW_RIGHT.create());
        arrow.addClassName("rule-arrow");
        arrow.getElement().setAttribute("aria-hidden", "true");
        return arrow;
    }

    private static void legendRow(Div legend, Glyph glyph, String meaning) {
        legend.add(glyph, new Span(meaning));
    }
}
