package com.elicitsoftware.author.definition;

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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/** A produced definition file (UC-008). */
public record ExportResult(String fileName, byte[] bytes, OffsetDateTime revision,
                           Map<String, Integer> counts, List<ValidationIssue> warnings) {
}
