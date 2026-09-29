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
 * A content language is a BCP-47 tag in the same form the mounted chrome bundles use, so that the
 * survey's published set and a site's own mount can be intersected without guessing (UC-044
 * BR-004). That is why a tag is canonicalized on the way in rather than stored as typed.
 */
public final class LanguageTags {

    /**
     * The twenty most used languages on the internet, in that order: English, Chinese
     * (Simplified), Spanish, Arabic, Portuguese, Indonesian, French, Japanese, Russian, German,
     * Korean, Turkish, Italian, Vietnamese, Hindi, Polish, Dutch, Persian, Thai, Ukrainian.
     * <p>
     * Keep the names in this comment: a bare column of tags is exactly what an author cannot read,
     * which is the complaint the picker answers. Chinese is Simplified only; an author who wants
     * Traditional types {@code zh-Hant}, which {@link #canonical} accepts.
     */
    public static final List<String> TOP_TWENTY = List.of(
            "en", "zh-Hans", "es", "ar", "pt", "id", "fr", "ja", "ru", "de",
            "ko", "tr", "it", "vi", "hi", "pl", "nl", "fa", "th", "uk");

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
        String trimmed = raw.strip();
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
     * {@link #TOP_TWENTY}. The base language is in neither -- it is the fallback every string has,
     * not a translation of anything (UC-044 BR-005).
     * <p>
     * Published first, and the twenty in their own ranking order rather than alphabetically: the
     * picker filters as the author types, which makes alphabetical order nearly worthless, so the
     * ordering is spent on what an author is likely to want. Sorting by display name would also
     * make the order depend on who is looking.
     *
     * @param baseLanguage the survey's base language, in any form
     * @param published    the survey's published set
     * @return canonical tags, in offer order, without duplicates
     */
    public static List<String> offerable(String baseLanguage, List<String> published) {
        String base = canonical(baseLanguage);
        Set<String> ordered = new LinkedHashSet<>();
        List<String> segments = new ArrayList<>(published == null ? List.of() : published);
        segments.addAll(TOP_TWENTY);
        for (String tag : segments) {
            String c = canonical(tag);
            if (c != null && !c.equals(base)) {
                ordered.add(c);
            }
        }
        return List.copyOf(ordered);
    }
}
