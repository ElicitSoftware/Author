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

import com.elicitsoftware.author.survey.TokenSamples;
import com.elicitsoftware.model.Question;
import com.elicitsoftware.model.Relationship;
import com.elicitsoftware.model.SectionsQuestion;
import com.elicitsoftware.model.StructuralElement;
import com.elicitsoftware.model.Survey;
import com.elicitsoftware.test.PostgresTestResource;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC-019 A6 / UC-042: the football-satisfaction sample is the worked example of a Repeat that reads
 * a multi-select — one "game" section repeated once per game attended, in place of one step and one
 * Show rule per game. It was drafted outside Author, so it reaches the tool the way any AI draft
 * does: imported, then validated. This holds the sample to both, and to the preview the designer
 * shows for it.
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class RepeatPerItemSampleTest {

    private static final Path SAMPLE = Path.of("samples", "UMich2026FootballSatisfaction.elicit");

    @Inject
    SurveyDefinitionImporter importer;

    @Inject
    ExportValidation validation;

    @Inject
    com.elicitsoftware.author.survey.ReportingService reporting;

    @Test
    @TestTransaction
    void theFootballSampleImportsValidatesAndPreviewsItsFirstGame() throws IOException {
        ImportResult imported;
        try (var in = Files.newInputStream(SAMPLE)) {
            imported = importer.importFile(in);
        }
        assertTrue(imported.draftFormat(), "UC-042: the sample is a draft");
        Survey survey = Survey.findById(imported.surveyId());

        List<ValidationIssue> errors = validation.validate(survey).stream().filter(ValidationIssue::isError).toList();
        assertTrue(errors.isEmpty(), "UC-007: the sample does not validate: "
                + errors.stream().map(ValidationIssue::message).toList());

        // UC-019 A6: one Repeat rule, reading the multi-select, onto the section's mount.
        List<Relationship> repeats = Relationship.<Relationship>list("surveyId = ?1 and effectiveTo = ?2", survey.id, StructuralElement.SENTINEL)
                .stream().filter(r -> "REPEAT".equals(r.actionType.name)).toList();
        assertEquals(1, repeats.size());
        Relationship repeat = repeats.get(0);
        SectionsQuestion upstream = SectionsQuestion.find("surveyId = ?1 and sectionsQuestionId = ?2 and effectiveTo = ?3",
                survey.id, repeat.upstreamSqId, StructuralElement.SENTINEL).firstResult();
        Question games = Question.find("surveyId = ?1 and questionId = ?2 and effectiveTo = ?3",
                survey.id, upstream.questionId, StructuralElement.SENTINEL).firstResult();
        assertEquals("MULTI_SELECT", games.questionType.name);
        assertEquals("FIELD_EXIST", repeat.operatorType.name, "BR-007: the operator that holds for a selection");
        assertEquals("GAME", repeat.token);

        // UC-019 BR-008: the preview shows one instance, titled with the first game of the list.
        TokenSamples.Values title = TokenSamples.load(survey.id).forMount(repeat.downstreamStepId, repeat.downstreamSsId, null);
        assertEquals("Sep 5 - vs. Western Michigan (home)", title.values().get("GAME"));

        // UC-024 BR-003: the dimensional-impact preview lists the two fixed columns the site fills
        // for the repeated section (Survey UC-008 BR-012).
        assertEquals(List.of("question_key", "item_key"),
                reporting.impact(survey.id).perItemColumns().stream().map(c -> c.column()).toList());
        assertEquals(List.of("dim_question", "dim_item"),
                reporting.impact(survey.id).perItemColumns().stream().map(c -> c.dimTable()).toList());
    }
}
