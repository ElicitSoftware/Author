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
import com.elicitsoftware.author.survey.TokenSamples;
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

    final Span heading = new Span();
    final Span hint = new Span();
    final Div pages = new Div();
    final List<Block> blocks = new ArrayList<>();
    /** The page titles in order (one per section shown). */
    final List<String> titles = new ArrayList<>();

    public SectionPreview() {
        addClassName("designer-preview");
        heading.setText(getTranslation("sectionPreview.heading"));
        getElement().setAttribute("aria-label", getTranslation("sectionPreview.aria"));
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
     * @param samples     what the survey's rules write into each text's tokens, scoped by step, section and question
     * @param ruleShown   anchors ("step-" + step id, "ss-"/"sq-" + assignment id) that only a SHOW rule reveals
     */
    void show(SurveyDesignerView view, StructureNode selected, Map<Integer, Question> questions,
              Map<Integer, List<String>> optionTexts, TokenSamples samples, Set<String> ruleShown) {
        pages.removeAll();
        blocks.clear();
        titles.clear();
        if (selected == null) {
            hint.setText(getTranslation("sectionPreview.hint.select"));
            return;
        }
        List<StructureNode> sections = switch (selected.kind()) {
            case STEP -> view.childrenOf(selected).stream().filter(s -> !s.retired()).toList();
            case SECTION -> List.of(selected);
            case QUESTION -> List.of(selected.parent());
        };
        if (sections.isEmpty()) {
            hint.setText(getTranslation("sectionPreview.hint.noSections", selected.label()));
            return;
        }
        hint.setText(selected.kind() == StructureNode.Kind.STEP
                ? getTranslation("sectionPreview.hint.step")
                : getTranslation("sectionPreview.hint.section"));
        StructureNode step = selected;
        while (step != null && step.kind() != StructureNode.Kind.STEP) {
            step = step.parent();
        }
        if (step != null && ruleShown.contains("step-" + step.durableId())) {
            pages.add(note(getTranslation("sectionPreview.note.stepRuleShown", step.label())));
        }
        for (int i = 0; i < sections.size(); i++) {
            pages.add(page(view, sections.get(i), i, sections.size(), questions, optionTexts, samples, ruleShown));
        }
    }

    private Div page(SurveyDesignerView view, StructureNode section, int index, int count, Map<Integer, Question> questions,
                     Map<Integer, List<String>> optionTexts, TokenSamples samples, Set<String> ruleShown) {
        Div page = new Div();
        page.addClassName("designer-preview-page");
        if (count > 1) {
            Span caption = new Span(getTranslation("sectionPreview.page", index + 1, count));
            caption.addClassName("designer-preview-caption");
            page.add(caption);
        }
        // The section's name as the respondent reads it: rules on its step and on this mount fill its tokens.
        Integer stepId = stepOf(section);
        H3 title = new H3(PreviewFields.substituteTokens(section.label(), samples.forMount(stepId, section.assignmentId(), null)));
        title.addClassName("designer-preview-title");
        title.getElement().setAttribute("data-i18n-content", ""); // the authored section name
        page.add(title);
        titles.add(section.label());
        if (section.retired()) {
            page.add(note(getTranslation("sectionPreview.note.removed")));
        } else if (ruleShown.contains("ss-" + section.assignmentId())) {
            page.add(note(getTranslation("sectionPreview.note.ruleShown")));
        }
        List<StructureNode> rows = view.childrenOf(section).stream().filter(q -> !q.retired()).toList();
        if (rows.isEmpty()) {
            page.add(note(getTranslation("sectionPreview.note.noQuestions")));
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
                      TokenSamples samples, Set<String> ruleShown) {
        // What the runtime substitutes here: the step's rules, then the section mount's, then the assignment's own.
        StructureNode section = row.parent();
        Map<String, String> values = samples.forMount(stepOf(row), section == null ? null : section.assignmentId(), row.assignmentId());
        Div box = new Div();
        box.addClassName("designer-preview-question");
        box.getElement().setAttribute("data-node-id", "sq-" + row.assignmentId());
        if (view.isSelected(row)) {
            box.getElement().setAttribute("aria-selected", "true");
        }
        String typeName = q.questionType == null ? "" : q.questionType.name;
        Component field;
        if ("HTML".equalsIgnoreCase(typeName)) {
            field = PreviewFields.html(PreviewFields.substituteTokens(q.text, values));
        } else {
            List<String> options = q.selectGroupId == null ? List.of() : optionTexts.getOrDefault(q.selectGroupId, List.of());
            field = PreviewFields.field(typeName, PreviewFields.substituteTokens(q.text, values), q.placeholder, q.toolTip,
                    q.required, QuestionDialog.parseVariants(q.variant), options);
            field.getElement().getClassList().add("designer-preview-field");
        }
        field.getElement().setAttribute("data-i18n-content", ""); // the authored question as the respondent sees it
        box.add(field);
        if (ruleShown.contains("sq-" + row.assignmentId())) {
            box.add(note(getTranslation("sectionPreview.note.ruleShown")));
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

    /** The durable id of the step a node sits in, walking up the tree. */
    private static Integer stepOf(StructureNode n) {
        for (StructureNode p = n; p != null; p = p.parent()) {
            if (p.kind() == StructureNode.Kind.STEP) {
                return p.durableId();
            }
        }
        return null;
    }
}
