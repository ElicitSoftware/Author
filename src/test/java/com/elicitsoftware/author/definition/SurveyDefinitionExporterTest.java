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

import com.elicitsoftware.model.Survey;
import com.elicitsoftware.test.PostgresTestResource;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** UC-008 Export Survey Definition, NFR-011 round trip. */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class SurveyDefinitionExporterTest {

    @Inject
    SurveyDefinitionImporter importer;

    @Inject
    SurveyDefinitionExporter exporter;

    @Inject
    EntityManager em;

    /** Records keyed by table + element key, with local ids and display-key prefixes neutralised. */
    static Map<String, String> normalised(String text) {
        Map<String, String> out = new TreeMap<>();
        for (String line : text.split("\n")) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            int colon = line.indexOf(':');
            String table = line.substring(0, colon);
            String[] f = ElicitFormat.parseFields(line.substring(colon + 1).trim());
            List<String> keep = new ArrayList<>();
            for (int i = 0; i < f.length; i++) {
                keep.add(f[i]);
            }
            keep.set(0, "*");                                           // local source id
            switch (table) {
                case "surveys" -> { keep.set(3, "*"); keep.set(6, prefixless(f[6])); keep.set(8, "*"); keep.set(9, "*"); }
                case "select_groups" -> { if (f[4].isEmpty()) keep.set(4, "Text"); }          // import default
                case "select_items" -> keep.set(2, "*");
                case "steps", "sections" -> { if (f[4].isEmpty()) keep.set(4, f[3]); }       // dimension_name defaults to name
                case "steps_sections" -> { keep.set(2, "*"); keep.set(4, "*"); keep.set(6, prefixless(f[6])); }
                case "questions" -> keep.set(10, "*");
                case "sections_questions" -> { keep.set(2, "*"); keep.set(3, "*"); }
                case "relationships" -> { for (int i = 2; i <= 6; i++) keep.set(i, "*"); }
                default -> { }
            }
            // Type 2 tail: version/effective range are DB-side; compare content only.
            if (ElicitFormat.VERSIONED_TABLES.contains(table)) {
                int tail = ElicitFormat.REQUIRED_FIELDS.get(table) - 5;
                while (keep.size() > tail) {
                    keep.remove(keep.size() - 1);
                }
            }
            out.put(table + ":" + f[1], String.join("|", keep));
        }
        return out;
    }

    static String prefixless(String displayKey) {
        int i = displayKey.indexOf('-');
        return i < 0 ? displayKey : "*" + displayKey.substring(i);
    }

    @Test
    @TestTransaction
    void roundTripOfTheDesignerSampleIsFieldForFieldEqual() throws IOException {
        String original = Files.readString(Path.of("samples", "ElicitDesigner.elicit"));
        ImportResult imported = importer.importFile(new java.io.ByteArrayInputStream(original.getBytes(StandardCharsets.UTF_8)));
        Survey survey = Survey.findById(imported.surveyId());

        ExportResult result = exporter.export(survey);
        String exported = new String(result.bytes(), StandardCharsets.UTF_8);

        assertTrue(exported.startsWith("# " + ElicitFormat.FORMAT_VERSION + "\n"));
        assertTrue(exported.contains("# survey_key: b3a7c2e4-5d18-4f92-8a60-1c9e3f75d2b4\n"));
        assertTrue(exported.contains(ElicitFormat.REVISION_HEADER + " " + result.revision()));
        assertEquals(SurveyDefinitionImporterTest.headerCounts().values().stream().mapToInt(Integer::intValue).sum(),
                result.counts().values().stream().mapToInt(Integer::intValue).sum(), "every record written back");
        Map<String, String> expectedRows = normalised(original);
        Map<String, String> actualRows = normalised(exported);
        List<String> differences = new ArrayList<>();
        expectedRows.forEach((key, value) -> {
            String actual = actualRows.get(key);
            if (!value.equals(actual)) {
                differences.add(key + "\n   file: " + value + "\n export: " + actual);
            }
        });
        actualRows.keySet().stream().filter(k -> !expectedRows.containsKey(k)).forEach(k -> differences.add("unexpected " + k));
        assertTrue(differences.isEmpty(), () -> "NFR-011 round trip, " + differences.size() + " difference(s):\n"
                + String.join("\n", differences.subList(0, Math.min(8, differences.size()))));
        assertTrue(result.fileName().startsWith("ElicitDesigner_") && result.fileName().endsWith(".elicit"));
        assertFalse(exported.contains("|false\n"), "no trailing is_draft field after V015");
    }

    @Test
    @TestTransaction
    void retiredRowsTravelWithTheFile() {
        Survey fixture = Survey.find("name", "AuthorFixture").firstResult();
        em.createNativeQuery("UPDATE survey.questions SET effective_to = now() WHERE survey_id = ?1 AND short_text = 'FixtureQ5'")
                .setParameter(1, fixture.id).executeUpdate();
        em.createNativeQuery("UPDATE survey.sections_questions sq SET effective_to = now() FROM survey.questions q "
                        + "WHERE sq.question_id = q.question_id AND q.short_text = 'FixtureQ5' AND sq.survey_id = ?1")
                .setParameter(1, fixture.id).executeUpdate();
        em.flush();
        em.clear();

        String exported = new String(exporter.export(Survey.findById(fixture.id)).bytes(), StandardCharsets.UTF_8);
        String q5 = exported.lines().filter(l -> l.startsWith("questions:") && l.contains("FixtureQ5")).findFirst().orElseThrow();
        String[] f = ElicitFormat.parseFields(q5.substring("questions:".length()).trim());
        assertTrue(ElicitFormat.isRetired("questions", f), "UC-008 BR-002: retired row written with its close instant");
    }

    @Test
    @TestTransaction
    void validationErrorsBlockTheExport() {
        Survey fixture = Survey.find("name", "AuthorFixture").firstResult();
        // Retire the select group the RADIO question uses: dangling reference => error.
        em.createNativeQuery("UPDATE survey.select_groups SET effective_to = now() WHERE survey_id = ?1")
                .setParameter(1, fixture.id).executeUpdate();
        em.flush();
        em.clear();
        DefinitionException ex = assertThrows(DefinitionException.class, () -> exporter.export(Survey.findById(fixture.id)));
        assertFalse(ex.details().isEmpty());
    }
}
