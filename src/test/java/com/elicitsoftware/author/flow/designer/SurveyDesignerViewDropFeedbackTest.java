package com.elicitsoftware.author.flow.designer;

/*-
 * ***LICENSE_START***
 * Elicit Author
 * %%
 * Copyright (C) 2025 - 2026 Elicit Software LLC
 * %%
 * PolyForm Noncommercial License 1.0.0
 * <https://polyformproject.org/licenses/noncommercial/1.0.0>
 * ***LICENSE_END***
 */

import com.elicitsoftware.author.flow.SurveyEditorView;
import com.elicitsoftware.author.survey.DesignerService;
import com.elicitsoftware.model.Question;
import com.elicitsoftware.model.Step;
import com.elicitsoftware.model.Survey;
import com.elicitsoftware.test.PostgresTestResource;
import com.vaadin.browserless.quarkus.QuarkusBrowserlessTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.dnd.DragEndEvent;
import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.dnd.DragStartEvent;
import com.vaadin.flow.component.dnd.DropEffect;
import com.vaadin.flow.component.dnd.DropEvent;
import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.component.notification.Notification;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC-006 BR-007 and BR-008 (NFR-028): while something is dragged, only the targets that take it
 * respond; a refused drop that lands anyway is answered; and a tool with nowhere to land cannot be
 * dragged. The drop effect checked here is what Vaadin's connector reads in the browser: NONE means
 * no highlight, a not-allowed pointer and no drop (Author #130, #133).
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class SurveyDesignerViewDropFeedbackTest extends QuarkusBrowserlessTest {

    @Inject
    EntityManager em;

    @Inject
    DesignerService designer;

    private Survey fixture() {
        return Survey.find("name", "AuthorFixture").firstResult();
    }

    private SurveyDesignerView open(Survey s) {
        return navigate(SurveyDesignerView.class, Map.of(SurveyEditorView.SURVEY_ID, String.valueOf(s.id)));
    }

    /** Closes every row of {@code table} for the survey, the way an import can leave one. */
    private void retireAll(Survey s, String table) {
        em.createNativeQuery("UPDATE survey." + table + " SET effective_to = now() WHERE survey_id = ?1")
                .setParameter(1, s.id).executeUpdate();
        em.flush();
        em.clear();
    }

    private static void startDrag(Component source) {
        ComponentUtil.fireEvent(source, new DragStartEvent<>(source, true, 0, 0, 0, 0));
    }

    private static void endDrag(Component source) {
        ComponentUtil.fireEvent(source, new DragEndEvent<>(source, true, "none", 0, 0));
    }

    /** A drop the server receives whatever the browser was told: the drag start had not reached it yet. */
    private static void drop(Component target) {
        ComponentUtil.fireEvent(target, new DropEvent<>(target, true, "move", 0, 0, 0, 0));
    }

    private static List<DropZone> zones(Component container) {
        return container.getChildren().filter(DropZone.class::isInstance).map(DropZone.class::cast).toList();
    }

    private static Stream<DropZone> stepBars(SurveyDesignerView view) {
        return zones(view.board).stream();
    }

    private static Stream<Component> laneTargets(SurveyDesignerView view) {
        return view.lanes.stream().flatMap(lane -> Stream.of(lane.header, lane.body));
    }

    private static Stream<DropZone> sectionBars(SurveyDesignerView view) {
        return view.lanes.stream().flatMap(lane -> zones(lane.body).stream());
    }

    private static Stream<SectionCard> cards(SurveyDesignerView view) {
        return view.lanes.stream().flatMap(lane -> lane.cards.stream());
    }

    private static Stream<Component> cardTargets(SurveyDesignerView view) {
        return cards(view).flatMap(card -> Stream.of(card.header, card.body));
    }

    private static Stream<DropZone> questionBars(SurveyDesignerView view) {
        return cards(view).flatMap(card -> zones(card.body).stream());
    }

    private static Stream<QuestionRow> rows(SurveyDesignerView view) {
        return cards(view).flatMap(card -> card.rows.stream());
    }

    private static void assertEffect(DropEffect expected, Stream<? extends Component> targets, String message) {
        List<? extends Component> all = targets.toList();
        assertFalse(all.isEmpty(), message + " (the board has such targets)");
        all.forEach(target -> assertEquals(expected, DropTarget.configure(target).getDropEffect(), message));
    }

    private static boolean draggable(Component source) {
        return DragSource.configure(source).isDraggable();
    }

    private static void assertUnavailable(Glyph glyph, String reason) {
        assertFalse(draggable(glyph), glyph.accessibleName() + " cannot be dragged");
        assertTrue(glyph.hasClassName("unavailable"), glyph.accessibleName() + " is dimmed");
        assertEquals(reason, glyph.getElement().getAttribute("title"), glyph.accessibleName() + " says what to add first");
    }

    private static void assertAvailable(Glyph glyph) {
        assertTrue(draggable(glyph), glyph.accessibleName() + " can be dragged");
        assertFalse(glyph.hasClassName("unavailable"), glyph.accessibleName() + " is not dimmed");
        assertEquals(glyph.accessibleName(), glyph.getElement().getAttribute("title"), "its tooltip is its name");
    }

    private String notificationText() {
        return test(find(Notification.class).single()).getText();
    }

    // ---- UC-006 BR-007: a drag lights up only what can take it -----------------------------------

    @Test
    @TestSecurity(user = "drop.author", roles = {"elicit_author"})
    void aQuestionDragIsTakenOnlyInsideSections_UC011A8() {
        SurveyDesignerView view = open(fixture());
        Glyph chip = view.palette.questionChip();

        startDrag(chip);
        assertTrue(view.board.hasClassName("dragging-question"));
        assertEffect(DropEffect.NONE, stepBars(view), "#130: no bar between steps takes a question");
        assertEffect(DropEffect.NONE, laneTargets(view), "#130: a step lane's header and body take no question");
        assertEffect(DropEffect.NONE, sectionBars(view), "no bar between sections takes a question");
        assertEffect(DropEffect.MOVE, cardTargets(view), "a section card's header and body take it");
        assertEffect(DropEffect.MOVE, questionBars(view), "so do the bars between its rows");
        assertEffect(DropEffect.MOVE, rows(view), "and its rows, before which it lands");

        endDrag(chip);
        assertFalse(view.board.hasClassName("dragging-question"));
        assertEffect(DropEffect.MOVE, Stream.of(stepBars(view), laneTargets(view), sectionBars(view)).flatMap(s -> s),
                "once the drag ends every target takes drops again");

        startDrag(view.toolbox().questions.items.get(0).glyph);
        assertEffect(DropEffect.NONE, laneTargets(view), "UC-011 A8: the same for a question from the Questions panel");
    }

    @Test
    @TestSecurity(user = "drop.author", roles = {"elicit_author"})
    void aRuleDragIsTakenOnlyByQuestionRows_UC019A7() {
        SurveyDesignerView view = open(fixture());

        startDrag(view.palette.showRuleChip);
        assertTrue(view.board.hasClassName("dragging-rule"));
        assertEffect(DropEffect.NONE, stepBars(view), "#133: the canvas takes no rule");
        assertEffect(DropEffect.NONE, laneTargets(view), "#133: nor does a step");
        assertEffect(DropEffect.NONE, sectionBars(view), "#133: nor a bar between sections");
        assertEffect(DropEffect.NONE, cardTargets(view), "#133: nor a section");
        assertEffect(DropEffect.NONE, questionBars(view), "#133: nor a bar between questions");
        assertEffect(DropEffect.MOVE, rows(view), "UC-019 step 1: a question row does");
    }

    @Test
    @TestSecurity(user = "drop.author", roles = {"elicit_author"})
    void stepsSectionsAndListsAreTakenOnlyWhereTheyGo_UC016A5_UC015A8_UC014A5() {
        SurveyDesignerView view = open(fixture());
        StepLane laneOne = view.lanes.get(0);

        startDrag(laneOne.header);
        assertEffect(DropEffect.MOVE, stepBars(view), "UC-016: a step goes between steps");
        assertEffect(DropEffect.NONE, Stream.of(laneTargets(view), sectionBars(view), cardTargets(view), questionBars(view), rows(view))
                .flatMap(s -> s), "UC-016 A5: and nowhere else");
        endDrag(laneOne.header);

        SectionCard sectionA = laneOne.cards.get(0);
        startDrag(sectionA.header);
        assertEffect(DropEffect.NONE, stepBars(view), "UC-015 A8: a section is refused between steps");
        assertEffect(DropEffect.MOVE, laneTargets(view), "a step lane takes it");
        assertEffect(DropEffect.MOVE, sectionBars(view), "so do the bars between sections");
        assertEffect(DropEffect.NONE, Stream.of(cardTargets(view), questionBars(view), rows(view)).flatMap(s -> s),
                "UC-015 A8: nothing inside a card takes a section");
        endDrag(sectionA.header);

        startDrag(view.toolbox().lists.items.get(0).glyph);
        assertEffect(DropEffect.MOVE, rows(view), "UC-014 step 6: a question row takes a list");
        assertEffect(DropEffect.NONE, Stream.of(stepBars(view), laneTargets(view), cardTargets(view), questionBars(view)).flatMap(s -> s),
                "UC-014 A5: nothing else does");
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "drop.author", roles = {"elicit_author"})
    void aQuestionDroppedOnAStepAnywayIsAnsweredNotIgnored_UC011A8() {
        Survey s = fixture();
        SurveyDesignerView view = open(s);
        long questions = Question.count("surveyId", s.id);

        startDrag(view.palette.questionChip());
        drop(view.lanes.get(0).header);

        assertTrue(find(QuestionDialog.class).all().isEmpty(), "#130: no question is started");
        assertEquals(questions, Question.count("surveyId", s.id), "nothing is created");
        assertEquals(view.getTranslation("surveyDesignerView.refused.question"), notificationText(),
                "#130: the author is told that a question goes in a section");
    }

    @Test
    @TestSecurity(user = "drop.author", roles = {"elicit_author"})
    void aRuleDroppedOnAnInsertionBarAnywayIsAnsweredNotIgnored_UC019A7() {
        SurveyDesignerView view = open(fixture());

        startDrag(view.palette.showRuleChip);
        drop(stepBars(view).findFirst().orElseThrow());

        assertTrue(find(RelationshipDialog.class).all().isEmpty(), "#133: no rule is started");
        assertEquals(view.getTranslation("surveyDesignerView.refused.rule"), notificationText(),
                "#133: the author is told where a rule is dropped");
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "drop.author", roles = {"elicit_author"})
    void aStepDroppedOnAQuestionRowAnywayIsAnsweredNotIgnored_UC016A5() {
        SurveyDesignerView view = open(fixture());
        List<String> order = view.lanes.stream().map(lane -> lane.node().label()).toList();
        StepLane laneTwo = view.lanes.get(1);

        startDrag(laneTwo.header);
        drop(view.lanes.get(0).cards.get(0).rows.get(0));

        assertEquals(order, view.lanes.stream().map(lane -> lane.node().label()).toList(), "nothing moves");
        assertEquals(view.getTranslation("surveyDesignerView.refused.step"), notificationText());
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "drop.author", roles = {"elicit_author"})
    void aRemovedRowTakesNothing_UC006BR007() {
        Survey s = fixture();
        SurveyDesignerView view = open(s);
        QuestionRow q2 = view.lanes.get(0).cards.get(0).rows.get(1);
        designer.retireQuestionAssignment(s.id, q2.node().assignmentId());
        view.showRemoved.setValue(true); // UC-006 A3: the removed row is shown, struck through
        QuestionRow removed = rows(view).filter(row -> row.node().retired()).findFirst().orElseThrow();

        startDrag(view.palette.questionChip());
        assertEffect(DropEffect.NONE, Stream.of(removed), "a removed row takes no question");
        startDrag(view.palette.showRuleChip);
        assertEffect(DropEffect.NONE, Stream.of(removed), "nor a rule");

        drop(removed);
        assertEquals(view.getTranslation("surveyDesignerView.refused.removed", removed.node().label()), notificationText(),
                "a drop that lands on it anyway says it has to be restored first");
    }

    // ---- UC-006 BR-008: a tool with nowhere to go cannot be dragged ------------------------------

    @Test
    @TestTransaction
    @TestSecurity(user = "drop.author", roles = {"elicit_author"})
    void aSurveyWithNoStepOffersOnlyTheStepGlyph_UC006A4() {
        Survey s = fixture();
        retireAll(s, "relationships");
        retireAll(s, "sections_questions");
        retireAll(s, "steps_sections");
        retireAll(s, "steps");
        SurveyDesignerView view = open(s);
        String needsStep = view.getTranslation("designer.needsStep");

        assertTrue(view.lanes.isEmpty(), "the board is empty");
        assertAvailable(view.palette.stepChip);
        assertUnavailable(view.palette.sectionChip, needsStep);
        assertUnavailable(view.palette.questionChip(), needsStep);
        assertUnavailable(view.palette.showRuleChip, needsStep);
        assertUnavailable(view.palette.repeatRuleChip, needsStep);
        assertUnavailable(view.palette.textRuleChip, needsStep);

        SectionBankPanel.Row section = view.toolbox().sections.items.get(0);
        assertUnavailable(section.glyph, needsStep);
        assertFalse(section.menu.find(view.getTranslation("sectionBankPanel.menu.mountInStep")).isEnabled(),
                "UC-015 A8: with no step there is nothing to mount it in");
        QuestionBankPanel.Row question = view.toolbox().questions.items.get(0);
        assertUnavailable(question.glyph, needsStep);
        assertFalse(question.menu.find(view.getTranslation("questionBankPanel.menu.addToSection")).isEnabled());
        ListPanel.Row list = view.toolbox().lists.items.get(0);
        assertFalse(draggable(list.glyph), "UC-014 A5: no question to give a list to");
        assertTrue(list.glyph.hasClassName("unavailable"));
        assertEquals(needsStep, list.glyph.getElement().getAttribute("title"));
        assertTrue(list.menu.find(view.getTranslation("designer.menu.edit")).isEnabled(), "the list's other actions stay");
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "drop.author", roles = {"elicit_author"})
    void noSectionMeansNoQuestionTools_UC011A8() {
        Survey s = fixture();
        retireAll(s, "relationships");
        retireAll(s, "sections_questions");
        retireAll(s, "steps_sections");
        SurveyDesignerView view = open(s);
        String needsSection = view.getTranslation("designer.needsSection");

        assertAvailable(view.palette.sectionChip);
        assertUnavailable(view.palette.questionChip(), needsSection);
        assertUnavailable(view.palette.showRuleChip, needsSection);
        assertTrue(draggable(view.toolbox().sections.items.get(0).glyph), "a section can be mounted in a step");
        QuestionBankPanel.Row question = view.toolbox().questions.items.get(0);
        assertUnavailable(question.glyph, needsSection);
        assertFalse(question.menu.find(view.getTranslation("questionBankPanel.menu.addToSection")).isEnabled());
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "drop.author", roles = {"elicit_author"})
    void noQuestionMeansNoRuleOrListTools_UC019A7_UC014A5() {
        Survey s = fixture();
        retireAll(s, "relationships");
        retireAll(s, "sections_questions");
        SurveyDesignerView view = open(s);
        String needsQuestion = view.getTranslation("designer.needsQuestion");

        assertAvailable(view.palette.questionChip());
        assertUnavailable(view.palette.showRuleChip, needsQuestion);
        assertUnavailable(view.palette.repeatRuleChip, needsQuestion);
        assertUnavailable(view.palette.textRuleChip, needsQuestion);
        assertTrue(draggable(view.toolbox().questions.items.get(0).glyph), "a question can be added to a section");
        ListPanel.Row list = view.toolbox().lists.items.get(0);
        assertFalse(draggable(list.glyph));
        assertEquals(needsQuestion, list.glyph.getElement().getAttribute("title"));
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "drop.author", roles = {"elicit_author"})
    void theToolsComeBackWhenTheBoardReloads_UC006BR008() {
        Survey s = fixture();
        retireAll(s, "relationships");
        retireAll(s, "sections_questions");
        retireAll(s, "steps_sections");
        retireAll(s, "steps");
        SurveyDesignerView view = open(s);
        assertFalse(draggable(view.palette.sectionChip));

        Step draft = new Step();
        draft.name = "Drop feedback step";
        designer.createStep(s.id, draft, 0);
        view.load();

        assertAvailable(view.palette.sectionChip);
        assertTrue(draggable(view.toolbox().sections.items.get(0).glyph), "the first step brings the Sections panel back");
        assertUnavailable(view.palette.questionChip(), view.getTranslation("designer.needsSection"));
    }
}
