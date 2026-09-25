package com.elicitsoftware.author.survey;

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

import com.elicitsoftware.author.AuthoringException;
import com.elicitsoftware.model.Question;
import com.elicitsoftware.model.QuestionType;
import com.elicitsoftware.model.StructuralElement;
import com.elicitsoftware.model.Survey;
import com.elicitsoftware.model.Translation;
import com.elicitsoftware.test.PostgresTestResource;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC-043: writing and reading a survey's translations.
 * <p>
 * The rules that are easy to get wrong and expensive when they are: a write hashes the base text as
 * it stands now, editing base text leaves the translation current but out of date, and removal
 * closes the row rather than emptying it.
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class TranslationServiceTest {

    private static final String SPANISH = "es-419";

    @Inject
    TranslationService service;

    @Inject
    SurveyService surveys;

    @Inject
    ElementService elements;

    @Inject
    EntityManager em;

    private Survey survey(String contentLanguages) {
        Survey bean = new Survey();
        bean.name = "TS " + UUID.randomUUID();
        bean.title = "Translation service fixture";
        bean.contentLanguages = contentLanguages;
        return surveys.create(bean);
    }

    /** Through ElementService, so the durable id and the element key are allocated as in production. */
    private Question question(int surveyId, String text) {
        Question q = new Question();
        q.surveyId = surveyId;
        q.questionType = QuestionType.findById(8);
        q.text = text;
        q.shortText = "Q";
        q.required = false;
        return elements.create(q);
    }

    @Test
    @TestTransaction
    void everyTranslatableStringOfTheSurveyIsListed() {
        Survey s = survey(SPANISH);
        question(s.id, "What is your name?");

        List<TranslationService.Row> rows = service.rows(s.id, SPANISH);

        assertTrue(rows.stream().anyMatch(r -> "surveys".equals(r.elementType()) && "title".equals(r.field())));
        assertTrue(rows.stream().anyMatch(r -> "questions".equals(r.elementType()) && "text".equals(r.field())));
        assertTrue(rows.stream().allMatch(r -> r.status() == TranslationService.Status.MISSING),
                "nothing is translated yet");
        assertTrue(rows.stream().noneMatch(r -> r.baseText() == null || r.baseText().isBlank()),
                "a string with no base text is nothing to translate");
    }

    @Test
    @TestTransaction
    void upsertStoresTheValueWithTheHashOfTheCurrentBaseText() {
        Survey s = survey(SPANISH);
        Question q = question(s.id, "What is your name?");

        Translation t = service.upsert(s.id, "questions", q.questionKey, "text", SPANISH, "¿Cómo se llama?");

        assertNotNull(t);
        assertEquals(ContentHash.of("What is your name?"), t.sourceHash);
        assertEquals("What is your name?", t.sourceText, "the snapshot behind the out-of-date diff");
        assertEquals(0, t.version, "Author holds working copies; the console makes the versions");
        assertTrue(t.isCurrent());
        assertEquals(TranslationService.Status.TRANSLATED, statusOf(s.id, q.questionKey, "text"));
    }

    @Test
    @TestTransaction
    void editingTheBaseTextLeavesTheTranslationCurrentButOutOfDate() {
        // The distinction the whole design turns on: retirement is structural and applies to every
        // language; a wording change is a translator's problem and must not touch the row.
        Survey s = survey(SPANISH);
        Question q = question(s.id, "What is your name?");
        service.upsert(s.id, "questions", q.questionKey, "text", SPANISH, "¿Cómo se llama?");

        q.text = "What name do you go by?";
        em.flush();

        assertEquals(TranslationService.Status.STALE, statusOf(s.id, q.questionKey, "text"));
        Translation t = Translation.find("surveyId = ?1 and targetKey = ?2", s.id, q.questionKey).firstResult();
        assertTrue(t.isCurrent(), "still current, just no longer faithful");
        assertEquals("¿Cómo se llama?", t.value);
    }

    @Test
    @TestTransaction
    void retranslatingAfterAnEditMakesItCurrentAgain() {
        Survey s = survey(SPANISH);
        Question q = question(s.id, "What is your name?");
        service.upsert(s.id, "questions", q.questionKey, "text", SPANISH, "¿Cómo se llama?");
        q.text = "What name do you go by?";
        em.flush();

        service.upsert(s.id, "questions", q.questionKey, "text", SPANISH, "¿Qué nombre usa?");

        assertEquals(TranslationService.Status.TRANSLATED, statusOf(s.id, q.questionKey, "text"));
    }

    @Test
    @TestTransaction
    void aBlankValueClosesTheRowRatherThanEmptyingIt() {
        Survey s = survey(SPANISH);
        Question q = question(s.id, "What is your name?");
        service.upsert(s.id, "questions", q.questionKey, "text", SPANISH, "¿Cómo se llama?");

        assertNull(service.upsert(s.id, "questions", q.questionKey, "text", SPANISH, "  "));

        assertEquals(TranslationService.Status.MISSING, statusOf(s.id, q.questionKey, "text"));
        long rows = Translation.count("surveyId = ?1 and targetKey = ?2", s.id, q.questionKey);
        assertEquals(1, rows, "closed, not deleted: a site must tell 'no translation' from 'translated to nothing'");
    }

    @Test
    @TestTransaction
    void aFieldThatCarriesNoRespondentTextIsRefused() {
        Survey s = survey(SPANISH);
        Question q = question(s.id, "What is your name?");

        assertThrows(AuthoringException.class,
                () -> service.upsert(s.id, "questions", q.questionKey, "default_value", SPANISH, "Sí"));
    }

    @Test
    @TestTransaction
    void countsSummariseTheLanguage() {
        Survey s = survey(SPANISH);
        Question q = question(s.id, "What is your name?");
        service.upsert(s.id, "questions", q.questionKey, "text", SPANISH, "¿Cómo se llama?");

        TranslationService.Counts counts = service.counts(s.id, SPANISH);

        assertEquals(1, counts.translated());
        assertTrue(counts.missing() > 0, "the survey's own title is still untranslated");
        assertEquals(0, counts.stale());
        assertEquals(counts.total(), service.rows(s.id, SPANISH).size());
    }

    @Test
    @TestTransaction
    void retireAndRestoreCarryTheTranslationsWithTheElement() {
        Survey s = survey(SPANISH);
        Question q = question(s.id, "What is your name?");
        service.upsert(s.id, "questions", q.questionKey, "text", SPANISH, "¿Cómo se llama?");

        java.time.OffsetDateTime at = java.time.OffsetDateTime.now();
        assertEquals(1, service.retireTranslationsOf(s.id, q.questionKey, at));
        assertEquals(0, Translation.count("surveyId = ?1 and targetKey = ?2 and effectiveTo = ?3",
                s.id, q.questionKey, StructuralElement.SENTINEL));

        assertEquals(1, service.restoreTranslationsOf(s.id, q.questionKey, at));
        assertEquals(1, Translation.count("surveyId = ?1 and targetKey = ?2 and effectiveTo = ?3",
                s.id, q.questionKey, StructuralElement.SENTINEL));
    }

    @Test
    @TestTransaction
    void thePublishedSetIsParsedAndOrdered() {
        Survey s = survey(" ar , es-419 ,, ");
        assertEquals(List.of("ar", "es-419"), service.contentLanguages(s));
        assertEquals(List.of(), service.contentLanguages(survey(null)));
    }

    private TranslationService.Status statusOf(int surveyId, UUID key, String field) {
        return service.rows(surveyId, SPANISH).stream()
                .filter(r -> r.targetKey().equals(key) && r.field().equals(field))
                .findFirst().orElseThrow().status();
    }
}
