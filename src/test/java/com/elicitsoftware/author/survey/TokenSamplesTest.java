package com.elicitsoftware.author.survey;

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
        TokenSamples samples = TokenSamples.of(List.of(rule("TEXT", "NAME", null, 400, 10, null, null)),
                MOUNTS, ASSIGNMENTS, samplesOf(freeText(40, "Bob")));
        assertEquals("Bob", samples.forMount(10, 200, 300).values().get("NAME"));
        assertEquals("Bob", samples.forMount(10, 200, 301).values().get("NAME"));
        assertEquals("Bob", samples.forMount(10, 200, null).values().get("NAME"), "the section title in that step too");
        assertNull(samples.forMount(11, 201, 300).values().get("NAME"), "but not the same section mounted in another step");
    }

    @Test
    void innerRulesOverrideOuterOnes() {
        // The upstream is a choice question, so the author's constant outranks its sample answer and
        // the inner rules' own values are what land (see theConstantWinsForACodedAnswer).
        TokenSamples samples = TokenSamples.of(List.of(
                rule("TEXT", "NAME", null, 400, 10, null, null),
                rule("TEXT", "NAME", "the section's", 400, 10, 200, null),
                rule("TEXT", "NAME", "the question's", 400, 10, 200, 301)),
                MOUNTS, ASSIGNMENTS, samplesOf(coded(40, "Bob")));
        assertEquals("the section's", samples.forMount(10, 200, 300).values().get("NAME"));
        assertEquals("the question's", samples.forMount(10, 200, 301).values().get("NAME"));
    }

    @Test
    void aRuleWithNothingToWriteStillFillsTheToken() {
        TokenSamples samples = TokenSamples.of(List.of(rule("TEXT", "NAME", null, 400, 10, null, null)), MOUNTS, ASSIGNMENTS, Map.of());
        assertTrue(samples.fills(new Tokens.Placeholder("Has <NAME> been", "Were you"), 10, 200, 300), "validation sees the rule");
        assertFalse(samples.fills(new Tokens.Placeholder("<OTHER>", null), 10, 200, 300));
        assertFalse(samples.forMount(10, 200, 300).values().containsKey("NAME"), "the preview falls back to the placeholder's default");
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
        // Both rules read the one upstream, a choice question, so NAME takes its sample (its rule
        // has no constant) and OTHER takes its own constant. What is under test is the placements.
        TokenSamples samples = TokenSamples.of(List.of(
                rule("TEXT", "NAME", null, 400, 10, null, null),
                rule("TEXT", "OTHER", "elsewhere", 400, 11, null, null)),
                MOUNTS, ASSIGNMENTS, samplesOf(coded(40, "Bob")));
        TokenSamples.Values values = samples.forQuestion(30);
        assertEquals("Bob", values.values().get("NAME"), "from the step 10 placement");
        assertEquals("elsewhere", values.values().get("OTHER"), "from the step 11 placement");
        TokenSamples.Values upstream = samples.forQuestion(40);
        assertEquals("Bob", upstream.values().get("NAME"), "the step 10 rule reaches the upstream's own section too");
        assertFalse(upstream.values().containsKey("OTHER"), "the step 11 rule does not");
    }

    // ---- UC-011 BR-004: which of a rule's two possible values lands, and where it came from
    //      (ElicitSoftware/Author#22) -----------------------------------------------------------

    /**
     * The runtime lets a respondent's own words outrank the constant the author wrote as their
     * stand-in, so the sample answer standing in for those words outranks it here too. Previewing
     * the constant showed the author a sentence no respondent who answered would ever read.
     */
    @Test
    void theSampleAnswerWinsForAFreeTextSource() {
        TokenSamples samples = TokenSamples.of(List.of(rule("TEXT", "NAME", "your mother", 400, 10, null, null)),
                MOUNTS, ASSIGNMENTS, samplesOf(freeText(40, "Bob")));
        assertEquals("Bob", samples.forMount(10, 200, 300).values().get("NAME"));
        assertFalse(samples.forMount(10, 200, 300).authored().contains("NAME"),
                "a respondent's words, so the preview escapes them and does not re-read them");
    }

    /** A moment the respondent chose is theirs as much as their words are. */
    @Test
    void theSampleAnswerWinsForADateSource() {
        TokenSamples samples = TokenSamples.of(List.of(rule("TEXT", "WHEN", "some time ago", 400, 10, null, null)),
                MOUNTS, ASSIGNMENTS, samplesOf(question(40, "DATE_PICKER", "Date", null, "1965-04-30")));
        assertEquals("1965-04-30", samples.forMount(10, 200, 300).values().get("WHEN"));
        assertFalse(samples.forMount(10, 200, 300).authored().contains("WHEN"));
    }

    /**
     * A choice question stores the chosen option's coded value, not its text, so the author's prose
     * wins -- whatever the type's data_type says. The select group is what gives it away.
     */
    @Test
    void theConstantWinsForACodedAnswer() {
        TokenSamples samples = TokenSamples.of(List.of(rule("TEXT", "NAME", "your mother", 400, 10, null, null)),
                MOUNTS, ASSIGNMENTS, samplesOf(coded(40, "MOTHER")));
        assertEquals("your mother", samples.forMount(10, 200, 300).values().get("NAME"));
        assertTrue(samples.forMount(10, 200, 300).authored().contains("NAME"),
                "the author's prose, so the preview splices it raw and re-reads it");
    }

    /** A bare figure would read as nonsense in a sentence, so the constant wins there too. */
    @Test
    void theConstantWinsForANumberOrBoolean() {
        TokenSamples numeric = TokenSamples.of(List.of(rule("TEXT", "N", "several", 400, 10, null, null)),
                MOUNTS, ASSIGNMENTS, samplesOf(question(40, "INTEGER", "Number", null, "3")));
        assertEquals("several", numeric.forMount(10, 200, 300).values().get("N"));

        TokenSamples bool = TokenSamples.of(List.of(rule("TEXT", "B", "yes", 400, 10, null, null)),
                MOUNTS, ASSIGNMENTS, samplesOf(question(40, "CHECKBOX", "Boolean", null, "true")));
        assertEquals("yes", bool.forMount(10, 200, 300).values().get("B"));
        assertTrue(bool.forMount(10, 200, 300).authored().contains("B"));
    }

    /** A blank sample is the case where the respondent has not answered, which is the constant's. */
    @Test
    void aBlankSampleFallsBackToTheConstant() {
        TokenSamples samples = TokenSamples.of(List.of(rule("TEXT", "NAME", "your mother", 400, 10, null, null)),
                MOUNTS, ASSIGNMENTS, samplesOf(freeText(40, "  ")));
        assertEquals("your mother", samples.forMount(10, 200, 300).values().get("NAME"));
        assertTrue(samples.forMount(10, 200, 300).authored().contains("NAME"));
    }

    /** A coded source with no constant has only its sample, and that is still the respondent's. */
    @Test
    void aCodedSourceWithNoConstantStillOffersItsSample() {
        TokenSamples samples = TokenSamples.of(List.of(rule("TEXT", "NAME", null, 400, 10, null, null)),
                MOUNTS, ASSIGNMENTS, samplesOf(coded(40, "MOTHER")));
        assertEquals("MOTHER", samples.forMount(10, 200, 300).values().get("NAME"));
        assertFalse(samples.forMount(10, 200, 300).authored().contains("NAME"));
    }

    /**
     * An inner rule supplying a sample answer has to clear the flag the outer rule set, or the
     * respondent's words would reach the text unescaped. QuestionManager.getKeyValues does the same
     * with sources().remove(key).
     */
    @Test
    void anInnerSampleAnswerClearsTheFlagAnOuterConstantSet() {
        TokenSamples samples = TokenSamples.of(List.of(
                rule("TEXT", "NAME", "your mother", 400, 10, null, null),
                rule("TEXT", "NAME", null, 400, 10, 200, null)),
                MOUNTS, ASSIGNMENTS, samplesOf(coded(40, "MOTHER")));

        assertTrue(samples.forMount(10, null, null).authored().contains("NAME"), "the step rule's own prose");
        TokenSamples.Values inner = samples.forMount(10, 200, 300);
        assertEquals("MOTHER", inner.values().get("NAME"), "the section rule has no constant, so its sample lands");
        assertFalse(inner.authored().contains("NAME"), "and it must not be spliced raw");
    }

    // ---- UC-019 BR-006: a display-only question has no sample a token could take ---------------

    @Test
    void aDisplayOnlyQuestionsSampleIsNotATokenValue() {
        Map<Integer, TokenSamples.Sample> samples = TokenSamples.samplesOf(List.of(
                question(40, "HTML", "", null, "Your Mother"),
                question(41, "MODAL", "", null, "Your Father"),
                question(42, "TEXT", "Text", null, "Bob")));

        assertFalse(samples.containsKey(40), "HTML collects nothing, so its sample fills no token");
        assertFalse(samples.containsKey(41), "nor does MODAL's");
        assertEquals("Bob", samples.get(42).value());
        assertTrue(samples.get(42).beatsConstant(), "free text is the respondent's own words");
    }

    @Test
    void aRuleReadingADisplayOnlyQuestionPreviewsThePlaceholdersDefault() {
        // The rule has no default upstream value of its own, so nothing fills the slot -- which is
        // what the respondent would see. Previewing the HTML question's sample is the lie #10 is about.
        TokenSamples samples = TokenSamples.of(List.of(rule("TEXT", "NAME", null, 400, 10, null, null)),
                MOUNTS, ASSIGNMENTS, samplesOf(question(40, "HTML", "", null, "Your Mother")));

        assertNull(samples.forMount(10, 200, 300).values().get("NAME"));
        assertTrue(samples.tokensAt(10, 200, 300).contains("NAME"), "the rule still reaches the text");
    }

    @Test
    void aDisplayOnlyRuleWithItsOwnValueStillPreviewsThatValue() {
        // FHHS fills {<S1>} from rules on an HTML question whose value is "{<G1>'s|your} mother".
        TokenSamples samples = TokenSamples.of(
                List.of(rule("SHOW", "NAME", "{<G1>'s|your} mother", 400, 10, null, null)),
                MOUNTS, ASSIGNMENTS, samplesOf(question(40, "HTML", "", null, "Your Mother")));

        assertEquals("{<G1>'s|your} mother", samples.forMount(10, 200, 300).values().get("NAME"));
        assertTrue(samples.forMount(10, 200, 300).authored().contains("NAME"),
                "the author's own phrase, which the preview must re-read for <G1>");
    }

    // ---- UC-019 BR-008: inside a per-item instance a token holds the item's text -----------------

    /** A MULTI_SELECT over list 7, whose first item reads "Western Michigan". */
    private static Map<Integer, TokenSamples.Sample> gamesSample(String sample) {
        return TokenSamples.samplesOf(List.of(question(40, "MULTI_SELECT", "Text", 7, sample)), Map.of(7, "Western Michigan"));
    }

    @Test
    void aRepeatReadingAMultiSelectPreviewsTheFirstItemOfTheList() {
        TokenSamples samples = TokenSamples.of(List.of(rule("REPEAT", "GAME", null, 400, 10, 200, null)),
                MOUNTS, ASSIGNMENTS, gamesSample(null));

        TokenSamples.Values title = samples.forMount(10, 200, null);
        assertEquals("Western Michigan", title.values().get("GAME"), "the repeated section's title");
        assertFalse(title.authored().contains("GAME"), "an item's text is escaped and not re-read, like an answer");
        assertEquals("Western Michigan", samples.forMount(10, 200, 300).values().get("GAME"), "and every question in it");
    }

    @Test
    void theItemOutranksTheRulesConstantInsideAnInstance() {
        // For a coded answer the constant normally wins (theConstantWinsForACodedAnswer). Inside an
        // instance it is the same for every item, so the item's text is what the respondent reads.
        TokenSamples samples = TokenSamples.of(List.of(rule("REPEAT", "GAME", "a game", 400, 10, 200, null)),
                MOUNTS, ASSIGNMENTS, gamesSample("G02"));
        assertEquals("Western Michigan", samples.forMount(10, 200, 300).values().get("GAME"));
    }

    @Test
    void aTextRuleFromTheSameMultiSelectOntoTheRepeatedSectionPreviewsTheItemToo() {
        TokenSamples samples = TokenSamples.of(List.of(
                rule("REPEAT", null, null, 400, 10, 200, null),
                rule("TEXT", "OPP", "some", 400, 10, 200, null)),
                MOUNTS, ASSIGNMENTS, gamesSample(null));
        assertEquals("Western Michigan", samples.forMount(10, 200, 300).values().get("OPP"));
    }

    @Test
    void aRepeatedQuestionPreviewsTheItemOnThatQuestionOnly() {
        TokenSamples samples = TokenSamples.of(List.of(rule("REPEAT", "WHO", null, 400, null, null, 300)),
                MOUNTS, ASSIGNMENTS, gamesSample(null));
        assertEquals("Western Michigan", samples.forMount(10, 200, 300).values().get("WHO"));
        assertNull(samples.forMount(10, 200, 301).values().get("WHO"), "the question beside it is not repeated");
    }

    @Test
    void outsideAnInstanceAMultiSelectFillsATokenAsAnyCodedAnswerDoes() {
        // A Text rule onto a section nothing repeats: the constant wins, as for any choice question.
        TokenSamples samples = TokenSamples.of(List.of(rule("TEXT", "GAMES", "your games", 400, 10, 200, null)),
                MOUNTS, ASSIGNMENTS, gamesSample("G02"));
        TokenSamples.Values values = samples.forMount(10, 200, 300);
        assertEquals("your games", values.values().get("GAMES"));
        assertTrue(values.authored().contains("GAMES"));
    }

    @Test
    void aCountDrivenRepeatIsUnchanged() {
        TokenSamples samples = TokenSamples.of(List.of(rule("REPEAT", "N", "several", 400, 10, 200, null)),
                MOUNTS, ASSIGNMENTS, samplesOf(question(40, "INTEGER", "Number", null, "3")));
        assertEquals("several", samples.forMount(10, 200, 300).values().get("N"));
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

    private static Map<Integer, TokenSamples.Sample> samplesOf(Question... questions) {
        return TokenSamples.samplesOf(List.of(questions));
    }

    /** The type names and data types are the ones V003 and V018 seed. */
    private static Question question(int questionId, String typeName, String dataType, Integer selectGroupId, String sample) {
        QuestionType type = new QuestionType();
        type.name = typeName;
        type.dataType = dataType;
        Question q = new Question();
        q.questionId = questionId;
        q.questionType = type;
        q.selectGroupId = selectGroupId;
        q.sample = sample;
        return q;
    }

    private static Question freeText(int questionId, String sample) {
        return question(questionId, "TEXT", "Text", null, sample);
    }

    /** A COMBOBOX stores its option's coded value, so its data_type of Text is not prose. */
    private static Question coded(int questionId, String sample) {
        return question(questionId, "COMBOBOX", "Text", 7, sample);
    }
}
