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

import com.elicitsoftware.model.ActionType;
import com.elicitsoftware.model.OperatorType;
import com.elicitsoftware.model.QuestionType;
import com.elicitsoftware.model.Survey;
import com.elicitsoftware.test.PostgresTestResource;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Keeps {@code docs/ai/AUTHORING_FOR_AI.md} honest (UC-042, NFR-019).
 *
 * <p>The document tells an AI agent how to write a {@code .elicit} definition without access to
 * this source tree, so anything in it that restates the code can go stale silently and the only
 * symptom is a model producing files nobody can import. This test is the gate: the document's
 * field counts, table order, format markers, lookup ids and worked example are all checked
 * against the code and the database that define them, in the spirit of the umbrella's
 * {@code docs/manual/check-properties.sh}.</p>
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class AuthoringForAiDocTest {

    private static final Path DOC = Path.of("docs", "ai", "AUTHORING_FOR_AI.md");

    @Inject
    SurveyDefinitionImporter importer;

    @Inject
    ExportValidation validation;

    private static String doc() throws IOException {
        return Files.readString(DOC, StandardCharsets.UTF_8);
    }

    /** UC-042: the record layout table names every table, with the field count the reader enforces. */
    @Test
    void layoutTableMatchesRequiredFields() throws IOException {
        // | `table` | 20 | `layout` |
        Pattern row = Pattern.compile("^\\| `(\\w+)` \\| (\\d+) \\|", Pattern.MULTILINE);
        Matcher m = row.matcher(doc());
        List<String> documented = new ArrayList<>();
        while (m.find()) {
            String table = m.group(1);
            int fields = Integer.parseInt(m.group(2));
            Integer required = ElicitFormat.REQUIRED_FIELDS.get(table);
            assertEquals(required, fields,
                    "NFR-019: the document's field count for '" + table + "' disagrees with ElicitFormat.REQUIRED_FIELDS");
            documented.add(table);
        }
        assertTrue(documented.containsAll(ElicitFormat.TABLES),
                "NFR-019: the document's layout table is missing " + ElicitFormat.TABLES.stream()
                        .filter(t -> !documented.contains(t)).toList());
    }

    /** UC-042: records must be written in dependency order, so the document must list that order. */
    @Test
    void recordOrderMatchesElicitFormat() throws IOException {
        String text = doc();
        int previous = -1;
        for (String table : ElicitFormat.TABLES) {
            int at = text.indexOf("`" + table + "` →");
            if (at < 0) {
                at = text.indexOf("`" + table + "`\n");
            }
            assertTrue(at > previous,
                    "NFR-019: '" + table + "' does not appear in ElicitFormat.TABLES order in the record-order list");
            previous = at;
        }
    }

    /** UC-042: the draft marker is what keeps a generated file out of Admin, so state it exactly. */
    @Test
    void matchesTheFormatMarkers() throws IOException {
        String text = doc();
        assertTrue(text.contains(ElicitFormat.DRAFT_FORMAT_VERSION),
                "NFR-019: the document must name " + ElicitFormat.DRAFT_FORMAT_VERSION);
        assertFalse(ElicitFormat.DRAFT_FORMAT_VERSION.contains(ElicitFormat.FORMAT_VERSION),
                "UC-042: the draft marker must not contain the export marker, or Admin's substring check lets it in");
        assertTrue(text.contains(String.valueOf(ElicitFormat.MAX_FILE_BYTES / (1024 * 1024)) + " MB"),
                "NFR-019: the document's size cap disagrees with ElicitFormat.MAX_FILE_BYTES");
    }

    /**
     * UC-042: type, operator and action ids travel verbatim, so the tables must match the database.
     *
     * <p>A row satisfies this if the name appears anywhere in it, not only in the name cell, so a
     * document may carry a renamed type's former spelling in its notes and stay correct on
     * deployments either side of that migration.</p>
     */
    @Test
    void lookupIdsMatchTheDatabase() throws IOException {
        String text = doc();
        Map<Integer, String> questionTypes = idTable(text, "### Question types");
        Map<Integer, String> operators = idTable(text, "### Operators");
        Map<Integer, String> actions = idTable(text, "### Actions");

        QuestionType.<QuestionType>listAll().forEach(t -> assertRow(questionTypes, t.id, t.name, "question type"));
        OperatorType.<OperatorType>listAll().forEach(t -> assertRow(operators, t.id, t.name, "operator"));
        ActionType.<ActionType>listAll().forEach(t -> assertRow(actions, t.id, t.name, "action"));
    }

    private static void assertRow(Map<Integer, String> table, Integer id, String name, String what) {
        String row = table.get(id);
        assertTrue(row != null, "NFR-019: the document's id table has no row for " + what + " " + id
                + " ('" + name + "')");
        assertTrue(row.contains("`" + name + "`"),
                "NFR-019: " + what + " " + id + " is '" + name + "' in the database, but the document's row "
                        + "for that id says: " + row);
    }

    /** The {@code | id | ... |} rows of one id table, from its heading to the next heading. */
    private static Map<Integer, String> idTable(String text, String heading) {
        int from = text.indexOf(heading);
        assertTrue(from > 0, "NFR-019: the document no longer has a '" + heading + "' table");
        int to = text.indexOf("\n### ", from + heading.length());
        String block = text.substring(from, to < 0 ? text.length() : to);
        Map<Integer, String> rows = new LinkedHashMap<>();
        Matcher m = Pattern.compile("^\\| (\\d+) \\|(.*)$", Pattern.MULTILINE).matcher(block);
        while (m.find()) {
            rows.put(Integer.valueOf(m.group(1)), m.group(2));
        }
        return rows;
    }

    /**
     * UC-042: the worked example is the part a model copies, so it must import as a draft and pass
     * the same validation an author faces before export.
     */
    @Test
    @TestTransaction
    void workedExampleImportsAsADraftAndValidatesClean() throws IOException {
        String example = fencedBlockContaining(doc(), ElicitFormat.DRAFT_FORMAT_VERSION + "\n\nsurveys:");

        ImportResult imported = importer.importFile(
                new ByteArrayInputStream(example.getBytes(StandardCharsets.UTF_8)));

        assertTrue(imported.draftFormat(), "UC-042: the example carries the draft marker, so import must flag it");
        assertTrue(imported.totalImported() > 0, "UC-042: the example imported no records");

        Survey survey = Survey.findById(imported.surveyId());
        List<ValidationIssue> errors = validation.validate(survey).stream().filter(ValidationIssue::isError).toList();
        assertTrue(errors.isEmpty(), "UC-042: the document's worked example does not validate: "
                + errors.stream().map(ValidationIssue::message).toList());
    }

    /** UC-042: every record in the example carries its full field count, trailing separators included. */
    @Test
    void workedExampleRecordsCarryTheirFullFieldCount() throws IOException {
        String example = fencedBlockContaining(doc(), ElicitFormat.DRAFT_FORMAT_VERSION + "\n\nsurveys:");
        for (String line : example.split("\n")) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            int colon = line.indexOf(':');
            String table = line.substring(0, colon).trim();
            Integer required = ElicitFormat.REQUIRED_FIELDS.get(table);
            String[] fields = ElicitFormat.parseFields(line.substring(colon + 1).trim());
            assertTrue(fields.length >= required,
                    "UC-042: example '" + table + "' record has " + fields.length + " fields, needs " + required);
        }
    }

    private static String fencedBlockContaining(String text, String needle) {
        int at = text.indexOf(needle);
        assertTrue(at > 0, "NFR-019: the document no longer contains a worked example starting '" + needle + "'");
        int open = text.lastIndexOf("```", at);
        int start = text.indexOf('\n', open) + 1;
        int close = text.indexOf("```", start);
        return text.substring(start, close);
    }
}
