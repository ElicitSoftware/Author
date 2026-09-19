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

import com.elicitsoftware.author.flow.SurveyModelGuideView;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.details.DetailsVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.RouterLink;

/**
 * The designer drawer above the structure tree: the guide link and three collapsible panels —
 * Controls (the palette), Lists (UC-014) and Questions (UC-011 A6). The tree stays at the bottom.
 */
public class DesignerToolbox extends VerticalLayout {

    final RouterLink guide = new RouterLink("How surveys work", SurveyModelGuideView.class);
    /** Filters the Lists and Questions panels by name, text and options as the author types. */
    final TextField search = new TextField();
    final DesignerPalette palette;
    final ListPanel lists;
    final QuestionBankPanel questions;
    final Details controlsPanel;
    final Details listsPanel;
    final Details questionsPanel;

    public DesignerToolbox(SurveyDesignerView view, DesignerPalette palette) {
        this.palette = palette;
        this.lists = new ListPanel(view);
        this.questions = new QuestionBankPanel();
        addClassName("designer-toolbox");
        setPadding(false);
        setSpacing(false);
        setAlignItems(Alignment.STRETCH);
        guide.addClassName("designer-guide-link");
        search.setPlaceholder("Search lists and questions");
        search.setPrefixComponent(VaadinIcon.SEARCH.create());
        search.setClearButtonVisible(true);
        search.setValueChangeMode(ValueChangeMode.LAZY);
        search.setWidthFull();
        search.setAriaLabel("Search lists and questions by name, text or option");
        search.addClassName("designer-search");
        search.addValueChangeListener(e -> applySearch());
        controlsPanel = panel("Controls", palette, true);
        listsPanel = panel("Lists", lists, false);
        questionsPanel = panel("Questions", questions, false);
        add(guide, search, controlsPanel, listsPanel, questionsPanel);
    }

    /** Filter both panels; a search with matches opens the panels that hold them. */
    void applySearch() {
        String q = search.getValue() == null ? "" : search.getValue().trim();
        int listHits = lists.filter(q);
        int questionHits = questions.filter(q);
        listsPanel.setSummaryText(q.isEmpty() ? "Lists" : "Lists (" + listHits + ")");
        questionsPanel.setSummaryText(q.isEmpty() ? "Questions" : "Questions (" + questionHits + ")");
        if (!q.isEmpty()) {
            if (listHits > 0) {
                listsPanel.setOpened(true);
            }
            if (questionHits > 0) {
                questionsPanel.setOpened(true);
            }
        }
    }

    public DesignerPalette palette() {
        return palette;
    }

    private static Details panel(String title, com.vaadin.flow.component.Component body, boolean opened) {
        Details d = new Details(title, body);
        d.addThemeVariants(DetailsVariant.SMALL);
        d.addClassName("designer-toolbox-panel");
        d.setOpened(opened);
        d.setWidthFull();
        return d;
    }
}
