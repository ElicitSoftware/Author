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

import com.elicitsoftware.test.PostgresTestResource;
import com.elicitsoftware.author.survey.TokenSamples;
import com.elicitsoftware.model.Question;
import com.elicitsoftware.model.Relationship;
import com.elicitsoftware.model.SectionsQuestion;
import com.elicitsoftware.model.StructuralElement;
import com.elicitsoftware.model.Survey;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC-019 A6: the umbrella's census sample is the worked example of a Repeat that reads a
 * multi-select -- one "Speakers of {<LANG>|this language}" section repeated once per language
 * selected, in place of a question per language. The sample is an export, so it reaches Author
 * the way a definition from a site does (UC-005): imported, then validated. This holds the sample
 * to both, to the preview the designer shows for the repeated section, and to the reporting
 * columns the site fills for it.
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class RepeatPerItemSampleTest {

    /** The umbrella's golden sample; Author sits inside the umbrella checkout (see scripts/sync-survey-schema.sh). */
    private static final Path SAMPLE = Path.of("..", "samples", "census-household-survey.elicit");

    @Inject
    SurveyDefinitionImporter importer;

    @Inject
    ExportValidation validation;

    @Inject
    com.elicitsoftware.author.survey.ReportingService reporting;

    @Test
    @TestTransaction
    void theCensusSampleImportsValidatesAndPreviewsItsFirstLanguage() throws IOException {
        ImportResult imported;
        try (var in = Files.newInputStream(SAMPLE)) {
            imported = importer.importFile(in);
        }
        assertFalse(imported.draftFormat(), "UC-005: the sample is an export, not a draft");
        Survey survey = Survey.findById(imported.surveyId());

        List<ValidationIssue> errors = validation.validate(survey).stream().filter(ValidationIssue::isError).toList();
        assertTrue(errors.isEmpty(), "UC-007: the sample does not validate: "
                + errors.stream().map(ValidationIssue::message).toList());

        // UC-019 A6: of the sample's Repeat rules, exactly one reads a multi-select, onto the
        // Speakers section's mount; the other two are count-driven.
        List<Relationship> repeats = Relationship.<Relationship>list("surveyId = ?1 and effectiveTo = ?2", survey.id, StructuralElement.SENTINEL)
                .stream().filter(r -> "REPEAT".equals(r.actionType.name)).toList();
        assertEquals(3, repeats.size(), "the census sample repeats a section per count, a question per count and a section per item");
        List<Relationship> perItem = repeats.stream()
                .filter(r -> "MULTI_SELECT".equals(upstreamQuestion(survey, r).questionType.name)).toList();
        assertEquals(1, perItem.size());
        Relationship repeat = perItem.get(0);
        assertTrue(repeat.downstreamSsId != null && repeat.downstreamSqId == null, "the target is the section's mount, not a question");
        assertEquals("FIELD_EXIST", repeat.operatorType.name, "BR-007: the operator that holds for a selection");
        assertEquals("LANG", repeat.token);

        // UC-019 BR-008: the preview shows one instance, titled with the first language of the list.
        TokenSamples.Values title = TokenSamples.load(survey.id).forMount(repeat.downstreamStepId, repeat.downstreamSsId, null);
        assertEquals("English", title.values().get("LANG"));

        // UC-024 BR-003: the dimensional-impact preview lists the two fixed columns the site fills
        // for the repeated section (Survey UC-008 BR-012).
        assertEquals(List.of("question_key", "item_key"),
                reporting.impact(survey.id).perItemColumns().stream().map(c -> c.column()).toList());
        assertEquals(List.of("dim_question", "dim_item"),
                reporting.impact(survey.id).perItemColumns().stream().map(c -> c.dimTable()).toList());
    }

    private static Question upstreamQuestion(Survey survey, Relationship rule) {
        SectionsQuestion upstream = SectionsQuestion.find("surveyId = ?1 and sectionsQuestionId = ?2 and effectiveTo = ?3",
                survey.id, rule.upstreamSqId, StructuralElement.SENTINEL).firstResult();
        return Question.find("surveyId = ?1 and questionId = ?2 and effectiveTo = ?3",
                survey.id, upstream.questionId, StructuralElement.SENTINEL).firstResult();
    }
}
