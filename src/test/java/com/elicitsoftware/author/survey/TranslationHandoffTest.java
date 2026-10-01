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
import com.elicitsoftware.model.Survey;
import com.elicitsoftware.test.PostgresTestResource;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC-045 and UC-046: the file a translator fills in, and what comes back.
 * <p>
 * The round trip is the headline; the rejection table is the substance, because it is what stops a
 * plausible-looking file from quietly breaking a survey in one language.
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class TranslationHandoffTest {

    private static final String SPANISH = "es-419";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Inject
    TranslationHandoff handoff;

    @Inject
    TranslationService translations;

    @Inject
    SurveyService surveys;

    @Inject
    ElementService elements;

    private Survey survey() {
        Survey bean = new Survey();
        bean.name = "TH " + UUID.randomUUID();
        bean.title = "Handoff fixture";
        bean.contentLanguages = SPANISH;
        return surveys.create(bean);
    }

    private Question question(int surveyId, String text) {
        Question q = new Question();
        q.surveyId = surveyId;
        q.questionType = QuestionType.findById(8);
        q.text = text;
        q.shortText = "Q";
        q.required = false;
        return elements.create(q);
    }

    private JsonNode exported(Survey s, boolean onlyMissing) throws Exception {
        return MAPPER.readTree(new String(handoff.export(s, SPANISH, onlyMissing), StandardCharsets.UTF_8));
    }

    /** Fills every item's translation with {@code f} applied to its source. */
    private byte[] filled(JsonNode file, java.util.function.Function<String, String> f) throws Exception {
        ObjectNode root = (ObjectNode) file.deepCopy();
        for (JsonNode item : root.path("items")) {
            ((ObjectNode) item).put("translation", f.apply(item.path("source").asText()));
        }
        return MAPPER.writeValueAsBytes(root);
    }

    private TranslationHandoff.ImportResult importing(Survey s, byte[] bytes) {
        return handoff.importFile(s, new ByteArrayInputStream(bytes));
    }

    @Test
    @TestTransaction
    void theFileCarriesEverythingATranslatorNeeds() throws Exception {
        Survey s = survey();
        question(s.id, "Has {S1's mother|your mother} had cancer?");

        JsonNode file = exported(s, false);

        assertEquals(TranslationHandoff.FORMAT, file.path("format").asText());
        assertEquals(String.valueOf(s.surveyKey), file.path("survey_key").asText());
        assertEquals(SPANISH, file.path("language").asText());
        assertTrue(file.path("instructions").asText().contains("{phrase containing a <TOKEN>|default text}"),
                "the rules travel in the document, so an agent needs nothing else");

        JsonNode item = null;
        for (JsonNode candidate : file.path("items")) {
            if ("text".equals(candidate.path("field").asText())) {
                item = candidate;
            }
        }
        assertTrue(item != null);
        assertEquals("missing", item.path("status").asText());
        assertEquals(ContentHash.of(item.path("source").asText()), item.path("source_hash").asText());
        assertTrue(item.path("max_length").asInt() > 0);
        assertTrue(item.path("translation").asText().isEmpty(), "the one field the translator fills");
        // "tokens" lists the keys the survey's rules name; this fixture has no rules, so it is
        // empty and the placeholder-count check is what protects the substitution
        // (aTranslationThatDropsATokenIsRejected).
        assertEquals(0, item.path("tokens").size());
    }

    @Test
    @TestTransaction
    void aFilledFileImportsAndTheSurveyIsThenTranslated() throws Exception {
        Survey s = survey();
        question(s.id, "What is your name?");

        TranslationHandoff.ImportResult result = importing(s, filled(exported(s, false), src -> "ES: " + src));

        assertEquals(0, result.rejected(), () -> result.rejections().toString());
        assertTrue(result.imported() > 0);
        assertEquals(0, translations.counts(s.id, SPANISH).missing(), "nothing left untranslated");
        assertEquals(0, translations.counts(s.id, SPANISH).stale());

        // Re-exporting after the import is the proof-reading round.
        JsonNode again = exported(s, false);
        for (JsonNode item : again.path("items")) {
            assertEquals("translated", item.path("status").asText());
        }
    }

    @Test
    @TestTransaction
    void reimportingTheSameFileChangesNothing() throws Exception {
        Survey s = survey();
        question(s.id, "What is your name?");
        byte[] file = filled(exported(s, false), src -> "ES: " + src);
        importing(s, file);

        TranslationHandoff.ImportResult second = importing(s, file);

        assertEquals(0, second.imported());
        assertTrue(second.unchanged() > 0);
    }

    @Test
    @TestTransaction
    void anEmptyTranslationIsSkippedRatherThanStoredBlank() throws Exception {
        Survey s = survey();
        question(s.id, "What is your name?");

        TranslationHandoff.ImportResult result = importing(s, MAPPER.writeValueAsBytes(exported(s, false)));

        assertEquals(0, result.imported());
        assertTrue(result.skipped() > 0);
        assertEquals(0, translations.counts(s.id, SPANISH).translated());
    }

    @Test
    @TestTransaction
    void aTranslationThatDropsATokenIsRejected() throws Exception {
        Survey s = survey();
        question(s.id, "Has {S1's mother|your mother} had cancer?");

        TranslationHandoff.ImportResult result = importing(s,
                filled(exported(s, false), src -> src.contains("{") ? "¿Ha tenido cáncer?" : "ES: " + src));

        assertTrue(result.rejections().stream().anyMatch(r -> r.contains("placeholder")),
                () -> "expected the dropped placeholder to be rejected, got " + result.rejections());
        assertTrue(result.imported() > 0, "one broken item must not fail the rest of the file");
    }

    @Test
    @TestTransaction
    void anOverLongTranslationIsRejected() throws Exception {
        Survey s = survey();
        question(s.id, "Name?");

        TranslationHandoff.ImportResult result = importing(s, filled(exported(s, false), src -> "x".repeat(9000)));

        assertTrue(result.rejections().stream().anyMatch(r -> r.contains("over the")),
                () -> result.rejections().toString());
    }

    @Test
    @TestTransaction
    void unbalancedBracesAreRejected() throws Exception {
        Survey s = survey();
        question(s.id, "What is your name?");

        TranslationHandoff.ImportResult result = importing(s, filled(exported(s, false), src -> "¿{Cómo se llama?"));

        assertTrue(result.rejections().stream().anyMatch(r -> r.contains("braces")),
                () -> result.rejections().toString());
    }

    @Test
    @TestTransaction
    void aFileForAnotherSurveyIsRefusedOutright() throws Exception {
        Survey s = survey();
        question(s.id, "What is your name?");
        byte[] file = filled(exported(s, false), src -> "ES: " + src);
        Survey other = survey();

        assertThrows(AuthoringException.class, () -> importing(other, file));
    }

    @Test
    @TestTransaction
    void somethingThatIsNotAHandoffFileIsRefused() {
        Survey s = survey();
        assertThrows(AuthoringException.class,
                () -> importing(s, "{\"format\":\"SOMETHING_ELSE\"}".getBytes(StandardCharsets.UTF_8)));
        assertThrows(AuthoringException.class, () -> importing(s, "not json".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    @TestTransaction
    void onlyMissingLeavesOutWhatIsAlreadyTranslated() throws Exception {
        Survey s = survey();
        Question q = question(s.id, "What is your name?");
        translations.upsert(s.id, "questions", q.questionKey, "text", SPANISH, "¿Cómo se llama?");

        JsonNode file = exported(s, true);

        boolean carriesTheTranslatedOne = false;
        for (JsonNode item : file.path("items")) {
            if (item.path("element_key").asText().equals(String.valueOf(q.questionKey))
                    && "text".equals(item.path("field").asText())) {
                carriesTheTranslatedOne = true;
            }
        }
        assertFalse(carriesTheTranslatedOne, "a finished string is not sent out again");
        assertTrue(file.path("items").size() > 0, "the survey's own title is still missing");
    }
}
