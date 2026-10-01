package com.elicitsoftware.author.definition;

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

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The {@code ELICIT_SURVEY_EXPORT_V1} definition-file format, shared with Admin's
 * {@code SurveyDefinitionFileFields} / export service (C-015). One record per line,
 * {@code table: field|field|...}; {@code #} lines are header/comments; pipe, backslash, newline
 * and carriage return are escaped as {@code \|}, {@code \\}, {@code \n}, {@code \r}; an empty
 * field is {@code null}.
 *
 * <p>Record layouts (field counts are the <em>minimum</em> a reader requires; a writer emits
 * exactly this many). Since Survey V015 the Type 2 records end with {@code published_comment};
 * files written before V015 carry one more trailing {@code is_draft} field, which readers ignore.</p>
 *
 * <p>Survey V019 redefined two records in place, under the same header: {@code surveys} gained the
 * trailing {@code base_language|content_languages} (arity 10 to 12), and {@code translations} was
 * added last in {@link #TABLES}, because it references every other table's element keys. Nothing
 * needs a compatibility layer -- no file with this header exists outside this repository.</p>
 */
public final class ElicitFormat {

    public static final String FORMAT_VERSION = "ELICIT_SURVEY_EXPORT_V1";
    /**
     * Marker for a definition drafted outside Author -- an AI session writing straight to the
     * format, guided by {@code docs/ai/AUTHORING_FROM_AI.md}. Author imports it; Admin refuses it,
     * because Admin's readers gate on {@link #FORMAT_VERSION} as a substring and this deliberately
     * does not contain it. A draft therefore cannot reach a deployed site: it must be reviewed in
     * Author and re-exported under {@link #FORMAT_VERSION} first. Author writes this marker only on
     * the backup taken before a survey is deleted (UC-047 BR-006), for the same reason.
     */
    public static final String DRAFT_FORMAT_VERSION = "ELICIT_SURVEY_DRAFT_V1";
    public static final String REVISION_HEADER = "# survey_revision:";
    public static final String SENTINEL_LITERAL = "9999-12-31 23:59:59+00";
    public static final OffsetDateTime SENTINEL = OffsetDateTime.parse("9999-12-31T23:59:59Z");
    public static final int MAX_FILE_BYTES = 5 * 1024 * 1024;

    /**
     * Fields of a {@code translations} record, in file order:
     * {@code translation_id|translation_key|element_type|element_key|field|language|value|source_hash}
     * followed by the Type 2 tail. {@code translation_id} is a local link only and is remapped on
     * import; {@code translation_key} and {@code element_key} are preserved verbatim, and
     * {@code source_hash} travels as it stands so a translation that is stale at the master is
     * stale at every site. {@code source_text} is authoring-only and is never exported, on the
     * same footing as a question's sample.
     */
    public static final String TRANSLATION_FIELDS =
            "translation_id, translation_key, element_type, element_key, field, language, value, source_hash";
    public static final String FILE_EXTENSION = ".elicit";

    /** Tables in dependency (and file) order. */
    public static final List<String> TABLES = List.of(
            "surveys", "select_groups", "select_items", "steps", "sections", "steps_sections",
            "questions", "sections_questions", "relationships", "reports", "post_survey_actions",
            "dimensions", "ontology", "metadata", "translations");

    /** Tables whose rows carry the Type 2 tail {@code version|effective_from|effective_to|published_by|published_comment}. */
    public static final List<String> VERSIONED_TABLES = List.of(
            "select_groups", "select_items", "steps", "sections", "steps_sections",
            "questions", "sections_questions", "relationships", "translations");

    /** Minimum field count per record type (post-V015 layout, without {@code is_draft}). */
    public static final Map<String, Integer> REQUIRED_FIELDS = Map.ofEntries(
            // 10, not 12: base_language and content_languages (V019) are read when present and
            // default to 'en' and "nothing published" when they are not, so a file written before
            // them -- or drafted by hand or by an agent (DRAFT_FORMAT_VERSION) -- stays valid. A
            // writer emits all 12.
            Map.entry("surveys", 10),
            Map.entry("select_groups", 10),
            Map.entry("select_items", 11),
            Map.entry("steps", 11),
            Map.entry("sections", 11),
            Map.entry("steps_sections", 12),
            Map.entry("questions", 20),
            Map.entry("sections_questions", 10),
            Map.entry("relationships", 19),
            Map.entry("reports", 6),
            Map.entry("post_survey_actions", 6),
            Map.entry("dimensions", 3),
            Map.entry("ontology", 5),
            Map.entry("metadata", 7),
            Map.entry("translations", 13));

    /** Zero-based index of {@code effective_to} within each versioned record. */
    public static final Map<String, Integer> EFFECTIVE_TO_INDEX = Map.ofEntries(
            Map.entry("select_groups", 7),
            Map.entry("select_items", 8),
            Map.entry("steps", 8),
            Map.entry("sections", 8),
            Map.entry("steps_sections", 9),
            Map.entry("questions", 17),
            Map.entry("sections_questions", 7),
            Map.entry("relationships", 16),
            Map.entry("translations", 10));

    private ElicitFormat() {
    }

    public static OffsetDateTime parseRevisionHeader(String line) {
        if (!line.startsWith(REVISION_HEADER)) {
            return null;
        }
        String value = line.substring(REVISION_HEADER.length()).trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("File carries an empty \"" + REVISION_HEADER + "\" header");
        }
        try {
            return OffsetDateTime.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Unparseable \"" + REVISION_HEADER + "\" header value: " + value, e);
        }
    }

    public static String[] parseFields(String data) {
        List<String> fieldList = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < data.length(); i++) {
            char c = data.charAt(i);
            if (escaped) {
                switch (c) {
                    case '|' -> current.append('|');
                    case '\\' -> current.append('\\');
                    case 'n' -> current.append('\n');
                    case 'r' -> current.append('\r');
                    default -> {
                        current.append('\\');
                        current.append(c);
                    }
                }
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '|') {
                fieldList.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        fieldList.add(current.toString());
        return fieldList.toArray(new String[0]);
    }

    public static String escapeField(Object value) {
        if (value == null) {
            return "";
        }
        String s = value.toString();
        StringBuilder out = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '|' -> out.append("\\|");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                default -> out.append(c);
            }
        }
        return out.toString();
    }

    public static String joinFields(Object... values) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                out.append('|');
            }
            out.append(escapeField(values[i]));
        }
        return out.toString();
    }

    public static String nullIfEmpty(String value) {
        return (value == null || value.isEmpty()) ? null : value;
    }

    /**
     * Rewrites the leading survey component of a display key (or initial display key) onto the
     * given survey id; non-structured values are returned unchanged.
     */
    public static String rebaseDisplayKey(String displayKey, Number surveyId) {
        String value = nullIfEmpty(displayKey);
        if (value == null || surveyId == null) {
            return value;
        }
        int firstSeparator = value.indexOf('-');
        if (firstSeparator < 0) {
            return value;
        }
        return String.format("%04d", surveyId.longValue()) + value.substring(firstSeparator);
    }

    public static Integer parseIntOrNull(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static java.math.BigDecimal parseDecimalOrNull(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return new java.math.BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Long parseLongOrNull(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static boolean parseBooleanOrFalse(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        return Boolean.parseBoolean(value) || "t".equalsIgnoreCase(value);
    }

    /** The file's own value if present, otherwise a fresh key; malformed values are rejected. */
    public static UUID parseKeyOrMint(String raw, String what) {
        String value = nullIfEmpty(raw);
        if (value == null) {
            return UUID.randomUUID();
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid " + what + " in file: " + value, e);
        }
    }

    /** True when a versioned record's {@code effective_to} field says the row was retired. */
    public static boolean isRetired(String table, String[] fields) {
        Integer idx = EFFECTIVE_TO_INDEX.get(table);
        if (idx == null || fields.length <= idx) {
            return false;
        }
        String value = nullIfEmpty(fields[idx]);
        if (value == null) {
            return false;
        }
        try {
            return OffsetDateTime.parse(value.trim().replace(' ', 'T')).isBefore(SENTINEL);
        } catch (DateTimeParseException e) {
            return !value.startsWith("9999");
        }
    }

    public static Long resolveRequired(Long oldId, Map<Long, Long> map, String entityDesc) {
        if (oldId == null) {
            throw new IllegalStateException("Expected non-null source id for " + entityDesc);
        }
        Long newId = map.get(oldId);
        if (newId == null) {
            throw new IllegalStateException("No ID mapping found for " + entityDesc + " source_id=" + oldId);
        }
        return newId;
    }

    public static Long resolveNullable(Long oldId, Map<Long, Long> map, String entityDesc) {
        return oldId == null ? null : resolveRequired(oldId, map, entityDesc);
    }

    public static void requireFields(String table, String[] fields) {
        int required = REQUIRED_FIELDS.getOrDefault(table, 0);
        if (fields.length < required) {
            throw new IllegalArgumentException(table + " requires " + required + " fields, got " + fields.length);
        }
    }
}
