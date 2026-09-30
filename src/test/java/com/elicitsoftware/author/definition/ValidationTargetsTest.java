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

import com.elicitsoftware.author.survey.StructureNode;
import com.elicitsoftware.author.survey.SurveyStructureQuery;
import com.elicitsoftware.model.Relationship;
import com.elicitsoftware.model.Survey;
import com.elicitsoftware.test.PostgresTestResource;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** UC-007 BR-008, BR-009 / FR-061: where a finding points, and the path it shows. */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class ValidationTargetsTest {

    @Inject
    ValidationTargets targets;

    @Inject
    SurveyStructureQuery structure;

    @Inject
    EntityManager em;

    private Survey fixture() {
        return Survey.find("name", "AuthorFixture").firstResult();
    }

    private StructureNode node(int surveyId, StructureNode.Kind kind, String label) {
        return structure.tree(surveyId, false).all().stream()
                .filter(n -> n.kind() == kind && label.equals(n.label())).findFirst().orElseThrow();
    }

    private ValidationTargets.Target targetOf(Survey s, ValidationIssue issue) {
        return targets.forSurvey(s.id).of(issue).orElseThrow();
    }

    @Test
    void aStepFindingLinksToTheStepAndShowsNoPath() {
        Survey s = fixture();
        StructureNode step = node(s.id, StructureNode.Kind.STEP, "Fixture Step One");
        ValidationTargets.Target target = targetOf(s, ValidationIssue.warning("steps", step.durableId(), "..."));

        assertEquals(ValidationTargets.Destination.DESIGNER, target.destination());
        assertEquals("step:" + step.durableId(), target.address().toString());
        assertEquals(List.of(), target.location(), "BR-008: a step's message already names it");
    }

    @Test
    void aSectionFindingNamesTheStepItSitsIn() {
        Survey s = fixture();
        StructureNode section = node(s.id, StructureNode.Kind.SECTION, "Fixture Section B");
        ValidationTargets.Target target = targetOf(s, ValidationIssue.warning("sections", section.durableId(), "..."));

        assertEquals("section:" + section.durableId(), target.address().toString());
        assertEquals(List.of("Fixture Step One"), target.location());
    }

    @Test
    void aQuestionFindingNamesItsStepAndSection() {
        Survey s = fixture();
        StructureNode question = node(s.id, StructureNode.Kind.QUESTION, "FixtureQ4");
        ValidationTargets.Target target = targetOf(s, ValidationIssue.error("questions", question.durableId(), "..."));

        assertEquals("question:" + question.durableId(), target.address().toString());
        assertEquals(List.of("Fixture Step One", "Fixture Section B"), target.location());
    }

    @Test
    void anAssignmentFindingSelectsTheElementInThatContainer() {
        Survey s = fixture();
        StructureNode section = node(s.id, StructureNode.Kind.SECTION, "Fixture Section C");
        StructureNode question = node(s.id, StructureNode.Kind.QUESTION, "FixtureQ5");

        assertEquals("ss:" + section.assignmentId(),
                targetOf(s, ValidationIssue.error("steps_sections", section.assignmentId(), "...")).address().toString());
        assertEquals("sq:" + question.assignmentId(),
                targetOf(s, ValidationIssue.error("sections_questions", question.assignmentId(), "...")).address().toString());
        assertEquals(List.of("Fixture Step Two", "Fixture Section C"),
                targetOf(s, ValidationIssue.error("sections_questions", question.assignmentId(), "...")).location());
    }

    @Test
    void aRuleFindingOpensTheRuleOverTheElementItTargets() {
        Survey s = fixture();
        Relationship rule = Relationship.find("surveyId", s.id).firstResult();
        ValidationTargets.Target target = targetOf(s, ValidationIssue.warning("relationships", rule.relationshipId, "..."));

        assertEquals("rule:" + rule.relationshipId, target.address().toString());
        assertEquals(List.of("Fixture Step One", "Fixture Section B", "FixtureQ4"), target.location(),
                "BR-008: a rule has no place of its own, so the whole path to its target is shown");
    }

    @Test
    void reportingAndTranslationFindingsOpenTheirOwnPages() {
        Survey s = fixture();
        assertEquals(ValidationTargets.Destination.REPORTING,
                targetOf(s, ValidationIssue.warning("ontology", 1, "...")).destination());
        assertEquals(ValidationTargets.Destination.REPORTING,
                targetOf(s, ValidationIssue.warning("metadata", 1, "...")).destination());
        assertEquals(ValidationTargets.Destination.TRANSLATIONS,
                targetOf(s, ValidationIssue.warning("translations", s.id, "...")).destination());
    }

    @Test
    void aFindingWithNothingToOpenIsNotALink() {
        Survey s = fixture();
        ValidationTargets.Resolver resolver = targets.forSurvey(s.id);
        assertTrue(resolver.of(ValidationIssue.warning("surveys", s.id, "The survey has no steps")).isEmpty());
        assertTrue(resolver.of(ValidationIssue.error("select_items", 1, "...")).isEmpty());
        assertTrue(resolver.of(ValidationIssue.error("relationships", 999999, "...")).isEmpty(),
                "BR-009: a rule that is gone has nothing to open");
    }

    @Test
    @TestTransaction
    void aRetiredElementIsReportedWithoutALinkTheBoardCannotFollow() {
        Survey s = fixture();
        StructureNode step = node(s.id, StructureNode.Kind.STEP, "Fixture Step Two");
        em.createNativeQuery("UPDATE survey.steps SET effective_to = now() WHERE survey_id = ?1 AND step_id = ?2")
                .setParameter(1, s.id).setParameter(2, step.durableId()).executeUpdate();
        em.flush();
        em.clear();

        Optional<ValidationTargets.Target> target = targets.forSurvey(s.id)
                .of(ValidationIssue.warning("steps", step.durableId(), "..."));
        assertTrue(target.isEmpty(), "BR-009: the board hides retired elements, so the link would land nowhere");
    }
}
