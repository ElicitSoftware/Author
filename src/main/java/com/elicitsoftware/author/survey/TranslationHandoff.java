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

import com.elicitsoftware.author.AuthoringException;
import com.elicitsoftware.model.Survey;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The file a translator or an AI agent fills in (UC-045, UC-046).
 * <p>
 * Nobody translates a survey one grid cell at a time. This writes one JSON document per survey and
 * target language carrying every string with the context, tokens and length budget needed to
 * translate it, and reads the same document back with per-item validation. It round-trips by
 * itself: every field the import needs is in it, and the only field the translator fills is
 * {@code translation}.
 * <p>
 * Author-local. It never reaches the console or the survey application; translations travel to a
 * site only inside the definition file (UC-045 BR-001).
 */
@ApplicationScoped
public class TranslationHandoff {

    /** Marks a file this class wrote; refused if it says anything else. */
    public static final String FORMAT = "ELICIT_CONTENT_TRANSLATION_V1";

    /** A translator's file of eight hundred strings should not be refused for one broken item. */
    public static final int MAX_FILE_BYTES = 5 * 1024 * 1024;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Inject
    TranslationService translations;

    /** What an import did, per item and in total. */
    public record ImportResult(int imported, int unchanged, int skipped, int stale,
                               List<String> rejections, List<String> warnings) {

        public int rejected() {
            return rejections.size();
        }
    }

    /**
     * Writes the hand-off document.
     *
     * @param survey      the survey being translated
     * @param language    the target language
     * @param onlyMissing true to carry only the missing and out-of-date strings
     * @return the document, UTF-8
     */
    public byte[] export(Survey survey, String language, boolean onlyMissing) {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("format", FORMAT);
        root.put("survey_key", String.valueOf(survey.surveyKey));
        root.put("survey_name", survey.name);
        root.put("base_language", survey.baseLanguage == null ? "en" : survey.baseLanguage);
        root.put("language", language);
        root.put("generated", OffsetDateTime.now().toString());
        root.put("instructions", instructions(survey, language));

        ArrayNode items = root.putArray("items");
        for (TranslationService.Row row : translations.rows(survey.id, language)) {
            if (onlyMissing && row.status() == TranslationService.Status.TRANSLATED) {
                continue;
            }
            ObjectNode item = items.addObject();
            item.put("id", row.id());
            item.put("element_type", row.elementType());
            item.put("element_key", String.valueOf(row.targetKey()));
            item.put("field", row.field());
            item.put("context", row.context());
            item.put("max_length", TranslationBudget.of(row.elementType(), row.field(), row.baseText()));
            ArrayNode flags = item.putArray("flags");
            // Markup, not "contains an angle bracket": every token is written <NAME>, so the
            // cruder test flagged every personalized string in the survey as HTML.
            if (Tokens.hasMarkup(row.baseText())) {
                flags.add("html");
            }
            ArrayNode tokens = item.putArray("tokens");
            tokensOf(survey.id, row.baseText()).forEach(tokens::add);
            if (!tokens.isEmpty()) {
                flags.add("tokens");
            }
            item.put("source", row.baseText());
            item.put("source_hash", ContentHash.of(row.baseText()));
            item.put("status", row.status().name().toLowerCase());
            if (row.status() == TranslationService.Status.STALE) {
                item.put("previous_source", row.previousBase());
            }
            item.put("current_translation", row.translation() == null ? "" : row.translation());
            item.put("translation", "");
        }
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsBytes(root);
        } catch (IOException e) {
            throw new AuthoringException("Could not write the translation request: " + e.getMessage());
        }
    }

    /** The name the file is offered under. */
    public String fileName(Survey survey, String language) {
        String name = survey.name == null ? "survey" : survey.name.replaceAll("[^A-Za-z0-9._-]+", "_");
        return name + "_" + language + ".translation.json";
    }

    /**
     * Reads a returned document back, item by item.
     * <p>
     * Acceptance is per item and the accepted set is one transaction: a translator's file is not
     * refused for one unbalanced brace (UC-046 BR-001). The envelope is all-or-nothing, because a
     * file for the wrong survey is not partially right.
     *
     * @param survey the survey the file must belong to
     * @param in     the uploaded document
     * @return what landed, what was skipped and what was rejected
     */
    public ImportResult importFile(Survey survey, InputStream in) {
        JsonNode root;
        try {
            byte[] bytes = in.readAllBytes();
            if (bytes.length > MAX_FILE_BYTES) {
                throw new AuthoringException("That file is larger than " + (MAX_FILE_BYTES / (1024 * 1024)) + " MB");
            }
            root = MAPPER.readTree(new String(bytes, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new AuthoringException("That file is not readable JSON: " + e.getMessage());
        }
        if (root == null || !FORMAT.equals(root.path("format").asText())) {
            throw new AuthoringException("That is not a translation request: the file does not carry " + FORMAT);
        }
        if (!String.valueOf(survey.surveyKey).equals(root.path("survey_key").asText())) {
            throw new AuthoringException("That file belongs to another survey");
        }
        String language = root.path("language").asText(null);
        if (language == null || language.isBlank()) {
            throw new AuthoringException("That file names no language");
        }

        int imported = 0;
        int unchanged = 0;
        int skipped = 0;
        int stale = 0;
        List<String> rejections = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Map<String, Set<String>> optionLabelsPerGroup = new LinkedHashMap<>();

        for (JsonNode item : root.path("items")) {
            String id = item.path("id").asText("(unnamed)");
            String translation = item.path("translation").asText("");
            if (translation.isBlank()) {
                skipped++;
                continue;
            }
            String elementType = item.path("element_type").asText(null);
            String field = item.path("field").asText(null);
            UUID key;
            try {
                key = UUID.fromString(item.path("element_key").asText());
            } catch (IllegalArgumentException e) {
                rejections.add(id + ": element_key is not a UUID");
                continue;
            }
            if (!TranslatableFields.isTranslatable(elementType, field)) {
                rejections.add(id + ": " + elementType + "." + field + " carries no respondent-facing text");
                continue;
            }
            String baseText = translations.baseText(survey.id, elementType, key, field);
            if (baseText == null) {
                rejections.add(id + ": that element no longer exists, or has been retired");
                continue;
            }
            // Two checks, because neither is sufficient alone. The token set catches a renamed or
            // invented key, but it can only see the keys the survey's rules name and the bare-word
            // placeholders; a phrase placeholder in a survey whose rules are not written yet names
            // nothing. The placeholder count catches exactly that case -- a translation that simply
            // dropped the substitution -- and needs no rule list at all.
            int sourcePlaceholders = Tokens.placeholders(baseText).size();
            int givenPlaceholders = Tokens.placeholders(translation).size();
            if (sourcePlaceholders != givenPlaceholders) {
                rejections.add(id + ": the source has " + sourcePlaceholders + " placeholder(s) and the "
                        + "translation has " + givenPlaceholders + "; each one fills in an earlier answer");
                continue;
            }
            Set<String> sourceTokens = tokensOf(survey.id, baseText);
            Set<String> given = tokensOf(survey.id, translation);
            if (!sourceTokens.equals(given)) {
                rejections.add(id + ": tokens " + given + " do not match the source's " + sourceTokens);
                continue;
            }
            if (unbalanced(translation)) {
                rejections.add(id + ": unbalanced braces or bars in the translation");
                continue;
            }
            int budget = TranslationBudget.of(elementType, field, baseText);
            if (translation.length() > budget) {
                rejections.add(id + ": " + translation.length() + " characters, over the " + budget + " allowed");
                continue;
            }
            if ("select_items".equals(elementType)) {
                Set<String> seen = optionLabelsPerGroup.computeIfAbsent(elementType, k -> new HashSet<>());
                if (!seen.add(translation)) {
                    rejections.add(id + ": another option of this question is translated the same way");
                    continue;
                }
            }
            if (tagsDiffer(baseText, translation)) {
                warnings.add(id + ": the HTML tags differ from the source's");
            }

            String before = currentValue(survey.id, key, field, language);
            translations.upsert(survey.id, elementType, key, field, language, translation);
            if (translation.equals(before)) {
                unchanged++;
            } else {
                imported++;
            }
            if (!ContentHash.of(baseText).equals(item.path("source_hash").asText())) {
                stale++;
                warnings.add(id + ": the base text changed since the file was exported");
            }
        }
        return new ImportResult(imported, unchanged, skipped, stale, rejections, warnings);
    }

    private String currentValue(int surveyId, UUID key, String field, String language) {
        // map after findFirst: a row whose translation is null is a found row, not an absent one,
        // and Optional.of(null) would throw.
        return translations.rows(surveyId, language).stream()
                .filter(r -> r.targetKey().equals(key) && r.field().equals(field))
                .findFirst().map(TranslationService.Row::translation).orElse(null);
    }

    /**
     * The tokens a string uses, as the runtime fills them: the {@code <TOKEN>} references inside its
     * placeholders.
     * <p>
     * The survey's own token list is not consulted, and does not need to be. A token declares itself
     * by its brackets, so every use is visible in the string alone -- which is the point of the
     * brackets. (This used to walk {@code Tokens.known(surveyId)} as well and keep any token the
     * phrase <em>contained</em>, because a phrase carrying a bare name was the common form. Since a
     * reference matches only in brackets, that pass could no longer add anything {@code names} had
     * not already found. A bare name in a base text is reported at export instead, where it can be
     * fixed: UC-007 BR-005.)
     *
     * @param surveyId the survey the string belongs to, for callers' symmetry
     * @param text     the string to inspect
     * @return every token the string uses, ordered
     */
    static Set<String> tokensOf(int surveyId, String text) {
        return new java.util.TreeSet<>(Tokens.names(text));
    }

    /** Every {@code &#123;} must be consumed by a placeholder, and none may nest. */
    private static boolean unbalanced(String text) {
        int open = 0;
        for (char c : text.toCharArray()) {
            if (c == '{') {
                open++;
                if (open > 1) {
                    return true;
                }
            } else if (c == '}') {
                open--;
                if (open < 0) {
                    return true;
                }
            }
        }
        return open != 0;
    }

    /** Warned, not rejected: a translator may legitimately move emphasis (UC-046 BR-005). */
    private static boolean tagsDiffer(String source, String translation) {
        return tagCount(source) != tagCount(translation);
    }

    private static long tagCount(String text) {
        return text == null ? 0 : text.chars().filter(c -> c == '<').count();
    }

    /**
     * The preamble, in the file itself, so an agent needs nothing but the document (UC-045 BR-002).
     */
    private String instructions(Survey survey, String language) {
        return """
                Translate each item's "source" into its "translation" field, from %s into %s. \
                Leave every other field exactly as it is; do not add, remove or reorder items.

                A placeholder is written {phrase containing a <TOKEN>|default text}. The token is \
                the upper-case key in angle brackets, listed in that item's "tokens"; copy \
                <TOKEN> across exactly, brackets and all, and never translate what is inside them. \
                Translate the phrase around it, and translate the default after the bar -- the \
                default is what a respondent reads when the survey has no value to fill in, so one \
                left in the source language shows through in the middle of your sentence. Move the \
                token to where the language needs it, and reshape the phrase where the language \
                requires it: English "{<NAME>'s|this person's} gender" becomes "el genero de \
                {<NAME>|esta persona}" in a language that forms no possessive with 's. Keep the \
                braces, the angle brackets and the bar; never add or drop one. A translation must \
                use exactly the tokens its source uses -- the bracketed keys, not the phrases that \
                carry them.

                Where "flags" contains "html", keep the tags and translate only the text between \
                them. Tags sit outside the braces and never inside one -- \
                <b>{<NAME>|name}</b>, never {<b><NAME></b>|<b>name</b>} -- so leave them where \
                they are; inside a placeholder every <...> is a token. Respect "max_length": it already allows for the target language running \
                longer than the source, so it is a ceiling for the space the string is drawn in, \
                not a length to fill. Option labels of one question must stay distinct from each \
                other.

                Leave "translation" empty for anything you cannot translate; an empty value means \
                "not translated" and is skipped rather than stored as a blank.

                Glossary, to keep as they are: Elicit, survey, step, section, question, access \
                code. Return this same document, UTF-8, with the translations filled in.
                """.formatted(survey.baseLanguage == null ? "en" : survey.baseLanguage, language);
    }
}
