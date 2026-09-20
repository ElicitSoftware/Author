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

import com.elicitsoftware.model.Question;
import com.elicitsoftware.model.StepsSections;
import com.elicitsoftware.model.Survey;
import com.elicitsoftware.test.PostgresTestResource;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** UC-005 Import Survey Definition. */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
public class SurveyDefinitionImporterTest {

    static final Path SAMPLE = Path.of("samples", "ElicitDesigner.elicit");

    @Inject
    SurveyDefinitionImporter importer;

    static InputStream sample() throws IOException {
        return Files.newInputStream(SAMPLE);
    }

    static InputStream of(String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }

    /** The per-table record counts the sample's own header declares. */
    public static java.util.Map<String, Integer> headerCounts() throws IOException {
        java.util.Map<String, Integer> counts = new java.util.LinkedHashMap<>();
        for (String line : Files.readAllLines(SAMPLE)) {
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

    @Test
    @TestTransaction
    void importsTheDesignerSampleKeepingKeysAndRebasingDisplayKeys() throws IOException {
        ImportResult r = importer.importFile(sample());

        assertEquals("b3a7c2e4-5d18-4f92-8a60-1c9e3f75d2b4", r.surveyKey().toString(), "UC-005 BR-001");
        java.util.Map<String, Integer> expected = headerCounts();
        assertEquals(1, expected.get("surveys"), "sample header declares one survey");
        expected.forEach((table, n) -> assertEquals(n, r.imported().get(table), "records imported for " + table));
        assertEquals(expected.values().stream().mapToInt(Integer::intValue).sum(), r.totalImported());
        assertTrue(r.imported().get("steps") > 0 && r.imported().get("questions") > 0, "sample carries structure");
        assertEquals(0, r.totalSkipped());
        assertNotNull(r.fileRevision());

        Survey s = Survey.findById(r.surveyId());
        assertEquals("ElicitDesigner", s.name);
        String prefix = String.format("%04d", s.id);
        assertTrue(s.initialDisplayKey.startsWith(prefix + "-"), "initial display key rebased: " + s.initialDisplayKey);
        StepsSections ss = StepsSections.find("surveyId", s.id).firstResult();
        assertTrue(ss.displayKey.startsWith(prefix + "-"), "display keys rebased: " + ss.displayKey);

        Question q = Question.find("surveyId = ?1", s.id).firstResult();
        assertNotNull(q.questionKey, "element key carried from the file");
        assertEquals(0, q.version);
        assertTrue(q.isCurrent());
    }

    @Test
    @TestTransaction
    void secondImportOfTheSameSurveyKeyIsRejected() throws IOException {
        importer.importFile(sample());
        DefinitionException ex = assertThrows(DefinitionException.class, () -> importer.importFile(sample()));
        assertTrue(ex.getMessage().contains("already open in Author"), ex.getMessage());
    }

    @Test
    @TestTransaction
    void retiredRowsAreSkippedAndCounted() {
        UUID key = UUID.randomUUID();
        String file = "# ELICIT_SURVEY_EXPORT_V1\n# survey_key: " + key + "\n\n"
                + "surveys: 1|" + key + "|RetireTest|1|Retire Test|||||\n"
                + "steps: 1|" + UUID.randomUUID() + "|1|Kept|Kept||0|1970-01-01 00:00:00+00|9999-12-31 23:59:59+00||\n"
                + "steps: 2|" + UUID.randomUUID() + "|2|Gone|Gone||0|1970-01-01 00:00:00+00|2026-09-17 10:00:00+00||\n";
        ImportResult r = importer.importFile(of(file));
        assertEquals(1, r.imported().get("steps"));
        assertEquals(1, r.skippedRetired().get("steps"));
    }

    @Test
    @TestTransaction
    void preV015FilesWithTrailingIsDraftStillImport() {
        UUID key = UUID.randomUUID();
        String file = "# ELICIT_SURVEY_EXPORT_V1\n# survey_key: " + key + "\n\n"
                + "surveys: 1|" + key + "|OldLayout|1|Old Layout|||||\n"
                + "steps: 1|" + UUID.randomUUID() + "|1|Step|Step||0|||||false\n";
        assertEquals(1, importer.importFile(of(file)).imported().get("steps"));
    }

    @Test
    void notADefinitionFileIsRejected() {
        DefinitionException ex = assertThrows(DefinitionException.class, () -> importer.importFile(of("hello|world\n")));
        assertTrue(ex.getMessage().contains("not a survey definition export"));
    }

    @Test
    void danglingReferenceRollsBackEverything() {
        UUID key = UUID.randomUUID();
        String file = "# ELICIT_SURVEY_EXPORT_V1\n# survey_key: " + key + "\n\n"
                + "surveys: 1|" + key + "|Dangling|1|Dangling|||||\n"
                + "select_items: 1|" + UUID.randomUUID() + "|99|Orphan|1|X|0|||\n";
        DefinitionException ex = assertThrows(DefinitionException.class, () -> importer.importFile(of(file)));
        assertTrue(ex.getMessage().startsWith("Line "), ex.getMessage());
        assertEquals(0, Survey.count("surveyKey", key), "UC-005 BR-004: nothing left behind");
    }

    @Test
    void malformedRevisionHeaderIsRejected() {
        DefinitionException ex = assertThrows(DefinitionException.class,
                () -> importer.importFile(of("# ELICIT_SURVEY_EXPORT_V1\n# survey_revision: yesterday\n")));
        assertTrue(ex.getMessage().contains("survey_revision"));
    }
}
