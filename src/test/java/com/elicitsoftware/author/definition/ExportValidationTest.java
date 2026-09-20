package com.elicitsoftware.author.definition;

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

import com.elicitsoftware.author.survey.DesignerService;
import com.elicitsoftware.model.Question;
import com.elicitsoftware.model.SectionsQuestion;
import com.elicitsoftware.model.StructuralElement;
import com.elicitsoftware.model.Survey;
import com.elicitsoftware.author.survey.RuleService;
import com.elicitsoftware.model.Step;
import com.elicitsoftware.model.Relationship;
import com.elicitsoftware.model.OperatorType;
import com.elicitsoftware.model.ActionType;
import com.elicitsoftware.test.PostgresTestResource;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** UC-007 Validate Survey Definition. */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class ExportValidationTest {

    @Inject
    ExportValidation validation;

    @Inject
    EntityManager em;

    @Inject
    DesignerService designer;

    @Inject
    RuleService rules;

    private Survey fixture() {
        return Survey.find("name", "AuthorFixture").firstResult();
    }

    @Test
    void populatedFixtureHasNoErrors() {
        List<ValidationIssue> issues = validation.validate(fixture());
        assertTrue(issues.stream().noneMatch(ValidationIssue::isError), issues.toString());
    }

    @Test
    void emptySurveyIsAWarningNotAnError() {
        Survey empty = Survey.find("name", "AuthorFixtureEmpty").firstResult();
        List<ValidationIssue> issues = validation.validate(empty);
        assertTrue(issues.stream().anyMatch(i -> !i.isError() && i.message().contains("no steps")));
        assertTrue(issues.stream().noneMatch(ValidationIssue::isError));
    }

    @Test
    @TestTransaction
    void relationshipToARetiredQuestionAssignmentIsAnError() {
        Survey s = fixture();
        em.createNativeQuery("UPDATE survey.sections_questions sq SET effective_to = now() FROM survey.questions q "
                        + "WHERE sq.question_id = q.question_id AND q.short_text = 'FixtureQ4' AND sq.survey_id = ?1")
                .setParameter(1, s.id).executeUpdate();
        em.flush();
        em.clear();
        List<ValidationIssue> issues = validation.validate(Survey.findById(s.id));
        assertTrue(issues.stream().anyMatch(i -> i.isError() && "relationships".equals(i.kind())
                && i.message().contains("targets a removed or missing question assignment")), issues.toString());
    }

    @Test
    @TestTransaction
    void tagOnARetiredQuestionAssignmentIsAWarning() {
        Survey s = fixture();
        em.createNativeQuery("UPDATE survey.sections_questions sq SET effective_to = now() FROM survey.questions q "
                        + "WHERE sq.question_id = q.question_id AND q.short_text = 'FixtureQ3' AND sq.survey_id = ?1")
                .setParameter(1, s.id).executeUpdate();
        em.flush();
        em.clear();
        List<ValidationIssue> issues = validation.validate(Survey.findById(s.id));
        assertTrue(issues.stream().anyMatch(i -> !i.isError() && "metadata".equals(i.kind())
                && i.message().contains("removed or missing")), "UC-024 step 3: " + issues);
    }

    @Test
    @TestTransaction
    void tagNameTheSiteWouldRejectIsAnError() {
        Survey s = fixture();
        em.createNativeQuery("UPDATE survey.ontology SET tag = 'Has Children!' WHERE survey_id = ?1 AND tag = 'Has Children'")
                .setParameter(1, s.id).executeUpdate();
        em.flush();
        em.clear();
        List<ValidationIssue> issues = validation.validate(Survey.findById(s.id));
        assertTrue(issues.stream().anyMatch(i -> i.isError() && "ontology".equals(i.kind())
                && i.message().contains("reporting build would fail")), "UC-024 BR-002: " + issues);
    }

    @Test
    @TestTransaction
    void choiceQuestionWithoutOptionsIsAnError() {
        Survey s = fixture();
        em.createNativeQuery("UPDATE survey.select_items SET effective_to = now() WHERE survey_id = ?1")
                .setParameter(1, s.id).executeUpdate();
        em.flush();
        em.clear();
        List<ValidationIssue> issues = validation.validate(Survey.findById(s.id));
        assertTrue(issues.stream().anyMatch(i -> i.isError() && "questions".equals(i.kind())
                && i.message().contains("has no options")), issues.toString());
    }

    @Test
    @TestTransaction
    void emptyStepIsAWarning() {
        Survey s = fixture();
        em.createNativeQuery("UPDATE survey.steps_sections SET effective_to = now() WHERE survey_id = ?1 AND step_display_order = 2")
                .setParameter(1, s.id).executeUpdate();
        em.flush();
        em.clear();
        List<ValidationIssue> issues = validation.validate(Survey.findById(s.id));
        assertTrue(issues.stream().anyMatch(i -> !i.isError() && "steps".equals(i.kind()) && i.message().contains("has no sections")), issues.toString());
    }

    /** UC-019 BR-001 / UC-007 step 2: a rule whose downstream path does not hold together blocks export; a full path passes. */
    @Test
    @TestTransaction
    void ruleWithAnIncompletePathIsAnError() {
        Survey s = fixture();
        Question q5 = Question.find("shortText = ?1 and effectiveTo = ?2", "FixtureQ5", StructuralElement.SENTINEL).firstResult();
        SectionsQuestion sq5 = SectionsQuestion.find("surveyId = ?1 and questionId = ?2 and effectiveTo = ?3", s.id, q5.questionId, StructuralElement.SENTINEL).firstResult();
        // The fixture rule reads FixtureQ3 in Section B; point it at FixtureQ5 in Section C with no step or section named.
        em.createNativeQuery("UPDATE survey.relationships SET downstream_sq_id = ?2, downstream_ss_id = NULL, downstream_step_id = NULL WHERE survey_id = ?1")
                .setParameter(1, s.id).setParameter(2, sq5.sectionsQuestionId).executeUpdate();
        em.flush();
        em.clear();
        List<ValidationIssue> errors = validation.validate(Survey.findById(s.id)).stream().filter(ValidationIssue::isError).toList();
        assertTrue(errors.size() == 1 && "relationships".equals(errors.get(0).kind())
                && errors.get(0).message().contains("without naming the step and section"), errors.toString());

        // Name the mount and its step, the way the FHHS seed does: no error, and not backwards either.
        em.createNativeQuery("""
                UPDATE survey.relationships r SET downstream_ss_id = ss.steps_sections_id, downstream_step_id = ss.step_id
                FROM survey.steps_sections ss WHERE ss.survey_id = r.survey_id AND ss.section_id = ?2 AND r.survey_id = ?1
                """).setParameter(1, s.id).setParameter(2, sq5.sectionId).executeUpdate();
        em.flush();
        em.clear();
        assertTrue(validation.validate(Survey.findById(s.id)).stream().noneMatch(ValidationIssue::isError), "a full path passes");
    }

    /** UC-033 A3 / UC-007 BR-001: a rule whose target precedes its origin blocks export. */
    @Test
    @TestTransaction
    void ruleWhoseTargetPrecedesItsOriginIsAnError() {
        Survey s = fixture();
        Question q4 = Question.find("shortText = ?1 and effectiveTo = ?2", "FixtureQ4", StructuralElement.SENTINEL).firstResult();
        SectionsQuestion sq4 = SectionsQuestion.find("surveyId = ?1 and questionId = ?2 and effectiveTo = ?3", s.id, q4.questionId, StructuralElement.SENTINEL).firstResult();
        designer.moveQuestionWithinSection(s.id, sq4.sectionsQuestionId, 0);
        em.flush();
        em.clear();
        List<ValidationIssue> issues = validation.validate(Survey.findById(s.id));
        List<ValidationIssue> errors = issues.stream().filter(ValidationIssue::isError).toList();
        assertTrue(errors.size() == 1 && "relationships".equals(errors.get(0).kind())
                && errors.get(0).message().contains("points backwards"), issues.toString());
    }
    // ---- UC-007 BR-005: tokens are checked where they are shown -------------------------------

    private static final String STEP_TWO_QUESTION = "FixtureQ5";

    private void setText(Survey s, String shortText, String text) {
        em.createNativeQuery("UPDATE survey.questions SET text = ?1 WHERE short_text = ?2 AND survey_id = ?3")
                .setParameter(1, text).setParameter(2, shortText).setParameter(3, s.id).executeUpdate();
        em.flush();
        em.clear();
    }

    private Relationship textRuleOntoStepTwo(Survey s, String token) {
        SectionsQuestion name = SectionsQuestion.find("questionId = (select q.questionId from Question q where q.shortText = ?1 and q.surveyId = ?2 and q.effectiveTo = ?3) and effectiveTo = ?3",
                "FixtureQ2", s.id, StructuralElement.SENTINEL).firstResult();
        Step one = Step.find("name = ?1 and effectiveTo = ?2", "Fixture Step One", StructuralElement.SENTINEL).firstResult();
        Step two = Step.find("name = ?1 and effectiveTo = ?2", "Fixture Step Two", StructuralElement.SENTINEL).firstResult();
        Relationship draft = new Relationship();
        draft.upstreamSqId = name.sectionsQuestionId;
        draft.upstreamStepId = one.stepId;
        draft.downstreamStepId = two.stepId;
        draft.operatorType = OperatorType.find("name", "FIELD_EXIST").firstResult();
        draft.actionType = ActionType.find("name", "TEXT").firstResult();
        draft.token = token;
        Relationship created = rules.create(s.id, draft);
        em.flush();
        em.clear();
        return created;
    }

    @Test
    @TestTransaction
    void aPlaceholderWithoutADefaultThatNoRuleFillsIsAnError() {
        Survey s = fixture();
        setText(s, STEP_TWO_QUESTION, "Anything else, {NAME}?");
        List<ValidationIssue> issues = validation.validate(Survey.findById(s.id));
        assertTrue(issues.stream().anyMatch(i -> i.isError() && "questions".equals(i.kind())
                && i.message().contains("uses {NAME} without a default and no rule fills it there")), issues.toString());
    }

    @Test
    @TestTransaction
    void aPlaceholderWithADefaultThatNoRuleFillsIsAWarning() {
        Survey s = fixture();
        setText(s, STEP_TWO_QUESTION, "Anything else, {NAME|friend}?");
        List<ValidationIssue> issues = validation.validate(Survey.findById(s.id));
        assertTrue(issues.stream().anyMatch(i -> !i.isError() && "questions".equals(i.kind())
                && i.message().contains("uses {NAME|friend} but no rule fills it there")), issues.toString());
        assertTrue(issues.stream().noneMatch(i -> i.isError() && i.message().contains("{NAME}")), issues.toString());
    }

    @Test
    @TestTransaction
    void aTextRuleOnTheStepFillsEveryQuestionInIt() {
        Survey s = fixture();
        setText(s, STEP_TWO_QUESTION, "Anything else, {NAME}?");
        textRuleOntoStepTwo(s, "NAME");
        List<ValidationIssue> issues = validation.validate(Survey.findById(s.id));
        assertTrue(issues.stream().noneMatch(i -> i.message().contains("{NAME}")), issues.toString());
    }

    @Test
    @TestTransaction
    void aTextRuleWhoseTokenNothingUsesIsAWarning() {
        Survey s = fixture();
        Relationship r = textRuleOntoStepTwo(s, "ZZZ");
        List<ValidationIssue> issues = validation.validate(Survey.findById(s.id));
        assertTrue(issues.stream().anyMatch(i -> !i.isError() && "relationships".equals(i.kind()) && r.relationshipId.equals(i.durableId())
                && i.message().contains("fills {ZZZ} but no text it reaches uses it")), issues.toString());
    }
}
