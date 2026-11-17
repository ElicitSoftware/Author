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

import com.elicitsoftware.author.i18n.Translations;
import com.elicitsoftware.author.survey.StructureNode;
import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.dnd.EffectAllowed;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Drawer panel "Sections" (UC-015 A6): every current section of the survey once, whatever the
 * number of steps it is mounted in — including the sections no step mounts at all, which the board
 * cannot draw (UC-015 A7, UC-006 BR-003a). Its glyph drags into a lane to mount the section there
 * (one more assignment, not a copy); the row menu offers the same through a picker, and Edit… opens
 * the section dialog.
 */
public class SectionBankPanel extends VerticalLayout {

    /** Filters the sections by name and description; scoped to this panel. */
    final TextField search = ListPanel.searchField(Translations.get("sectionBankPanel.search"), Translations.get("sectionBankPanel.search.aria"));
    final Div rows = new Div();
    final List<Row> items = new ArrayList<>();
    private java.util.function.BiConsumer<String, Integer> filtered = (q, n) -> { };

    /** One section in the panel. */
    public final class Row extends Div {
        final StructureNode node;
        final Glyph glyph;
        final Span label = new Span();
        final Span uses = new Span();
        final NodeMenu menu;
        /** Lower-cased name and description, what the drawer search matches against. */
        final String searchText;
        /** False for a section no step mounts: it exports, but nothing on the board holds it. */
        final boolean placed;

        /**
         * @param unavailable why the section cannot be dragged yet (UC-006 BR-008: there is no step
         *                    to mount it in), or null when it can
         */
        Row(SurveyDesignerView view, StructureNode node, int mounts, String description, String unavailable) {
            this.placed = mounts > 0;
            this.node = node;
            this.searchText = (node.label() + " " + (description == null ? "" : description)).toLowerCase(java.util.Locale.ROOT);
            addClassName("designer-panel-row");
            glyph = Glyph.section();
            glyph.getElement().setAttribute("aria-roledescription", getTranslation("sectionBankPanel.roledescription"));
            DragSource<Glyph> source = DragSource.create(glyph);
            source.setDragData(new BankSection(node.durableId(), node.label()));
            source.setEffectAllowed(EffectAllowed.COPY_MOVE);
            source.setDraggable(unavailable == null);
            source.addDragStartListener(e -> view.dragStarted(StructureNode.Kind.SECTION));
            source.addDragEndListener(e -> view.dragEnded());
            if (unavailable == null) {
                glyph.getElement().setAttribute("draggable", "true");
            }
            glyph.setUnavailable(unavailable);
            label.setText(node.label());
            label.addClassName("designer-panel-label");
            label.getElement().setAttribute("data-i18n-content", "");
            uses.setText(placed
                    ? (mounts == 1 ? getTranslation("sectionBankPanel.uses.one") : getTranslation("sectionBankPanel.uses.many", mounts))
                    : getTranslation("sectionBankPanel.uses.none"));
            uses.addClassName("designer-badge");
            uses.addClassName(placed ? "designer-badge-placed" : "designer-badge-unplaced");
            menu = new NodeMenu(getTranslation("sectionBankPanel.menu.aria", node.label()));
            menu.item(getTranslation("sectionBankPanel.menu.mountInStep"), () -> view.mountIn(node), unavailable == null);
            menu.item(getTranslation("designer.menu.edit"), () -> view.edit(node));
            // UC-015 A7: an unplaced section has no assignment whose removal could retire it, so the
            // only way to be rid of one is to remove the element itself.
            if (!placed) {
                menu.item(getTranslation("common.remove"), () -> view.removeUnplaced(node));
            }
            add(glyph, label, uses, menu);
        }

        public StructureNode node() {
            return node;
        }
    }

    public SectionBankPanel() {
        addClassName("designer-panel");
        setPadding(false);
        setSpacing(false);
        setAlignItems(Alignment.STRETCH);
        rows.addClassName("designer-panel-rows");
        search.addValueChangeListener(e -> filter(e.getValue()));
        add(search, rows);
    }

    /** Called after every filter with the query and the number of visible rows (the panel title shows it). */
    void onFiltered(java.util.function.BiConsumer<String, Integer> listener) {
        filtered = listener;
    }

    final Span empty = new Span();
    private String query = "";

    /**
     * One row per section durable id; mounts counted over the current tree, unplaced sections last.
     * {@code unavailable} is why no section can be dragged yet, or null (UC-006 BR-008).
     */
    void refresh(SurveyDesignerView view, List<StructureNode> all, List<StructureNode> unplaced,
                 Map<Integer, String> descriptionBySectionId, String unavailable) {
        rows.removeAll();
        items.clear();
        Map<Integer, StructureNode> first = new LinkedHashMap<>();
        Map<Integer, Integer> mounts = new LinkedHashMap<>();
        for (StructureNode n : all) {
            if (n.kind() != StructureNode.Kind.SECTION || n.retired()) {
                continue;
            }
            first.putIfAbsent(n.durableId(), n);
            mounts.merge(n.durableId(), 1, Integer::sum);
        }
        for (StructureNode n : unplaced) {
            first.putIfAbsent(n.durableId(), n);
            mounts.putIfAbsent(n.durableId(), 0);
        }
        for (StructureNode n : first.values()) {
            Row row = new Row(view, n, mounts.get(n.durableId()), descriptionBySectionId.get(n.durableId()), unavailable);
            items.add(row);
            rows.add(row);
        }
        empty.addClassName("designer-empty-hint");
        rows.add(empty);
        filter(query);
    }

    /**
     * Brings one section into view (UC-007 BR-009): clears any search that hides it and marks the row.
     * Returns false when the panel does not hold that section.
     */
    boolean reveal(int durableId) {
        Row target = items.stream().filter(r -> r.node.durableId() == durableId).findFirst().orElse(null);
        items.forEach(r -> r.removeClassName("designer-panel-row-revealed"));
        if (target == null) {
            return false;
        }
        search.clear();
        filter("");
        target.addClassName("designer-panel-row-revealed");
        target.scrollIntoView();
        return true;
    }

    /** Drawer search: show only the sections whose name or description contain {@code q}; returns the match count. */
    int filter(String q) {
        query = q == null ? "" : q.trim().toLowerCase(java.util.Locale.ROOT);
        int shown = 0;
        for (Row r : items) {
            boolean hit = query.isEmpty() || r.searchText.contains(query);
            r.setVisible(hit);
            if (hit) {
                shown++;
            }
        }
        if (items.isEmpty()) {
            empty.setText(getTranslation("sectionBankPanel.empty"));
        } else if (shown == 0) {
            empty.setText(getTranslation("sectionBankPanel.noMatch", q.trim()));
        }
        empty.setVisible(items.isEmpty() || shown == 0);
        filtered.accept(query, shown);
        return shown;
    }
}
