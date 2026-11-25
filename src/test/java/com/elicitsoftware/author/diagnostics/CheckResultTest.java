package com.elicitsoftware.author.diagnostics;

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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** UC-037 / UC-034: one probe's outcome as the System screens render it. */
class CheckResultTest {

    @Test
    void up() {
        CheckResult r = CheckResult.up("Survey", "3.0.0", 12);
        assertEquals(new CheckResult("Survey", CheckResult.Status.UP, "3.0.0", 12), r);
        assertTrue(r.isUp());
    }

    @Test
    void down() {
        CheckResult r = CheckResult.down("Survey", "connection refused", 30);
        assertEquals(new CheckResult("Survey", CheckResult.Status.DOWN, "connection refused", 30), r);
        assertFalse(r.isUp());
    }

    @Test
    void unknown_hasNoDuration() {
        CheckResult r = CheckResult.unknown("Pedigree", "not configured");
        assertEquals(new CheckResult("Pedigree", CheckResult.Status.UNKNOWN, "not configured", 0), r);
        assertFalse(r.isUp());
    }
}
