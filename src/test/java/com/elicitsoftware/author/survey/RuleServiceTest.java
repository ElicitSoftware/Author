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
import com.elicitsoftware.model.ActionType;
import com.elicitsoftware.model.OperatorType;
import com.elicitsoftware.model.Question;
import com.elicitsoftware.model.Relationship;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    @Inject
    DesignerService designer;

    @Inject
    EntityManager em;

    private Survey fixture() {
        return Survey.find("name", "AuthorFixture").firstResult();
    }

    private SectionsQuestion assignmentOf(String shortText) {
        Question q = Question.find("shortText = ?1 and effectiveTo = ?2", shortText, StructuralElement.SENTINEL).firstResult();
        return elements.currentAssignmentsOfQuestion(q.surveyId, q.questionId).get(0);
    }

    private static Step step(String name) {
        return Step.find("name = ?1 and effectiveTo = ?2", name, StructuralElement.SENTINEL).firstResult();
    }

    private StepsSections mountOf(String sectionName) {
        return StepsSections.find("sectionId = (select s.sectionId from Section s where s.name = ?1 and s.effectiveTo = ?2) and effectiveTo = ?2",
                sectionName, StructuralElement.SENTINEL).firstResult();
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
        assertEquals(two.stepId, created.downstreamStepId, "UC-019 BR-001: the section's step completes the path");
        assertEquals("> 0", RelationshipQuery.condition(created));
        assertEquals("> 0", created.description, "description defaults to the condition");

        assertEquals(2, query.edges(s.id, false).size());
        assertTrue(query.edges(s.id, false).stream().anyMatch(e -> e.target().equals("ss-" + sectionC) && e.action().equals("REPEAT")));

        // UC-019 A5: re-point the head to a question; the tail to the 'children?' question
        SectionsQuestion q5 = assignmentOf("FixtureQ5");
        rules.repoint(s.id, created.relationshipId, RuleService.End.TARGET, "sq-" + q5.sectionsQuestionId);
        Relationship after = rules.current(s.id, created.relationshipId).orElseThrow();
        assertEquals(q5.sectionsQuestionId, after.downstreamSqId);
        assertEquals(sectionC, after.downstreamSsId, "UC-019 BR-001: a question in another section names its only mount");
        assertEquals(two.stepId, after.downstreamStepId, "and that mount's step");

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
        assertEquals("exists <NAME>", RelationshipQuery.condition(created));
        assertEquals("Replace <NAME>", RelationshipQuery.label(created, "ignored"), "TEXT reads Replace <TOKEN>");
    }

    /**
     * UC-019 BR-001: a target in the upstream question's own section is stored question-only, whatever
     * path the caller pre-filled; a target in another section keeps its step and section.
     */
    @Test
    @TestTransaction
    void create_sameSectionTargetIsStoredQuestionOnly() {
        Survey s = fixture();
        SectionsQuestion q3 = assignmentOf("FixtureQ3");
        SectionsQuestion q4 = assignmentOf("FixtureQ4");
        Step one = step("Fixture Step One");
        StepsSections sectionB = mountOf("Fixture Section B");

        Relationship draft = new Relationship();
        draft.upstreamSqId = q3.sectionsQuestionId;
        draft.upstreamStepId = one.stepId;
        draft.downstreamSqId = q4.sectionsQuestionId;
        draft.downstreamSsId = sectionB.stepsSectionsId; // the full path, as a card on the board would name it
        draft.downstreamStepId = one.stepId;
        draft.operatorType = operator("EQUAL");
        draft.actionType = action("REPEAT");
        draft.referenceValue = "N";
        Relationship created = rules.create(s.id, draft);
        Relationship stored = rules.current(s.id, created.relationshipId).orElseThrow();
        assertEquals(q4.sectionsQuestionId, stored.downstreamSqId);
        assertNull(stored.downstreamSsId, "same section: no downstream section");
        assertNull(stored.downstreamStepId, "same section: no downstream step");
        assertNull(stored.upstreamStepId, "same section: fires from every mount");

        Relationship cross = new Relationship();
        cross.upstreamSqId = q4.sectionsQuestionId;
        cross.upstreamStepId = one.stepId; // the mount the arrow was drawn from, as the dialog names it
        cross.downstreamSqId = assignmentOf("FixtureQ5").sectionsQuestionId;
        cross.operatorType = operator("FIELD_EXIST");
        cross.actionType = action("SHOW");
        Relationship crossCreated = rules.create(s.id, cross);
        Step two = step("Fixture Step Two");
        assertEquals(mountOf("Fixture Section C").stepsSectionsId, crossCreated.downstreamSsId, "another section: its mount is named");
        assertEquals(two.stepId, crossCreated.downstreamStepId, "and its step");
        assertEquals(one.stepId, crossCreated.upstreamStepId, "and the upstream step it fires from");

        // A5: re-pointing both ends onto rows of one card is stored question-only too
        Relationship repointed = rules.repoint(s.id, crossCreated.relationshipId, RuleService.End.SOURCE, "sq-" + q3.sectionsQuestionId, "ss-" + sectionB.stepsSectionsId);
        repointed = rules.repoint(s.id, repointed.relationshipId, RuleService.End.TARGET, "sq-" + q4.sectionsQuestionId, "ss-" + sectionB.stepsSectionsId);
        assertEquals(q4.sectionsQuestionId, repointed.downstreamSqId);
        assertNull(repointed.downstreamSsId, "re-pointed into the upstream's own section: question-only");
        assertNull(repointed.downstreamStepId);
        assertNull(repointed.upstreamStepId);
    }

    /** UC-019 BR-005: a token is a slot any rule may fill — a SHOW rule keeps the token it is saved with. */
    @Test
    @TestTransaction
    void create_showActionKeepsItsToken() {
        Survey s = fixture();
        Relationship draft = new Relationship();
        draft.upstreamSqId = assignmentOf("FixtureQ2").sectionsQuestionId;
        draft.downstreamSqId = assignmentOf("FixtureQ5").sectionsQuestionId;
        draft.operatorType = operator("FIELD_EXIST");
        draft.actionType = action("SHOW");
        Relationship without = rules.create(s.id, draft);
        assertNull(without.token, "a SHOW rule needs no token");

        draft.token = " NAME ";
        Relationship created = rules.create(s.id, draft);
        assertEquals("NAME", created.token, "the token is kept, trimmed, for a non-Text action");
        assertEquals("NAME", rules.current(s.id, created.relationshipId).orElseThrow().token, "and persisted");
        assertEquals("exists <NAME>", RelationshipQuery.condition(created), "the tooltip shows the token for any action");
        assertTrue(RelationshipQuery.label(created, "Q5").startsWith("Show "), "the label still reads the action");

        draft.token = "WAYTOOLONGTOKEN";
        assertEquals("token", assertThrows(AuthoringException.class, () -> rules.create(s.id, draft)).field(), "the 10-character limit holds for every action");
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
        AuthoringException path = assertThrows(AuthoringException.class, () -> rules.create(s.id, draft));
        assertEquals("downstream", path.field(), "UC-019 BR-001: a step that does not hold the question's section");
        assertTrue(path.getMessage().contains("without naming the step and section"), path.getMessage());

        draft.downstreamStepId = null;
        draft.downstreamSsId = mountOf("Fixture Section A").stepsSectionsId;
        path = assertThrows(AuthoringException.class, () -> rules.create(s.id, draft));
        assertEquals("downstream", path.field(), "UC-019 BR-001: a section that does not hold the question");
        assertTrue(path.getMessage().contains("does not hold that question"), path.getMessage());
    }

    /** UC-019 BR-001: a rule created the way the FHHS seed writes it — step, section and question all named — is accepted as is. */
    @Test
    @TestTransaction
    void create_acceptsAFullPathAndCompletesAPartialOne() {
        Survey s = fixture();
        Step one = step("Fixture Step One");
        Step two = step("Fixture Step Two");
        Relationship full = new Relationship();
        full.upstreamStepId = one.stepId;
        full.upstreamSqId = assignmentOf("FixtureQ4").sectionsQuestionId;
        full.downstreamStepId = two.stepId;
        full.downstreamSsId = mountOf("Fixture Section C").stepsSectionsId;
        full.downstreamSqId = assignmentOf("FixtureQ5").sectionsQuestionId;
        full.operatorType = operator("GREATER THAN");
        full.actionType = action("REPEAT");
        full.referenceValue = "0";
        Relationship created = rules.create(s.id, full);
        assertEquals(two.stepId, created.downstreamStepId);
        assertEquals(mountOf("Fixture Section C").stepsSectionsId, created.downstreamSsId);
        assertEquals(one.stepId, created.upstreamStepId, "the upstream step names the mount the rule fires from");
        assertEquals("sq-" + full.downstreamSqId, query.edges(s.id, false).stream()
                .filter(e -> e.relationshipId() == created.relationshipId).findFirst().orElseThrow().target(),
                "UC-020: the arrow lands on the innermost element of the path");

        // A question in the upstream question's own section keeps no path and fires from every mount.
        Relationship samePage = new Relationship();
        samePage.upstreamStepId = one.stepId;
        samePage.upstreamSqId = assignmentOf("FixtureQ3").sectionsQuestionId;
        samePage.downstreamStepId = one.stepId;
        samePage.downstreamSqId = assignmentOf("FixtureQ4").sectionsQuestionId;
        samePage.operatorType = operator("EQUAL");
        samePage.actionType = action("SHOW");
        samePage.referenceValue = "N";
        Relationship page = rules.create(s.id, samePage);
        assertEquals(null, page.downstreamStepId, "UC-019 BR-001: no path for a question on the same page");
        assertEquals(null, page.downstreamSsId);
        assertEquals(null, page.upstreamStepId, "and no upstream step: it fires from every mount");
    }

    /** UC-033 A1: a target that is not after the question the rule reads from is refused on create. */
    @Test
    @TestTransaction
    void create_refusesTargetBeforeOrigin() {
        Survey s = fixture();
        // FixtureQ4 sits in Step One › Section B as its second question.
        Relationship draft = new Relationship();
        draft.upstreamSqId = assignmentOf("FixtureQ4").sectionsQuestionId;
        draft.operatorType = operator("EQUAL");
        draft.actionType = action("SHOW");
        draft.referenceValue = "1";

        draft.downstreamStepId = step("Fixture Step One").stepId;
        AuthoringException ownStep = assertThrows(AuthoringException.class, () -> rules.create(s.id, draft));
        assertEquals("downstream", ownStep.field(), "UC-033 BR-001: the origin's own step is not after it");
        assertTrue(ownStep.getMessage().contains("points backwards"), ownStep.getMessage());
        assertTrue(ownStep.getMessage().contains("'Fixture Step One'"), "the reason names the target: " + ownStep.getMessage());

        draft.downstreamStepId = null;
        draft.downstreamSsId = mountOf("Fixture Section A").stepsSectionsId;
        assertEquals("downstream", assertThrows(AuthoringException.class, () -> rules.create(s.id, draft)).field(), "an earlier section in the same step");
        draft.downstreamSsId = mountOf("Fixture Section B").stepsSectionsId;
        assertEquals("downstream", assertThrows(AuthoringException.class, () -> rules.create(s.id, draft)).field(), "the origin's own section");

        draft.downstreamStepId = null; // completing the path filled in the section's step
        draft.downstreamSsId = null;
        draft.downstreamSqId = assignmentOf("FixtureQ3").sectionsQuestionId;
        assertEquals("downstream", assertThrows(AuthoringException.class, () -> rules.create(s.id, draft)).field(), "an earlier question in the same section");
        draft.downstreamSqId = assignmentOf("FixtureQ2").sectionsQuestionId;
        assertEquals("downstream", assertThrows(AuthoringException.class, () -> rules.create(s.id, draft)).field(), "a question in an earlier section");

        // Forward targets are accepted: a question in a later step, and the later step itself.
        draft.downstreamStepId = null;
        draft.downstreamSsId = null;
        draft.downstreamSqId = assignmentOf("FixtureQ5").sectionsQuestionId;
        assertNotNull(rules.create(s.id, draft).relationshipId);
        Relationship toStep = new Relationship();
        toStep.upstreamSqId = draft.upstreamSqId;
        toStep.downstreamStepId = step("Fixture Step Two").stepId;
        toStep.operatorType = operator("EQUAL");
        toStep.actionType = action("SHOW");
        toStep.referenceValue = "1";
        assertNotNull(rules.create(s.id, toStep).relationshipId);
        assertEquals(3, query.edges(s.id, false).size());
        assertTrue(query.edges(s.id, false).stream().noneMatch(RelationshipQuery.RuleEdge::invalid), "UC-033: nothing accepted is backward");
    }

    /** UC-033 A1 via UC-019 A5: re-pointing either end so the rule points backwards is refused and leaves the rule as it was. */
    @Test
    @TestTransaction
    void repoint_refusesABackwardHeadOrTail() {
        Survey s = fixture();
        Relationship existing = Relationship.find("surveyId = ?1 and effectiveTo = ?2", s.id, StructuralElement.SENTINEL).firstResult();
        int q4 = assignmentOf("FixtureQ4").sectionsQuestionId;
        assertEquals(q4, existing.downstreamSqId, "fixture: FixtureQ3 shows FixtureQ4");

        AuthoringException head = assertThrows(AuthoringException.class,
                () -> rules.repoint(s.id, existing.relationshipId, RuleService.End.TARGET, "sq-" + assignmentOf("FixtureQ2").sectionsQuestionId));
        assertEquals("downstream", head.field());
        assertEquals(q4, rules.current(s.id, existing.relationshipId).orElseThrow().downstreamSqId, "a refused re-point changes nothing");

        AuthoringException tail = assertThrows(AuthoringException.class,
                () -> rules.repoint(s.id, existing.relationshipId, RuleService.End.SOURCE, "sq-" + assignmentOf("FixtureQ5").sectionsQuestionId));
        assertTrue(tail.getMessage().contains("points backwards"), tail.getMessage());
        assertEquals(assignmentOf("FixtureQ3").sectionsQuestionId, rules.current(s.id, existing.relationshipId).orElseThrow().upstreamSqId);
    }

    /** UC-033 A2: a removed rule whose target has since moved before its origin cannot be restored. */
    @Test
    @TestTransaction
    void restore_refusesARuleThatIsNowBackward() {
        Survey s = fixture();
        Relationship existing = Relationship.find("surveyId = ?1 and effectiveTo = ?2", s.id, StructuralElement.SENTINEL).firstResult();
        rules.retire(s.id, existing.relationshipId);
        designer.moveQuestionWithinSection(s.id, assignmentOf("FixtureQ4").sectionsQuestionId, 0); // FixtureQ4 now before FixtureQ3

        AuthoringException refused = assertThrows(AuthoringException.class, () -> rules.restore(s.id, existing.relationshipId));
        assertTrue(refused.getMessage().contains("points backwards"), refused.getMessage());
        assertTrue(rules.current(s.id, existing.relationshipId).isEmpty(), "the rule stays removed");

        designer.moveQuestionWithinSection(s.id, assignmentOf("FixtureQ4").sectionsQuestionId, 1); // back where it was
        rules.restore(s.id, existing.relationshipId);
        assertTrue(rules.current(s.id, existing.relationshipId).isPresent(), "forward again, so restorable");
    }

    /** UC-033 BR-004: a second mount of the origin's section that sits after the target makes the rule backward. */
    @Test
    @TestTransaction
    void create_everyMountOfTheOriginCounts() {
        Survey s = fixture();
        Relationship draft = new Relationship();
        draft.upstreamSqId = assignmentOf("FixtureQ3").sectionsQuestionId; // Section B, mounted in Step One
        draft.downstreamSqId = assignmentOf("FixtureQ5").sectionsQuestionId; // Section C, Step Two
        draft.operatorType = operator("EQUAL");
        draft.actionType = action("SHOW");
        draft.referenceValue = "Y";
        Relationship created = rules.create(s.id, draft);
        assertTrue(query.edges(s.id, false).stream().noneMatch(RelationshipQuery.RuleEdge::invalid));

        // Mount Section B into Step Two after Section C: from that mount the rule points backwards.
        designer.mountSection(s.id, mountOf("Fixture Section B").sectionId, step("Fixture Step Two").stepId, 99);
        RelationshipQuery.RuleEdge edge = query.edges(s.id, false).stream()
                .filter(e -> e.relationshipId() == created.relationshipId).findFirst().orElseThrow();
        assertTrue(edge.invalid(), "UC-033 BR-004: backward for one mount pairing is backward");
        assertTrue(edge.problem().startsWith("Points backwards"), edge.problem());

        Relationship again = new Relationship();
        again.upstreamSqId = draft.upstreamSqId;
        again.downstreamSqId = draft.downstreamSqId;
        again.operatorType = operator("NOT_EQUAL");
        again.actionType = action("SHOW");
        again.referenceValue = "N";
        assertEquals("downstream", assertThrows(AuthoringException.class, () -> rules.create(s.id, again)).field());
    }
    // ---- UC-019 BR-006: a token source is a question that collects a value --------------------

    /** FixtureQ1 is the fixture's HTML question (type_id 4), so it collects no answer. */
    private Relationship displayOnlySourcedDraft(Survey s, String defaultUpstreamValue) {
        Relationship draft = new Relationship();
        draft.upstreamSqId = assignmentOf("FixtureQ1").sectionsQuestionId;
        draft.downstreamSqId = assignmentOf("FixtureQ5").sectionsQuestionId;
        draft.operatorType = operator("FIELD_EXIST");
        draft.actionType = action("TEXT");
        draft.token = "S1";
        draft.defaultUpstreamValue = defaultUpstreamValue;
        return draft;
    }

    @Test
    @TestTransaction
    void create_tokenFedByADisplayOnlyQuestionWithNoValueOfItsOwn_isRefused() {
        Survey s = fixture();

        AuthoringException refused = assertThrows(AuthoringException.class,
                () -> rules.create(s.id, displayOnlySourcedDraft(s, null)));

        assertEquals("upstreamSqId", refused.field(), "the message points at the question the rule reads");
        assertTrue(refused.getMessage().contains("HTML"), refused.getMessage());
        assertTrue(refused.getMessage().contains("{<S1>}"), refused.getMessage());
    }

    @Test
    @TestTransaction
    void create_tokenFedByADisplayOnlyQuestionThatCarriesItsOwnValue_isAccepted() {
        Survey s = fixture();

        // The idiom the Family History Survey uses on its Mother and Father steps: the phrase is the
        // rule's own constant, so it fills the slot without reading an answer. Warned about at export
        // (UC-007 BR-005), not refused here -- the runtime does fill it.
        Relationship created = rules.create(s.id, displayOnlySourcedDraft(s, "{G1's|your} mother"));

        assertNotNull(created.relationshipId);
        assertEquals("{G1's|your} mother", created.defaultUpstreamValue);
    }

    @Test
    @TestTransaction
    void create_tokenFedByAQuestionThatCollectsAnAnswer_isAccepted() {
        Survey s = fixture();
        Relationship draft = displayOnlySourcedDraft(s, null);
        draft.upstreamSqId = assignmentOf("FixtureQ2").sectionsQuestionId; // TEXT

        assertNotNull(rules.create(s.id, draft).relationshipId);
    }

    @Test
    void upstreamCollectsAnswer_readsTheTypesDataTypeFlag() {
        Survey s = fixture();

        assertFalse(rules.upstreamCollectsAnswer(s.id, assignmentOf("FixtureQ1").sectionsQuestionId), "HTML");
        assertTrue(rules.upstreamCollectsAnswer(s.id, assignmentOf("FixtureQ2").sectionsQuestionId), "TEXT");
        assertTrue(rules.upstreamCollectsAnswer(s.id, assignmentOf("FixtureQ4").sectionsQuestionId), "INTEGER");
        assertTrue(rules.upstreamCollectsAnswer(s.id, null), "an unresolvable upstream is reported by its own check");
    }

    // ---- UC-019 A2 / FR-012: a rule's token is a name, not a reference ------------------------

    private Relationship tokenDraft(Survey s, String token) {
        Relationship draft = new Relationship();
        draft.upstreamSqId = assignmentOf("FixtureQ2").sectionsQuestionId;
        draft.downstreamSqId = assignmentOf("FixtureQ5").sectionsQuestionId;
        draft.operatorType = operator("FIELD_EXIST");
        draft.actionType = action("TEXT");
        draft.token = token;
        return draft;
    }

    /**
     * Author#21: the runtime keys a text's values on this column verbatim, so a token stored
     * {@code <S1>} is asked for as {@code S1} and fills nothing. An author who types the reference, or
     * the name in lower case, is saved from it.
     */
    @Test
    @TestTransaction
    void create_storesTheNameAloneUpperCase() {
        Survey s = fixture();
        Relationship fromReference = rules.create(s.id, tokenDraft(s, " <s1> "));
        assertEquals("S1", fromReference.token, "the brackets belong to the text that names the token");
        assertEquals("S1", rules.current(s.id, fromReference.relationshipId).orElseThrow().token, "and persisted");
        assertEquals("exists <S1>", RelationshipQuery.condition(fromReference));

        assertEquals("S1", rules.create(s.id, tokenDraft(s, "s1")).token, "FR-012: upper case where it is authored");
    }

    /** The limit counts the name, not brackets that are about to be dropped. */
    @Test
    @TestTransaction
    void create_measuresTheLengthOfTheNameNotItsBrackets() {
        Survey s = fixture();
        assertEquals("GRANDMAMA", rules.create(s.id, tokenDraft(s, "<GRANDMAMA>")).token,
                "eleven characters as typed, nine of name");
        assertEquals("token", assertThrows(AuthoringException.class,
                () -> rules.create(s.id, tokenDraft(s, "WAYTOOLONGTOKEN"))).field(),
                "the " + Tokens.MAX_LENGTH + "-character limit still holds for the name itself");
    }

    /** A phrase names a token as {@code <NAME>}, so a token no phrase could spell fills nothing. */
    @Test
    @TestTransaction
    void create_refusesATokenNoTextCouldName() {
        Survey s = fixture();
        for (String bad : List.of("S 1", "{S1}", "Q#", "S-1", "1S")) {
            AuthoringException e = assertThrows(AuthoringException.class, () -> rules.create(s.id, tokenDraft(s, bad)),
                    "UC-019 A2 should refuse '" + bad + "'");
            assertEquals("token", e.field(), bad);
        }
    }

    /**
     * An import writes the column directly (UC-005), so a bracketed token can exist. Every write path
     * runs through validate, so touching such a rule at all repairs it.
     */
    @Test
    @TestTransaction
    void repoint_repairsATokenAnImportBroughtIn() {
        Survey s = fixture();
        Relationship created = rules.create(s.id, tokenDraft(s, "S1"));
        em.createNativeQuery("UPDATE survey.relationships SET token = '<S1>' WHERE id = ?1")
                .setParameter(1, created.id).executeUpdate();
        em.flush();
        em.clear();
        assertTrue(Tokens.known(s.id).contains("S1"), "known offers the name, whatever the column holds");
        assertFalse(Tokens.known(s.id).contains("<S1>"), "and never a second entry for the same slot");

        rules.repoint(s.id, created.relationshipId, RuleService.End.SOURCE, "sq-" + assignmentOf("FixtureQ3").sectionsQuestionId);

        assertEquals("S1", rules.current(s.id, created.relationshipId).orElseThrow().token);
    }
}
