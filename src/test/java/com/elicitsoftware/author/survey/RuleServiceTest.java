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
import com.elicitsoftware.model.ActionType;
import com.elicitsoftware.model.OperatorType;
import com.elicitsoftware.model.Question;
import com.elicitsoftware.model.Relationship;
import com.elicitsoftware.model.SectionsQuestion;
import com.elicitsoftware.model.Step;
import com.elicitsoftware.model.StructuralElement;
import com.elicitsoftware.model.Survey;
import com.elicitsoftware.test.PostgresTestResource;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class RuleServiceTest {

    @Inject
    RuleService rules;

    @Inject
    RelationshipQuery query;

    @Inject
    ElementService elements;

    private Survey fixture() {
        return Survey.find("name", "AuthorFixture").firstResult();
    }

    private SectionsQuestion assignmentOf(String shortText) {
        Question q = Question.find("shortText = ?1 and effectiveTo = ?2", shortText, StructuralElement.SENTINEL).firstResult();
        return elements.currentAssignmentsOfQuestion(q.surveyId, q.questionId).get(0);
    }

    private static OperatorType operator(String name) {
        return OperatorType.find("name", name).firstResult();
    }

    private static ActionType action(String name) {
        return ActionType.find("name", name).firstResult();
    }

    @Test
    void edges_anchorOnAssignmentsAndReadAsASentence() {
        Survey s = fixture();
        List<RelationshipQuery.RuleEdge> edges = query.edges(s.id, false);
        assertEquals(1, edges.size(), "UC-020 BR-001: one edge per current rule");
        RelationshipQuery.RuleEdge e = edges.get(0);
        assertEquals("sq-" + assignmentOf("FixtureQ3").sectionsQuestionId, e.source(), "UC-020 BR-002: anchored on the question assignment");
        assertEquals("sq-" + assignmentOf("FixtureQ4").sectionsQuestionId, e.target());
        assertEquals("SHOW", e.action());
        assertEquals("= Y", e.condition(), "UC-019 BR-003: (operator) value in the condition");
        assertTrue(e.label().startsWith("Show "), "the label reads the action and target: " + e.label());
        assertTrue(e.sourceFallbacks().get(0).startsWith("ss-"), "UC-020 step 3: the card holding the row comes first, for closed cards");
        assertTrue(e.sourceFallbacks().stream().anyMatch(f -> f.startsWith("step-")), "then its step, for a collapsed step: " + e.sourceFallbacks());
    }

    @Test
    @TestTransaction
    void create_repeatFromCountToSection_thenRepointAndRetire() {
        Survey s = fixture();
        SectionsQuestion count = assignmentOf("FixtureQ4");
        Step two = Step.find("name = ?1 and effectiveTo = ?2", "Fixture Step Two", StructuralElement.SENTINEL).firstResult();
        int sectionC = elements.currentSectionAssignmentsOfStep(s.id, two.stepId).get(0).stepsSectionsId;

        Relationship draft = new Relationship();
        draft.upstreamSqId = count.sectionsQuestionId;
        draft.downstreamSsId = sectionC;
        draft.operatorType = operator("GREATER THAN");
        draft.actionType = action("REPEAT");
        draft.referenceValue = "0";
        Relationship created = rules.create(s.id, draft);
        assertNotNull(created.relationshipKey, "UC-019 step 6: key minted once");
        assertEquals("> 0", RelationshipQuery.condition(created));
        assertEquals("> 0", created.description, "description defaults to the condition");

        assertEquals(2, query.edges(s.id, false).size());
        assertTrue(query.edges(s.id, false).stream().anyMatch(e -> e.target().equals("ss-" + sectionC) && e.action().equals("REPEAT")));

        // UC-019 A5: re-point the head to a question; the tail to the 'children?' question
        SectionsQuestion q5 = assignmentOf("FixtureQ5");
        rules.repoint(s.id, created.relationshipId, RuleService.End.TARGET, "sq-" + q5.sectionsQuestionId);
        Relationship after = rules.current(s.id, created.relationshipId).orElseThrow();
        assertEquals(q5.sectionsQuestionId, after.downstreamSqId);
        assertEquals(null, after.downstreamSsId, "UC-019 BR-001: exactly one downstream");

        // UC-019 A3: REPEAT onto a step is refused
        AuthoringException a3 = assertThrows(AuthoringException.class,
                () -> rules.repoint(s.id, created.relationshipId, RuleService.End.TARGET, "step-" + two.stepId));
        assertEquals("downstream", a3.field());

        rules.retire(s.id, created.relationshipId);
        assertEquals(1, query.edges(s.id, false).size());
        assertEquals(2, query.edges(s.id, true).size(), "UC-020 A1: removed rules on request");
        rules.restore(s.id, created.relationshipId);
        assertTrue(rules.current(s.id, created.relationshipId).isPresent());
    }

    @Test
    @TestTransaction
    void create_textActionNeedsAShortToken() {
        Survey s = fixture();
        Relationship draft = new Relationship();
        draft.upstreamSqId = assignmentOf("FixtureQ2").sectionsQuestionId;
        draft.downstreamSqId = assignmentOf("FixtureQ5").sectionsQuestionId;
        draft.operatorType = operator("FIELD_EXIST");
        draft.actionType = action("TEXT");
        AuthoringException missing = assertThrows(AuthoringException.class, () -> rules.create(s.id, draft));
        assertEquals("token", missing.field(), "UC-019 A2");

        draft.token = "WAYTOOLONGTOKEN";
        assertEquals("token", assertThrows(AuthoringException.class, () -> rules.create(s.id, draft)).field());

        draft.token = " NAME ";
        Relationship created = rules.create(s.id, draft);
        assertEquals("NAME", created.token);
        assertEquals("exists {NAME}", RelationshipQuery.condition(created));
        assertEquals("Replace {NAME}", RelationshipQuery.label(created, "ignored"), "TEXT reads Replace {token}");
    }

    @Test
    @TestTransaction
    void create_refusesTwoTargetsNoTargetAndSelfTarget() {
        Survey s = fixture();
        SectionsQuestion q2 = assignmentOf("FixtureQ2");
        Relationship draft = new Relationship();
        draft.upstreamSqId = q2.sectionsQuestionId;
        draft.operatorType = operator("EQUAL");
        draft.actionType = action("SHOW");
        draft.referenceValue = "x";
        assertEquals("downstream", assertThrows(AuthoringException.class, () -> rules.create(s.id, draft)).field(), "no target");

        draft.downstreamSqId = q2.sectionsQuestionId;
        assertEquals("downstream", assertThrows(AuthoringException.class, () -> rules.create(s.id, draft)).field(), "self target");

        draft.downstreamSqId = assignmentOf("FixtureQ5").sectionsQuestionId;
        draft.downstreamStepId = Step.<Step>find("name", "Fixture Step One").firstResult().stepId;
        assertEquals("downstream", assertThrows(AuthoringException.class, () -> rules.create(s.id, draft)).field(), "UC-019 BR-001: two targets");
    }
}
