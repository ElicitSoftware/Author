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

import com.elicitsoftware.model.ActionType;
import com.elicitsoftware.model.Question;
import com.elicitsoftware.model.QuestionType;
import com.elicitsoftware.model.Relationship;
import com.elicitsoftware.model.SectionsQuestion;
import com.elicitsoftware.model.StepsSections;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** FR-013 / UC-019 BR-005: token values scoped the way the Survey runtime scopes them. */
class TokenSamplesTest {

    // Step 10 mounts section 20 (mount 200) holding question 30 (assignment 300) and question 31 (301).
    // Section 20 is also mounted in step 11 (mount 201). Question 40 (assignment 400) is the upstream, in section 21 / step 10.
    private static final StepsSections MOUNT_200 = mount(200, 10, 20);
    private static final StepsSections MOUNT_201 = mount(201, 11, 20);
    private static final StepsSections MOUNT_210 = mount(210, 10, 21);
    private static final SectionsQuestion SQ_300 = assignment(300, 30, 20);
    private static final SectionsQuestion SQ_301 = assignment(301, 31, 20);
    private static final SectionsQuestion SQ_400 = assignment(400, 40, 21);
    private static final List<StepsSections> MOUNTS = List.of(MOUNT_200, MOUNT_201, MOUNT_210);
    private static final List<SectionsQuestion> ASSIGNMENTS = List.of(SQ_300, SQ_301, SQ_400);

    @Test
    void aRuleOnTheStepReachesEveryQuestionInIt() {
        TokenSamples samples = TokenSamples.of(List.of(rule("TEXT", "NAME", null, 400, 10, null, null)), MOUNTS, ASSIGNMENTS, Map.of(40, "Bob"));
        assertEquals("Bob", samples.forMount(10, 200, 300).get("NAME"));
        assertEquals("Bob", samples.forMount(10, 200, 301).get("NAME"));
        assertEquals("Bob", samples.forMount(10, 200, null).get("NAME"), "the section title in that step too");
        assertNull(samples.forMount(11, 201, 300).get("NAME"), "but not the same section mounted in another step");
    }

    @Test
    void innerRulesOverrideOuterOnes() {
        TokenSamples samples = TokenSamples.of(List.of(
                rule("TEXT", "NAME", null, 400, 10, null, null),
                rule("TEXT", "NAME", "the section's", 400, 10, 200, null),
                rule("TEXT", "NAME", "the question's", 400, 10, 200, 301)), MOUNTS, ASSIGNMENTS, Map.of(40, "Bob"));
        assertEquals("the section's", samples.forMount(10, 200, 300).get("NAME"));
        assertEquals("the question's", samples.forMount(10, 200, 301).get("NAME"));
    }

    @Test
    void aDefaultUpstreamValueBeatsTheSampleAndAnyActionFills() {
        // FHHS fills {<S1>} from SHOW rules whose default upstream value is "{<G1>'s|your} mother".
        TokenSamples samples = TokenSamples.of(List.of(rule("SHOW", "S1", "{<G1>'s|your} mother", 400, 10, null, null)),
                MOUNTS, ASSIGNMENTS, Map.of(40, "Bob"));
        assertEquals("{<G1>'s|your} mother", samples.forMount(10, 200, 300).get("S1"));
    }

    @Test
    void aRuleWithNothingToWriteStillFillsTheToken() {
        TokenSamples samples = TokenSamples.of(List.of(rule("TEXT", "NAME", null, 400, 10, null, null)), MOUNTS, ASSIGNMENTS, Map.of());
        assertTrue(samples.fills(new Tokens.Placeholder("Has <NAME> been", "Were you"), 10, 200, 300), "validation sees the rule");
        assertFalse(samples.fills(new Tokens.Placeholder("<OTHER>", null), 10, 200, 300));
        assertFalse(samples.forMount(10, 200, 300).containsKey("NAME"), "the preview falls back to the placeholder's default");
    }

    /**
     * UC-007 BR-005 / ElicitSoftware/Author#18: the Survey runtime abandons a phrase the moment one of
     * its references has no value, so a phrase is filled only when every reference is reached. Reading
     * it as "any reference is reached" passed a two-token phrase whose second token nothing fills, and
     * the respondent read the default.
     */
    @Test
    void aPhraseIsFilledOnlyWhenEveryReferenceIsReached() {
        TokenSamples samples = TokenSamples.of(List.of(
                rule("TEXT", "NAME", "Bob", 400, 10, null, null),
                rule("TEXT", "OTHER", "Sue", 400, 11, null, null)), MOUNTS, ASSIGNMENTS, Map.of());
        assertTrue(samples.fills(new Tokens.Placeholder("<NAME>", "you"), 10, 200, 300));
        assertFalse(samples.fills(new Tokens.Placeholder("<NAME> and <OTHER>", "you both"), 10, 200, 300),
                "the step 11 rule does not reach step 10, so the runtime would render \"you both\"");

        TokenSamples both = TokenSamples.of(List.of(
                rule("TEXT", "NAME", "Bob", 400, 10, null, null),
                rule("TEXT", "OTHER", "Sue", 400, 10, null, null)), MOUNTS, ASSIGNMENTS, Map.of());
        assertTrue(both.fills(new Tokens.Placeholder("<NAME> and <OTHER>", "you both"), 10, 200, 300),
                "both references reached");
    }

    /** A phrase naming no token fills nothing: the runtime has nothing to substitute and falls back. */
    @Test
    void aPhraseWithNoReferenceIsNotFilled() {
        TokenSamples samples = TokenSamples.of(List.of(rule("TEXT", "NAME", "Bob", 400, 10, null, null)),
                MOUNTS, ASSIGNMENTS, Map.of());
        assertFalse(samples.fills(new Tokens.Placeholder("Does NAME", "Do you"), 10, 200, 300));
    }

    @Test
    void forQuestionUnitesEveryPlacement() {
        TokenSamples samples = TokenSamples.of(List.of(
                rule("TEXT", "NAME", null, 400, 10, null, null),
                rule("TEXT", "OTHER", "elsewhere", 400, 11, null, null)), MOUNTS, ASSIGNMENTS, Map.of(40, "Bob"));
        Map<String, String> values = samples.forQuestion(30);
        assertEquals("Bob", values.get("NAME"), "from the step 10 placement");
        assertEquals("elsewhere", values.get("OTHER"), "from the step 11 placement");
        Map<String, String> upstream = samples.forQuestion(40);
        assertEquals("Bob", upstream.get("NAME"), "the step 10 rule reaches the upstream's own section too");
        assertFalse(upstream.containsKey("OTHER"), "the step 11 rule does not");
    }

    private static Relationship rule(String action, String token, String defaultValue, int upstreamSq, Integer step, Integer ss, Integer sq) {
        Relationship r = new Relationship();
        r.actionType = new ActionType();
        r.actionType.name = action;
        r.token = token;
        r.defaultUpstreamValue = defaultValue;
        r.upstreamSqId = upstreamSq;
        r.downstreamStepId = step;
        r.downstreamSsId = ss;
        r.downstreamSqId = sq;
        return r;
    }

    private static StepsSections mount(int id, int stepId, int sectionId) {
        StepsSections ss = new StepsSections();
        ss.stepsSectionsId = id;
        ss.stepId = stepId;
        ss.sectionId = sectionId;
        return ss;
    }

    private static SectionsQuestion assignment(int id, int questionId, int sectionId) {
        SectionsQuestion sq = new SectionsQuestion();
        sq.sectionsQuestionId = id;
        sq.questionId = questionId;
        sq.sectionId = sectionId;
        return sq;
    }
    // ---- UC-019 BR-006: a display-only question has no sample a token could take ---------------

    private static Question question(int questionId, String typeName, String dataType, String sample) {
        QuestionType type = new QuestionType();
        type.name = typeName;
        type.dataType = dataType;
        Question q = new Question();
        q.questionId = questionId;
        q.questionType = type;
        q.sample = sample;
        return q;
    }

    @Test
    void aDisplayOnlyQuestionsSampleIsNotATokenValue() {
        Map<Integer, String> samples = TokenSamples.samplesOf(List.of(
                question(40, "HTML", "", "Your Mother"),
                question(41, "MODAL", "", "Your Father"),
                question(42, "TEXT", "Text", "Bob")));

        assertFalse(samples.containsKey(40), "HTML collects nothing, so its sample fills no token");
        assertFalse(samples.containsKey(41), "nor does MODAL's");
        assertEquals("Bob", samples.get(42));
    }

    @Test
    void aRuleReadingADisplayOnlyQuestionPreviewsThePlaceholdersDefault() {
        // The rule has no default upstream value of its own, so nothing fills the slot -- which is
        // what the respondent would see. Previewing the HTML question's sample is the lie #10 is about.
        TokenSamples samples = TokenSamples.of(List.of(rule("TEXT", "NAME", null, 400, 10, null, null)),
                MOUNTS, ASSIGNMENTS, TokenSamples.samplesOf(List.of(question(40, "HTML", "", "Your Mother"))));

        assertNull(samples.forMount(10, 200, 300).get("NAME"));
        assertTrue(samples.tokensAt(10, 200, 300).contains("NAME"), "the rule still reaches the text");
    }

    @Test
    void aDisplayOnlyRuleWithItsOwnValueStillPreviewsThatValue() {
        TokenSamples samples = TokenSamples.of(
                List.of(rule("TEXT", "NAME", "{<G1>'s|your} mother", 400, 10, null, null)),
                MOUNTS, ASSIGNMENTS, TokenSamples.samplesOf(List.of(question(40, "HTML", "", "Your Mother"))));

        assertEquals("{<G1>'s|your} mother", samples.forMount(10, 200, 300).get("NAME"));
    }

}
