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

import com.elicitsoftware.author.i18n.Translations;
import com.elicitsoftware.model.Survey;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.elicitsoftware.author.definition.ElicitFormat.joinFields;

/**
 * Writes a survey as an {@code ELICIT_SURVEY_EXPORT_V1} file (UC-008): a port of Admin's
 * {@code SurveyDefinitionExportService} with the same header and column order. Differences:
 * retired rows of the eight Type 2 tables are included with their closing instant so that a site
 * applying the file closes them too (UC-008 BR-002), records no longer end with {@code is_draft}
 * (Survey V015), and validation runs first — errors abort the export.
 */
@ApplicationScoped
public class SurveyDefinitionExporter {

    private static final String T2_TAIL = "version, effective_from, effective_to, published_by, published_comment";
    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");

    @Inject
    EntityManager em;

    @Inject
    ExportValidation validation;

    @Transactional
    public ExportResult export(Survey survey) {
        List<ValidationIssue> issues = validation.validate(survey);
        List<String> errors = issues.stream().filter(ValidationIssue::isError).map(ValidationIssue::message).toList();
        if (!errors.isEmpty()) {
            throw new DefinitionException(Translations.get("error.export.hasErrors", String.valueOf(errors.size())), errors);
        }
        return write(survey, ElicitFormat.FORMAT_VERSION, "", issues.stream().filter(i -> !i.isError()).toList());
    }

    /**
     * UC-047 BR-006: the backup taken before a survey is deleted. Every record, as it stands and
     * unvalidated, under the draft marker, so Admin refuses it and only Author can import it back
     * (C-022). Records no export: the survey is not stamped.
     */
    @Transactional
    public ExportResult backup(Survey survey) {
        return write(survey, ElicitFormat.DRAFT_FORMAT_VERSION, "_backup", List.of());
    }

    private ExportResult write(Survey survey, String marker, String fileSuffix, List<ValidationIssue> warnings) {
        Integer surveyId = survey.id;
        Object[] s = rows("SELECT id, survey_key, name, display_order, title, description, initial_display_key, post_survey_url, "
                + "published_by, published_comment, base_language, content_languages "
                + "FROM survey.surveys WHERE id = :sid", surveyId).get(0);

        Map<String, List<Object[]>> tables = new LinkedHashMap<>();
        tables.put("select_groups", rows("SELECT select_group_id, select_group_key, name, description, data_type, " + T2_TAIL
                + " FROM survey.select_groups WHERE survey_id = :sid ORDER BY select_group_id, version", surveyId));
        tables.put("select_items", rows("SELECT select_item_id, select_item_key, select_group_id, display_text, display_order, coded_value, " + T2_TAIL
                + " FROM survey.select_items WHERE survey_id = :sid ORDER BY select_group_id, display_order, version", surveyId));
        tables.put("steps", rows("SELECT step_id, step_key, display_order, name, dimension_name, description, " + T2_TAIL
                + " FROM survey.steps WHERE survey_id = :sid ORDER BY display_order, version", surveyId));
        tables.put("sections", rows("SELECT section_id, section_key, display_order, name, dimension_name, description, " + T2_TAIL
                + " FROM survey.sections WHERE survey_id = :sid ORDER BY display_order, version", surveyId));
        tables.put("steps_sections", rows("SELECT steps_sections_id, steps_sections_key, step_id, step_display_order, section_id, section_display_order, display_key, " + T2_TAIL
                + " FROM survey.steps_sections WHERE survey_id = :sid ORDER BY display_key, version", surveyId));
        tables.put("questions", rows("SELECT question_id, question_key, type_id, text, short_text, tool_tip, required, min_value, max_value, "
                + "validation_text, select_group_id, mask, placeholder, default_value, variant, " + T2_TAIL
                + " FROM survey.questions WHERE survey_id = :sid ORDER BY question_id, version", surveyId));
        tables.put("sections_questions", rows("SELECT sections_question_id, sections_question_key, question_id, section_id, display_order, " + T2_TAIL
                + " FROM survey.sections_questions WHERE survey_id = :sid ORDER BY sections_question_id, version", surveyId));
        tables.put("relationships", rows("SELECT relationship_id, relationship_key, upstream_step_id, upstream_sq_id, downstream_step_id, downstream_ss_id, "
                + "downstream_sq_id, operator_id, action_id, description, token, reference_value, default_upstream_value, override_upstream_value, " + T2_TAIL
                + " FROM survey.relationships WHERE survey_id = :sid ORDER BY relationship_id, version", surveyId));
        tables.put("reports", rows("SELECT id, report_key, name, description, url, display_order FROM survey.reports WHERE survey_id = :sid ORDER BY display_order", surveyId));
        tables.put("post_survey_actions", rows("SELECT id, post_survey_action_key, name, description, url, execution_order, method, sends_external_id FROM survey.post_survey_actions WHERE survey_id = :sid ORDER BY execution_order", surveyId));
        tables.put("dimensions", rows("SELECT DISTINCT d.id, d.dimension_key, d.name FROM survey.dimensions d INNER JOIN survey.ontology o ON o.dimension = d.id WHERE o.survey_id = :sid ORDER BY d.id", surveyId));
        tables.put("ontology", rows("SELECT id, ontology_key, name, tag, dimension FROM survey.ontology WHERE survey_id = :sid ORDER BY id", surveyId));
        tables.put("metadata", rows("SELECT id, metadata_key, steps_sections_id, question_id, sections_question_id, ontology_id, value FROM survey.metadata WHERE survey_id = :sid ORDER BY id", surveyId));
        // UC-008 BR-006: every mark, by report then order, as Admin writes them (UC-049 BR-005).
        tables.put("report_items", rows("SELECT id, report_item_key, report_type, steps_sections_id, question_id, sections_question_id, label, display_order "
                + "FROM survey.report_items WHERE survey_id = :sid ORDER BY report_type, display_order, id", surveyId));
        // Last, because it references every other table's element keys. source_text is not
        // selected: it is the authoring-only snapshot behind the out-of-date diff (UC-043).
        tables.put("translations", rows("SELECT " + ElicitFormat.TRANSLATION_FIELDS + ", " + T2_TAIL
                + " FROM survey.translations WHERE survey_id = :sid "
                + "ORDER BY element_type, element_key, field, language, version", surveyId));

        OffsetDateTime exportedAt = OffsetDateTime.now();
        StringBuilder out = new StringBuilder();
        out.append("# ").append(marker).append('\n');
        out.append("# survey_id: ").append(surveyId).append('\n');
        out.append("# survey_key: ").append(s[1]).append('\n');
        out.append("# survey_name: ").append(headerValue(s[2])).append('\n');
        out.append("# surveys: 1\n");
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("surveys", 1);
        tables.forEach((name, list) -> {
            counts.put(name, list.size());
            out.append("# ").append(name).append(": ").append(list.size()).append('\n');
        });
        out.append("# generated: ").append(exportedAt).append('\n');
        out.append(ElicitFormat.REVISION_HEADER).append(' ').append(exportedAt).append("\n\n");
        out.append("surveys: ").append(joinFields(s)).append('\n');
        tables.forEach((name, list) -> list.forEach(row -> out.append(name).append(": ").append(joinFields(row)).append('\n')));

        String fileName = safeName(String.valueOf(s[2])) + "_" + FILE_STAMP.format(exportedAt) + fileSuffix + ElicitFormat.FILE_EXTENSION;
        return new ExportResult(fileName, out.toString().getBytes(StandardCharsets.UTF_8), exportedAt, counts, warnings);
    }

    private List<Object[]> rows(String sql, Integer surveyId) {
        List<?> raw = em.createNativeQuery(sql).setParameter("sid", surveyId).getResultList();
        List<Object[]> out = new ArrayList<>(raw.size());
        for (Object r : raw) {
            out.add(r instanceof Object[] cols ? cols : new Object[]{r});
        }
        return out;
    }

    static String safeName(String name) {
        String cleaned = name == null ? "survey" : name.replaceAll("[^A-Za-z0-9._-]+", "_").replaceAll("^_+|_+$", "");
        return cleaned.isEmpty() ? "survey" : cleaned;
    }

    /**
     * A value as a header comment carries it (UC-008 BR-004, C-015): raw, as Admin's exporter
     * writes it, but on one line -- a line break is written as the escape a field would use, so
     * every line after the header is a record this export wrote as one.
     */
    static String headerValue(Object value) {
        return value == null ? "" : value.toString().replace("\n", "\\n").replace("\r", "\\r");
    }
}
