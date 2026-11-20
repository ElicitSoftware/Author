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

import java.util.List;

/** A definition file or survey could not be imported/exported; carries author-readable reasons. */
public class DefinitionException extends RuntimeException {

    private final List<String> details;

    /**
     * A failure with no further reasons.
     *
     * @param message the author-readable message
     */
    public DefinitionException(String message) {
        this(message, List.of());
    }

    /**
     * A failure with the reasons behind it.
     *
     * @param message the author-readable message
     * @param details the author-readable reasons, copied; must not be null
     */
    public DefinitionException(String message, List<String> details) {
        super(message);
        this.details = List.copyOf(details);
    }

    /**
     * A failure caused by another exception, with no further reasons.
     *
     * @param message the author-readable message
     * @param cause   the exception that caused it
     */
    public DefinitionException(String message, Throwable cause) {
        super(message, cause);
        this.details = List.of();
    }

    /**
     * The author-readable reasons for the failure.
     *
     * @return an immutable list; empty when none were given
     */
    public List<String> details() {
        return details;
    }
}
