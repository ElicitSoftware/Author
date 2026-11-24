package com.elicitsoftware.author.definition;

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

import com.elicitsoftware.model.Question;
import com.elicitsoftware.model.Survey;
import com.elicitsoftware.test.PostgresTestResource;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC-005: the umbrella's keyless templates ({@code ../samples/*.template.elicit}) are the Family
 * History Survey and the census sample with every key cleared, for an author to build a survey
 * of their own on. Import definition is the only way one reaches Author, so this holds each to
 * it: the whole file imports, every key is minted (C-017), the result validates (UC-007), and a
 * second import, once the first survey is renamed, is a second survey related to the first by
 * nothing.
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class SampleTemplateImportTest {

    /** The umbrella's samples; Author sits inside the umbrella checkout (see scripts/sync-survey-schema.sh). */
    private static final Path SAMPLES = Path.of("..", "samples");

    @Inject
    SurveyDefinitionImporter importer;

    @Inject
    ExportValidation validation;

    @ParameterizedTest
    @ValueSource(strings = {"family-history-survey.template.elicit", "census-household-survey.template.elicit"})
    @TestTransaction
    void aKeylessTemplateImportsWholeWithMintedKeysAndValidates(String file) throws IOException {
        Path template = SAMPLES.resolve(file);
        assertTrue(Files.readString(template).lines().noneMatch(l -> l.matches(".*\\b[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}\\b.*")),
                "the template carries no key");

        ImportResult first = importTemplate(template);
        assertTrue(first.draftFormat(), "C-022: a template is a draft, so no site applies it before Author exports it");
        assertNotNull(first.surveyKey(), "C-017: the survey key is minted");
        headerCounts(template).forEach((table, n) -> assertEquals(n,
                first.imported().getOrDefault(table, 0) + first.skippedRetired().getOrDefault(table, 0),
                "records read for " + table));

        Survey survey = Survey.findById(first.surveyId());
        List<Question> questions = Question.list("surveyId", survey.id);
        assertEquals(questions.size(), questions.stream().map(q -> q.questionKey).distinct().count(),
                "C-017: every question has a minted key of its own");

        List<ValidationIssue> errors = validation.validate(survey).stream().filter(ValidationIssue::isError).toList();
        assertTrue(errors.isEmpty(), "UC-007: the template does not validate: "
                + errors.stream().map(ValidationIssue::message).toList());

        // The keyed file is refused a second time (UC-005 BR-001); the template is a new survey each
        // time, once the first has a name of its own: a survey's name is unique in a database.
        survey.name = survey.name + " of mine";
        survey.persistAndFlush();
        ImportResult second = importTemplate(template);
        assertNotEquals(first.surveyId(), second.surveyId());
        assertNotEquals(first.surveyKey(), second.surveyKey(), "two imports share no key");
    }

    private ImportResult importTemplate(Path template) throws IOException {
        try (var in = Files.newInputStream(template)) {
            return importer.importFile(in);
        }
    }

    /** The per-table record counts the template's own header declares. */
    private static Map<String, Integer> headerCounts(Path template) throws IOException {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String line : Files.readAllLines(template)) {
            if (!line.startsWith("# ") || !line.contains(": ")) {
                continue;
            }
            String key = line.substring(2, line.indexOf(':'));
            if (ElicitFormat.TABLES.contains(key)) {
                counts.put(key, Integer.parseInt(line.substring(line.indexOf(':') + 1).trim()));
            }
        }
        return counts;
    }
}
