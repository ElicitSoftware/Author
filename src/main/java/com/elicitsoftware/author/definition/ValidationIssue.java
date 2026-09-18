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

/**
 * One validation finding (UC-007). {@code kind}/{@code durableId} point at the element concerned so
 * the UI can link to it; {@code kind} is a table name ({@code steps}, {@code questions}, ...).
 */
public record ValidationIssue(Severity severity, String kind, Integer durableId, String message) {

    public enum Severity { ERROR, WARNING }

    public static ValidationIssue error(String kind, Integer durableId, String message) {
        return new ValidationIssue(Severity.ERROR, kind, durableId, message);
    }

    public static ValidationIssue warning(String kind, Integer durableId, String message) {
        return new ValidationIssue(Severity.WARNING, kind, durableId, message);
    }

    public boolean isError() {
        return severity == Severity.ERROR;
    }
}
