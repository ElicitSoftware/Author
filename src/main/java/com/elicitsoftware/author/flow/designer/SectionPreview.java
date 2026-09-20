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
import com.elicitsoftware.model.Question;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The right-hand pane of the designer: the selected section rendered the way the Survey app shows
 * it to a respondent — the section name as the page title, then each current question's field in
 * display order, built by {@link PreviewFields} from the same settings the {@code Elicit*} input
 * wrappers read. A selected question previews its section with that question highlighted; a
 * selected step previews its sections as the pages the respondent walks through. Clicking a
 * question in the preview selects it on the board.
 */
public class SectionPreview extends Div {

    /** One question rendered in the preview, so tests and the board can find it by node. */
    public record Block(StructureNode node, Div box, Component field) { }

    final Span heading = new Span("Preview");
    final Span hint = new Span();
    final Div pages = new Div();
    final List<Block> blocks = new ArrayList<>();
    /** The page titles in order (one per section shown). */
    final List<String> titles = new ArrayList<>();

    public SectionPreview() {
        addClassName("designer-preview");
        getElement().setAttribute("aria-label", "Preview of the selected section as a respondent sees it");
        heading.addClassName("designer-preview-heading");
        hint.addClassName("designer-preview-hint");
        pages.addClassName("designer-preview-pages");
        add(heading, hint, pages);
    }

    /**
     * Rebuild for the current selection.
     *
     * @param questions   the survey's current questions by durable id
     * @param optionTexts option display texts per select group (durable id)
     * @param samples     token samples from TEXT rules, for label substitution
     * @param ruleShown   anchors ("step-" + step id, "ss-"/"sq-" + assignment id) that only a SHOW rule reveals
     */
    void show(SurveyDesignerView view, StructureNode selected, Map<Integer, Question> questions,
              Map<Integer, List<String>> optionTexts, Map<String, String> samples, Set<String> ruleShown) {
        pages.removeAll();
        blocks.clear();
        titles.clear();
        if (selected == null) {
            hint.setText("Select a step, section or question on the board to see it as a respondent will.");
            return;
        }
        List<StructureNode> sections = switch (selected.kind()) {
            case STEP -> view.childrenOf(selected).stream().filter(s -> !s.retired()).toList();
            case SECTION -> List.of(selected);
            case QUESTION -> List.of(selected.parent());
        };
        if (sections.isEmpty()) {
            hint.setText("Step '" + selected.label() + "' has no sections yet, so respondents would see nothing here.");
            return;
        }
        hint.setText(selected.kind() == StructureNode.Kind.STEP
                ? "Respondents see one section per page. Answers typed here are not saved."
                : "What a respondent sees on this page. Answers typed here are not saved.");
        StructureNode step = selected;
        while (step != null && step.kind() != StructureNode.Kind.STEP) {
            step = step.parent();
        }
        if (step != null && ruleShown.contains("step-" + step.durableId())) {
            pages.add(note("Step '" + step.label() + "' is shown only when a rule reveals it."));
        }
        for (int i = 0; i < sections.size(); i++) {
            pages.add(page(view, sections.get(i), i, sections.size(), questions, optionTexts, samples, ruleShown));
        }
    }

    private Div page(SurveyDesignerView view, StructureNode section, int index, int count, Map<Integer, Question> questions,
                     Map<Integer, List<String>> optionTexts, Map<String, String> samples, Set<String> ruleShown) {
        Div page = new Div();
        page.addClassName("designer-preview-page");
        if (count > 1) {
            Span caption = new Span("Page " + (index + 1) + " of " + count);
            caption.addClassName("designer-preview-caption");
            page.add(caption);
        }
        H3 title = new H3(section.label());
        title.addClassName("designer-preview-title");
        page.add(title);
        titles.add(section.label());
        if (section.retired()) {
            page.add(note("Removed: respondents do not see this section."));
        } else if (ruleShown.contains("ss-" + section.assignmentId())) {
            page.add(note("Shown only when a rule reveals it."));
        }
        List<StructureNode> rows = view.childrenOf(section).stream().filter(q -> !q.retired()).toList();
        if (rows.isEmpty()) {
            page.add(note("No questions yet."));
        }
        for (StructureNode row : rows) {
            Question q = questions.get(row.durableId());
            if (q == null) {
                continue;
            }
            page.add(block(view, row, q, optionTexts, samples, ruleShown));
        }
        return page;
    }

    private Div block(SurveyDesignerView view, StructureNode row, Question q, Map<Integer, List<String>> optionTexts,
                      Map<String, String> samples, Set<String> ruleShown) {
        Div box = new Div();
        box.addClassName("designer-preview-question");
        box.getElement().setAttribute("data-node-id", "sq-" + row.assignmentId());
        if (view.isSelected(row)) {
            box.getElement().setAttribute("aria-selected", "true");
        }
        String typeName = q.questionType == null ? "" : q.questionType.name;
        Component field;
        if ("HTML".equalsIgnoreCase(typeName)) {
            field = PreviewFields.html(PreviewFields.substituteTokens(q.text, samples));
        } else {
            List<String> options = q.selectGroupId == null ? List.of() : optionTexts.getOrDefault(q.selectGroupId, List.of());
            field = PreviewFields.field(typeName, PreviewFields.substituteTokens(q.text, samples), q.placeholder, q.toolTip,
                    q.required, QuestionDialog.parseVariants(q.variant), options);
            field.getElement().getClassList().add("designer-preview-field");
        }
        box.add(field);
        if (ruleShown.contains("sq-" + row.assignmentId())) {
            box.add(note("Shown only when a rule reveals it."));
        }
        // Clicking a question here selects it on the board; the selected one is left alone so a
        // click inside its field (opening a list, say) does not rebuild the pane under the pointer.
        box.getElement().addEventListener("click", e -> {
            if (!view.isSelected(row)) {
                view.select(row);
            }
        });
        blocks.add(new Block(row, box, field));
        return box;
    }

    private static Span note(String text) {
        Span note = new Span(text);
        note.addClassName("designer-preview-note");
        return note;
    }

    /** The preview's page titles, in order. */
    public List<String> titles() {
        return List.copyOf(titles);
    }

    public List<Block> blocks() {
        return List.copyOf(blocks);
    }
}
