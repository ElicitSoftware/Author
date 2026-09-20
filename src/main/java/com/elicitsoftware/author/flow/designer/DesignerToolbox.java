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

import com.elicitsoftware.author.flow.SurveyModelGuideView;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.details.DetailsVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouterLink;

/**
 * The designer drawer above the structure tree: the guide link and four collapsible panels —
 * Controls (the palette), Lists (UC-014), Sections (UC-015 A6) and Questions (UC-011 A6). The tree
 * stays at the bottom.
 */
public class DesignerToolbox extends VerticalLayout {

    final RouterLink guide = new RouterLink("How surveys work", SurveyModelGuideView.class);
    final DesignerPalette palette;
    final ListPanel lists;
    final SectionBankPanel sections;
    final QuestionBankPanel questions;
    final Details controlsPanel;
    final Details listsPanel;
    final Details sectionsPanel;
    final Details questionsPanel;

    public DesignerToolbox(SurveyDesignerView view, DesignerPalette palette) {
        this.palette = palette;
        this.lists = new ListPanel(view);
        this.sections = new SectionBankPanel();
        this.questions = new QuestionBankPanel();
        addClassName("designer-toolbox");
        setPadding(false);
        setSpacing(false);
        setAlignItems(Alignment.STRETCH);
        guide.addClassName("designer-guide-link");
        controlsPanel = panel("Controls", palette, true);
        listsPanel = panel("Lists", lists, false);
        sectionsPanel = panel("Sections", sections, false);
        questionsPanel = panel("Questions", questions, false);
        // Each panel filters itself; the title shows the match count while a search is active.
        lists.onFiltered((q, hits) -> listsPanel.setSummaryText(q.isEmpty() ? "Lists" : "Lists (" + hits + ")"));
        sections.onFiltered((q, hits) -> sectionsPanel.setSummaryText(q.isEmpty() ? "Sections" : "Sections (" + hits + ")"));
        questions.onFiltered((q, hits) -> questionsPanel.setSummaryText(q.isEmpty() ? "Questions" : "Questions (" + hits + ")"));
        add(guide, controlsPanel, listsPanel, sectionsPanel, questionsPanel);
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
