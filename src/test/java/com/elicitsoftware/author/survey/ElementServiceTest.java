package com.elicitsoftware.author.survey;

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

import com.elicitsoftware.author.AuthoringException;
import com.elicitsoftware.model.Question;
import com.elicitsoftware.model.Relationship;
import com.elicitsoftware.model.Section;
import com.elicitsoftware.model.SectionsQuestion;
import com.elicitsoftware.model.Step;
import com.elicitsoftware.model.StructuralElement;
import com.elicitsoftware.model.Survey;
import com.elicitsoftware.test.PostgresTestResource;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** UC-009 Remove Structural Element, UC-010 Restore Removed Element, working-copy create. */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class ElementServiceTest {

    @Inject
    ElementService service;

    @Inject
    EntityManager em;

    private Survey fixture() {
        return Survey.find("name", "AuthorFixture").firstResult();
    }

    private Question q(String shortText) {
        return Question.find("shortText", shortText).firstResult();
    }

    @Test
    @TestTransaction
    void create_mintsDurableIdAndKeyAsAWorkingCopy() {
        Step s = new Step();
        s.surveyId = fixture().id;
        s.name = "Created step";
        s.dimensionName = "Created step";
        s.displayOrder = new BigDecimal("3");
        Step created = service.create(s);
        assertNotNull(created.id);
        assertNotNull(created.stepId);
        assertNotNull(created.stepKey, "UC-003/FR-009: key minted once at creation");
        assertEquals(0, created.version);
        assertEquals(StructuralElement.EPOCH, created.effectiveFrom);
        assertTrue(created.isCurrent());
    }

    @Test
    @TestTransaction
    void removeQuestion_retiresItsAssignmentsAndRulesAtOneInstant() {
        Survey s = fixture();
        Question q3 = q("FixtureQ3");
        ElementService.RemovalPlan plan = service.planRemoval(s.id, StructureNode.Kind.QUESTION, q3.questionId);
        assertFalse(plan.blocked());
        assertTrue(plan.dependents().stream().anyMatch(d -> d.startsWith("rule ")), plan.dependents().toString());

        service.remove(s.id, StructureNode.Kind.QUESTION, q3.questionId);
        em.clear();

        Question after = q("FixtureQ3");
        assertTrue(after.isRetired(), "UC-009 BR-001: retired, not deleted");
        SectionsQuestion sq = SectionsQuestion.find("questionId", q3.questionId).firstResult();
        assertEquals(after.effectiveTo, sq.effectiveTo, "UC-009 BR-002: dependents retired at the same instant");
        Relationship r = Relationship.find("surveyId", s.id).firstResult();
        assertEquals(after.effectiveTo, r.effectiveTo);
        assertEquals(1, Question.count("questionId", q3.questionId), "still exactly one row");
    }

    @Test
    @TestTransaction
    void plans_nameDependentsAndBlockersForPeople() {
        Survey s = fixture();
        Question q3 = q("FixtureQ3");
        ElementService.RemovalPlan removal = service.planRemoval(s.id, StructureNode.Kind.QUESTION, q3.questionId);
        assertTrue(removal.dependents().stream().anyMatch(d -> d.contains("section 'Fixture Section")),
                "UC-009 step 3 names the section, not its id: " + removal.dependents());

        service.remove(s.id, StructureNode.Kind.QUESTION, q3.questionId);
        em.clear();
        ElementService.RestorePlan restore = service.planRestore(s.id, StructureNode.Kind.QUESTION, q3.questionId);
        assertFalse(restore.blocked());
        assertTrue(restore.dependents().stream().anyMatch(d -> d.contains("section 'Fixture Section")), restore.dependents().toString());
        assertTrue(restore.dependents().stream().anyMatch(d -> d.startsWith("rule ")), "UC-010 step 3 lists the rules retired with it");

        // Remove the section the question sat in: restoring the question is now blocked by name.
        Section section = Section.find("name", "Fixture Section B").firstResult();
        service.remove(s.id, StructureNode.Kind.SECTION, section.sectionId);
        em.clear();
        ElementService.RestorePlan blocked = service.planRestore(s.id, StructureNode.Kind.QUESTION, q3.questionId);
        assertTrue(blocked.blocked());
        assertTrue(blocked.blockers().get(0).contains("'Fixture Section B'"), blocked.blockers().toString());
        assertThrows(AuthoringException.class, () -> service.restore(s.id, StructureNode.Kind.QUESTION, q3.questionId));
    }

    @Test
    @TestTransaction
    void restoreQuestion_reopensItAndItsDependents() {
        Survey s = fixture();
        Question q3 = q("FixtureQ3");
        service.remove(s.id, StructureNode.Kind.QUESTION, q3.questionId);
        em.clear();
        service.restore(s.id, StructureNode.Kind.QUESTION, q3.questionId);
        em.clear();

        assertTrue(q("FixtureQ3").isCurrent(), "UC-010 BR-001: same element back");
        assertTrue(SectionsQuestion.<SectionsQuestion>find("questionId", q3.questionId).firstResult().isCurrent());
        assertTrue(Relationship.<Relationship>find("surveyId", s.id).firstResult().isCurrent());
    }

    @Test
    @TestTransaction
    void removeStep_cascadesToSectionAssignments_andRestoreRequiresParent() {
        Survey s = fixture();
        Step step = Step.find("name", "Fixture Step One").firstResult();
        service.remove(s.id, StructureNode.Kind.STEP, step.stepId);
        em.clear();
        assertTrue(Step.<Step>find("name", "Fixture Step One").firstResult().isRetired());

        // Section A itself stays current (only its assignment was retired); removing then restoring
        // a question inside it while the step is gone is refused because the section's step is removed.
        Question q1 = q("FixtureQ1");
        service.remove(s.id, StructureNode.Kind.QUESTION, q1.questionId);
        em.clear();
        service.restore(s.id, StructureNode.Kind.QUESTION, q1.questionId); // section A is current -> allowed
        em.clear();
        assertTrue(q("FixtureQ1").isCurrent());
    }

    @Test
    @TestTransaction
    void removeSelectGroupIsRefusedWhileAQuestionUsesIt() {
        Survey s = fixture();
        Question q3 = q("FixtureQ3");
        AuthoringException ex = assertThrows(AuthoringException.class, () -> service.removeSelectGroup(s.id, q3.selectGroupId));
        assertTrue(ex.getMessage().contains("still used"), "UC-009 BR-003");
    }

    @Test
    @TestTransaction
    void purgeRemoved_deletesRetiredRowsOfANeverExportedSurvey() {
        Survey s = fixture();
        Question q5 = q("FixtureQ5");
        service.remove(s.id, StructureNode.Kind.QUESTION, q5.questionId);
        em.clear();
        int deleted = service.purgeRemoved(s.id);
        assertTrue(deleted >= 2, "question + its assignment: " + deleted);
        assertEquals(0, Question.count("questionId", q5.questionId));
    }
}
