package com.elicitsoftware.author.flow.designer;

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

import com.elicitsoftware.author.survey.TokenSamples;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FR-013 / UC-011 BR-004: the preview substitutes tokens the way QuestionManager.replaceTokens does.
 * The cases below mirror Survey's ReplaceTokensTest one for one; where the two disagree, the
 * designer is lying to the author about what the site will render.
 */
class PreviewFieldsTest {

    /** Token values as the designer builds them: a sample answer unless the token is named authored. */
    private static TokenSamples.Values values(Map<String, String> values, String... authored) {
        return new TokenSamples.Values(values, Set.of(authored));
    }

    private static TokenSamples.Values none() {
        return TokenSamples.Values.NONE;
    }

    @Test
    void substitutesAValueElseTheDefaultElseThePhraseAsWritten() {
        assertEquals("Hello Bob", PreviewFields.substituteTokens("Hello {<PNAME>}", values(Map.of("PNAME", "Bob"))));
        assertEquals("Hello friend", PreviewFields.substituteTokens("Hello {<PNAME>|friend}", none()));
        assertEquals("Hello <PNAME>", PreviewFields.substituteTokens("Hello {<PNAME>}", none()));
    }

    /** The token is the bracketed name, not any word the phrase happens to contain (#16). */
    @Test
    void theTokenIsMatchedOnItsBrackets() {
        assertEquals("Is Bob living?", PreviewFields.substituteTokens("Is {<S1>|they} living?", values(Map.of("S1", "Bob"))));
        assertEquals("Is they living?", PreviewFields.substituteTokens("Is {<S10>|they} living?", values(Map.of("S1", "Bob"))));
        assertEquals("Please write your full name",
                PreviewFields.substituteTokens("Please write your {name|full name}", values(Map.of("NAME", "Bob"))));
    }

    /** The three authored forms read differently when nothing fills them; an empty default is a default. */
    @Test
    void theAuthoredFormDecidesWhatAnUnfilledPlaceholderReads() {
        assertEquals("Is they living?", PreviewFields.substituteTokens("Is {<S1>|they} living?", none()));
        assertEquals("Is  living?", PreviewFields.substituteTokens("Is {<S1>|} living?", none()));
        assertEquals("Is <S1> living?", PreviewFields.substituteTokens("Is {<S1>} living?", none()));
    }

    /** {Q#} and {S#} belong to the Survey runtime, which fills them from the answer's own key. */
    @Test
    void theRuntimeCountersAreLeftForTheRuntime() {
        assertEquals("Name of person {Q#}",
                PreviewFields.substituteTokens("Name of person {Q#}", values(Map.of("NAME", "Bob"))));
        assertEquals("Child {S#} of Bob",
                PreviewFields.substituteTokens("Child {S#} of {<NAME>}", values(Map.of("NAME", "Bob"))));
        assertEquals("Name of person { Q# }", PreviewFields.substituteTokens("Name of person { Q# }", none()));
    }

    @Test
    void aPhraseIsFilledWhollyOrNotAtAll() {
        assertEquals("x and y",
                PreviewFields.substituteTokens("{<A> and <B>|neither}", values(Map.of("A", "x", "B", "y"))));
        assertEquals("neither", PreviewFields.substituteTokens("{<A> and <B>|neither}", values(Map.of("A", "x"))));
    }

    /**
     * A rule whose token was typed {@code <S1>} rather than {@code S1} fills nothing at runtime,
     * where the values map is keyed on relationship.token verbatim, so it fills nothing here either.
     */
    @Test
    void aBracketedRuleTokenFillsNothing() {
        assertEquals("Is they living?",
                PreviewFields.substituteTokens("Is {<S1>|they} living?", values(Map.of("<S1>", "Bob"))));
    }

    @Test
    void thePhraseAroundTheTokenSurvives() {
        assertEquals("Has Dennis been diagnosed",
                PreviewFields.substituteTokens("{Has <CS2> been|Were you} diagnosed", values(Map.of("CS2", "Dennis"))));
        assertEquals("Were you diagnosed",
                PreviewFields.substituteTokens("{Has <CS2> been|Were you} diagnosed", none()));
    }

    @Test
    void tidiesPossessivesLikeTheRuntime() {
        assertEquals("Dennis' Cancers",
                PreviewFields.substituteTokens("{<CS2>'s|Your} Cancers", values(Map.of("CS2", "Dennis"))));
        assertEquals("Your Cancers", PreviewFields.substituteTokens("{<CS2>'s|Your} Cancers", none()));
        assertEquals("What is her name",
                PreviewFields.substituteTokens("What is {<S1>'s|your} name", values(Map.of("S1", "her"))));
    }

    // ---- UC-011 BR-004: an authored value is prose, a sample answer stands in for a respondent's
    //      words (ElicitSoftware/Author#22) ----------------------------------------------------

    /**
     * The Family History Survey's own shape: S1's value is the phrase "{@code {<G1>'s|your} mother}"
     * and G1's value is the patient's name, so the sentence needs two rounds.
     */
    @Test
    void anAuthoredValueThatIsItselfAPlaceholderIsResolved() {
        assertEquals("Is Bob's mother living?",
                PreviewFields.substituteTokens("Is {<S1>} living?",
                        values(Map.of("S1", "{<G1>'s|your} mother", "G1", "Bob"), "S1")));
    }

    @Test
    void anAuthoredValuesOwnDefaultAppliesWhenItsTokenHasNoValue() {
        assertEquals("Is your mother living?",
                PreviewFields.substituteTokens("Is {<S1>} living?",
                        values(Map.of("S1", "{<G1>'s|your} mother"), "S1")));
    }

    @Test
    void nestingIsBoundedRatherThanEndless() {
        // A value that refers to itself must terminate, not hang the preview rebuild.
        String out = PreviewFields.substituteTokens("Ask {<A>|someone}.",
                values(Map.of("A", "{<A>'s|their} friend"), "A"));
        assertTrue(out.startsWith("Ask ") && out.endsWith("."), out);
    }

    /**
     * A sample answer is HTML-escaped, because the respondent's own words it stands in for are
     * attacker-controllable and the runtime escapes them. Previewing it raw showed the author bold
     * text where the site renders the literal tag.
     */
    @Test
    void aSampleAnswerIsHtmlEscapedButAnAuthoredValueIsNot() {
        assertEquals("Hello &lt;b&gt;Bob&lt;/b&gt;",
                PreviewFields.substituteTokens("Hello {<NAME>|friend}", values(Map.of("NAME", "<b>Bob</b>"))));
        assertEquals("Hello &lt;script&gt;",
                PreviewFields.substituteTokens("Hello {<NAME>|friend}", values(Map.of("NAME", "<script>"))));
        // An authored value has already had its own substitutions escaped; escaping it again would
        // double-escape them, and would destroy the <TOKEN> brackets before they could be read.
        assertEquals("Hello <b>your mother</b>",
                PreviewFields.substituteTokens("Hello {<S1>|friend}", values(Map.of("S1", "<b>your mother</b>"), "S1")));
    }

    /**
     * All five replacements, in the order QuestionManager.escapeHtml applies them. The ampersand
     * goes first, so the entities the others introduce are not escaped a second time -- reversing
     * that order turns a lone {@code <} into {@code &amp;lt;}, which is the bug this pins.
     */
    @Test
    void everyHtmlSignificantCharacterInASampleAnswerIsEscaped() {
        assertEquals("Hello &amp;&lt;&gt;&quot;&#39;",
                PreviewFields.substituteTokens("Hello {<NAME>}", values(Map.of("NAME", "&<>\"'"))));
        assertEquals("Hello &amp;lt;",
                PreviewFields.substituteTokens("Hello {<NAME>}", values(Map.of("NAME", "&lt;"))));
    }

    /**
     * A sample answer is spliced in once and never re-read, so an answer that happens to hold braces
     * is shown as typed rather than taken for a placeholder. An authored value is re-read, which is
     * the whole point of the distinction.
     */
    @Test
    void aSampleAnswerIsNeverRescannedForPlaceholders() {
        assertEquals("You said {not a placeholder}.",
                PreviewFields.substituteTokens("You said {<ANSWER>|nothing}.",
                        values(Map.of("ANSWER", "{not a placeholder}"))));
        assertEquals("You said not a placeholder.",
                PreviewFields.substituteTokens("You said {<ANSWER>|nothing}.",
                        values(Map.of("ANSWER", "{not a placeholder}"), "ANSWER")));
    }

    @Test
    void aSampleAnswerInsideAnAuthoredValueIsStillEscaped() {
        // The escaping runs before the possessive tidy, so "s's" is not collapsed here: the character
        // in front of the apostrophe is the ';' of an entity, not an 's'. Escaped either way, which is
        // what this is checking.
        assertEquals("Ask &lt;b&gt;Dennis&lt;/b&gt;'s mother.",
                PreviewFields.substituteTokens("Ask {<S1>|them}.",
                        values(Map.of("S1", "{<G1>'s|your} mother", "G1", "<b>Dennis</b>"), "S1")));
        assertEquals("Ask Dennis' mother.",
                PreviewFields.substituteTokens("Ask {<S1>|them}.",
                        values(Map.of("S1", "{<G1>'s|your} mother", "G1", "Dennis"), "S1")));
    }
}
