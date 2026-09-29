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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The curated content languages (UC-044 BR-004). Plain unit tests: the bean reads one property and
 * parses it, so injecting a container to watch it do that would test Quarkus rather than this.
 */
class ContentLanguagesTest {

    private static ContentLanguages parsed(String configured) {
        ContentLanguages languages = new ContentLanguages();
        languages.configured = configured;
        languages.parse();
        return languages;
    }

    @Test
    void tags_areCanonicalAndInTheOrderConfigured() {
        assertEquals(List.of("en", "ar", "es-419"), parsed("en,ar,es-419").tags(),
                "configured order, not alphabetical: it is the order the picker offers");
    }

    @Test
    void tags_areCanonicalizedHoweverTheyWereWritten() {
        assertEquals(List.of("en", "es-419", "zh-Hans"), parsed(" EN , es_419 ,ZH-hans ").tags(),
                "an underscore, stray spaces and odd case all name the same languages");
    }

    @Test
    void tags_dropADuplicateRatherThanOfferingItTwice() {
        assertEquals(List.of("en", "ar"), parsed("en,ar,EN,ar").tags());
    }

    @Test
    void tags_ignoreWhatIsNotALanguage() {
        // Warned about at startup rather than refused: one typo in a property should not stop the
        // application from starting with the languages that were spelled correctly.
        assertEquals(List.of("en", "ar"), parsed("en,zzz,ar,,français").tags());
    }

    @Test
    void tags_fallBackToEnglishWhenThePropertyNamesNothingUsable() {
        // Never empty: an empty set would make every language unsupported and leave an author
        // unable to publish anything at all.
        assertEquals(List.of("en"), parsed("").tags());
        assertEquals(List.of("en"), parsed(null).tags());
        assertEquals(List.of("en"), parsed("zzz,,  ").tags());
    }

    @Test
    void supports_recognizesACuratedLanguageHoweverItIsSpelled() {
        ContentLanguages languages = parsed("en,ar,es-419");
        assertTrue(languages.supports("ar"));
        assertTrue(languages.supports("es-419"));
        assertTrue(languages.supports("es_419"), "the spelling the bundle files use");
        assertTrue(languages.supports("  AR  "));
    }

    @Test
    void supports_refusesALanguageElicitDoesNotCarry() {
        ContentLanguages languages = parsed("en,ar,es-419");
        assertFalse(languages.supports("fr"), "a real language Elicit has no application text for");
        assertFalse(languages.supports("es"), "es-419 is carried; plain Spanish is a different tag");
        assertFalse(languages.supports("zzz"));
        assertFalse(languages.supports(null));
        assertFalse(languages.supports(""));
    }
}
