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
import java.util.Map;
import java.util.UUID;

/** Outcome of a successful import (UC-005): the new local survey plus what the file carried. */
public record ImportResult(int surveyId, UUID surveyKey, String surveyName, OffsetDateTime fileRevision,
                           Map<String, Integer> imported, Map<String, Integer> skippedRetired) {

    public int totalImported() {
        return imported.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int totalSkipped() {
        return skippedRetired.values().stream().mapToInt(Integer::intValue).sum();
    }
}
