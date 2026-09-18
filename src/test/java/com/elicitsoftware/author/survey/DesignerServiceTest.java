package com.elicitsoftware.author.survey;

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

import com.elicitsoftware.author.AuthoringException;
import com.elicitsoftware.author.definition.ExportValidation;
import com.elicitsoftware.author.definition.ValidationIssue;
import com.elicitsoftware.model.Question;
import com.elicitsoftware.model.QuestionType;
import com.elicitsoftware.model.Relationship;
import com.elicitsoftware.model.Section;
import com.elicitsoftware.model.SectionsQuestion;
import com.elicitsoftware.model.Step;
import com.elicitsoftware.model.StepsSections;
import com.elicitsoftware.model.StructuralElement;
import com.elicitsoftware.model.Survey;
import com.elicitsoftware.test.PostgresTestResource;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class DesignerServiceTest {

    @Inject
    DesignerService service;

    @Inject
    DisplayOrdering ordering;

    @Inject
    ElementService elements;

    @Inject
    ExportValidation validation;

    @Inject
    RenameLedger renames;

    @Inject
    EntityManager em;

    private Survey fixture() {
        return Survey.find("name", "AuthorFixture").firstResult();
    }

    private Step step(String name) {
        return Step.find("name = ?1 and effectiveTo = ?2", name, StructuralElement.SENTINEL).firstResult();
    }

    private Section section(String name) {
        return Section.find("name = ?1 and effectiveTo = ?2", name, StructuralElement.SENTINEL).firstResult();
    }

    private Question question(String shortText) {
        return Question.find("shortText = ?1 and effectiveTo = ?2", shortText, StructuralElement.SENTINEL).firstResult();
    }

    /** UC-016 BR-001: every current assignment's key is derived from its step's and its own order; keys are unique. */
    private void assertKeysDerived(Survey s) {
        Set<String> keys = new HashSet<>();
        for (Step st : ordering.currentSteps(s.id)) {
            int expectedSection = 0;
            for (StepsSections ss : ordering.currentLane(s.id, st.stepId)) {
                expectedSection++;
                assertEquals(st.displayOrder.intValue(), ss.stepDisplayOrder.intValue(), "assignment carries its step's order");
                assertEquals(expectedSection, ss.sectionDisplayOrder.intValue(), "lane positions are 1..n");
                assertEquals(DisplayOrdering.displayKey(s.id, st.displayOrder.intValue(), expectedSection), ss.displayKey);
                assertTrue(keys.add(ss.displayKey), "display keys are unique");
            }
        }
        Survey reloaded = Survey.findById(s.id);
        assertTrue(keys.isEmpty() || keys.contains(reloaded.initialDisplayKey), "UC-016 BR-002: initial key names a current assignment");
    }

    @Test
    @TestTransaction
    void moveStep_renumbersStepsAndRewritesEveryDisplayKey() {
        Survey s = fixture();
        Step two = step("Fixture Step Two");
        service.moveStep(s.id, two.stepId, 0);
        em.flush();

        List<Step> steps = ordering.currentSteps(s.id);
        assertEquals(List.of("Fixture Step Two", "Fixture Step One"), steps.stream().map(st -> st.name).toList());
        assertEquals(1, steps.get(0).displayOrder.intValue());
        assertEquals(2, steps.get(1).displayOrder.intValue());
        assertKeysDerived(s);
        assertEquals(DisplayOrdering.displayKey(s.id, 1, 1), Survey.<Survey>findById(s.id).initialDisplayKey,
                "UC-016 BR-002: the entry point followed the first section");
    }

    @Test
    @TestTransaction
    void moveStep_toItsOwnPositionChangesNothing() {
        Survey s = fixture();
        Step one = step("Fixture Step One");
        String keyBefore = ordering.currentLane(s.id, one.stepId).get(0).displayKey;
        service.moveStep(s.id, one.stepId, 0);
        assertEquals(keyBefore, ordering.currentLane(s.id, one.stepId).get(0).displayKey, "UC-016 A2: no-op drop");
    }

    @Test
    @TestTransaction
    void moveSectionWithinStep_swapsLanePositionsAndKeys() {
        Survey s = fixture();
        Step one = step("Fixture Step One");
        List<StepsSections> lane = ordering.currentLane(s.id, one.stepId);
        assertEquals(2, lane.size());
        StepsSections b = lane.get(1);
        service.moveSectionWithinStep(s.id, b.stepsSectionsId, 0);

        List<StepsSections> after = ordering.currentLane(s.id, one.stepId);
        assertEquals(b.stepsSectionsId, after.get(0).stepsSectionsId);
        assertKeysDerived(s);
    }

    @Test
    @TestTransaction
    void moveQuestionWithinSection_renumbersContiguously() {
        Survey s = fixture();
        Section a = section("Fixture Section A");
        List<SectionsQuestion> rows = ordering.currentQuestions(s.id, a.sectionId);
        SectionsQuestion last = rows.get(rows.size() - 1);
        service.moveQuestionWithinSection(s.id, last.sectionsQuestionId, 0);

        List<SectionsQuestion> after = ordering.currentQuestions(s.id, a.sectionId);
        assertEquals(last.sectionsQuestionId, after.get(0).sectionsQuestionId);
        for (int i = 0; i < after.size(); i++) {
            assertEquals(i + 1, after.get(i).displayOrder.intValue(), "UC-016 BR-001: positions 1..n");
        }
    }

    @Test
    @TestTransaction
    void duplicateSectionInto_copiesTheSectionAndReusesItsQuestionsButNotItsRules() {
        Survey s = fixture();
        Section b = section("Fixture Section B");
        Step two = step("Fixture Step Two");
        long questionsBefore = Question.count("surveyId", s.id);
        long rulesBefore = Relationship.count("surveyId = ?1 and effectiveTo = ?2", s.id, StructuralElement.SENTINEL);

        Section copy = service.duplicateSectionInto(s.id, b.sectionId, two.stepId, 0);

        assertNotNull(copy.sectionKey);
        assertNotEquals(b.sectionKey, copy.sectionKey, "UC-015 BR-001: a brand-new section");
        assertNotEquals(b.sectionId, copy.sectionId);
        assertEquals(b.name, copy.name);
        List<SectionsQuestion> copied = ordering.currentQuestions(s.id, copy.sectionId);
        assertEquals(ordering.currentQuestions(s.id, b.sectionId).stream().map(sq -> sq.questionId).toList(),
                copied.stream().map(sq -> sq.questionId).toList(), "UC-015 BR-002: same questions, same order");
        assertEquals(questionsBefore, Question.count("surveyId", s.id), "no question rows were copied");
        assertEquals(rulesBefore, Relationship.count("surveyId = ?1 and effectiveTo = ?2", s.id, StructuralElement.SENTINEL), "rules do not follow");
        List<StepsSections> lane = ordering.currentLane(s.id, two.stepId);
        assertEquals(copy.sectionId, lane.get(0).sectionId, "placed at the drop position");
        assertKeysDerived(s);
        assertTrue(section("Fixture Section B").isCurrent(), "the original stays");
    }

    @Test
    @TestTransaction
    void mountSection_addsAnAssignmentAndRefusesASecondInTheSameStep() {
        Survey s = fixture();
        Section a = section("Fixture Section A");
        Step two = step("Fixture Step Two");
        StepsSections mount = service.mountSection(s.id, a.sectionId, two.stepId, 99);
        assertEquals(2, elements.currentStepAssignmentsOfSection(s.id, a.sectionId).size(), "UC-015 BR-003: mounted in two steps");
        assertEquals(2, ordering.currentLane(s.id, two.stepId).size());
        assertEquals(mount.stepsSectionsId, ordering.currentLane(s.id, two.stepId).get(1).stepsSectionsId, "clamped to the end");
        assertKeysDerived(s);
        assertThrows(AuthoringException.class, () -> service.mountSection(s.id, a.sectionId, two.stepId, 0), "UC-015 A2");
    }

    @Test
    @TestTransaction
    void assignQuestionInto_reusesTheQuestionAndRefusesDuplicates() {
        Survey s = fixture();
        Question q5 = question("FixtureQ5");
        Section a = section("Fixture Section A");
        service.assignQuestionInto(s.id, q5.questionId, a.sectionId, 1);
        List<SectionsQuestion> rows = ordering.currentQuestions(s.id, a.sectionId);
        assertEquals(3, rows.size());
        assertEquals(q5.questionId, rows.get(1).questionId, "UC-011 A4: placed at the drop position");
        assertEquals(2, elements.currentAssignmentsOfQuestion(s.id, q5.questionId).size(), "UC-011 BR-001: one question, two assignments");
        assertThrows(AuthoringException.class, () -> service.assignQuestionInto(s.id, q5.questionId, a.sectionId, 0));
    }

    @Test
    @TestTransaction
    void retireQuestionAssignment_lastAssignmentRetiresRuleAndQuestion() {
        Survey s = fixture();
        Question q3 = question("FixtureQ3");
        SectionsQuestion sq = elements.currentAssignmentsOfQuestion(s.id, q3.questionId).get(0);
        service.retireQuestionAssignment(s.id, sq.sectionsQuestionId);
        em.clear();

        assertTrue(Question.<Question>find("shortText", "FixtureQ3").firstResult().isRetired(), "UC-011 A5 / UC-009 BR-002");
        Relationship r = Relationship.find("surveyId", s.id).firstResult();
        assertTrue(r.isRetired(), "the rule reading from the assignment is retired with it");
        Section b = section("Fixture Section B");
        List<SectionsQuestion> rows = ordering.currentQuestions(s.id, b.sectionId);
        assertEquals(1, rows.size());
        assertEquals(1, rows.get(0).displayOrder.intValue(), "the section was renumbered");
    }

    @Test
    @TestTransaction
    void retireSectionAssignment_lastMountRetiresTheSection() {
        Survey s = fixture();
        Step one = step("Fixture Step One");
        StepsSections first = ordering.currentLane(s.id, one.stepId).get(0);
        int sectionId = first.sectionId;
        service.retireSectionAssignment(s.id, first.stepsSectionsId);
        em.clear();

        assertTrue(elements.section(s.id, sectionId).orElseThrow().isRetired(), "UC-015 A3 step 2");
        List<StepsSections> lane = ordering.currentLane(s.id, one.stepId);
        assertEquals(1, lane.size());
        assertEquals(DisplayOrdering.displayKey(s.id, 1, 1), lane.get(0).displayKey, "UC-015 BR-004: lane renumbered");
        assertKeysDerived(s);
    }

    @Test
    @TestTransaction
    void createStep_placesItAndRequiresAUniqueName() {
        Survey s = fixture();
        Step draft = new Step();
        draft.name = "Fixture Step Zero";
        Step created = service.createStep(s.id, draft, 0);
        assertNotNull(created.stepKey);
        assertEquals("Fixture Step Zero", created.dimensionName, "dimension name defaults to the name");
        assertEquals(List.of("Fixture Step Zero", "Fixture Step One", "Fixture Step Two"),
                ordering.currentSteps(s.id).stream().map(st -> st.name).toList());
        assertKeysDerived(s);

        Step duplicate = new Step();
        duplicate.name = "Fixture Step One";
        AuthoringException ae = assertThrows(AuthoringException.class, () -> service.createStep(s.id, duplicate, 9));
        assertEquals("name", ae.field(), "UC-018 A1 / BR-003");
    }

    @Test
    @TestTransaction
    void createSectionIn_andCreateQuestionIn_placeNewElements() {
        Survey s = fixture();
        Step two = step("Fixture Step Two");
        Section draft = new Section();
        draft.name = "Fixture Section D";
        Section d = service.createSectionIn(s.id, two.stepId, draft, 0);
        assertNotNull(d.sectionKey);
        assertEquals(d.sectionId, ordering.currentLane(s.id, two.stepId).get(0).sectionId, "UC-015 A4");
        assertKeysDerived(s);

        Question q = new Question();
        q.questionType = QuestionType.find("name", "TEXT").firstResult();
        q.text = "A new question";
        q.shortText = "FixtureQNew";
        Question created = service.createQuestionIn(s.id, d.sectionId, q, 0);
        assertNotNull(created.questionKey, "UC-011 BR-001");
        assertEquals(created.questionId, ordering.currentQuestions(s.id, d.sectionId).get(0).questionId);

        Question choice = new Question();
        choice.questionType = QuestionType.find("name", "RADIO").firstResult();
        choice.text = "Pick one";
        AuthoringException ae = assertThrows(AuthoringException.class, () -> service.createQuestionIn(s.id, d.sectionId, choice, 0));
        assertEquals("selectGroupId", ae.field(), "UC-011 A1 / BR-002");
    }

    @Test
    @TestTransaction
    void rename_recordsTheRetroactiveWarningUntilExport() {
        Survey s = fixture();
        renames.clear(s.id);
        Step one = step("Fixture Step One");
        Step edited = new Step();
        edited.stepId = one.stepId;
        edited.name = "Fixture Step Uno";
        edited.dimensionName = one.dimensionName;
        service.rename(s.id, edited);
        assertEquals("Fixture Step Uno", elements.step(s.id, one.stepId).orElseThrow().name);

        List<ValidationIssue> issues = validation.validate(Survey.findById(s.id));
        assertTrue(issues.stream().anyMatch(i -> !i.isError() && i.message().contains("Fixture Step One") && i.message().contains("renamed")),
                "UC-018 step 6 / UC-007 step 4: " + issues);
        renames.clear(s.id);
    }

    @Test
    @TestTransaction
    void validation_flagsAnInitialDisplayKeyThatNamesNoAssignment() {
        Survey s = Survey.findById(fixture().id);
        s.initialDisplayKey = "0001-0009-0000-0009-0000-0000-0000";
        em.flush();
        List<ValidationIssue> issues = validation.validate(s);
        assertTrue(issues.stream().anyMatch(i -> i.isError() && i.message().contains("initial display key")), "UC-016 BR-002: " + issues);

        ordering.syncInitialDisplayKey(s.id);
        assertFalse(validation.validate(Survey.findById(s.id)).stream().anyMatch(i -> i.message().contains("initial display key")));
    }

    @Test
    @TestTransaction
    void restoreSection_takesAFreePositionInsteadOfCollidingOnItsOldKey() {
        Survey s = fixture();
        Step one = step("Fixture Step One");
        List<StepsSections> lane = ordering.currentLane(s.id, one.stepId);
        int firstSection = lane.get(0).sectionId;
        elements.remove(s.id, StructureNode.Kind.SECTION, firstSection);
        ordering.renumberSurvey(s.id); // the survivor now owns key ...-0001-
        assertEquals(DisplayOrdering.displayKey(s.id, 1, 1), ordering.currentLane(s.id, one.stepId).get(0).displayKey);

        elements.restore(s.id, StructureNode.Kind.SECTION, firstSection);
        List<StepsSections> after = ordering.currentLane(s.id, one.stepId);
        assertEquals(2, after.size(), "UC-010: the section is back in its step");
        assertKeysDerived(s);
    }
}
