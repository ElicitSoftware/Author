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

import com.elicitsoftware.author.flow.MainLayout;
import com.elicitsoftware.author.flow.StructureNavigationTree;
import com.elicitsoftware.author.flow.SurveyEditorView;
import com.elicitsoftware.author.flow.SurveysView;
import com.elicitsoftware.author.survey.DisplayOrdering;
import com.elicitsoftware.author.survey.StructureNode;
import com.elicitsoftware.model.Step;
import com.elicitsoftware.model.StructuralElement;
import com.elicitsoftware.model.Survey;
import com.elicitsoftware.test.PostgresTestResource;
import com.vaadin.browserless.quarkus.QuarkusBrowserlessTest;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dnd.DragStartEvent;
import com.vaadin.flow.component.dnd.DropEvent;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class SurveyDesignerViewTest extends QuarkusBrowserlessTest {

    @Inject
    DisplayOrdering ordering;

    private Survey fixture() {
        return Survey.find("name", "AuthorFixture").firstResult();
    }

    private SurveyDesignerView open(Survey s) {
        return navigate(SurveyDesignerView.class, Map.of(SurveyEditorView.SURVEY_ID, String.valueOf(s.id)));
    }

    private MainLayout layout() {
        return (MainLayout) UI.getCurrent().getChildren().filter(MainLayout.class::isInstance).findFirst().orElseThrow();
    }

    private DropZone zone(SurveyDesignerView view, StructureNode container, int index, StructureNode.Kind accepts) {
        return find(DropZone.class, view).all().stream()
                .filter(z -> z.spot().accepts() == accepts && z.spot().index() == index
                        && (container == null ? z.spot().container() == null
                        : z.spot().container() != null && z.spot().container().durableId().equals(container.durableId())))
                .findFirst().orElseThrow();
    }

    @Test
    @TestSecurity(user = "designer.author", roles = {"elicit_author"})
    void drawsLanesWithClosedCardsAndKeepsTheTreeInTheDrawer() {
        SurveyDesignerView view = open(fixture());

        assertEquals(2, view.lanes.size(), "UC-006/UC-016: one lane per step in display order");
        assertEquals("Fixture Step One", view.lanes.get(0).node().label());
        assertTrue(view.lanes.stream().noneMatch(StepLane::isOpen), "steps start collapsed");
        // Expand both steps so their sections (and section-level insertion zones) render.
        view.toggleAndSelect(view.lanes.get(0).node());
        view.toggleAndSelect(view.lanes.get(1).node());

        StepLane first = view.lanes.get(0);
        assertEquals(2, first.cards.size());
        assertTrue(first.cards.stream().noneMatch(SectionCard::isOpen), "cards start closed");
        assertEquals("1", first.glyph.getText(), "the step glyph shows its order");
        assertNotNull(view.palette.questionChip(), "palette carries a draggable question glyph");

        StructureNavigationTree tree = find(StructureNavigationTree.class, layout()).first();
        assertEquals(fixture().id, tree.surveyId(), "UC-029 / user: the structure tree stays in the drawer");
        assertTrue(find(DropZone.class, view).all().size() >= 3 + 3 + 2, "insertion zones between steps and sections");
    }

    @Test
    @TestSecurity(user = "designer.author", roles = {"elicit_author"})
    void clickingACardOpensItSelectsItAndUpdatesTheBreadcrumb() {
        SurveyDesignerView view = open(fixture());
        SectionCard card = view.lanes.get(0).cards.get(0);

        view.toggleAndSelect(card.node());

        SectionCard reopened = view.lanes.get(0).cards.get(0);
        assertTrue(reopened.isOpen(), "the card reveals its questions");
        assertEquals(2, reopened.rows.size());
        assertTrue(view.isSelected(reopened.node()));
        String crumbs = view.breadcrumb.getChildren().map(c -> c.getElement().getText()).reduce("", String::concat);
        assertTrue(crumbs.contains("Step: Fixture Step One") && crumbs.contains("Section: Fixture Section A"), crumbs);

        view.select(reopened.rows.get(1).node());
        crumbs = view.breadcrumb.getChildren().map(c -> c.getElement().getText()).reduce("", String::concat);
        assertTrue(crumbs.contains("Question: FixtureQ2"), crumbs);
        assertTrue(view.lanes.get(0).cards.get(0).isOpen(), "the selected question's card stays open");
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "designer.author", roles = {"elicit_author"})
    void draggingAStepGlyphOntoTheFirstZoneReordersTheSteps() {
        Survey s = fixture();
        SurveyDesignerView view = open(s);
        StepLane second = view.lanes.get(1);

        ComponentUtil.fireEvent(second.header, new DragStartEvent<>(second.header, true)); // the whole header drags; the glyph is its handle
        assertTrue(view.board.getClassNames().contains("dragging"), "zones expand while a drag is in progress");
        DropZone target = zone(view, null, 0, StructureNode.Kind.STEP);
        ComponentUtil.fireEvent(target, new DropEvent<>(target, true, "move"));

        assertEquals(List.of("Fixture Step Two", "Fixture Step One"),
                view.lanes.stream().map(l -> l.node().label()).toList(), "UC-016: the board reflects the new order");
        List<Step> steps = ordering.currentSteps(s.id);
        assertEquals("Fixture Step Two", steps.get(0).name);
        assertEquals(DisplayOrdering.displayKey(s.id, 1, 1), ordering.currentLane(s.id, steps.get(0).stepId).get(0).displayKey,
                "UC-016 BR-001: keys were recomputed");
        assertTrue(view.isSelected(view.lanes.get(0).node()), "the moved step stays selected");
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "designer.author", roles = {"elicit_author"})
    void droppingASectionIntoAnotherLaneCopiesIt() {
        Survey s = fixture();
        SurveyDesignerView view = open(s);
        // Expand Step Two so its section-level insertion zone is rendered (drop target).
        view.toggleAndSelect(view.lanes.get(1).node());
        SectionCard sectionA = view.lanes.get(0).cards.get(0);
        StructureNode stepTwo = view.lanes.get(1).node();

        ComponentUtil.fireEvent(sectionA.header, new DragStartEvent<>(sectionA.header, true));
        DropZone target = zone(view, stepTwo, 0, StructureNode.Kind.SECTION);
        ComponentUtil.fireEvent(target, new DropEvent<>(target, true, "move"));

        assertEquals(2, view.lanes.get(0).cards.size(), "UC-015 BR-001: the original stays");
        assertEquals(2, view.lanes.get(1).cards.size());
        SectionCard copy = view.lanes.get(1).cards.get(0);
        assertEquals("Fixture Section A", copy.node().label());
        assertFalse(copy.node().durableId().equals(sectionA.node().durableId()), "a brand-new section");
        assertTrue(view.isSelected(copy.node()), "the copy is selected");
    }

    @Test
    @TestSecurity(user = "designer.author", roles = {"elicit_author"})
    void paletteDropOpensTheDialogAndCancelCreatesNothing() {
        SurveyDesignerView view = open(fixture());
        int lanesBefore = view.lanes.size();

        ComponentUtil.fireEvent(view.palette.stepChip, new DragStartEvent<>(view.palette.stepChip, true));
        DropZone target = zone(view, null, lanesBefore, StructureNode.Kind.STEP);
        ComponentUtil.fireEvent(target, new DropEvent<>(target, true, "copy"));

        ElementDialog<?> dialog = $(ElementDialog.class).single();
        assertTrue(dialog.isOpened(), "UC-015 A4 / UC-016: a palette drop opens the dialog first");
        assertFalse(dialog.renameWarningShown(), "no rename warning when creating");
        test(dialog.cancel).click();
        assertEquals(lanesBefore, view.lanes.size(), "cancel creates nothing");
    }

    @Test
    @TestSecurity(user = "designer.author", roles = {"elicit_author"})
    void renamingAStepShowsTheRetroactiveWarningAndGatesSave() {
        SurveyDesignerView view = open(fixture());
        StructureNode stepOne = view.lanes.get(0).node();

        view.edit(stepOne);
        ElementDialog<?> dialog = $(ElementDialog.class).single();
        assertTrue(dialog.isOpened());
        assertFalse(dialog.renameWarningShown(), "UC-018 A2: no warning until the name changes");
        assertTrue(dialog.save.isEnabled());

        test(dialog.name).setValue("Fixture Step Uno");
        assertTrue(dialog.renameWarningShown(), "UC-018 step 4: renaming is retroactive");
        assertFalse(dialog.save.isEnabled(), "UC-018 step 4: Save waits for the acknowledgement");
        test(dialog.acknowledge).click();
        assertTrue(dialog.save.isEnabled(), "UC-018 step 5");

        test(dialog.name).setValue("Fixture Step One");
        assertFalse(dialog.renameWarningShown(), "back to the stored name: no warning");
        test(dialog.cancel).click();
    }

    @Test
    @TestSecurity(user = "designer.author", roles = {"elicit_author"})
    void everyDragHasAMenuEquivalent() {
        SurveyDesignerView view = open(fixture());
        StepLane lane = view.lanes.get(0);
        assertNotNull(lane.menu.find("Move down"), "NFR-012: steps reorder from the menu");
        assertFalse(lane.menu.find("Move up").isEnabled(), "first step cannot move up");
        SectionCard card = lane.cards.get(0);
        for (String label : List.of("Edit…", "Add question", "Copy to step…", "Also mount in step…", "Reporting tags…", "Move down", "Open details", "Remove from this step")) {
            assertNotNull(card.menu.find(label), "NFR-012: section menu has " + label);
        }
        view.toggleAndSelect(card.node());
        QuestionRow row = view.lanes.get(0).cards.get(0).rows.get(0);
        for (String label : List.of("Edit…", "Copy to section…", "Reporting tags…", "Move down", "Open details", "Remove from this section")) {
            assertNotNull(row.menu.find(label), "NFR-012: question menu has " + label);
        }
        assertFalse(row.menu.find("Move up").isEnabled(), "first question cannot move up");
    }

    @Test
    @TestSecurity(user = "designer.author", roles = {"elicit_author"})
    void reportingTagsMenuOpensTheElementsTagList() {
        SurveyDesignerView view = open(fixture());
        SectionCard sectionB = view.lanes.get(0).cards.get(1);
        assertNotNull(sectionB.menu.find("Reporting tags…"), "UC-021 step 1: reachable from the card menu");
        view.reportingTags(sectionB.node());
        com.elicitsoftware.author.flow.reporting.ElementTagsDialog dialog = $(com.elicitsoftware.author.flow.reporting.ElementTagsDialog.class).single();
        assertTrue(dialog.isOpened());
        assertEquals(1, dialog.grid.getGenericDataView().getItems().count(), "Section B as mounted in Step One carries Gender = Female");
        dialog.close();
        view.toggleAndSelect(sectionB.node());
        QuestionRow q3 = view.lanes.get(0).cards.get(1).rows.get(0);
        view.reportingTags(q3.node());
        dialog = $(com.elicitsoftware.author.flow.reporting.ElementTagsDialog.class).single();
        assertEquals(1, dialog.grid.getGenericDataView().getItems().count(), "FixtureQ3 is tagged in this section only");
        dialog.close();
    }

    @Test
    @TestSecurity(user = "designer.author", roles = {"elicit_author"})
    void leavingTheDesignerRestoresTheApplicationNavigation() {
        open(fixture());
        assertNotNull(find(StructureNavigationTree.class, layout()).first());
        navigate(SurveysView.class);
        assertNull(find(StructureNavigationTree.class, layout()).all().stream().findFirst().orElse(null),
                "UC-006 step 7: the drawer shows the application navigation again");
    }
}
