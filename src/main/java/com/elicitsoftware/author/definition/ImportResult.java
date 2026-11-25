package com.elicitsoftware.author.definition;

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

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Outcome of a successful import (UC-005): the new local survey plus what the file carried.
 *
 * @param surveyId       the id of the imported survey in Author's database
 * @param surveyKey      the survey key the file carried
 * @param surveyName     the survey's name
 * @param fileRevision   the revision stamped in the file's header, or null when it has none
 * @param imported       rows imported, by {@code .elicit} table name
 * @param skippedRetired retired rows skipped, by versioned table name
 * @param draftFormat    whether the file was an AI-drafted definition ({@code ELICIT_SURVEY_DRAFT_V1},
 *                       UC-042) rather than an export
 */
public record ImportResult(int surveyId, UUID surveyKey, String surveyName, OffsetDateTime fileRevision,
                           Map<String, Integer> imported, Map<String, Integer> skippedRetired,
                           boolean draftFormat) {

    /**
     * Returns the number of rows imported across all tables.
     *
     * @return the sum of {@link #imported()}
     */
    public int totalImported() {
        return imported.values().stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * Returns the number of retired rows skipped across all tables.
     *
     * @return the sum of {@link #skippedRetired()}
     */
    public int totalSkipped() {
        return skippedRetired.values().stream().mapToInt(Integer::intValue).sum();
    }
}
