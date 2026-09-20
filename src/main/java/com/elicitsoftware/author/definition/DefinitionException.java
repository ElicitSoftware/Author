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

    public DefinitionException(String message) {
        this(message, List.of());
    }

    public DefinitionException(String message, List<String> details) {
        super(message);
        this.details = List.copyOf(details);
    }

    public DefinitionException(String message, Throwable cause) {
        super(message, cause);
        this.details = List.of();
    }

    public List<String> details() {
        return details;
    }
}
