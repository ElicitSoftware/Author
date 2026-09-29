package com.elicitsoftware.author.i18n;

/*-
 * ***LICENSE_START***
 * Elicit Author
 * %%
 * Copyright (C) 2025 The Regents of the University of Michigan - Rogel Cancer Center
 * %%
 * PolyForm Noncommercial License 1.0.0
 * <https://polyformproject.org/licenses/noncommercial/1.0.0>
 * ***LICENSE_END***
 */

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC-040 / FR-053: the committed translation handoff document must match what the generator
 * produces from the English bundle. The freshly generated copy is always written to
 * {@code target/i18n/TRANSLATION_REQUEST.md}; on drift, copy it over the committed file.
 */
class TranslationRequestGeneratorTest {

    @Test
    void committedHandoffDocument_isUpToDate() throws IOException {
        Path root = DisplayedStringsCoverageTest.moduleRoot();
        String generated = TranslationRequestGenerator.generate(root);
        Path fresh = root.resolve("target/i18n/TRANSLATION_REQUEST.md");
        Files.createDirectories(fresh.getParent());
        Files.writeString(fresh, generated, StandardCharsets.UTF_8);

        Path committed = root.resolve(TranslationRequestGenerator.OUTPUT);
        String actual = Files.exists(committed) ? Files.readString(committed, StandardCharsets.UTF_8) : "";
        assertEquals(generated, actual,
                "i18n/TRANSLATION_REQUEST.md is out of date; run: cp target/i18n/TRANSLATION_REQUEST.md i18n/TRANSLATION_REQUEST.md");
    }

    /**
     * A note may quote a placeholder holding a pipe of its own. Splitting the entry on every pipe
     * read those as column breaks, so {@code relationshipDialog.token.helper}'s Max length column
     * held the words "default} and {Has <TOKEN> been" and its real budget never reached the
     * translator at all.
     */
    @Test
    void aNoteMayQuoteAPlaceholderThatHoldsAPipe() {
        assertArrayEquals(
                new String[]{"Rule dialog ",
                        " field helper text; {<TOKEN>|default} and {Has <TOKEN> been|Were you} are literal examples, keep them ",
                        " 180 ", ""},
                TranslationRequestGenerator.splitContext(
                        "Rule dialog | field helper text; {<TOKEN>|default} and {Has <TOKEN> been|Were you}"
                                + " are literal examples, keep them | 180 |"));
    }

    @Test
    void anOrdinaryNoteStillSplitsIntoItsFourFields() {
        assertArrayEquals(new String[]{"Translations page ", " summary line ", " 60 ", " params: {0} = translated"},
                TranslationRequestGenerator.splitContext("Translations page | summary line | 60 | params: {0} = translated"));
        assertArrayEquals(new String[]{"Question dialog ", " field tooltip ", " 140 ", ""},
                TranslationRequestGenerator.splitContext("Question dialog | field tooltip | 140 |"));
        // Malformed rather than pipe-bearing: fewer than three pipes, so nothing to anchor.
        assertArrayEquals(new String[]{"just a note", "", "", ""},
                TranslationRequestGenerator.splitContext("just a note"));
    }

    /** The budget reaches the row for a pipe-bearing note, which is what the shift used to eat. */
    @Test
    void theBudgetSurvivesIntoTheGeneratedRow() throws IOException {
        String generated = TranslationRequestGenerator.generate(DisplayedStringsCoverageTest.moduleRoot());
        String row = generated.lines()
                .filter(l -> l.startsWith("| `relationshipDialog.token.helper` |"))
                .findFirst().orElseThrow();
        assertTrue(row.endsWith("| 180 |  |"), "the Max length column should hold the budget, not prose: " + row);
    }
}
