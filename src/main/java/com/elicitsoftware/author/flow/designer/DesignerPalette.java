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

import com.elicitsoftware.model.QuestionType;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.dnd.EffectAllowed;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.quarkus.panache.common.Sort;

import java.util.List;

/**
 * The shape palette (UC-011 step 1, UC-015 A4, UC-016): one draggable glyph per kind of element
 * — step, section, and a question whose type is picked beside it — plus the three rule glyphs, all
 * in a single card in the drawer. Elements are created by dragging a glyph onto the board.
 */
public class DesignerPalette extends VerticalLayout {

    final Glyph stepChip = Glyph.step(null);
    final Glyph sectionChip = Glyph.section();
    final ComboBox<QuestionType> questionTypePicker = new ComboBox<>();
    final Glyph showRuleChip = Glyph.rule("SHOW");
    final Glyph repeatRuleChip = Glyph.rule("REPEAT");
    final Glyph textRuleChip = Glyph.rule("TEXT");

    private Glyph questionChip;
    private final Div questionSlot = new Div();
    private final Runnable onDragStart;
    private final Runnable onDragEnd;

    public DesignerPalette(Runnable onDragStart, Runnable onDragEnd) {
        this.onDragStart = onDragStart;
        this.onDragEnd = onDragEnd;
        addClassName("designer-palette");
        setPadding(false);
        setSpacing(false);
        setAlignItems(Alignment.STRETCH);
        getElement().setAttribute("role", "toolbar");
        getElement().setAttribute("aria-label", getTranslation("designerPalette.aria"));

        draggable(stepChip, PaletteItem.step());
        draggable(sectionChip, PaletteItem.section());

        List<QuestionType> types = QuestionType.listAll(Sort.by("name"));
        questionTypePicker.setItems(types);
        questionTypePicker.setItemLabelGenerator(t -> t.name);
        questionTypePicker.setWidthFull();
        questionTypePicker.setAriaLabel(getTranslation("designerPalette.questionType"));
        types.stream().filter(t -> "TEXT".equals(t.name)).findFirst().ifPresentOrElse(questionTypePicker::setValue,
                () -> types.stream().findFirst().ifPresent(questionTypePicker::setValue));
        questionTypePicker.addValueChangeListener(e -> rebuildQuestionChip());
        rebuildQuestionChip();

        draggable(showRuleChip, PaletteItem.rule("SHOW"));
        draggable(repeatRuleChip, PaletteItem.rule("REPEAT"));
        draggable(textRuleChip, PaletteItem.rule("TEXT"));

        Div card = new Div();
        card.addClassName("designer-palette-card");
        card.add(row(getTranslation("designerPalette.step"), chips(stepChip)),
                row(getTranslation("designerPalette.section"), chips(sectionChip)),
                row(getTranslation("designerPalette.question"), chips(questionSlot)),
                questionTypePicker,
                row(getTranslation("designerPalette.rule"), chips(showRuleChip, repeatRuleChip, textRuleChip)));
        add(card);
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

    /** A labelled row inside the tools card: a caption on the left, its draggable chips on the right. */
    private static Div row(String caption, Component chips) {
        Div r = new Div(label(caption), chips);
        r.addClassName("designer-palette-row");
        return r;
    }

    /** A horizontal strip of draggable chips. */
    private static HorizontalLayout chips(Component... items) {
        HorizontalLayout row = new HorizontalLayout(items);
        row.addClassName("designer-palette-chips");
        row.setSpacing(true);
        row.setPadding(false);
        row.setAlignItems(Alignment.CENTER);
        return row;
    }

    private void draggable(Glyph glyph, PaletteItem item) {
        DragSource<Glyph> source = DragSource.create(glyph);
        source.setDragData(item);
        // Board targets ask for the MOVE effect. Chromium cancels a drop whose target effect is
        // outside the source's allowed effects, so a COPY-only palette chip would never land.
        source.setEffectAllowed(EffectAllowed.COPY_MOVE);
        source.addDragStartListener(e -> onDragStart.run());
        source.addDragEndListener(e -> onDragEnd.run());
        glyph.getElement().setAttribute("aria-roledescription", getTranslation("designerPalette.roledescription"));
    }

    private static Span label(String text) {
        Span s = new Span(text);
        s.addClassName("designer-palette-label");
        return s;
    }
}
