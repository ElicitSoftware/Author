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
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.dnd.DropEffect;
import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.component.dnd.EffectAllowed;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

/**
 * One question assignment inside a section card. The glyph is the drag handle (UC-016,
 * UC-011 A4); the row itself is focusable and selects on click or Enter; the menu carries
 * the keyboard equivalents.
 */
public class QuestionRow extends Div {

    final StructureNode node;
    final Glyph glyph;
    final Span label = new Span();
    final Span type = new Span();
    final NodeMenu menu;

    public QuestionRow(SurveyDesignerView view, StructureNode node, int indexInSection, int sectionSize) {
        this.node = node;
        addClassName("designer-row");
        getElement().setAttribute("data-node-id", "sq-" + node.assignmentId());
        getElement().setAttribute("tabindex", "0");
        getElement().setAttribute("role", "button");
        getElement().setAttribute("aria-label", "Question " + node.label() + (node.retired() ? " (removed)" : ""));
        if (node.retired()) {
            addClassName("retired");
        }
        if (view.isSelected(node)) {
            getElement().setAttribute("aria-selected", "true");
        }

        glyph = Glyph.question(node.detail());
        DragSource<Glyph> source = DragSource.create(glyph);
        source.setDragData(node);
        source.setEffectAllowed(EffectAllowed.MOVE);
        source.setDraggable(!node.retired());
        source.addDragStartListener(e -> view.dragStarted());
        source.addDragEndListener(e -> view.dragEnded());

        label.setText(node.label());
        label.addClassName("designer-row-label");
        type.setText(node.detail() == null ? "" : node.detail().toLowerCase());
        type.addClassName("structure-kind");

        menu = new NodeMenu("Actions for question " + node.label());
        menu.item("Edit…", () -> view.edit(node), !node.retired());
        menu.item("Copy to section…", () -> view.copyTo(node), !node.retired());
        menu.item("Move up", () -> view.move(node, -1), !node.retired() && indexInSection > 0);
        menu.item("Move down", () -> view.move(node, 1), !node.retired() && indexInSection < sectionSize - 1);
        menu.item("Add rule…", () -> view.addRule(node), !node.retired());
        menu.item("Open details", () -> view.openDetails(node));
        menu.item("Remove from this section", () -> view.removeAssignment(node), !node.retired());

        // UC-019 step 1: a rule glyph dropped on the row starts a rule that reads this question.
        DropTarget<QuestionRow> ruleDrop = DropTarget.create(this);
        ruleDrop.setDropEffect(DropEffect.COPY);
        ruleDrop.addDropListener(e -> e.getDragData().ifPresent(data -> {
            if (data instanceof PaletteItem item && item.isRule() && !node.retired()) {
                view.startRule(item.action(), node);
            }
        }));

        add(glyph, label, type, menu);
        addClickListener(e -> view.select(node));
        getElement().addEventListener("keydown", e -> view.select(node)).setFilter("event.key === 'Enter' || event.key === ' '");
    }

    public StructureNode node() {
        return node;
    }
}
