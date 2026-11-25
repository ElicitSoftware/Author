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

import java.util.ArrayList;
import java.util.IllformedLocaleException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Language tags for a survey's content languages (UC-044): which ones to offer, how to write one,
 * and how to name one for a human.
 * <p>
 * A content language is a BCP-47 tag in the same form the shipped chrome bundles use, so that the
 * survey's published set and the languages a site offers can be intersected without guessing
 * (UC-044 BR-004). That is why a tag is canonicalized on the way in rather than stored as typed.
 * <p>
 * Which tags may be offered is not decided here: see {@link ContentLanguages}.
 */
public final class LanguageTags {

    /** ISO 639-1, the two-letter codes; see {@link #canonical} for why it is not the whole test. */
    private static final Set<String> ISO_639_1 = Set.of(Locale.getISOLanguages());

    /** {@code surveys.base_language} is {@code varchar(35)}, and no real tag comes close. */
    private static final int MAX_LENGTH = 35;

    private LanguageTags() {
    }

    /**
     * The tag as it is stored, or {@code null} when the text does not name a language.
     * <p>
     * Three questions, and only the first is syntax. {@link Locale.Builder} throws on a tag BCP-47
     * cannot parse; {@code Locale.forLanguageTag} never throws -- it answers {@code und} and
     * silently drops what it did not understand -- which is why it is never used to validate.
     * Second, there has to be a primary language subtag at all. Third, it has to be a language that
     * exists: {@code zzz} parses perfectly and means nothing.
     * <p>
     * That last test is two-sided because ISO 639-1 holds only the two-letter codes. Real
     * three-letter languages ({@code fil}, {@code yue}, {@code ckb}) are absent from it but the JDK
     * can name them, while {@code zzz} is named only by itself.
     *
     * @param raw text typed by an author, or a tag read back from the database
     * @return the canonical tag, or {@code null} when it is not a usable language tag
     */
    public static String canonical(String raw) {
        if (raw == null) {
            return null;
        }
        // Bundle files spell a tag with underscores (translations_es_419.properties) while every
        // tag in the database, the config and the runtime uses hyphens. Both spellings reach this
        // method -- from a file name, from a property, from an author typing what they saw -- and
        // Locale.Builder rejects the underscore outright, so the two are reconciled here rather
        // than at each of the half-dozen call sites.
        String trimmed = raw.strip().replace('_', '-');
        if (trimmed.isEmpty() || trimmed.length() > MAX_LENGTH) {
            return null;
        }
        Locale locale;
        try {
            locale = new Locale.Builder().setLanguageTag(trimmed).build();
        } catch (IllformedLocaleException e) {
            return null;
        }
        String primary = locale.getLanguage();
        if (primary.isEmpty() || "und".equals(primary)) {
            return null;
        }
        // Named in English, never in the author's own locale: whether a tag is a language cannot
        // depend on who is looking at it.
        if (!ISO_639_1.contains(primary) && primary.equals(locale.getDisplayLanguage(Locale.ENGLISH))) {
            return null;
        }
        // toLanguageTag(), not getLanguage(): it is what canonicalizes case and subtag order, so
        // "EN-gb" is stored as "en-GB" and "ZH-hans" as "zh-Hans".
        return locale.toLanguageTag();
    }

    /**
     * The language's name in the reader's own language, for a picker that has to show more than a
     * code. Not capitalized: {@code es-419} names French "francés", and Spanish is right to
     * lower-case it. (The header's language switcher capitalizes because it shows autonyms as
     * standalone labels; this name sits inside a phrase.)
     *
     * @param tag a canonical tag
     * @param ui  the locale to name the language in; English when {@code null}
     * @return the name, or {@code tag} itself when the JDK has no name for it
     */
    public static String displayName(String tag, Locale ui) {
        if (tag == null) {
            return "";
        }
        String name = Locale.forLanguageTag(tag).getDisplayName(ui == null ? Locale.ENGLISH : ui);
        return name == null || name.isBlank() || name.equals(tag) ? tag : name;
    }

    /**
     * Every language a survey can be worked on: the ones it already publishes, then the rest of
     * the curated set. The base language is in neither -- it is the fallback every string has, not
     * a translation of anything (UC-044 BR-005).
     * <p>
     * Published first so that an author's own languages sit at the top, and the curated set in the
     * order it was configured rather than alphabetically, which would make the order depend on who
     * is looking.
     * <p>
     * A published tag the applications do not carry is kept rather than dropped. It can only have
     * arrived from an imported definition or from before the set was curated, and hiding it would
     * leave the survey publishing a language its author can neither see nor remove. The caller
     * marks it; {@link ContentLanguages#supports} is how it is recognized.
     *
     * @param baseLanguage the survey's base language, in any form
     * @param published    the survey's published set
     * @param supported    the curated tags, from {@link ContentLanguages#tags()}
     * @return canonical tags, in offer order, without duplicates
     */
    public static List<String> offerable(String baseLanguage, List<String> published,
                                         List<String> supported) {
        String base = canonical(baseLanguage);
        Set<String> ordered = new LinkedHashSet<>();
        List<String> segments = new ArrayList<>(published == null ? List.of() : published);
        segments.addAll(supported == null ? List.of() : supported);
        for (String tag : segments) {
            String c = canonical(tag);
            if (c != null && !c.equals(base)) {
                ordered.add(c);
            }
        }
        return List.copyOf(ordered);
    }
}
