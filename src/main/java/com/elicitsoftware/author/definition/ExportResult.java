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
import java.util.List;
import java.util.Map;

/**
 * A produced definition file (UC-008).
 *
 * @param fileName the file's name: the survey name made file-safe, the export instant, and the
 *                 {@code .elicit} extension
 * @param bytes    the file's content in UTF-8
 * @param revision the instant the file was written, also carried in its revision header
 * @param counts   records per table, in file order, {@code surveys} first
 * @param warnings the validation warnings found before writing; empty for a backup
 */
public record ExportResult(String fileName, byte[] bytes, OffsetDateTime revision,
                           Map<String, Integer> counts, List<ValidationIssue> warnings) {
}
