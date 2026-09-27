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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokensTest {

    @Test
    void placeholdersArePhrasesThatHoldTheToken() {
        List<Tokens.Placeholder> found = Tokens.placeholders("{<G1>'s|Your} Child {S#} - {<S1>}");
        assertEquals(List.of("<G1>'s", "<S1>"), found.stream().map(Tokens.Placeholder::phrase).toList(), "the runtime counters are not tokens");
        assertEquals("Your", found.get(0).defaultText());
        assertNull(found.get(1).defaultText());
        assertTrue(found.get(0).uses("G1"), "the phrase refers to the token");
        assertFalse(found.get(0).uses("S1"));
        assertTrue(Tokens.uses("{Has <CS2> been|Were you} diagnosed", "CS2"));
        assertTrue(Tokens.placeholders(null).isEmpty());
        assertTrue(Tokens.placeholders("no tokens").isEmpty());
    }

    /** A reference matches whole or not at all: S1 is a prefix of S10, and name of surname. */
    @Test
    void oneTokenIsNotAnotherTokensPrefix() {
        assertFalse(Tokens.uses("{<S10>|this step}", "S1"));
        assertTrue(Tokens.uses("{<S10>|this step}", "S10"));
        assertFalse(Tokens.uses("{<SURNAME>|Doe}", "NAME"));
    }

    /** Prose is prose: only a bracketed reference names a token. */
    @Test
    void proseThatReadsLikeATokenIsNotOne() {
        assertFalse(Tokens.uses("{the name of this person|someone}", "name"));
        assertTrue(Tokens.names("What is the name of person {Q#}?").isEmpty());
    }

    @Test
    void aTokenNameMustBeUpperCaseWhereItIsAuthored() {
        assertEquals(List.of("name"), List.copyOf(Tokens.malformed("{<name>|this person}")));
        assertTrue(Tokens.malformed("{<NAME>|this person}").isEmpty());
    }

    @Test
    void everyUseDeclaresItsToken() {
        // G1 is only ever written inside a phrase; the old bare-word rule could not see it.
        assertEquals(List.of("NAME", "G1", "S1"),
                List.copyOf(Tokens.names("Hello {<NAME>|friend}, {<G1>'s|your} child {<S1>}")));
    }

    @Test
    void alwaysDefaultedLooksAtEveryUseOfTheToken() {
        assertTrue(Tokens.alwaysDefaulted("{Has <CS2> been|Were you} diagnosed", "CS2"));
        assertFalse(Tokens.alwaysDefaulted("Age when {<C1>} was diagnosed", "C1"));
        assertFalse(Tokens.alwaysDefaulted("{<S1>|you} and {<S1>}", "S1"));
    }
}
