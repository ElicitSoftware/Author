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
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/** UC-040: a database constraint violation reaches the author as a message in their language, on the field concerned. */
class ConstraintViolationTranslatorTest {

    private static RuntimeException violation(String constraint, String sqlState) {
        ConstraintViolationException cve = new ConstraintViolationException(
                "violation", new SQLException("duplicate", sqlState), constraint);
        return new PersistenceException("flush failed", new RuntimeException("wrapped", cve));
    }

    private static AuthoringException translate(RuntimeException e) {
        return assertInstanceOf(AuthoringException.class, ConstraintViolationTranslator.translate(e));
    }

    @Test
    void knownConstraint_becomesItsMessageOnItsField() {
        AuthoringException a = translate(violation("survey.steps_survey_name_un", "23505"));
        assertEquals(Translations.get("constraint.steps_survey_name_un"), a.getMessage());
        assertEquals("name", a.field());
    }

    @Test
    void constraintName_isMatchedCaseInsensitively() {
        AuthoringException a = translate(violation("SURVEY.SELECT_ITEMS_DISPLAY_TEXT_UN", "23505"));
        assertEquals(Translations.get("constraint.select_items_display_text_un"), a.getMessage());
        assertEquals("displayText", a.field());
    }

    @Test
    void constraintWithoutAField_isAGeneralMessage() {
        AuthoringException a = translate(violation("surveys_survey_key_un", "23505"));
        assertEquals(Translations.get("constraint.surveys_survey_key_un"), a.getMessage());
        assertNull(a.field());
    }

    @Test
    void unknownForeignKeyViolation_usesTheSqlStateMessage() {
        AuthoringException a = translate(violation(null, "23503"));
        assertEquals(Translations.get("constraint.sqlstate.23503"), a.getMessage());
        assertNull(a.field());
    }

    @Test
    void unknownUniqueViolation_namesTheConstraint() {
        AuthoringException a = translate(violation("Some_Other_UN", "23505"));
        assertEquals(Translations.get("constraint.sqlstate.23505", "Some_Other_UN"), a.getMessage());
    }

    @Test
    void unknownViolationOfAnotherKind_isRethrownAsAPersistenceException() {
        RuntimeException persistence = violation("some_check", "23514");
        assertSame(persistence, ConstraintViolationTranslator.translate(persistence));

        RuntimeException plain = new IllegalStateException("x", violation("some_check", "23514").getCause());
        PersistenceException wrapped = assertInstanceOf(PersistenceException.class, ConstraintViolationTranslator.translate(plain));
        assertSame(plain, wrapped.getCause());
    }

    @Test
    void anythingElse_isReturnedUnchanged() {
        RuntimeException e = new IllegalArgumentException("not a database error", new RuntimeException("cause"));
        assertSame(e, ConstraintViolationTranslator.translate(e));
    }

    @Test
    void authoringException_withoutAField() {
        assertNull(new AuthoringException("message").field());
        assertEquals("message", new AuthoringException("message").getMessage());
    }
}
