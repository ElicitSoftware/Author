package com.elicitsoftware.author.survey;

/*-
 * ***LICENSE_START***
 * Elicit Author
 * %%
 * Copyright (C) 2026 The Regents of the University of Michigan - Rogel Cancer Center
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

    private ReportingNames() {
    }

    /** {@code dim_<lower(name)>}. */
    public static String dimTableForDimension(String dimensionName) {
        return "dim_" + lower(dimensionName);
    }

    /** {@code lower('dim_' || replace(tag, ' ', '_'))} — the hyphen is kept, as the ETL keeps it. */
    public static String dimTableForTag(String tag) {
        return lower("dim_" + safe(tag).replace(' ', '_'));
    }

    /** {@code lower(replace(replace(tag, ' ', '_'), '-', '')) || '_key'}. */
    public static String factColumn(String tag) {
        return lower(safe(tag).replace(' ', '_').replace("-", "")) + "_key";
    }

    /** The dimension table this tag's values land in. */
    public static String dimTable(Ontology o) {
        return o.dimension != null && o.dimension.name != null ? dimTableForDimension(o.dimension.name) : dimTableForTag(o.tag);
    }

    /** The dimension table for a tag given a dimension name that may be null or blank. */
    public static String dimTable(String tag, String dimensionName) {
        return dimensionName == null || dimensionName.isBlank() ? dimTableForTag(tag) : dimTableForDimension(dimensionName);
    }

    /**
     * Why the ETL would reject {@code identifier}, or empty when it is usable as a table or column
     * name at a deployed site.
     */
    public static Optional<String> identifierProblem(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return Optional.of("is empty");
        }
        if (!IDENTIFIER.matcher(identifier).matches()) {
            return Optional.of("'" + identifier + "' contains characters other than a-z, 0-9 and _");
        }
        if (identifier.length() > MAX_IDENTIFIER_LENGTH) {
            return Optional.of("'" + identifier + "' is longer than " + MAX_IDENTIFIER_LENGTH + " characters");
        }
        return Optional.empty();
    }

    private static String lower(String s) {
        return safe(s).toLowerCase(Locale.ROOT);
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
