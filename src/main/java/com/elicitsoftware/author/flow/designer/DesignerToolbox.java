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

    final RouterLink guide = new RouterLink(SurveyModelGuideView.class);
    final DesignerPalette palette;
    final ListPanel lists;
    final SectionBankPanel sections;
    final QuestionBankPanel questions;
    final Details controlsPanel;
    final Details listsPanel;
    final Details sectionsPanel;
    final Details questionsPanel;

    /**
     * Builds the toolbox with the Controls panel open and the other three closed.
     *
     * @param view    the designer the Lists panel works for
     * @param palette the palette shown in the Controls panel
     */
    public DesignerToolbox(SurveyDesignerView view, DesignerPalette palette) {
        this.palette = palette;
        this.lists = new ListPanel(view);
        this.sections = new SectionBankPanel();
        this.questions = new QuestionBankPanel();
        addClassName("designer-toolbox");
        setPadding(false);
        setSpacing(false);
        setAlignItems(Alignment.STRETCH);
        guide.setText(getTranslation("designer.guideLink"));
        guide.addClassName("designer-guide-link");
        controlsPanel = panel(getTranslation("designerToolbox.controls"), palette, true);
        listsPanel = panel(getTranslation("designerToolbox.lists"), lists, false);
        sectionsPanel = panel(getTranslation("designerToolbox.sections"), sections, false);
        questionsPanel = panel(getTranslation("designerToolbox.questions"), questions, false);
        // Each panel filters itself; the title shows the match count while a search is active.
        lists.onFiltered((q, hits) -> listsPanel.setSummaryText(q.isEmpty()
                ? getTranslation("designerToolbox.lists") : getTranslation("designerToolbox.listsCount", hits)));
        sections.onFiltered((q, hits) -> sectionsPanel.setSummaryText(q.isEmpty()
                ? getTranslation("designerToolbox.sections") : getTranslation("designerToolbox.sectionsCount", hits)));
        questions.onFiltered((q, hits) -> questionsPanel.setSummaryText(q.isEmpty()
                ? getTranslation("designerToolbox.questions") : getTranslation("designerToolbox.questionsCount", hits)));
        add(guide, controlsPanel, listsPanel, sectionsPanel, questionsPanel);
    }

    /**
     * Returns the palette in the Controls panel.
     *
     * @return the palette
     */
    public DesignerPalette palette() {
        return palette;
    }

    /**
     * Opens the Sections panel on one section and marks it (UC-007 BR-009). This is how a finding
     * about a section no step mounts is followed: the board has nothing to select. For a section the
     * panel does not hold, the panel stays open or closed as it was.
     *
     * @param durableId the section's durable id
     * @return {@code true} when the panel holds the section
     */
    public boolean revealSection(int durableId) {
        boolean found = sections.reveal(durableId);
        sectionsPanel.setOpened(found || sectionsPanel.isOpened());
        return found;
    }

    /**
     * Opens the Questions panel on one question and marks it (UC-007 BR-009). For a question the
     * panel does not hold, the panel stays open or closed as it was.
     *
     * @param durableId the question's durable id
     * @return {@code true} when the panel holds the question
     */
    public boolean revealQuestion(int durableId) {
        boolean found = questions.reveal(durableId);
        questionsPanel.setOpened(found || questionsPanel.isOpened());
        return found;
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
