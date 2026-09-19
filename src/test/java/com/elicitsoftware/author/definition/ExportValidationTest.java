package com.elicitsoftware.author.definition;

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

import com.elicitsoftware.model.Survey;
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
}
