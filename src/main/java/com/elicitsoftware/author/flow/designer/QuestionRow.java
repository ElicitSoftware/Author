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

    final Span tagBadge = new Span();

    public QuestionRow(SurveyDesignerView view, StructureNode node, int indexInSection, int sectionSize) {
        this.node = node;
        addClassName("designer-row");
        getElement().setAttribute("data-node-id", "sq-" + node.assignmentId());
        getElement().setAttribute("tabindex", "0");
        getElement().setAttribute("role", "button");
        getElement().setAttribute("aria-label", node.retired()
                ? getTranslation("questionRow.aria.removed", node.label()) : getTranslation("questionRow.aria", node.label()));
        if (node.retired()) {
            addClassName("retired");
        }
        if (view.isSelected(node)) {
            getElement().setAttribute("aria-selected", "true");
        }

        glyph = Glyph.question(node.detail());
        // The glyph is the visual handle, but the whole row drags so the gesture cannot miss.
        DragSource<QuestionRow> source = DragSource.create(this);
        source.setDragData(node);
        source.setEffectAllowed(EffectAllowed.MOVE);
        source.setDraggable(!node.retired());
        source.addDragStartListener(e -> view.dragStarted(node.kind()));
        source.addDragEndListener(e -> view.dragEnded());
        if (!node.retired()) {
            glyph.getElement().setAttribute("draggable", "true");
        }

        label.setText(node.label());
        label.addClassName("designer-row-label");
        label.getElement().setAttribute("data-i18n-content", "");
        type.setText(node.detail() == null ? "" : node.detail().toLowerCase());
        type.addClassName("structure-kind");
        type.getElement().setAttribute("data-i18n-content", "");
        TagBadge.fill(tagBadge, view.tagsOf(node));

        menu = new NodeMenu(getTranslation("questionRow.menu.aria", node.label()));
        menu.item(getTranslation("designer.menu.edit"), () -> view.edit(node), !node.retired());
        menu.item(getTranslation("questionRow.menu.alsoAddToSection"), () -> view.addBankQuestionTo(node), !node.retired());
        menu.item(getTranslation("designer.menu.moveUp"), () -> view.move(node, -1), !node.retired() && indexInSection > 0);
        menu.item(getTranslation("designer.menu.moveDown"), () -> view.move(node, 1), !node.retired() && indexInSection < sectionSize - 1);
        menu.item(getTranslation("questionRow.menu.addRule"), () -> view.addRule(node), !node.retired());
        menu.item(getTranslation("designer.menu.reportingTags"), () -> view.reportingTags(node), !node.retired());
        menu.item(getTranslation("designer.menu.openDetails"), () -> view.openDetails(node));
        menu.item(getTranslation("questionRow.menu.removeFromSection"), () -> view.removeAssignment(node), !node.retired());

        // The row itself is a drop target: a rule glyph starts a rule that reads this question
        // (UC-019 step 1); a question glyph or palette question lands *before* this row, so the
        // natural gesture of dropping onto a row works as well as hitting the thin zone above it.
        DropTarget<QuestionRow> drop = DropTarget.create(this);
        drop.setDropEffect(DropEffect.MOVE);
        drop.addDropListener(e -> e.getDragData().ifPresent(data -> {
            if (node.retired()) {
                return;
            }
            if (data instanceof PaletteItem item && item.isRule()) {
                view.startRule(item.action(), node);
            } else if (data instanceof PaletteList list) {
                view.useList(node, list);
            } else {
                view.onDrop(data, new DropSpot(node.parent(), indexInSection, StructureNode.Kind.QUESTION));
            }
        }));

        add(glyph, label, type, tagBadge, menu);
        // Single click selects; double click opens the edit screen. The distinction is made on the
        // client (a debounced single vs. an immediate double) and delivered as two custom events, so
        // the single-click rebuild never detaches the row mid-gesture and breaks the double click.
        getElement().executeJs(
                "let t;"
                        + "this.addEventListener('click', function(){ clearTimeout(t); t = setTimeout(function(){ this.dispatchEvent(new CustomEvent('row-select')); }.bind(this), 220); });"
                        + "this.addEventListener('dblclick', function(){ clearTimeout(t); this.dispatchEvent(new CustomEvent('row-edit')); });");
        getElement().addEventListener("row-select", e -> view.select(node));
        getElement().addEventListener("row-edit", e -> {
            if (!node.retired()) {
                view.edit(node);
            }
        });
        getElement().addEventListener("keydown", e -> view.select(node)).setFilter("event.key === 'Enter' || event.key === ' '"); // i18n:ignore
    }

    public StructureNode node() {
        return node;
    }
}
