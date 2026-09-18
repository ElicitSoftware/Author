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

import com.elicitsoftware.author.flow.SurveyModelGuideView;
import com.elicitsoftware.model.QuestionType;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.dnd.EffectAllowed;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.RouterLink;
import io.quarkus.panache.common.Sort;

import java.util.List;
import java.util.function.Consumer;

/**
 * The shape palette (UC-011 step 1, UC-015 A4, UC-016): one draggable glyph per kind of element
 * — step, section, and a question whose type is picked beside it — plus keyboard-reachable
 * "Add" buttons that do the same at the end of the selected container (NFR-012).
 */
public class DesignerPalette extends HorizontalLayout {

    final Glyph stepChip = Glyph.step(null);
    final Glyph sectionChip = Glyph.section();
    final ComboBox<QuestionType> questionTypePicker = new ComboBox<>();
    final Button addStep = new Button("Add step", VaadinIcon.PLUS.create());
    final Button addSection = new Button("Add section", VaadinIcon.PLUS.create());
    final Button addQuestion = new Button("Add question", VaadinIcon.PLUS.create());
    final Glyph showRuleChip = Glyph.rule("SHOW");
    final Glyph repeatRuleChip = Glyph.rule("REPEAT");
    final Glyph textRuleChip = Glyph.rule("TEXT");

    private Glyph questionChip;
    private final Div questionSlot = new Div();
    private final Runnable onDragStart;
    private final Runnable onDragEnd;

    public DesignerPalette(Runnable onDragStart, Runnable onDragEnd,
                           Runnable addStepAction, Runnable addSectionAction, Consumer<QuestionType> addQuestionAction) {
        this.onDragStart = onDragStart;
        this.onDragEnd = onDragEnd;
        addClassName("designer-palette");
        setAlignItems(Alignment.CENTER);
        setSpacing(true);
        getElement().setAttribute("role", "toolbar");
        getElement().setAttribute("aria-label", "Palette: drag a glyph onto the board, or use the Add buttons");

        draggable(stepChip, PaletteItem.step());
        draggable(sectionChip, PaletteItem.section());

        List<QuestionType> types = QuestionType.listAll(Sort.by("name"));
        questionTypePicker.setItems(types);
        questionTypePicker.setItemLabelGenerator(t -> t.name);
        questionTypePicker.setWidth("11rem");
        questionTypePicker.setAriaLabel("Question type for new questions");
        types.stream().filter(t -> "TEXT".equals(t.name)).findFirst().ifPresentOrElse(questionTypePicker::setValue,
                () -> types.stream().findFirst().ifPresent(questionTypePicker::setValue));
        questionTypePicker.addValueChangeListener(e -> rebuildQuestionChip());
        rebuildQuestionChip();

        addStep.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        addSection.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        addQuestion.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        addStep.addClickListener(e -> addStepAction.run());
        addSection.addClickListener(e -> addSectionAction.run());
        addQuestion.addClickListener(e -> addQuestionAction.accept(questionTypePicker.getValue()));

        RouterLink guide = new RouterLink("How surveys work", SurveyModelGuideView.class);
        guide.addClassName("designer-guide-link");

        draggable(showRuleChip, PaletteItem.rule("SHOW"));
        draggable(repeatRuleChip, PaletteItem.rule("REPEAT"));
        draggable(textRuleChip, PaletteItem.rule("TEXT"));
        Span ruleHint = label("Rule");
        ruleHint.setTitle("Drop a rule glyph on the question it reads, then drag the arrowhead onto its target");

        add(label("Step"), stepChip, addStep, label("Section"), sectionChip, addSection,
                label("Question"), questionSlot, questionTypePicker, addQuestion,
                ruleHint, showRuleChip, repeatRuleChip, textRuleChip, guide);
    }

    public QuestionType selectedQuestionType() {
        return questionTypePicker.getValue();
    }

    Glyph questionChip() {
        return questionChip;
    }

    private void rebuildQuestionChip() {
        QuestionType t = questionTypePicker.getValue();
        questionSlot.removeAll();
        questionChip = Glyph.question(t == null ? null : t.name);
        draggable(questionChip, PaletteItem.question(t == null ? null : t.name));
        questionSlot.add(questionChip);
    }

    private void draggable(Glyph glyph, PaletteItem item) {
        DragSource<Glyph> source = DragSource.create(glyph);
        source.setDragData(item);
        source.setEffectAllowed(EffectAllowed.COPY);
        source.addDragStartListener(e -> onDragStart.run());
        source.addDragEndListener(e -> onDragEnd.run());
        glyph.getElement().setAttribute("aria-roledescription", "draggable palette item");
    }

    private static Span label(String text) {
        Span s = new Span(text);
        s.addClassName("designer-palette-label");
        return s;
    }
}
