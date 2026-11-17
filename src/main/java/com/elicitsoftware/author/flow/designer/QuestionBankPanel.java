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
 * Drawer panel "Questions" (UC-011 A6): every current question of the survey once, whatever the
 * number of sections it sits in — including the questions no section assigns at all, which the
 * board cannot draw (UC-011 A7, UC-006 BR-003a). Its glyph drags into a card to reuse the question
 * there; the row menu offers the same through a picker, and Edit… opens the question dialog.
 */
public class QuestionBankPanel extends VerticalLayout {

    /** Filters the questions by short name, question text and type; scoped to this panel. */
    final TextField search = ListPanel.searchField(Translations.get("questionBankPanel.search"), Translations.get("questionBankPanel.search.aria"));
    final Div rows = new Div();
    final List<Row> items = new ArrayList<>();
    private java.util.function.BiConsumer<String, Integer> filtered = (q, n) -> { };

    /** One question in the panel. */
    public final class Row extends Div {
        final StructureNode node;
        final Glyph glyph;
        final Span label = new Span();
        final Span uses = new Span();
        final NodeMenu menu;
        /** Lower-cased short text, question text and type, what the drawer search matches against. */
        final String searchText;
        /** False for a question no section assigns: it exports, but nothing on the board holds it. */
        final boolean placed;

        /**
         * @param unavailable why the question cannot be dragged yet (UC-006 BR-008: there is no
         *                    section to add it to), or null when it can
         */
        Row(SurveyDesignerView view, StructureNode node, int placements, String questionText, String unavailable) {
            this.node = node;
            this.placed = placements > 0;
            this.searchText = (node.label() + " " + (questionText == null ? "" : questionText.replaceAll("<[^>]+>", " ")) + " "
                    + (node.detail() == null ? "" : node.detail())).toLowerCase(java.util.Locale.ROOT);
            addClassName("designer-panel-row");
            glyph = Glyph.question(node.detail());
            glyph.getElement().setAttribute("aria-roledescription", getTranslation("questionBankPanel.roledescription"));
            DragSource<Glyph> source = DragSource.create(glyph);
            source.setDragData(new BankQuestion(node.durableId(), node.label(), node.detail()));
            source.setEffectAllowed(EffectAllowed.COPY_MOVE);
            source.setDraggable(unavailable == null);
            source.addDragStartListener(e -> view.dragStarted(StructureNode.Kind.QUESTION));
            source.addDragEndListener(e -> view.dragEnded());
            if (unavailable == null) {
                glyph.getElement().setAttribute("draggable", "true");
            }
            glyph.setUnavailable(unavailable);
            label.setText(node.label());
            label.addClassName("designer-panel-label");
            label.getElement().setAttribute("data-i18n-content", "");
            uses.setText(placed
                    ? (placements == 1 ? getTranslation("questionBankPanel.uses.one") : getTranslation("questionBankPanel.uses.many", placements))
                    : getTranslation("questionBankPanel.uses.none"));
            uses.addClassName("designer-badge");
            uses.addClassName(placed ? "designer-badge-placed" : "designer-badge-unplaced");
            menu = new NodeMenu(getTranslation("questionBankPanel.menu.aria", node.label()));
            menu.item(getTranslation("questionBankPanel.menu.addToSection"), () -> view.addBankQuestionTo(node), unavailable == null);
            menu.item(getTranslation("designer.menu.edit"), () -> view.edit(node));
            // UC-011 A7: an unassigned question has no assignment whose removal could retire it.
            if (!placed) {
                menu.item(getTranslation("common.remove"), () -> view.removeUnplaced(node));
            }
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
     * One row per question durable id; placements counted over the current tree, unplaced questions
     * last. {@code unavailable} is why no question can be dragged yet, or null (UC-006 BR-008).
     */
    void refresh(SurveyDesignerView view, List<StructureNode> all, List<StructureNode> unplaced,
                 Map<Integer, String> textByQuestionId, String unavailable) {
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
        for (StructureNode n : unplaced) {
            first.putIfAbsent(n.durableId(), n);
            placements.putIfAbsent(n.durableId(), 0);
        }
        for (StructureNode n : first.values()) {
            Row row = new Row(view, n, placements.get(n.durableId()), textByQuestionId.get(n.durableId()), unavailable);
            items.add(row);
            rows.add(row);
        }
        empty.addClassName("designer-empty-hint");
        rows.add(empty);
        filter(query);
    }

    /**
     * Brings one question into view (UC-007 BR-009): clears any search that hides it and marks the row.
     * Returns false when the panel does not hold that question.
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

    /** Drawer search: show only the questions whose name, text or type contain {@code q}; returns the match count. */
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
            empty.setText(getTranslation("questionBankPanel.empty"));
        } else if (shown == 0) {
            empty.setText(getTranslation("questionBankPanel.noMatch", q.trim()));
        }
        empty.setVisible(items.isEmpty() || shown == 0);
        filtered.accept(query, shown);
        return shown;
    }
}
