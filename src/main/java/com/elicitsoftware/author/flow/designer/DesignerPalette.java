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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.dnd.EffectAllowed;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import java.util.function.Consumer;

/**
 * The shape palette (UC-011 step 1, UC-015 A4, UC-016): one draggable glyph per kind of element
 * — step, section and question — plus the three rule glyphs, all in a single card in the drawer.
 * Elements are created by dragging a glyph onto the board; a question's type is chosen in the
 * dialog the drop opens (UC-011 BR-006).
 */
public class DesignerPalette extends VerticalLayout {

    final Glyph stepChip = Glyph.step(null);
    final Glyph sectionChip = Glyph.section();
    /** Drawn as a TEXT question, the type a new question's dialog opens on (UC-011 BR-006). */
    final Glyph questionChip = Glyph.question("TEXT");
    final Glyph showRuleChip = Glyph.rule("SHOW");
    final Glyph repeatRuleChip = Glyph.rule("REPEAT");
    final Glyph textRuleChip = Glyph.rule("TEXT");

    private final Consumer<PaletteItem> onDragStart;
    private final Runnable onDragEnd;

    public DesignerPalette(Consumer<PaletteItem> onDragStart, Runnable onDragEnd) {
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
        draggable(questionChip, PaletteItem.question());

        draggable(showRuleChip, PaletteItem.rule("SHOW"));
        draggable(repeatRuleChip, PaletteItem.rule("REPEAT"));
        draggable(textRuleChip, PaletteItem.rule("TEXT"));

        Div card = new Div();
        card.addClassName("designer-palette-card");
        card.add(row(getTranslation("designerPalette.step"), chips(stepChip)),
                row(getTranslation("designerPalette.section"), chips(sectionChip)),
                row(getTranslation("designerPalette.question"), chips(questionChip)),
                row(getTranslation("designerPalette.rule"), chips(showRuleChip, repeatRuleChip, textRuleChip)));
        add(card);
    }

    Glyph questionChip() {
        return questionChip;
    }

    /**
     * UC-006 BR-008: a glyph that could land nowhere on the board cannot be dragged. The Step glyph
     * always can; each other one is given why it cannot yet, or null once it has somewhere to go.
     */
    void offer(String sectionUnavailable, String questionUnavailable, String ruleUnavailable) {
        setUnavailable(sectionChip, sectionUnavailable);
        setUnavailable(questionChip, questionUnavailable);
        setUnavailable(showRuleChip, ruleUnavailable);
        setUnavailable(repeatRuleChip, ruleUnavailable);
        setUnavailable(textRuleChip, ruleUnavailable);
    }

    private static void setUnavailable(Glyph chip, String reason) {
        DragSource.configure(chip).setDraggable(reason == null);
        // The attribute too, as the board's handles carry it: it is what the browser and the grab
        // cursor read, whatever became of the property.
        chip.getElement().setAttribute("draggable", String.valueOf(reason == null));
        chip.setUnavailable(reason);
    }

    /** A labeled row inside the tools card: a caption on the left, its draggable chips on the right. */
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
        source.addDragStartListener(e -> onDragStart.accept(item));
        source.addDragEndListener(e -> onDragEnd.run());
        glyph.getElement().setAttribute("aria-roledescription", getTranslation("designerPalette.roledescription"));
    }

    private static Span label(String text) {
        Span s = new Span(text);
        s.addClassName("designer-palette-label");
        return s;
    }
}
