package com.elicitsoftware.author;

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

import com.elicitsoftware.author.i18n.Translations;
import jakarta.persistence.PersistenceException;
import org.hibernate.exception.ConstraintViolationException;

import java.util.Locale;
import java.util.Map;

/**
 * Turns database constraint violations into {@link AuthoringException}s with a message an author
 * can act on, keyed by the constraint names Survey's schema declares. Each constraint maps to the
 * field it concerns and to a translation key ({@code constraint.<constraint name>}) resolved for
 * the current UI locale at lookup time (UC-034).
 */
public final class ConstraintViolationTranslator {

    /** Constraint name suffix -> the field it concerns (or null); the message key is {@code constraint.<name>}. */
    private static final Map<String, String> FIELD_BY_CONSTRAINT = Map.ofEntries(
            Map.entry("surveys_name_un", "name"),
            Map.entry("surveys_survey_key_un", ""),
            Map.entry("steps_survey_name_un", "name"),
            Map.entry("steps_survey_display_order_un", "displayOrder"),
            Map.entry("sections_survey_display_order_un", "displayOrder"),
            Map.entry("select_groups_name_un", "name"),
            Map.entry("select_items_display_text_un", "displayText"),
            Map.entry("steps_sections_display_key_un", "displayKey"),
            Map.entry("dimensions_un", "dimension"),
            Map.entry("ontology_un", "tag"),
            Map.entry("metadata_un", "")
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
        for (Map.Entry<String, String> entry : FIELD_BY_CONSTRAINT.entrySet()) {
            if (name.endsWith(entry.getKey())) {
                String field = entry.getValue().isEmpty() ? null : entry.getValue();
                return new AuthoringException(Translations.get("constraint." + entry.getKey()), field);
            }
        }
        if ("23503".equals(cve.getSQLState())) {
            return new AuthoringException(Translations.get("constraint.sqlstate.23503"));
        }
        if ("23505".equals(cve.getSQLState())) {
            return new AuthoringException(Translations.get("constraint.sqlstate.23505", cve.getConstraintName()));
        }
        return e instanceof PersistenceException ? e : new PersistenceException(e);
    }
}
