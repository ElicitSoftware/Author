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
    // ---- UC-007 BR-005: a token named as a bare word fills nothing ----------------------------

    private static final java.util.List<String> KNOWN = java.util.List.of("S1", "G1", "CS2");

    @Test
    void aBareTokenNameInsideAPlaceholderIsReported() {
        assertEquals(java.util.Set.of("S1"), Tokens.bareTokenNames("{Does S1|Do you} have cancer?", KNOWN));
        assertEquals(java.util.Set.of("G1"), Tokens.bareTokenNames("{G1's|Your} mother", KNOWN));
        assertEquals(java.util.Set.of("S1"), Tokens.bareTokenNames("Ask {S1}.", KNOWN));
        assertEquals(java.util.Set.of("CS2"), Tokens.bareTokenNames("{Has CS2 been|Were you} diagnosed", KNOWN));
    }

    @Test
    void abracketedTokenIsNotReported() {
        assertTrue(Tokens.bareTokenNames("{Does <S1>|Do you} have cancer?", KNOWN).isEmpty());
        assertTrue(Tokens.bareTokenNames("{<G1>'s|Your} mother", KNOWN).isEmpty());
    }

    @Test
    void proseOutsideAPlaceholderIsNotReported() {
        assertTrue(Tokens.bareTokenNames("Section S1 of the form", KNOWN).isEmpty(),
                "only a placeholder's own text can name a token");
    }

    @Test
    void aNameThatOnlyLooksLikeATokenIsNotReported() {
        assertTrue(Tokens.bareTokenNames("{S10's|Your} record", KNOWN).isEmpty(),
                "S1 is not S10: the match is a whole word");
        assertTrue(Tokens.bareTokenNames("{G1X|Your} record", KNOWN).isEmpty());
        assertTrue(Tokens.bareTokenNames("{Does S1|Do you} have cancer?", java.util.List.of()).isEmpty(),
                "a survey with no tokens has nothing to report");
    }

    @Test
    void bothSidesOfTheBarAreInspected() {
        assertEquals(java.util.Set.of("S1"), Tokens.bareTokenNames("{Ask them|about S1}", KNOWN));
    }

    @Test
    void aMixOfBracketedAndBareIsReportedForTheBareOneOnly() {
        assertEquals(java.util.Set.of("G1"), Tokens.bareTokenNames("{<S1> and G1|you both}", KNOWN));
    }

    /** UC-007 BR-006: a token is not markup, so a personalized string is not an HTML one. */
    @Test
    void markupIsWhatLiesOutsideThePlaceholders() {
        assertTrue(Tokens.hasMarkup("<p>How old is {<NAME>|this person}?</p>"));
        assertFalse(Tokens.hasMarkup("How old is {<NAME>|this person}?"), "a token is not markup");
        assertFalse(Tokens.hasMarkup("{<G1>'s|Your} Mother"));
        assertFalse(Tokens.hasMarkup(null));
        assertTrue(Tokens.hasMarkup("Plain <b>bold</b> text"));
    }

    /**
     * UC-007 BR-006: a tag inside a placeholder reads as a token reference, which is what makes it
     * catchable -- the survey defines no such token. Only the phrase carries references; the
     * default is used verbatim.
     */
    @Test
    void aTagInsideAPlaceholderReadsAsATokenReference() {
        assertEquals(java.util.Set.of("b", "NAME"),
                Tokens.placeholders("{<b><NAME>|name}").get(0).tokens());
        assertEquals(java.util.Set.of("NAME"),
                Tokens.placeholders("Ask {<NAME>|<i>someone</i>}").get(0).tokens(),
                "the default is never scanned for references");
        assertEquals(java.util.Set.of("NAME"),
                Tokens.placeholders("<b>{<NAME>|name}</b>").get(0).tokens(),
                "a tag outside the braces is not a reference at all");
    }

    // ---- UC-007 BR-007 / UC-019 A2: a rule's token is a name, not a reference -----------------

    @Test
    void aTokenIsTheNameWhicheverWayItWasTyped() {
        assertEquals("S1", Tokens.bare("<S1>"));
        assertEquals("S1", Tokens.bare("  S1  "));
        assertEquals("<S1", Tokens.bare("<S1"), "half a reference is not one, so it is left alone");
        assertEquals("<S1>", Tokens.reference("<S1>"), "written as a phrase writes it, however it arrived");
        assertEquals("<s1>", Tokens.reference("s1"), "reference reads a token back as written; canonical is what stores it");
        assertEquals("S1", Tokens.canonical(" <s1> "), "the name alone, upper case (FR-012)");
        assertEquals("GRANDMAMA", Tokens.canonical("<grandmama>"));
        assertEquals("", Tokens.canonical(null));
    }

    /**
     * A phrase names a token as {@code <NAME>}, so only a token spelled exactly as that name can ever
     * be named -- {@code <S1>} cannot, because a phrase naming it would read {@code <<S1>>}.
     */
    @Test
    void aPhraseCanOnlyNameAPlainName() {
        assertTrue(Tokens.nameable("S1"));
        assertTrue(Tokens.nameable("s1"), "case is free; the runtime matches without regard to it");
        assertTrue(Tokens.nameable("A_1"));
        assertTrue(Tokens.nameable(" S1 "), "trimmed, as the write path stores it");
        assertFalse(Tokens.nameable("<S1>"), "the brackets belong to the text, not the token");
        assertFalse(Tokens.nameable("S 1"));
        assertFalse(Tokens.nameable("{S1}"));
        assertFalse(Tokens.nameable("S-1"));
        assertFalse(Tokens.nameable("Q#"), "a runtime counter is not a token");
        assertFalse(Tokens.nameable("<>"));
        assertFalse(Tokens.nameable(""));
        assertFalse(Tokens.nameable(null));
    }
}
