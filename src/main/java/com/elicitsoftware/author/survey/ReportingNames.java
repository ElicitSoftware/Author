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

import com.elicitsoftware.model.Ontology;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * The names Survey's reporting ETL derives from tags and dimensions (UC-024 BR-001). These mirror
 * {@code com.elicitsoftware.etl.Sql} in the Survey module exactly: a tag on a dimension shares the
 * dimension's table {@code dim_<lower(name)>}; a tag without one gets
 * {@code lower('dim_' || replace(tag, ' ', '_'))}; every tag gets the {@code fact_sections} column
 * {@code lower(replace(replace(tag, ' ', '_'), '-', '')) || '_key'}. The ETL splices these into SQL
 * after checking {@code ^[a-z0-9_]+$}, and PostgreSQL truncates identifiers past 63 characters, so
 * Author refuses anything that would fail there.
 */
public final class ReportingNames {

    /** What the ETL accepts as a spliced identifier ({@code Sql.requireValidIdentifier}). */
    public static final Pattern IDENTIFIER = Pattern.compile("^[a-z0-9_]+$");

    /** PostgreSQL's {@code NAMEDATALEN - 1}. */
    public static final int MAX_IDENTIFIER_LENGTH = 63;

    /** Characters a tag may carry: the ETL maps spaces to underscores and drops hyphens. */
    public static final Pattern TAG_CHARS = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9 \\-]*$");

    /** A dimension name is used verbatim (lowercased) as a table suffix, so it must already be identifier-safe. */
    public static final Pattern DIMENSION_CHARS = Pattern.compile("^[A-Za-z0-9_]+$");

    /**
     * The fixed columns of every survey's {@code fact_sections} (Survey UC-008 BR-007, BR-012):
     * the step and section of the row, and the question and item a section repeated per selected
     * item is about. A tag whose column would be one of them is refused by the site's reporting
     * build, so Author refuses it first.
     */
    public static final java.util.Set<String> RESERVED_COLUMNS = java.util.Set.of("step_key", "section_key", "question_key", "item_key");

    private ReportingNames() {
    }

    /**
     * {@code dim_<lower(name)>}.
     *
     * @param dimensionName the dimension's name; {@code null} counts as empty
     * @return the dimension's table name
     */
    public static String dimTableForDimension(String dimensionName) {
        return "dim_" + lower(dimensionName);
    }

    /**
     * {@code lower('dim_' || replace(tag, ' ', '_'))} — the hyphen is kept, as the ETL keeps it.
     *
     * @param tag the tag; trimmed, and {@code null} counts as empty
     * @return the tag's own dimension table name
     */
    public static String dimTableForTag(String tag) {
        return lower("dim_" + safe(tag).replace(' ', '_'));
    }

    /**
     * {@code lower(replace(replace(tag, ' ', '_'), '-', '')) || '_key'}.
     *
     * @param tag the tag; trimmed, and {@code null} counts as empty
     * @return the tag's {@code fact_sections} column name
     */
    public static String factColumn(String tag) {
        return lower(safe(tag).replace(' ', '_').replace("-", "")) + "_key";
    }

    /**
     * The dimension table this tag's values land in.
     *
     * @param o the tag
     * @return its dimension's table when it has a named dimension, else the tag's own table
     */
    public static String dimTable(Ontology o) {
        return o.dimension != null && o.dimension.name != null ? dimTableForDimension(o.dimension.name) : dimTableForTag(o.tag);
    }

    /**
     * The dimension table for a tag given a dimension name that may be null or blank.
     *
     * @param tag the tag
     * @param dimensionName the dimension's name, or {@code null} or blank for none
     * @return the dimension's table, or the tag's own table when no dimension is named
     */
    public static String dimTable(String tag, String dimensionName) {
        return dimensionName == null || dimensionName.isBlank() ? dimTableForTag(tag) : dimTableForDimension(dimensionName);
    }

    /** What is wrong with a name; the caller words it from the language files (UC-007 BR-013, NFR-018). */
    public enum IdentifierDefect {
        EMPTY("empty"), CHARACTERS("characters"), TOO_LONG("tooLong");

        private final String keySuffix;

        IdentifierDefect(String keySuffix) {
            this.keySuffix = keySuffix;
        }

        /**
         * The last segment of the {@code validation.tag.table.*} / {@code validation.tag.column.*} key.
         *
         * @return the key suffix
         */
        public String keySuffix() {
            return keySuffix;
        }
    }

    /**
     * Why the ETL would reject {@code identifier}, or empty when it is usable as a table or column
     * name at a deployed site.
     *
     * @param identifier the table or column name to check
     * @return the first defect found (empty, characters, then length), or empty when there is none
     */
    public static Optional<IdentifierDefect> identifierDefect(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return Optional.of(IdentifierDefect.EMPTY);
        }
        if (!IDENTIFIER.matcher(identifier).matches()) {
            return Optional.of(IdentifierDefect.CHARACTERS);
        }
        if (identifier.length() > MAX_IDENTIFIER_LENGTH) {
            return Optional.of(IdentifierDefect.TOO_LONG);
        }
        return Optional.empty();
    }

    /**
     * Whether a tag's column is one of the fixed columns of {@code fact_sections}.
     *
     * @param column the column name
     * @return {@code true} when it is reserved; {@code false} for {@code null}
     */
    public static boolean reservedColumn(String column) {
        return column != null && RESERVED_COLUMNS.contains(column);
    }

    private static String lower(String s) {
        return safe(s).toLowerCase(Locale.ROOT);
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
