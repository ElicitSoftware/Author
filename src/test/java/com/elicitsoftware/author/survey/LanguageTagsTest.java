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
    }

    @Test
    void canonical_rejectsWhatIsNotALanguage() {
        assertNull(LanguageTags.canonical(null));
        assertNull(LanguageTags.canonical(""));
        assertNull(LanguageTags.canonical("   "));
        assertNull(LanguageTags.canonical("fr_CA"), "underscore is not BCP-47");
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

    @Test
    void offerable_putsThePublishedSetFirstThenTheCommonOnes() {
        List<String> offered = LanguageTags.offerable("en", List.of("ar", "es-419"));

        assertEquals(List.of("ar", "es-419"), offered.subList(0, 2),
                "the author's own languages come first");
        assertEquals("zh-Hans", offered.get(2),
                "then the twenty in their own order, English being the base");
        assertTrue(offered.contains("fr"));
    }

    @Test
    void offerable_keepsAPublishedTagThatIsNotOneOfTheTwenty() {
        // The point of the exception: Elicit's own bundles are es-419, which the twenty do not hold.
        assertTrue(LanguageTags.offerable("en", List.of("es-419")).contains("es-419"));
        assertEquals(1, LanguageTags.offerable("en", List.of("es-419")).stream()
                .filter("es-419"::equals).count(), "and exactly once");
    }

    @Test
    void offerable_listsAPublishedTagThatIsOneOfTheTwentyOnlyOnce() {
        List<String> offered = LanguageTags.offerable("en", List.of("fr"));
        assertEquals(1, offered.stream().filter("fr"::equals).count());
        assertEquals("fr", offered.get(0), "and as a published one, so first");
        assertEquals(LanguageTags.TOP_TWENTY.size() - 1, offered.size(),
                "the twenty, less the base language, with fr merged rather than repeated");
    }

    @Test
    void offerable_neverOffersTheBaseLanguage() {
        assertFalse(LanguageTags.offerable("fr", List.of()).contains("fr"),
                "UC-044 BR-005: the base language is the fallback, not a translation");
        assertFalse(LanguageTags.offerable("FR", List.of()).contains("fr"),
                "recognized however it was stored");
        assertFalse(LanguageTags.offerable("es-419", List.of("es-419", "ar")).contains("es-419"),
                "even when the stored set wrongly contains it");
    }

    @Test
    void offerable_toleratesJunkInTheStoredSet() {
        List<String> offered = LanguageTags.offerable("en", List.of("ar", "zzz", ""));
        assertTrue(offered.contains("ar"));
        assertFalse(offered.contains("zzz"));
        assertFalse(offered.contains(""));
    }
}
