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

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** FR-013 / UC-011 BR-004: the preview substitutes tokens the way QuestionManager.replaceTokens does. */
class PreviewFieldsTest {

    @Test
    void substitutesAValueElseTheDefaultElseThePhraseAsWritten() {
        assertEquals("Hello Bob", PreviewFields.substituteTokens("Hello {<PNAME>}", Map.of("PNAME", "Bob")));
        assertEquals("Hello friend", PreviewFields.substituteTokens("Hello {<PNAME>|friend}", Map.of()));
        assertEquals("Hello <PNAME>", PreviewFields.substituteTokens("Hello {<PNAME>}", Map.of()));
    }

    /** The token is the bracketed name, not any word the phrase happens to contain (#16). */
    @Test
    void theTokenIsMatchedOnItsBrackets() {
        assertEquals("Is Bob living?", PreviewFields.substituteTokens("Is {<S1>|they} living?", Map.of("S1", "Bob")));
        assertEquals("Is they living?", PreviewFields.substituteTokens("Is {<S10>|they} living?", Map.of("S1", "Bob")));
        assertEquals("Please write your full name",
                PreviewFields.substituteTokens("Please write your {name|full name}", Map.of("NAME", "Bob")));
    }

    /** The three authored forms read differently when nothing fills them; an empty default is a default. */
    @Test
    void theAuthoredFormDecidesWhatAnUnfilledPlaceholderReads() {
        assertEquals("Is they living?", PreviewFields.substituteTokens("Is {<S1>|they} living?", Map.of()));
        assertEquals("Is  living?", PreviewFields.substituteTokens("Is {<S1>|} living?", Map.of()));
        assertEquals("Is <S1> living?", PreviewFields.substituteTokens("Is {<S1>} living?", Map.of()));
    }

    /** {Q#} and {S#} belong to the Survey runtime, which fills them from the answer's own key. */
    @Test
    void theRuntimeCountersAreLeftForTheRuntime() {
        assertEquals("Name of person {Q#}", PreviewFields.substituteTokens("Name of person {Q#}", Map.of("NAME", "Bob")));
        assertEquals("Child {S#} of Bob", PreviewFields.substituteTokens("Child {S#} of {<NAME>}", Map.of("NAME", "Bob")));
        assertEquals("Name of person { Q# }", PreviewFields.substituteTokens("Name of person { Q# }", Map.of()));
    }

    @Test
    void aPhraseIsFilledWhollyOrNotAtAll() {
        assertEquals("x and y", PreviewFields.substituteTokens("{<A> and <B>|neither}", Map.of("A", "x", "B", "y")));
        assertEquals("neither", PreviewFields.substituteTokens("{<A> and <B>|neither}", Map.of("A", "x")));
    }

    /**
     * A rule whose token was typed {@code <S1>} rather than {@code S1} fills nothing at runtime,
     * where the values map is keyed on relationship.token verbatim, so it fills nothing here either.
     */
    @Test
    void aBracketedRuleTokenFillsNothing() {
        assertEquals("Is they living?", PreviewFields.substituteTokens("Is {<S1>|they} living?", Map.of("<S1>", "Bob")));
    }

    @Test
    void valuesMayThemselvesHoldPlaceholders() {
        Map<String, String> values = Map.of("S1", "{<G1>'s|your} mother", "G1", "Bob");
        assertEquals("Is Bob's mother living?", PreviewFields.substituteTokens("Is {<S1>} living?", values));
        assertEquals("Is your mother living?",
                PreviewFields.substituteTokens("Is {<S1>} living?", Map.of("S1", "{<G1>'s|your} mother")));
    }

    @Test
    void thePhraseAroundTheTokenSurvives() {
        assertEquals("Has Dennis been diagnosed",
                PreviewFields.substituteTokens("{Has <CS2> been|Were you} diagnosed", Map.of("CS2", "Dennis")));
        assertEquals("Were you diagnosed",
                PreviewFields.substituteTokens("{Has <CS2> been|Were you} diagnosed", Map.of()));
    }

    @Test
    void tidiesPossessivesLikeTheRuntime() {
        assertEquals("Dennis' Cancers", PreviewFields.substituteTokens("{<CS2>'s|Your} Cancers", Map.of("CS2", "Dennis")));
        assertEquals("Your Cancers", PreviewFields.substituteTokens("{<CS2>'s|Your} Cancers", Map.of()));
        assertEquals("What is her name", PreviewFields.substituteTokens("What is {<S1>'s|your} name", Map.of("S1", "her")));
    }
}
