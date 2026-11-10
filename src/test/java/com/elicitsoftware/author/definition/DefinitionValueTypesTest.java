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

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The value types the import (UC-005) and validation (UC-007) hand to the UI: an import's totals,
 * a finding's severity, and the reasons a definition was refused.
 */
class DefinitionValueTypesTest {

    @Test
    void importResult_totalsEveryTable() {
        ImportResult r = new ImportResult(1, UUID.randomUUID(), "FHHS", OffsetDateTime.now(),
                Map.of("steps", 3, "questions", 40), Map.of("questions", 2, "select_items", 5), false);
        assertEquals(43, r.totalImported());
        assertEquals(7, r.totalSkipped());

        ImportResult empty = new ImportResult(1, UUID.randomUUID(), "FHHS", null, Map.of(), Map.of(), true);
        assertEquals(0, empty.totalImported());
        assertEquals(0, empty.totalSkipped());
    }

    @Test
    void validationIssue_severity() {
        ValidationIssue error = ValidationIssue.error("steps", 4, "broken");
        assertEquals(new ValidationIssue(ValidationIssue.Severity.ERROR, "steps", 4, "broken"), error);
        assertTrue(error.isError());

        ValidationIssue warning = ValidationIssue.warning("questions", 9, "odd");
        assertEquals(new ValidationIssue(ValidationIssue.Severity.WARNING, "questions", 9, "odd"), warning);
        assertFalse(warning.isError());
    }

    @Test
    void definitionException_details() {
        List<String> reasons = new ArrayList<>(List.of("line 3", "line 9"));
        DefinitionException e = new DefinitionException("refused", reasons);
        reasons.add("line 12");
        assertEquals(List.of("line 3", "line 9"), e.details(), "a copy, not the caller's list");
        assertEquals("refused", e.getMessage());

        assertEquals(List.of(), new DefinitionException("refused").details());

        IllegalStateException cause = new IllegalStateException("io");
        DefinitionException withCause = new DefinitionException("refused", cause);
        assertEquals(List.of(), withCause.details());
        assertSame(cause, withCause.getCause());
    }
}
