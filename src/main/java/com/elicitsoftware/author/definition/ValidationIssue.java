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

/**
 * One validation finding (UC-007). {@code kind}/{@code durableId} point at the element concerned so
 * the UI can link to it; {@code kind} is a table name ({@code steps}, {@code questions}, ...).
 *
 * @param severity  whether the finding is an error or a warning
 * @param kind      the table of the element concerned
 * @param durableId the durable id of the element concerned, or {@code null} when there is none
 * @param message   the finding, worded for the author
 */
public record ValidationIssue(Severity severity, String kind, Integer durableId, String message) {

    /** How serious a finding is. */
    public enum Severity { ERROR, WARNING }

    /**
     * An error finding.
     *
     * @param kind      the table of the element concerned
     * @param durableId the durable id of the element concerned, or {@code null}
     * @param message   the finding, worded for the author
     * @return the issue at {@link Severity#ERROR}
     */
    public static ValidationIssue error(String kind, Integer durableId, String message) {
        return new ValidationIssue(Severity.ERROR, kind, durableId, message);
    }

    /**
     * A warning finding.
     *
     * @param kind      the table of the element concerned
     * @param durableId the durable id of the element concerned, or {@code null}
     * @param message   the finding, worded for the author
     * @return the issue at {@link Severity#WARNING}
     */
    public static ValidationIssue warning(String kind, Integer durableId, String message) {
        return new ValidationIssue(Severity.WARNING, kind, durableId, message);
    }

    /**
     * Whether this finding is an error.
     *
     * @return true at {@link Severity#ERROR}
     */
    public boolean isError() {
        return severity == Severity.ERROR;
    }
}
