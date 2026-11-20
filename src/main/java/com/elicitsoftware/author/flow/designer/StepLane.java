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

    /**
     * Builds the lane and its section cards; a retired step is shown but cannot be dragged or edited.
     *
     * @param view         the designer the lane reports its gestures to
     * @param node         the step
     * @param order        the step's 1-based number, shown on its glyph
     * @param sections     the sections mounted in the step, in order
     * @param indexInBoard the step's 0-based position on the board, which enables Move up
     * @param boardSize    how many steps the board shows, which enables Move down
     */
    public StepLane(SurveyDesignerView view, StructureNode node, int order, List<StructureNode> sections, int indexInBoard, int boardSize) {
        this.node = node;
        addClassName("designer-lane");
        getElement().setAttribute("data-node-id", "step-" + node.durableId());
        getElement().setAttribute("role", "group");
        getElement().setAttribute("aria-label", getTranslation("stepLane.aria", order, node.label()));
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
        source.addDragStartListener(e -> view.dragStarted(node.kind()));
        source.addDragEndListener(e -> view.dragEnded());
        if (!node.retired()) {
            glyph.getElement().setAttribute("draggable", "true");
        }

        toggle = (open ? VaadinIcon.ANGLE_DOWN : VaadinIcon.ANGLE_RIGHT).create();
        toggle.addClassName("designer-card-toggle");
        name.setText(node.label());
        name.addClassName("designer-lane-name");
        name.getElement().setAttribute("data-i18n-content", "");
        sectionCount.setText(sections.size() == 1 ? getTranslation("stepLane.sectionCount.one") : getTranslation("stepLane.sectionCount.many", sections.size()));
        sectionCount.addClassName("designer-badge");

        menu = new NodeMenu(getTranslation("stepLane.menu.aria", node.label()));
        menu.item(getTranslation("designer.menu.edit"), () -> view.edit(node), !node.retired());
        menu.item(getTranslation("stepLane.menu.addStepAfter"), () -> view.addStepAfter(node), !node.retired());
        menu.item(getTranslation("stepLane.menu.addSection"), () -> view.addChild(node), !node.retired());
        menu.item(getTranslation("designer.menu.moveUp"), () -> view.move(node, -1), !node.retired() && indexInBoard > 0);
        menu.item(getTranslation("designer.menu.moveDown"), () -> view.move(node, 1), !node.retired() && indexInBoard < boardSize - 1);
        menu.item(getTranslation("designer.menu.openDetails"), () -> view.openDetails(node));
        menu.item(getTranslation("stepLane.menu.removeStep"), () -> view.removeStep(node), !node.retired());

        header.addClassName("designer-lane-header");
        header.getElement().setAttribute("role", "button");
        header.getElement().setAttribute("tabindex", "0");
        header.getElement().setAttribute("aria-expanded", String.valueOf(open));
        header.getElement().setAttribute("aria-label", node.retired()
                ? getTranslation("stepLane.header.aria.removed", order, node.label(), sectionCount.getText())
                : getTranslation("stepLane.header.aria", order, node.label(), sectionCount.getText()));
        header.add(toggle, glyph, name, sectionCount, menu);
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
        DropSpot append = new DropSpot(node, sections.size(), StructureNode.Kind.SECTION);
        DropTarget<Div> headerDrop = DropTarget.create(header);
        headerDrop.setDropEffect(DropEffect.MOVE);
        headerDrop.addDropListener(e -> e.getDragData().ifPresent(data -> view.onDrop(data, append)));
        view.admit(headerDrop, append::takes);

        body.addClassName("designer-lane-body");
        body.setVisible(open);
        // Dropping anywhere in the lane body (not on a card or zone) appends.
        DropTarget<Div> bodyDrop = DropTarget.create(body);
        bodyDrop.setDropEffect(DropEffect.MOVE);
        bodyDrop.addDropListener(e -> e.getDragData().ifPresent(data -> view.onDrop(data, append)));
        view.admit(bodyDrop, append::takes);
        for (int i = 0; i < sections.size(); i++) {
            body.add(new DropZone(view, new DropSpot(node, i, StructureNode.Kind.SECTION)));
            StructureNode section = sections.get(i);
            SectionCard card = new SectionCard(view, section, view.childrenOf(section), i, sections.size(), view.mountCount(section));
            cards.add(card);
            body.add(card);
        }
        body.add(new DropZone(view, append));
        if (sections.isEmpty()) {
            Span empty = new Span(getTranslation("stepLane.empty"));
            empty.addClassName("designer-empty");
            body.add(empty);
        }
        add(header, body);
    }

    /**
     * The step this lane shows.
     *
     * @return the step's node
     */
    public StructureNode node() {
        return node;
    }

    /**
     * Whether the lane's body is showing.
     *
     * @return true when the section cards are visible
     */
    public boolean isOpen() {
        return body.isVisible();
    }
}
