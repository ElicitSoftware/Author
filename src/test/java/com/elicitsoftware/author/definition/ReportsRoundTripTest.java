/*-
 * ***LICENSE_START***
 * Elicit Author
 * %%
 * Copyright (C) 2025 - 2026 The Regents of the University of Michigan - Rogel Cancer Center
 * %%
 * PolyForm Noncommercial License 1.0.0
 * <https://polyformproject.org/licenses/noncommercial/1.0.0>
 * ***LICENSE_END***
 */
package com.elicitsoftware.author.definition;

import com.elicitsoftware.author.survey.ReportMarkService;
import com.elicitsoftware.author.survey.ReportMarkService.Mark;
import com.elicitsoftware.author.survey.ReportService;
import com.elicitsoftware.author.survey.StructureNode;
import com.elicitsoftware.author.survey.SurveyStructureQuery;
import com.elicitsoftware.model.Metadata.ElementRef;
import com.elicitsoftware.model.Report;
import com.elicitsoftware.model.ReportItem;
import com.elicitsoftware.model.ReportItem.ReportType;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** UC-008 BR-006 and UC-005 step 4: report services and report marks travel in the file. */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class ReportsRoundTripTest {

    @Inject
    SurveyDefinitionImporter importer;

    @Inject
    SurveyDefinitionExporter exporter;

    @Inject
    ReportService reports;

    @Inject
    ReportMarkService marks;

    @Inject
    com.elicitsoftware.author.survey.PostSurveyActionService actions;

    @Inject
    SurveyStructureQuery structure;

    private static String sample() throws IOException {
        return Files.readString(Path.of("samples", "ElicitDesigner.elicit"));
    }

    /** A file rewritten with fresh element keys and a fresh survey name, so it imports beside its source. */
    record Remapped(String text, Map<UUID, UUID> keys) {
    }

    private static final Pattern UUID_PATTERN = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    private static final Pattern NAME_HEADER = Pattern.compile("(?m)^# survey_name: (.*)$");

    private static Remapped remap(String text) {
        Map<UUID, UUID> keys = new HashMap<>();
        Matcher m = UUID_PATTERN.matcher(text);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            UUID fresh = keys.computeIfAbsent(UUID.fromString(m.group()), k -> UUID.randomUUID());
            m.appendReplacement(out, fresh.toString());
        }
        m.appendTail(out);
        String rewritten = out.toString();
        Matcher name = NAME_HEADER.matcher(rewritten);
        if (name.find()) {
            String oldName = name.group(1);
            String newName = "ElicitDesigner " + UUID.randomUUID().toString().substring(0, 8);
            rewritten = rewritten.replace("# survey_name: " + oldName, "# survey_name: " + newName)
                    .replace("|" + oldName + "|", "|" + newName + "|");
        }
        return new Remapped(rewritten, keys);
    }

    private static String withSurveyKey(String text, UUID ignored) {
        return remap(text).text();
    }

    private static ImportResult importText(SurveyDefinitionImporter importer, String text) {
        return importer.importFile(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    @TestTransaction
    void reportsAndMarksAreWrittenAndReadBackWithTheirKeys() throws IOException {
        ImportResult first = importText(importer, withSurveyKey(sample(), UUID.randomUUID()));
        int sid = first.surveyId();
        assertTrue(reports.reports(sid).isEmpty(), "UC-048 BR-001: a definition import seeds nothing");
        assertTrue(marks.marks(sid).isEmpty());

        Report builtIn = reports.seedBuiltIn(sid);
        Report custom = reports.add(sid, "Pedigree", "The family tree.", "https://example.org/pedigree/report");
        List<StructureNode> all = structure.tree(sid, false).all();
        StructureNode question = all.stream().filter(n -> n.kind() == StructureNode.Kind.QUESTION
                && ReportMarkService.markable(n)).findFirst().orElseThrow();
        StructureNode section = all.stream().filter(n -> n.kind() == StructureNode.Kind.SECTION).findFirst().orElseThrow();
        ReportItem m1 = marks.mark(sid, ElementRef.QUESTION, question.durableId(), ReportType.RESPONDENT, "Your answer");
        ReportItem m2 = marks.mark(sid, ElementRef.STEPS_SECTIONS, section.assignmentId(), ReportType.ADMINISTRATOR, null);
        ReportItem m3 = marks.mark(sid, ElementRef.SECTIONS_QUESTION, question.assignmentId(), ReportType.ADMINISTRATOR, "Placed");

        String exported = new String(exporter.export(Survey.findById(sid)).bytes(), StandardCharsets.UTF_8);
        assertTrue(exported.contains("\n# reports: 2\n"), "UC-008: header counts the rows");
        assertTrue(exported.contains("\n# metadata: 0\n# report_items: 3\n# translations:"), "report_items sits after metadata, before translations");
        assertTrue(exported.contains("\nreports: " + builtIn.id + "|" + builtIn.reportKey + "|" + ReportService.BUILT_IN_RESPONDENT_NAME + "|"
                + ReportService.BUILT_IN_RESPONDENT_DESCRIPTION + "|" + ReportService.BUILT_IN_RESPONDENT_PATH + "|1\n"), exported);
        assertTrue(exported.contains("\nreport_items: " + m1.id + "|" + m1.reportItemKey + "|RESPONDENT||" + question.durableId() + "||Your answer|1\n"), exported);
        assertTrue(exported.contains("\nreport_items: " + m2.id + "|" + m2.reportItemKey + "|ADMINISTRATOR|" + section.assignmentId() + "|||" + "|1\n"), exported);
        assertTrue(exported.contains("\nreport_items: " + m3.id + "|" + m3.reportItemKey + "|ADMINISTRATOR|||" + question.assignmentId() + "|Placed|2\n"), exported);
        int builtInLine = exported.indexOf("\nreports: " + builtIn.id + "|");
        int customLine = exported.indexOf("\nreports: " + custom.id + "|");
        assertTrue(builtInLine < customLine, "BR-006: reports in display order");

        // UC-005 step 4: the same file under a new survey key reads back with the same keys.
        Remapped again = remap(exported);
        ImportResult second = importText(importer, again.text());
        assertEquals(2, second.imported().get("reports"));
        assertEquals(3, second.imported().get("report_items"));
        List<Report> readBack = reports.reports(second.surveyId());
        assertEquals(List.of(again.keys().get(builtIn.reportKey), again.keys().get(custom.reportKey)),
                readBack.stream().map(r -> r.reportKey).toList(), "keys verbatim, in order");
        assertTrue(ReportService.isBuiltIn(readBack.get(0)));
        // Ordered by report type as the file is: ADMINISTRATOR (m2, m3) before RESPONDENT (m1).
        List<Mark> readMarks = marks.marks(second.surveyId());
        assertEquals(3, readMarks.size());
        Mark r1 = readMarks.get(2);
        assertEquals(again.keys().get(m1.reportItemKey), r1.item().reportItemKey);
        assertEquals("Your answer", r1.item().label);
        assertEquals(question.label(), r1.elementLabel(), "the element reference was remapped to the new survey");
        assertFalse(r1.dangling());
        assertEquals(ElementRef.STEPS_SECTIONS, readMarks.get(0).ref());
        assertEquals(again.keys().get(m2.reportItemKey), readMarks.get(0).item().reportItemKey);
        assertEquals(section.label(), readMarks.get(0).elementLabel());
        assertEquals(ElementRef.SECTIONS_QUESTION, readMarks.get(1).ref());
        assertEquals("Placed", readMarks.get(1).item().label);
        assertEquals(List.of(1, 2, 1), readMarks.stream().map(m -> m.item().displayOrder).toList());
    }

    @Test
    @TestTransaction
    void aFileWithoutReportItemsImportsWithNoMarks_andADraftIsSeeded() throws IOException {
        ImportResult plain = importText(importer, withSurveyKey(sample(), UUID.randomUUID()));
        assertTrue(marks.marks(plain.surveyId()).isEmpty(), "UC-005 step 4: no report_items records, no marks");
        assertEquals(0, plain.imported().getOrDefault("report_items", 0));

        // UC-042 / UC-048 BR-001: a draft omits the reports table and no count lines, so it starts
        // with the built-in report like a survey made from the blank form.
        String draft = withSurveyKey(sample(), UUID.randomUUID())
                .replace("# " + ElicitFormat.FORMAT_VERSION, "# " + ElicitFormat.DRAFT_FORMAT_VERSION)
                .replaceAll("(?m)^# (reports|post_survey_actions|dimensions|ontology|metadata): \\d+\\n", "");
        ImportResult drafted = importText(importer, draft);
        assertTrue(drafted.draftFormat());
        List<Report> seeded = reports.reports(drafted.surveyId());
        assertEquals(1, seeded.size());
        assertTrue(ReportService.isBuiltIn(seeded.get(0)));
        assertEquals(1, drafted.imported().get("reports"), "the seeded row is counted");

        // UC-047: a backup counts its tables, so restoring one with no reports seeds nothing.
        String backup = withSurveyKey(sample(), UUID.randomUUID())
                .replace("# " + ElicitFormat.FORMAT_VERSION, "# " + ElicitFormat.DRAFT_FORMAT_VERSION);
        ImportResult restored = importText(importer, backup);
        assertTrue(reports.reports(restored.surveyId()).isEmpty());
    }

    /** UC-052 with UC-008 and UC-005: actions written from the page export in order and read back with their keys. */
    @Test
    @TestTransaction
    void postSurveyActionsAreWrittenAndReadBackWithTheirKeys() throws IOException {
        int sid = importText(importer, withSurveyKey(sample(), UUID.randomUUID())).surveyId();
        com.elicitsoftware.model.PostSurveyAction second = actions.add(sid, "Notify", "Tells the coordinator.", "https://example.org/notify");
        com.elicitsoftware.model.PostSurveyAction first = actions.add(sid, "File", "Files the PDF.", "https://example.org/file");
        actions.reorder(sid, List.of(first.id));

        String exported = new String(exporter.export(Survey.findById(sid)).bytes(), StandardCharsets.UTF_8);
        assertTrue(exported.contains("\n# post_survey_actions: 2\n"), "UC-008: header counts the actions");
        int fileLine = exported.indexOf("\npost_survey_actions: " + first.id + "|" + first.postSurveyActionKey + "|File|Files the PDF.|https://example.org/file|1\n");
        int notifyLine = exported.indexOf("\npost_survey_actions: " + second.id + "|" + second.postSurveyActionKey + "|Notify|");
        assertTrue(fileLine >= 0 && notifyLine > fileLine, "UC-052 BR-005: in execution order: " + exported);

        Remapped again = remap(exported);
        ImportResult back = importText(importer, again.text());
        assertEquals(2, back.imported().get("post_survey_actions"));
        assertEquals(List.of(again.keys().get(first.postSurveyActionKey), again.keys().get(second.postSurveyActionKey)),
                actions.actions(back.surveyId()).stream().map(a -> a.postSurveyActionKey).toList(), "BR-007: keys verbatim, in order");
    }
}
