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

import com.elicitsoftware.author.survey.StructureNode;
import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.dnd.EffectAllowed;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Drawer panel "Questions" (UC-011 A6): every current question of the survey once, whatever the
 * number of sections it sits in. Its glyph drags into a card to reuse the question there; the row
 * menu offers the same through a picker, and Edit… opens the question dialog.
 */
public class QuestionBankPanel extends VerticalLayout {

    final Div rows = new Div();
    final List<Row> items = new ArrayList<>();

    /** One question in the panel. */
    public final class Row extends Div {
        final StructureNode node;
        final Glyph glyph;
        final Span label = new Span();
        final Span uses = new Span();
        final NodeMenu menu;

        Row(SurveyDesignerView view, StructureNode node, int placements) {
            this.node = node;
            addClassName("designer-panel-row");
            glyph = Glyph.question(node.detail());
            glyph.getElement().setAttribute("draggable", "true");
            glyph.getElement().setAttribute("aria-roledescription", "draggable question");
            DragSource<Glyph> source = DragSource.create(glyph);
            source.setDragData(new BankQuestion(node.durableId(), node.label(), node.detail()));
            source.setEffectAllowed(EffectAllowed.COPY_MOVE);
            source.addDragStartListener(e -> view.dragStarted());
            source.addDragEndListener(e -> view.dragEnded());
            label.setText(node.label());
            label.addClassName("designer-panel-label");
            uses.setText("in " + placements + (placements == 1 ? " section" : " sections"));
            uses.addClassName("designer-badge");
            menu = new NodeMenu("Actions for question " + node.label());
            menu.item("Add to section…", () -> view.addBankQuestionTo(node));
            menu.item("Edit…", () -> view.edit(node));
            add(glyph, label, uses, menu);
        }

        public StructureNode node() {
            return node;
        }
    }

    public QuestionBankPanel() {
        addClassName("designer-panel");
        setPadding(false);
        setSpacing(false);
        setAlignItems(Alignment.STRETCH);
        rows.addClassName("designer-panel-rows");
        add(rows);
    }

    /** One row per question durable id; placements counted over the current tree. */
    void refresh(SurveyDesignerView view, List<StructureNode> all) {
        rows.removeAll();
        items.clear();
        Map<Integer, StructureNode> first = new LinkedHashMap<>();
        Map<Integer, Integer> placements = new LinkedHashMap<>();
        for (StructureNode n : all) {
            if (n.kind() != StructureNode.Kind.QUESTION || n.retired()) {
                continue;
            }
            first.putIfAbsent(n.durableId(), n);
            placements.merge(n.durableId(), 1, Integer::sum);
        }
        for (StructureNode n : first.values()) {
            Row row = new Row(view, n, placements.get(n.durableId()));
            items.add(row);
            rows.add(row);
        }
        if (first.isEmpty()) {
            Span none = new Span("No questions yet. Questions you add appear here and can be reused in other sections.");
            none.addClassName("designer-empty-hint");
            rows.add(none);
        }
    }
}
