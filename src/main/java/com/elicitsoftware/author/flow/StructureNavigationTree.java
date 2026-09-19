package com.elicitsoftware.author.flow;

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
import com.elicitsoftware.author.survey.SurveyStructureQuery;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import com.vaadin.flow.router.RouteParameters;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Drawer navigation for a survey (UC-006): steps → sections → questions with the current element
 * highlighted, after Survey's {@code SectionNavigationTreeGrid}. Selecting a node navigates to
 * {@code survey/:id/:kind/:durableId}; the highlighted row is derived from the address, never from
 * click history (UC-006 BR-003).
 */
public class StructureNavigationTree extends VerticalLayout {

    final TreeGrid<StructureNode> grid = new TreeGrid<>();
    final Checkbox showRemoved = new Checkbox("Show removed");
    private final TreeData<StructureNode> data = new TreeData<>();
    private final TreeDataProvider<StructureNode> provider = new TreeDataProvider<>(data);

    private final int surveyId;
    private final SurveyStructureQuery query;
    private StructureNode.Kind currentKind;
    private Integer currentDurableId;
    private Consumer<StructureNode> selectionHandler;
    private boolean suppressSelection;

    public StructureNavigationTree(int surveyId, SurveyStructureQuery query, Consumer<StructureNode> onSelect) {
        this.surveyId = surveyId;
        this.query = query;
        setPadding(false);
        setSpacing(false);
        // Width only: the tree grows to its rows so the drawer scrolls as one surface (palette,
        // lists, questions and tree together) instead of the grid scrolling inside a fixed box.
        setWidthFull();
        addClassName("structure-navigation");

        grid.setDataProvider(provider);
        // Display order is the whole point of the tree (UC-006 step 2): a sortable header would let a
        // click re-sort it alphabetically.
        grid.addHierarchyColumn(StructureNode::label).setHeader("Survey structure").setFlexGrow(1).setSortable(false);
        grid.setPartNameGenerator(n -> {
            StringBuilder parts = new StringBuilder();
            if (isCurrent(n)) {
                parts.append("current-element-row ");
            }
            if (n.retired()) {
                parts.append("retired-element-row");
            }
            return parts.isEmpty() ? null : parts.toString().trim();
        });
        grid.addClassName("structure-navigation-tree");
        grid.setWidthFull();
        grid.setAllRowsVisible(true);
        grid.addSelectionListener(e -> e.getFirstSelectedItem().ifPresent(n -> {
            if (suppressSelection) {
                return; // a programmatic selection mirroring the board; don't echo it back
            }
            onSelect.accept(n);
            if (selectionHandler != null) {
                selectionHandler.accept(n);
                return;
            }
            getUI().ifPresent(ui -> ui.navigate(ElementView.class, new RouteParameters(
                    java.util.Map.of(SurveyEditorView.SURVEY_ID, String.valueOf(surveyId),
                            ElementView.KIND, n.routeKind(), ElementView.DURABLE_ID, String.valueOf(n.durableId())))));
        }));
        showRemoved.addValueChangeListener(e -> refresh());
        add(showRemoved, grid);
        refresh();
    }

    public void refresh() {
        data.clear();
        SurveyStructureQuery.Tree tree = query.tree(surveyId, Boolean.TRUE.equals(showRemoved.getValue()));
        for (StructureNode step : tree.roots()) {
            data.addItem(null, step);
            for (StructureNode section : tree.childrenOf(step)) {
                data.addItem(step, section);
                for (StructureNode q : tree.childrenOf(section)) {
                    data.addItem(section, q);
                }
            }
        }
        provider.refreshAll();
        grid.expand(tree.all());
    }

    /** Called from the layout after navigation so the highlighted row matches the address. */
    /** Replaces navigation on selection (null restores it); the designer uses it to select on the board. */
    public void setSelectionHandler(Consumer<StructureNode> handler) {
        this.selectionHandler = handler;
    }

    public void highlight(StructureNode.Kind kind, Integer durableId) {
        this.currentKind = kind;
        this.currentDurableId = durableId;
        provider.refreshAll();
        // Mirror the board's selection into the tree: select the row and scroll to it if off-screen.
        StructureNode target = findNode(kind, durableId);
        suppressSelection = true;
        try {
            if (target != null) {
                grid.select(target);
                grid.scrollToItem(target);
            } else {
                grid.deselectAll();
            }
        } finally {
            suppressSelection = false;
        }
    }

    private StructureNode findNode(StructureNode.Kind kind, Integer durableId) {
        if (kind == null || durableId == null) {
            return null;
        }
        return findIn(data.getRootItems(), kind, durableId);
    }

    private StructureNode findIn(java.util.List<StructureNode> items, StructureNode.Kind kind, Integer durableId) {
        for (StructureNode n : items) {
            if (n.kind() == kind && Objects.equals(n.durableId(), durableId)) {
                return n;
            }
            StructureNode child = findIn(data.getChildren(n), kind, durableId);
            if (child != null) {
                return child;
            }
        }
        return null;
    }

    boolean isCurrent(StructureNode n) {
        return currentKind == n.kind() && Objects.equals(currentDurableId, n.durableId());
    }

    public int surveyId() {
        return surveyId;
    }
}
