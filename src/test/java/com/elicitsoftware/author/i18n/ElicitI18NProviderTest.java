package com.elicitsoftware.author.i18n;

/*-
 * ***LICENSE_START***
 * Elicit Author
 * %%
 * Copyright (C) 2025 The Regents of the University of Michigan - Rogel Cancer Center
 * Copyright (C) 2026 Elicit Software LLC
 * %%
 * PolyForm Noncommercial License 1.0.0
 * <https://polyformproject.org/licenses/noncommercial/1.0.0>
 * ***LICENSE_END***
 */

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UC-040 BR-001: the image carries English only; every other language is read from this
 * application's sub-directory of the translations mount, {@code <i18n.file.system.path>/author/},
 * and overlays English key by key. The offered set is English plus every mounted language, narrowed
 * by {@code i18n.offered.locales}. The fields are package-private (the BrandUtilTest pattern) so they
 * can be set without CDI. {@link #CURATED} is the umbrella's {@code elicit-i18n/}, one level up from
 * this module in a local checkout and fetched to the same place by CI.
 */
class ElicitI18NProviderTest {

    static final String CURATED = "../elicit-i18n";

    private static final Locale ES_419 = Locale.forLanguageTag("es-419");
    private static final Locale AR = Locale.forLanguageTag("ar");
    private static final Locale AR_EG = Locale.forLanguageTag("ar-EG");
    private static final Locale FR = Locale.FRENCH;

    private static ElicitI18NProvider provider(String mount, String offered) {
        ElicitI18NProvider p = new ElicitI18NProvider();
        p.fileSystemPath = mount;
        p.offeredLocales = Optional.ofNullable(offered);
        p.pseudoLocaleEnabled = false;
        return p;
    }

    private static ElicitI18NProvider curated() {
        return provider(CURATED, null);
    }

    @Test
    void unknownLanguage_fallsBackToEnglish() {
        // "fr" is not mounted: exact tag misses, language-only misses (same tag), so this
        // exercises the final tier landing on English.
        assertEquals("Cancel", curated().getTranslation("common.cancel", FR));
    }

    @Test
    void countryLocale_fallsBackToLanguageBundle() {
        // "ar-EG" has no file of its own; it must fall back to translations_ar.properties
        // rather than skipping straight to English.
        assertEquals("إلغاء", curated().getTranslation("common.cancel", AR_EG));
    }

    @Test
    void unknownKey_rendersVisibleMarker() {
        ElicitI18NProvider p = curated();

        assertEquals("!no.such.key!", p.getTranslation("no.such.key", Locale.ENGLISH));
        assertEquals("!no.such.key!", p.getTranslation("no.such.key", AR));
    }

    @Test
    void parameters_areFormatted_onlyWhenGiven() {
        ElicitI18NProvider p = curated();

        assertEquals("Imported family-history-survey.elicit",
                p.getTranslation("importDefinitionView.imported", Locale.ENGLISH, "family-history-survey.elicit"));
        assertEquals("Import failed: {0}", p.getTranslation("importDefinitionView.importFailed", Locale.ENGLISH),
                "no params given: the value is returned as-is, not run through MessageFormat");
    }

    @Test
    void getAllTranslations_mergesLanguageOverEnglish() {
        ElicitI18NProvider p = curated();

        assertEquals("Surveys", p.getAllTranslations(Locale.ENGLISH).get("surveysView.pageTitle"),
                "the default locale itself gets no overlay");
        assertEquals("الاستبيانات", p.getAllTranslations(AR).get("surveysView.pageTitle"),
                "a non-default locale overlays its file onto the English base");
    }

    @Test
    void pseudoLocale_marksEveryKnownKey() {
        ElicitI18NProvider p = curated();
        p.pseudoLocaleEnabled = true;

        assertTrue(p.getProvidedLocales().contains(ElicitI18NProvider.PSEUDO_LOCALE));
        assertEquals("⟦common.save⟧", p.getTranslation("common.save", ElicitI18NProvider.PSEUDO_LOCALE));
        assertEquals("!no.such.key!", p.getTranslation("no.such.key", ElicitI18NProvider.PSEUDO_LOCALE));
        assertTrue(p.getAllTranslations(ElicitI18NProvider.PSEUDO_LOCALE).values().stream()
                .allMatch(v -> v.startsWith("⟦")));
    }

    @Test
    void curatedMount_offersEveryMountedLanguage() {
        ElicitI18NProvider p = curated();

        List<Locale> locales = p.getProvidedLocales();
        assertEquals(List.of(Locale.ENGLISH, AR, ES_419), locales, "English first, then the mounted languages");
        assertEquals("الاستبيانات", p.getTranslation("surveysView.pageTitle", AR));
        assertEquals("Encuestas", p.getTranslation("surveysView.pageTitle", ES_419));
    }

    @Test
    void nothingMounted_offersEnglishOnly(@TempDir Path empty) {
        ElicitI18NProvider p = provider(empty.toString(), null);

        assertEquals(List.of(Locale.ENGLISH), p.getProvidedLocales());
        assertEquals("Cancel", p.getTranslation("common.cancel", AR),
                "the image carries no Arabic: with nothing mounted it falls back to English");
    }

    @Test
    void theImageCarriesEnglishOnly() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        assertNotNull(cl.getResource("vaadin-i18n/translations.properties"), "English is in the image");
        assertNull(cl.getResource("vaadin-i18n/translations_ar.properties"), "Arabic must come from the mount");
        assertNull(cl.getResource("vaadin-i18n/translations_es_419.properties"), "Spanish must come from the mount");
    }

    @Test
    void offeredLocales_narrowsTheMountedSet() {
        // i18n.offered.locales hides a mounted language from the selector and from ?lang=.
        ElicitI18NProvider p = provider(CURATED, "en,es-419");

        assertEquals(List.of(Locale.ENGLISH, ES_419), p.getProvidedLocales());
    }

    @Test
    void offeredLocales_cannotOfferWhatIsNotMounted() {
        // fr is listed but has no file: it is not offered (and a warning is logged).
        ElicitI18NProvider p = provider(CURATED, "en,fr,ar");

        assertEquals(List.of(Locale.ENGLISH, AR), p.getProvidedLocales());
    }

    @Test
    void offeredLocales_blank_offersEverythingMounted() {
        assertEquals(curated().getProvidedLocales(), provider(CURATED, "  ").getProvidedLocales());
    }

    @Test
    void englishIsOffered_evenWhenTheFilterLeavesItOut() {
        // English is the fallback for every key, so it cannot be switched off.
        assertEquals(List.of(Locale.ENGLISH, AR), provider(CURATED, "ar").getProvidedLocales());
    }

    @Test
    void aMountedFile_overlaysEnglishKeyByKey(@TempDir Path mount) throws IOException {
        // A partial file overrides only the keys it holds; the rest fall back to English, and the
        // file alone is enough to offer its language.
        Path dir = Files.createDirectories(mount.resolve("author"));
        Files.writeString(dir.resolve("translations_fr.properties"), "common.cancel=Annuler\n",
                StandardCharsets.UTF_8);
        ElicitI18NProvider p = provider(mount.toString(), null);

        assertEquals(List.of(Locale.ENGLISH, FR), p.getProvidedLocales());
        assertEquals("Annuler", p.getTranslation("common.cancel", FR));
        assertEquals("Imported X", p.getTranslation("importDefinitionView.imported", FR, "X"),
                "unmounted key falls back");
    }

    @Test
    void eachApplicationReadsItsOwnSubDirectory(@TempDir Path mount) throws IOException {
        // A file in another app's directory is not this app's language.
        Path survey = Files.createDirectories(mount.resolve("survey"));
        Files.writeString(survey.resolve("translations_fr.properties"), "common.cancel=Annuler\n",
                StandardCharsets.UTF_8);

        assertEquals(List.of(Locale.ENGLISH), provider(mount.toString(), null).getProvidedLocales());
    }

    @Test
    void appName_mayNotClimbOutOfTheMount() {
        ElicitI18NProvider p = curated();
        p.appName = "../survey";

        assertThrows(IllegalArgumentException.class, p::mountDirectory);
    }

}
