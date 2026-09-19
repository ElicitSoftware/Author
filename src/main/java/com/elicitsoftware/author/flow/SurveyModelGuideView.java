package com.elicitsoftware.author.flow;

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
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import jakarta.annotation.security.RolesAllowed;

import java.util.ArrayList;
import java.util.List;

/**
 * UC-029: the survey model explained in the designer's own vocabulary. Seven ideas in a fixed
 * order (BR-002 rules read as a sentence), each headed by the glyph the board uses for it
 * (BR-001 one vocabulary), all stated in text so the diagram is optional (BR-003).
 */
@Route(value = "guide", layout = MainLayout.class)
@PageTitle("How a survey is put together")
@RolesAllowed(AuthorRoles.AUTHOR)
public class SurveyModelGuideView extends VerticalLayout {

    /** Section headings in page order; tests assert the sequence UC-029 step 2 prescribes. */
    final List<H3> headings = new ArrayList<>();
    final Image diagram = new Image("images/survey-model.svg",
            "A survey drawn as the designer draws it: two step lanes holding section cards and question rows, "
                    + "a Show rule from the question 'Do you have children?' to the section 'Children', "
                    + "and a Repeat rule from 'How many children?' to the section 'Child' in the next step.");
    final RouterLink surveysLink = new RouterLink("Go to the Surveys list", SurveysView.class);

    public SurveyModelGuideView() {
        addClassName("survey-guide");
        add(new H2("How a survey is put together"));
        add(new Paragraph("An Elicit survey is a decision tree the respondent walks through. This page shows the "
                + "pieces a survey is made of and how the designer draws them, using the same names and glyphs "
                + "you will see on the board and in the structure tree."));

        Div figure = new Div(diagram);
        figure.addClassName("survey-guide-diagram");
        add(figure);

        section(VaadinIcon.ROAD_BRANCH.create(), "Elicit is a decision tree",
                new Paragraph("A respondent moves through the survey one page at a time. What they answer decides "
                        + "what they see next: a question can open a section, repeat a group of questions, or change "
                        + "the wording of a later question. Designing a survey means designing that path."));

        section(Glyph.step(1), "Containers nest",
                new Paragraph("Everything sits inside something else. A survey holds steps, a step holds sections, "
                        + "and a section holds questions."),
                bullets(
                        item(Glyph.step(null), "Step", " — a page in the respondent's progress bar. For you it is a lane on the board that groups related sections."),
                        item(Glyph.section(), "Section", " — a titled group of questions on that page. For you it is a card inside a lane."),
                        item(Glyph.question("TEXT"), "Question", " — one input the respondent answers. For you it is a row inside a card, with a glyph that shows its type.")));

        section(VaadinIcon.LIST_OL.create(), "Order is the default path",
                new Paragraph("Steps run in order, sections run in order within their step, and questions run in "
                        + "order within their section. Unless a rule says otherwise, that display order is exactly "
                        + "what the respondent experiences. Dragging an element to a new position changes it."));

        section(Glyph.rule("SHOW"), "Rules are the arrows",
                new Paragraph("A rule reads the answer to one upstream question, compares it with a reference value "
                        + "using an operator, and when the comparison holds performs an action on one downstream "
                        + "target: a step, a section, or a question. Written out, every rule is the same sentence:"),
                ruleSentence(),
                bullets(
                        item(Glyph.rule("SHOW"), "Show", " — the target appears only when the rule holds. Until then the respondent never sees it."),
                        item(Glyph.rule("REPEAT"), "Repeat", " — the target section or question is repeated as many times as the answer says, for example one 'Child' section per child."),
                        item(Glyph.rule("TEXT"), "Text", " — the answer is written into the target's text wherever a {TOKEN|default} placeholder appears, so a later question can say 'your daughter' instead of 'your child'.")),
                new Paragraph("Operators are Boolean, Greater than, Equal, Not equal, Field exists and Contains. "
                        + "When several rules point at the same target, all of them must hold."));

        section(VaadinIcon.SITEMAP.create(), "The designer draws all of this",
                new Paragraph("On the board, lanes are steps, cards are sections, rows are questions and arrows are "
                        + "rules. A card starts closed; click it to open it and see its questions. A breadcrumb at the "
                        + "top of the page shows where you are (survey, step, section, question), and the structure "
                        + "tree in the drawer follows your selection. Every element carries its glyph, and the glyph "
                        + "is its handle: drag a glyph from the "
                        + "palette to create an element, drag it within its container to reorder, or drag it into "
                        + "another container to copy it there. Drag a rule glyph onto a question to start a rule, then "
                        + "drag the arrowhead onto the target. Everything a drag can do, the element's menu can do too. The "
                        + "drawer's Lists panel holds the lists of options for choice questions (drag a list onto a "
                        + "question), and its Questions panel holds every question once, ready to be dragged into another section."));

        section(VaadinIcon.TABLE.create(), "Reporting tags",
                new Paragraph("A reporting tag names a column of the report a site builds from the answers. Tag a question and "
                        + "its answers fill that column; tag a section as it appears in a step with a fixed value, and every "
                        + "respondent who reaches it is classified by that value. Tags that share a dimension share one table of "
                        + "values. Open a survey's Reporting page, or the 'Reporting tags…' item on a question or section, to "
                        + "manage them. Changing or removing an assigned tag is retroactive: answers already recorded at deployed "
                        + "sites are reclassified."));

        section(VaadinIcon.LINK.create(), "Identity and publishing",
                new Paragraph("Every element gets a key the moment it is created and keeps it for life. Removing an "
                        + "element retires it rather than deleting it, so answers already recorded at a site keep "
                        + "their meaning. When the survey is ready you export a .elicit file; the Admin app installs "
                        + "it at a site the first time and updates it in place after that, matching elements by key."));

        add(new H3("Glyph legend"));
        Div legend = new Div();
        legend.addClassName("survey-guide-legend");
        legendRow(legend, Glyph.step(1), "Step, numbered by its display order");
        legendRow(legend, Glyph.section(), "Section");
        legendRow(legend, Glyph.question("TEXT"), "Text question — the icon changes with the question type (number, choice, date, …)");
        legendRow(legend, Glyph.rule("SHOW"), "Show rule");
        legendRow(legend, Glyph.rule("REPEAT"), "Repeat rule");
        legendRow(legend, Glyph.rule("TEXT"), "Text rule");
        add(legend);

        Div actions = new Div(surveysLink);
        actions.addClassName("survey-guide-actions");
        add(actions);
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

    private static ListItem item(Glyph glyph, String term, String rest) {
        Span name = new Span(term);
        name.getStyle().set("font-weight", "600");
        return new ListItem(glyph, new Span(" "), name, new Span(rest));
    }

    /** "Question (operator) value → (Action) target", the order the runtime evaluates and the arrow draws. */
    private static Div ruleSentence() {
        Div sentence = new Div(
                Glyph.question("INTEGER"), new Span("How many children?"),
                new Span("( > )"), new Span("0"),
                arrow(),
                Glyph.rule("REPEAT"), new Span("Repeat"),
                Glyph.section(), new Span("Child"), new Span("that many times"));
        sentence.addClassName("survey-guide-rule");
        sentence.getElement().setAttribute("aria-label",
                "Example rule: question 'How many children?' greater than 0 repeats section 'Child' that many times");
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
