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
    final Div rows = new Div();
    final List<Row> items = new ArrayList<>();

    /** One list in the panel. */
    public final class Row extends Div {
        final SelectGroup group;
        final Span glyph = new Span();
        final Span label = new Span();
        final Span count = new Span();
        final NodeMenu menu;

        Row(SurveyDesignerView view, SelectGroup group, long options, long users) {
            this.group = group;
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
        add(rows, newList);
    }

    void refresh(SurveyDesignerView view, List<SelectGroup> lists, Map<Integer, Long> optionCounts, Map<Integer, Long> users) {
        rows.removeAll();
        items.clear();
        for (SelectGroup g : lists) {
            Row row = new Row(view, g, optionCounts.getOrDefault(g.selectGroupId, 0L), users.getOrDefault(g.selectGroupId, 0L));
            items.add(row);
            rows.add(row);
        }
        if (lists.isEmpty()) {
            Span none = new Span("No lists yet. A choice question (radio, combo box, multi-select, checkbox group) needs one.");
            none.addClassName("designer-empty-hint");
            rows.add(none);
        }
    }
}
