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
import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.dnd.DropEffect;
import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.component.dnd.EffectAllowed;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

import java.util.List;

/**
 * One section assignment inside a step lane. Closed by default: the header shows the glyph
 * (drag handle), name and counts; clicking it opens the card to reveal the question rows with
 * an insertion zone before each and after the last. The header also accepts drops (append).
 */
public class SectionCard extends Div {

    final StructureNode node;
    final Glyph glyph;
    final Div header = new Div();
    final Div body = new Div();
    final Span name = new Span();
    final Span questionCount = new Span();
    final Span mountBadge = new Span();
    final Span tagBadge = new Span();
    final Span reportBadge = new Span();
    final Icon toggle;
    final NodeMenu menu;
    final List<QuestionRow> rows;

    public SectionCard(SurveyDesignerView view, StructureNode node, List<StructureNode> questions,
                       int indexInLane, int laneSize, int mountedInSteps) {
        this.node = node;
        addClassName("designer-card");
        getElement().setAttribute("data-node-id", "ss-" + node.assignmentId());
        if (node.retired()) {
            addClassName("retired");
        }
        boolean open = view.isOpen(node);
        if (open) {
            addClassName("open");
        }
        if (view.isSelected(node)) {
            getElement().setAttribute("aria-selected", "true");
        }

        glyph = Glyph.section();
        // The glyph is the visual handle, but the whole header drags so the gesture cannot miss.
        DragSource<Div> source = DragSource.create(header);
        source.setDragData(node);
        source.setEffectAllowed(EffectAllowed.MOVE);
        source.setDraggable(!node.retired());
        source.addDragStartListener(e -> view.dragStarted(node.kind()));
        source.addDragEndListener(e -> view.dragEnded());
        if (!node.retired()) {
            glyph.getElement().setAttribute("draggable", "true");
        }

        toggle = (open ? VaadinIcon.ANGLE_DOWN : VaadinIcon.ANGLE_RIGHT).create();
        toggle.addClassName("designer-card-toggle");
        name.setText(node.label());
        name.addClassName("designer-card-name");
        name.getElement().setAttribute("data-i18n-content", "");
        questionCount.setText(questions.size() == 1 ? getTranslation("sectionCard.questionCount.one") : getTranslation("sectionCard.questionCount.many", questions.size()));
        questionCount.addClassName("designer-badge");
        mountBadge.setText(getTranslation("sectionCard.mounted", mountedInSteps));
        mountBadge.addClassName("designer-badge");
        mountBadge.setTitle(getTranslation("sectionCard.mounted.title", mountedInSteps));
        mountBadge.setVisible(mountedInSteps > 1);
        TagBadge.fill(tagBadge, view.tagsOf(node));
        ReportBadge.fill(reportBadge, view.marksOf(node));

        menu = new NodeMenu(getTranslation("sectionCard.menu.aria", node.label()));
        menu.item(getTranslation("designer.menu.edit"), () -> view.edit(node), !node.retired());
        menu.item(getTranslation("sectionCard.menu.addQuestion"), () -> view.addChild(node), !node.retired());
        menu.item(getTranslation("sectionCard.menu.copyToStep"), () -> view.copyTo(node), !node.retired());
        menu.item(getTranslation("sectionCard.menu.alsoMountInStep"), () -> view.mountIn(node), !node.retired());
        menu.item(getTranslation("designer.menu.reportingTags"), () -> view.reportingTags(node), !node.retired());
        menu.item(getTranslation("designer.menu.reportMarks"), () -> view.reportMarks(node), !node.retired());
        menu.item(getTranslation("designer.menu.moveUp"), () -> view.move(node, -1), !node.retired() && indexInLane > 0);
        menu.item(getTranslation("designer.menu.moveDown"), () -> view.move(node, 1), !node.retired() && indexInLane < laneSize - 1);
        menu.item(getTranslation("designer.menu.openDetails"), () -> view.openDetails(node));
        menu.item(getTranslation("sectionCard.menu.removeFromStep"), () -> view.removeAssignment(node), !node.retired());

        header.addClassName("designer-card-header");
        header.getElement().setAttribute("role", "button");
        header.getElement().setAttribute("tabindex", "0");
        header.getElement().setAttribute("aria-expanded", String.valueOf(open));
        header.getElement().setAttribute("aria-label", node.retired()
                ? getTranslation("sectionCard.aria.removed", node.label(), questionCount.getText())
                : getTranslation("sectionCard.aria", node.label(), questionCount.getText()));
        header.add(toggle, glyph, name, questionCount, mountBadge, tagBadge, reportBadge, menu);
        // Single click toggles and selects; double click edits (UC-018 step 1). Told apart on the
        // client, as QuestionRow does: a debounced single vs. an immediate double, so the board
        // rebuild after a single click never detaches the header mid-gesture and eats the double.
        header.getElement().executeJs(
                "let t;"
                        + "this.addEventListener('click', function(){ clearTimeout(t); t = setTimeout(function(){ this.dispatchEvent(new CustomEvent('header-toggle')); }.bind(this), 220); });"
                        + "this.addEventListener('dblclick', function(){ clearTimeout(t); this.dispatchEvent(new CustomEvent('header-edit')); });");
        header.getElement().addEventListener("header-toggle", e -> view.toggleAndSelect(node));
        header.getElement().addEventListener("header-edit", e -> {
            if (!node.retired()) {
                view.edit(node);
            }
        });
        header.getElement().addEventListener("keydown", e -> view.toggleAndSelect(node)).setFilter("event.key === 'Enter' || event.key === ' '"); // i18n:ignore
        DropSpot append = new DropSpot(node, questions.size(), StructureNode.Kind.QUESTION);
        DropTarget<Div> headerDrop = DropTarget.create(header);
        headerDrop.setDropEffect(DropEffect.MOVE);
        headerDrop.addDropListener(e -> e.getDragData().ifPresent(data -> view.onDrop(data, append)));
        view.admit(headerDrop, append::takes);

        body.addClassName("designer-card-body");
        body.setVisible(open);
        // Dropping anywhere in the open card's body (not on a row or zone) appends.
        DropTarget<Div> bodyDrop = DropTarget.create(body);
        bodyDrop.setDropEffect(DropEffect.MOVE);
        bodyDrop.addDropListener(e -> e.getDragData().ifPresent(data -> view.onDrop(data, append)));
        view.admit(bodyDrop, append::takes);
        rows = new java.util.ArrayList<>();
        for (int i = 0; i < questions.size(); i++) {
            body.add(new DropZone(view, new DropSpot(node, i, StructureNode.Kind.QUESTION)));
            QuestionRow row = new QuestionRow(view, questions.get(i), i, questions.size());
            rows.add(row);
            body.add(row);
        }
        body.add(new DropZone(view, append));
        if (questions.isEmpty()) {
            Span empty = new Span(getTranslation("sectionCard.empty"));
            empty.addClassName("designer-empty");
            body.add(empty);
        }
        add(header, body);
    }

    public StructureNode node() {
        return node;
    }

    public boolean isOpen() {
        return body.isVisible();
    }
}
