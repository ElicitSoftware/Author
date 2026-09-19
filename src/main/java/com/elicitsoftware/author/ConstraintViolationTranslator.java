package com.elicitsoftware.author;

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

import jakarta.persistence.PersistenceException;
import org.hibernate.exception.ConstraintViolationException;

import java.util.Locale;
import java.util.Map;

/**
 * Turns database constraint violations into {@link AuthoringException}s with a message an author
 * can act on, keyed by the constraint names Survey's schema declares.
 */
public final class ConstraintViolationTranslator {

    private static final Map<String, String[]> BY_CONSTRAINT = Map.ofEntries(
            Map.entry("surveys_name_un", new String[]{"name", "Another survey already uses this name"}),
            Map.entry("surveys_survey_key_un", new String[]{null, "A survey with this key already exists in Author"}),
            Map.entry("steps_survey_name_un", new String[]{"name", "Another step in this survey already uses this name"}),
            Map.entry("steps_survey_display_order_un", new String[]{"displayOrder", "Another step already has this position"}),
            Map.entry("sections_survey_display_order_un", new String[]{"displayOrder", "Another section already has this position"}),
            Map.entry("select_groups_name_un", new String[]{"name", "Another list in this survey already uses this name"}),
            Map.entry("select_items_display_text_un", new String[]{"displayText", "This list already has an option with that text"}),
            Map.entry("steps_sections_display_key_un", new String[]{"displayKey", "Another assignment already uses this display key"}),
            Map.entry("dimensions_un", new String[]{"dimension", "A dimension with this name already exists"}),
            Map.entry("ontology_un", new String[]{"tag", "A tag with this name already exists in this survey's namespace"}),
            Map.entry("metadata_un", new String[]{null, "This element already carries this tag with the same value"})
    );

    private ConstraintViolationTranslator() {
    }

    /**
     * @return an {@link AuthoringException} when {@code e} wraps a recognised constraint violation,
     *         otherwise {@code e} itself so the caller can rethrow it unchanged.
     */
    public static RuntimeException translate(RuntimeException e) {
        Throwable t = e;
        while (t != null && !(t instanceof ConstraintViolationException)) {
            t = t.getCause();
        }
        if (t == null) {
            return e;
        }
        ConstraintViolationException cve = (ConstraintViolationException) t;
        String name = cve.getConstraintName() == null ? "" : cve.getConstraintName().toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String[]> entry : BY_CONSTRAINT.entrySet()) {
            if (name.endsWith(entry.getKey())) {
                return new AuthoringException(entry.getValue()[1], entry.getValue()[0]);
            }
        }
        if ("23503".equals(cve.getSQLState())) {
            return new AuthoringException("This element is still referenced by another element");
        }
        if ("23505".equals(cve.getSQLState())) {
            return new AuthoringException("A row with the same unique value already exists (" + cve.getConstraintName() + ")");
        }
        return e instanceof PersistenceException ? e : new PersistenceException(e);
    }
}
