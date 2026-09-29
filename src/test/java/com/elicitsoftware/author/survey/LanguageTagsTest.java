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
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** UC-044: which language tags are offered, accepted and how they are named. */
class LanguageTagsTest {

    @Test
    void canonical_acceptsARealLanguage() {
        assertEquals("fr", LanguageTags.canonical("fr"));
        assertEquals("pt-BR", LanguageTags.canonical("pt-BR"));
        assertEquals("es-419", LanguageTags.canonical("es-419"), "UC-044 BR-004: the form the chrome uses");
        assertEquals("zh-Hans", LanguageTags.canonical("zh-Hans"));
        assertEquals("zh-Hant", LanguageTags.canonical("zh-Hant"), "Traditional is typed, not listed");
    }

    @Test
    void canonical_acceptsAThreeLetterLanguageOutsideIso639_1() {
        // getISOLanguages() holds only the two-letter codes, so these pass on the JDK knowing a name.
        assertEquals("fil", LanguageTags.canonical("fil"));
        assertEquals("yue", LanguageTags.canonical("yue"));
        assertEquals("ckb", LanguageTags.canonical("ckb"));
    }

    @Test
    void canonical_normalizesCaseAndSubtagOrder() {
        assertEquals("en-GB", LanguageTags.canonical("EN-gb"));
        assertEquals("zh-Hans", LanguageTags.canonical("ZH-hans"));
        assertEquals("fr", LanguageTags.canonical("  fr  "), "typed with stray spaces");
        assertEquals("fr-CA", LanguageTags.canonical("fr_CA"),
                "the underscore spelling is read rather than refused: it is how the bundle files "
                        + "are named, so it is the spelling an author is most likely to copy");
    }

    @Test
    void canonical_rejectsWhatIsNotALanguage() {
        assertNull(LanguageTags.canonical(null));
        assertNull(LanguageTags.canonical(""));
        assertNull(LanguageTags.canonical("   "));
        assertNull(LanguageTags.canonical("français"), "a language name is not a tag");
        assertNull(LanguageTags.canonical("x"), "a single letter is ill-formed");
        assertNull(LanguageTags.canonical("en--us"), "an empty subtag is ill-formed");
        assertNull(LanguageTags.canonical("und"), "undetermined names no language");
        assertNull(LanguageTags.canonical("zz"), "well-formed and unassigned");
        assertNull(LanguageTags.canonical("zzz"), "well-formed, three letters, still nothing");
        assertNull(LanguageTags.canonical("a".repeat(40)), "longer than base_language holds");
    }

    @Test
    void displayName_namesTheLanguageInTheReadersLanguage() {
        assertEquals("French", LanguageTags.displayName("fr", Locale.ENGLISH));
        assertEquals("Chinese (Simplified)", LanguageTags.displayName("zh-Hans", Locale.ENGLISH));
        assertEquals("Spanish (Latin America)", LanguageTags.displayName("es-419", Locale.ENGLISH));
        assertEquals("francés", LanguageTags.displayName("fr", Locale.forLanguageTag("es-419")),
                "not capitalized: Spanish lower-cases a language name");
    }

    @Test
    void displayName_fallsBackToTheTag() {
        assertEquals("zzz", LanguageTags.displayName("zzz", Locale.ENGLISH), "no name to give");
        assertEquals("", LanguageTags.displayName(null, Locale.ENGLISH));
        assertNotNull(LanguageTags.displayName("fr", null), "a null locale names it in English");
    }

    /** What ElicitSoftware ships, as {@code author.content.languages} names it. */
    private static final List<String> CURATED = List.of("en", "ar", "es-419");

    @Test
    void offerable_putsThePublishedSetFirstThenTheCuratedOnes() {
        List<String> offered = LanguageTags.offerable("en", List.of("es-419"), CURATED);

        assertEquals("es-419", offered.get(0), "the author's own languages come first");
        assertEquals("ar", offered.get(1), "then the curated set in its configured order");
        assertEquals(2, offered.size(), "English is the base, so it is not offered");
    }

    @Test
    void offerable_keepsAPublishedTagElicitDoesNotCarry() {
        // UC-044 BR-004: it can only have come from an imported definition or from before the set
        // was curated. Hiding it would leave the survey publishing a language nobody can remove.
        List<String> offered = LanguageTags.offerable("en", List.of("hi"), CURATED);
        assertEquals("hi", offered.get(0));
        assertEquals(1, offered.stream().filter("hi"::equals).count(), "and exactly once");
    }

    @Test
    void offerable_listsAPublishedTagThatIsAlsoCuratedOnlyOnce() {
        List<String> offered = LanguageTags.offerable("en", List.of("ar"), CURATED);
        assertEquals(1, offered.stream().filter("ar"::equals).count());
        assertEquals("ar", offered.get(0), "and as a published one, so first");
        assertEquals(2, offered.size(), "ar merged rather than repeated");
    }

    @Test
    void offerable_neverOffersTheBaseLanguage() {
        assertFalse(LanguageTags.offerable("ar", List.of(), CURATED).contains("ar"),
                "UC-044 BR-005: the base language is the fallback, not a translation");
        assertFalse(LanguageTags.offerable("AR", List.of(), CURATED).contains("ar"),
                "recognized however it was stored");
        assertFalse(LanguageTags.offerable("es-419", List.of("es-419", "ar"), CURATED).contains("es-419"),
                "even when the stored set wrongly contains it");
    }

    @Test
    void offerable_toleratesJunkInTheStoredSet() {
        List<String> offered = LanguageTags.offerable("en", List.of("ar", "zzz", ""), CURATED);
        assertTrue(offered.contains("ar"));
        assertFalse(offered.contains("zzz"));
        assertFalse(offered.contains(""));
    }

    @Test
    void offerable_withNoCuratedSetIsJustWhatTheSurveyPublishes() {
        assertEquals(List.of("ar"), LanguageTags.offerable("en", List.of("ar"), null));
        assertEquals(List.of(), LanguageTags.offerable("en", List.of(), List.of()));
    }

    @Test
    void canonical_readsTheUnderscoreSpellingOfATag() {
        // Bundle files are named translations_es_419.properties, so that spelling is in front of
        // an author even though every tag elsewhere uses hyphens.
        assertEquals("es-419", LanguageTags.canonical("es_419"));
        assertEquals("es-419", LanguageTags.canonical("es-419"));
        assertEquals("zh-Hans", LanguageTags.canonical("zh_hans"));
    }

}
