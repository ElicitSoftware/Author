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

import com.elicitsoftware.model.SelectGroup;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.dnd.EffectAllowed;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.value.ValueChangeMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Drawer panel "Lists" (UC-014): the survey's lists of options. Each row's glyph drags onto a
 * choice question to give it those options; the row menu edits or removes the list; New list…
 * opens the dialog. The question dialog's picker is the keyboard route (NFR-012).
 */
public class ListPanel extends VerticalLayout {

    final Button newList = new Button("New list…", VaadinIcon.PLUS.create());
    /** Filters the lists by name, description and option text; scoped to this panel. */
    final TextField search = searchField("Search lists", "Search lists by name, description or option text");
    final Div rows = new Div();
    final List<Row> items = new ArrayList<>();
    private java.util.function.BiConsumer<String, Integer> filtered = (q, n) -> { };

    /** One list in the panel. */
    public final class Row extends Div {
        final SelectGroup group;
        final Span glyph = new Span();
        final Span label = new Span();
        final Span count = new Span();
        final NodeMenu menu;
        /** Lower-cased name, description and option texts, what the drawer search matches against. */
        final String searchText;

        Row(SurveyDesignerView view, SelectGroup group, long options, long users, List<String> optionTexts) {
            this.group = group;
            this.searchText = (group.name + " " + (group.description == null ? "" : group.description) + " "
                    + String.join(" ", optionTexts)).toLowerCase(java.util.Locale.ROOT);
            addClassName("designer-panel-row");
            glyph.add(new Icon(VaadinIcon.LIST));
            glyph.addClassName("designer-panel-glyph");
            glyph.getElement().setAttribute("aria-roledescription", "draggable list");
            glyph.getElement().setAttribute("draggable", "true");
            DragSource<Span> source = DragSource.create(glyph);
            source.setDragData(new PaletteList(group.selectGroupId, group.name));
            source.setEffectAllowed(EffectAllowed.COPY_MOVE);
            source.addDragStartListener(e -> view.dragStarted());
            source.addDragEndListener(e -> view.dragEnded());
            label.setText(group.name);
            label.addClassName("designer-panel-label");
            count.setText(options + (options == 1 ? " option" : " options") + (users > 0 ? " · " + users + (users == 1 ? " question" : " questions") : ""));
            count.addClassName("designer-badge");
            menu = new NodeMenu("Actions for list " + group.name);
            menu.item("Edit…", () -> view.editList(group));
            menu.item("Remove", () -> view.removeList(group), users == 0);
            add(glyph, label, count, menu);
        }

        public SelectGroup group() {
            return group;
        }
    }

    public ListPanel(SurveyDesignerView view) {
        addClassName("designer-panel");
        setPadding(false);
        setSpacing(false);
        setAlignItems(Alignment.STRETCH);
        newList.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        newList.addClickListener(e -> view.newList());
        rows.addClassName("designer-panel-rows");
        search.addValueChangeListener(e -> filter(e.getValue()));
        add(search, rows, newList);
    }

    /** Called after every filter with the query and the number of visible rows (the panel title shows it). */
    void onFiltered(java.util.function.BiConsumer<String, Integer> listener) {
        filtered = listener;
    }

    /** The panel's own search box: filters this panel only, as the author types. */
    static TextField searchField(String placeholder, String ariaLabel) {
        TextField f = new TextField();
        f.setPlaceholder(placeholder);
        f.setPrefixComponent(VaadinIcon.SEARCH.create());
        f.setClearButtonVisible(true);
        f.setValueChangeMode(ValueChangeMode.LAZY);
        f.setWidthFull();
        f.setAriaLabel(ariaLabel);
        f.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        f.addClassName("designer-search");
        return f;
    }

    final Span empty = new Span();
    private String query = "";

    void refresh(SurveyDesignerView view, List<SelectGroup> lists, Map<Integer, Long> optionCounts, Map<Integer, Long> users,
                 Map<Integer, List<String>> optionTexts) {
        rows.removeAll();
        items.clear();
        for (SelectGroup g : lists) {
            Row row = new Row(view, g, optionCounts.getOrDefault(g.selectGroupId, 0L), users.getOrDefault(g.selectGroupId, 0L),
                    optionTexts.getOrDefault(g.selectGroupId, List.of()));
            items.add(row);
            rows.add(row);
        }
        empty.addClassName("designer-empty-hint");
        rows.add(empty);
        filter(query);
    }

    /** Drawer search: show only the lists whose name, description or options contain {@code q}; returns the match count. */
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
            empty.setText("No lists yet. A choice question (radio, combo box, multi-select, checkbox group) needs one.");
        } else if (shown == 0) {
            empty.setText("No list matches '" + q.trim() + "'.");
        }
        empty.setVisible(items.isEmpty() || shown == 0);
        filtered.accept(query, shown);
        return shown;
    }
}
