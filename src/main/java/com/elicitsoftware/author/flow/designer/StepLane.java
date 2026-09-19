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
import com.vaadin.flow.component.dnd.DropEffect;
import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.component.dnd.EffectAllowed;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

import java.util.ArrayList;
import java.util.List;

/**
 * One step as a lane: a header (numbered glyph as drag handle, name, section count, menu) that
 * also accepts drops (append), and a body of section cards with an insertion zone before each
 * card and after the last.
 */
public class StepLane extends Div {

    final StructureNode node;
    final Glyph glyph;
    final Div header = new Div();
    final Div body = new Div();
    final H3 name = new H3();
    final Span sectionCount = new Span();
    final Icon toggle;
    final NodeMenu menu;
    final List<SectionCard> cards = new ArrayList<>();

    public StepLane(SurveyDesignerView view, StructureNode node, int order, List<StructureNode> sections, int indexInBoard, int boardSize) {
        this.node = node;
        addClassName("designer-lane");
        getElement().setAttribute("data-node-id", "step-" + node.durableId());
        getElement().setAttribute("role", "group");
        getElement().setAttribute("aria-label", "Step " + order + ": " + node.label());
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

        glyph = Glyph.step(order);
        // The glyph is the visual handle, but the whole header drags so the gesture cannot miss.
        DragSource<Div> source = DragSource.create(header);
        source.setDragData(node);
        source.setEffectAllowed(EffectAllowed.MOVE);
        source.setDraggable(!node.retired());
        source.addDragStartListener(e -> view.dragStarted());
        source.addDragEndListener(e -> view.dragEnded());
        if (!node.retired()) {
            glyph.getElement().setAttribute("draggable", "true");
        }

        toggle = (open ? VaadinIcon.ANGLE_DOWN : VaadinIcon.ANGLE_RIGHT).create();
        toggle.addClassName("designer-card-toggle");
        name.setText(node.label());
        name.addClassName("designer-lane-name");
        sectionCount.setText(sections.size() == 1 ? "1 section" : sections.size() + " sections");
        sectionCount.addClassName("designer-badge");

        menu = new NodeMenu("Actions for step " + node.label());
        menu.item("Edit…", () -> view.edit(node), !node.retired());
        menu.item("Add step after", () -> view.addStepAfter(node), !node.retired());
        menu.item("Add section", () -> view.addChild(node), !node.retired());
        menu.item("Move up", () -> view.move(node, -1), !node.retired() && indexInBoard > 0);
        menu.item("Move down", () -> view.move(node, 1), !node.retired() && indexInBoard < boardSize - 1);
        menu.item("Open details", () -> view.openDetails(node));
        menu.item("Remove step", () -> view.removeStep(node), !node.retired());

        header.addClassName("designer-lane-header");
        header.getElement().setAttribute("role", "button");
        header.getElement().setAttribute("tabindex", "0");
        header.getElement().setAttribute("aria-expanded", String.valueOf(open));
        header.getElement().setAttribute("aria-label", "Step " + order + ": " + node.label() + ", " + sectionCount.getText()
                + (node.retired() ? ", removed" : ""));
        header.add(toggle, glyph, name, sectionCount, menu);
        header.addClickListener(e -> view.toggleAndSelect(node));
        header.getElement().addEventListener("keydown", e -> view.toggleAndSelect(node)).setFilter("event.key === 'Enter' || event.key === ' '");
        DropTarget<Div> headerDrop = DropTarget.create(header);
        headerDrop.setDropEffect(DropEffect.MOVE);
        headerDrop.addDropListener(e -> e.getDragData().ifPresent(data ->
                view.onDrop(data, new DropSpot(node, sections.size(), StructureNode.Kind.SECTION))));

        body.addClassName("designer-lane-body");
        body.setVisible(open);
        // Dropping anywhere in the lane body (not on a card or zone) appends.
        DropTarget<Div> bodyDrop = DropTarget.create(body);
        bodyDrop.setDropEffect(DropEffect.MOVE);
        bodyDrop.addDropListener(e -> e.getDragData().ifPresent(data ->
                view.onDrop(data, new DropSpot(node, sections.size(), StructureNode.Kind.SECTION))));
        for (int i = 0; i < sections.size(); i++) {
            body.add(new DropZone(new DropSpot(node, i, StructureNode.Kind.SECTION), view::onDrop));
            StructureNode section = sections.get(i);
            SectionCard card = new SectionCard(view, section, view.childrenOf(section), i, sections.size(), view.mountCount(section));
            cards.add(card);
            body.add(card);
        }
        body.add(new DropZone(new DropSpot(node, sections.size(), StructureNode.Kind.SECTION), view::onDrop));
        if (sections.isEmpty()) {
            Span empty = new Span("No sections yet — drop a section glyph here or use Add section.");
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
