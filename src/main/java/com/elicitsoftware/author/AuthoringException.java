package com.elicitsoftware.author;

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

/** A rule violation or conflict the UI can show verbatim to the author. */
public class AuthoringException extends RuntimeException {

    /** Name of the form field the message belongs to, or {@code null} for a general message. */
    private final String field;

    /**
     * A general message, tied to no form field.
     *
     * @param message the author-readable message
     */
    public AuthoringException(String message) {
        this(message, null);
    }

    /**
     * A message that belongs to one form field.
     *
     * @param message the author-readable message
     * @param field   the form field's name, or {@code null} for a general message
     */
    public AuthoringException(String message, String field) {
        super(message);
        this.field = field;
    }

    /**
     * Returns the form field the message belongs to.
     *
     * @return the field's name, or {@code null} for a general message
     */
    public String field() {
        return field;
    }
}
