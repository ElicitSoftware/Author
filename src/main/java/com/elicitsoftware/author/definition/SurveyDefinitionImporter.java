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

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static com.elicitsoftware.author.definition.ElicitFormat.nullIfEmpty;
import static com.elicitsoftware.author.definition.ElicitFormat.parseBooleanOrFalse;
import static com.elicitsoftware.author.definition.ElicitFormat.parseDecimalOrNull;
import static com.elicitsoftware.author.definition.ElicitFormat.parseIntOrNull;
import static com.elicitsoftware.author.definition.ElicitFormat.parseKeyOrMint;
import static com.elicitsoftware.author.definition.ElicitFormat.parseLongOrNull;
import static com.elicitsoftware.author.definition.ElicitFormat.rebaseDisplayKey;
import static com.elicitsoftware.author.definition.ElicitFormat.requireFields;
import static com.elicitsoftware.author.definition.ElicitFormat.resolveNullable;
import static com.elicitsoftware.author.definition.ElicitFormat.resolveRequired;

/**
 * Loads an {@code ELICIT_SURVEY_EXPORT_V1} file into Author as a new working copy (UC-005).
 * A port of Admin's {@code SurveyDefinitionImportService}: same record grammar, same
 * source-id remapping, same key preservation and display-key rebasing. Differences: no
 * {@code survey_log}, retired rows are skipped and counted, a survey already present in Author
 * is rejected (UC-005 BR-002), and {@code published_by}/{@code published_comment} on the survey
 * are carried over as its last export attribution.
 *
 * <p>All-or-nothing: any record error rolls the whole import back (UC-005 BR-004).</p>
 */
@ApplicationScoped
public class SurveyDefinitionImporter {

    @Inject
    EntityManager em;

    @Transactional
    public ImportResult importFile(InputStream inputStream) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        Map<String, Integer> skipped = new LinkedHashMap<>();
        ElicitFormat.TABLES.forEach(t -> counts.put(t, 0));
        ElicitFormat.VERSIONED_TABLES.forEach(t -> skipped.put(t, 0));

        Map<Long, Long> selectGroupIdMap = new HashMap<>();
        Map<Long, Long> stepIdMap = new HashMap<>();
        Map<Long, Long> sectionIdMap = new HashMap<>();
        Map<Long, Long> stepsSectionIdMap = new HashMap<>();
        Map<Long, Long> questionIdMap = new HashMap<>();
        Map<Long, Long> sectionsQuestionIdMap = new HashMap<>();
        Map<Long, Long> dimensionIdMap = new HashMap<>();
        Map<Long, Long> ontologyIdMap = new HashMap<>();

        Long surveyId = null;
        UUID surveyKey = null;
        String surveyName = null;
        OffsetDateTime fileRevision = null;
        boolean versionValidated = false;
        boolean draftFormat = false;
        int lineNumber = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                if (line.startsWith("#")) {
                    if (line.contains(ElicitFormat.FORMAT_VERSION)) {
                        versionValidated = true;
                    } else if (line.contains(ElicitFormat.DRAFT_FORMAT_VERSION)) {
                        // A definition drafted outside Author (UC-042). Same records, different
                        // marker, so Admin cannot install it at a site without a round trip here.
                        versionValidated = true;
                        draftFormat = true;
                    }
                    try {
                        OffsetDateTime parsed = ElicitFormat.parseRevisionHeader(line);
                        if (parsed != null) {
                            fileRevision = parsed;
                        }
                    } catch (IllegalArgumentException e) {
                        throw new DefinitionException("Line " + lineNumber + ": " + e.getMessage());
                    }
                    continue;
                }
                if (!versionValidated) {
                    throw new DefinitionException("This is not a survey definition export: the file does not start with '# "
                            + ElicitFormat.FORMAT_VERSION + "'");
                }
                int colon = line.indexOf(':');
                if (colon < 0) {
                    throw new DefinitionException("Line " + lineNumber + ": invalid record, missing colon separator");
                }
                String table = line.substring(0, colon).trim().toLowerCase();
                String[] fields = ElicitFormat.parseFields(line.substring(colon + 1).trim());
                try {
                    if (!"surveys".equals(table) && surveyId == null) {
                        throw new DefinitionException("a " + table + " record appears before the surveys record");
                    }
                    if (ElicitFormat.VERSIONED_TABLES.contains(table)) {
                        requireFields(table, fields);
                        if (ElicitFormat.isRetired(table, fields)) {
                            skipped.merge(table, 1, Integer::sum);
                            continue;
                        }
                    }
                    switch (table) {
                        case "surveys" -> {
                            if (surveyId != null) {
                                throw new DefinitionException("multiple surveys records; only one survey per file");
                            }
                            requireFields(table, fields);
                            surveyKey = parseKeyOrMint(fields[1], "survey_key");
                            rejectIfPresent(surveyKey);
                            surveyId = insertSurvey(fields, surveyKey);
                            surveyName = nullIfEmpty(fields[2]);
                        }
                        case "select_groups" -> selectGroupIdMap.put(parseLongOrNull(fields[0]), insertSelectGroup(fields, surveyId));
                        case "select_items" -> insertSelectItem(fields, surveyId, selectGroupIdMap);
                        case "steps" -> stepIdMap.put(parseLongOrNull(fields[0]), insertStep(fields, surveyId));
                        case "sections" -> sectionIdMap.put(parseLongOrNull(fields[0]), insertSection(fields, surveyId));
                        case "steps_sections" -> stepsSectionIdMap.put(parseLongOrNull(fields[0]),
                                insertStepsSection(fields, surveyId, stepIdMap, sectionIdMap));
                        case "questions" -> questionIdMap.put(parseLongOrNull(fields[0]), insertQuestion(fields, surveyId, selectGroupIdMap));
                        case "sections_questions" -> sectionsQuestionIdMap.put(parseLongOrNull(fields[0]),
                                insertSectionsQuestion(fields, surveyId, questionIdMap, sectionIdMap));
                        case "relationships" -> insertRelationship(fields, surveyId, stepIdMap, sectionsQuestionIdMap, stepsSectionIdMap);
                        case "reports" -> insertReport(fields, surveyId);
                        case "post_survey_actions" -> insertPostSurveyAction(fields, surveyId);
                        case "dimensions" -> dimensionIdMap.put(parseLongOrNull(fields[0]), insertOrReuseDimension(fields));
                        case "ontology" -> ontologyIdMap.put(parseLongOrNull(fields[0]), insertOrReuseOntology(fields, surveyId, dimensionIdMap));
                        case "metadata" -> insertMetadata(fields, surveyId, stepsSectionIdMap, questionIdMap, sectionsQuestionIdMap, ontologyIdMap);
                        default -> throw new DefinitionException("unknown record type '" + table + "'");
                    }
                    counts.merge(table, 1, Integer::sum);
                } catch (DefinitionException e) {
                    throw new DefinitionException("Line " + lineNumber + ": " + e.getMessage(), e.details());
                } catch (RuntimeException e) {
                    Log.debugf(e, "Import failed at line %d", lineNumber);
                    throw new DefinitionException("Line " + lineNumber + ": " + rootMessage(e), e);
                }
            }
        } catch (IOException e) {
            throw new DefinitionException("Failed to read the file: " + e.getMessage(), e);
        }
        if (!versionValidated) {
            throw new DefinitionException("This is not a survey definition: the file must start with '# "
                    + ElicitFormat.FORMAT_VERSION + "' or '# " + ElicitFormat.DRAFT_FORMAT_VERSION + "'");
        }
        if (surveyId == null) {
            throw new DefinitionException("This file contains no surveys record, so it is not a survey definition export");
        }
        em.flush();
        return new ImportResult(surveyId.intValue(), surveyKey, surveyName, fileRevision, counts, skipped, draftFormat);
    }

    private static String rootMessage(Throwable t) {
        Throwable c = t;
        while (c.getCause() != null && c.getCause() != c) {
            c = c.getCause();
        }
        return c.getMessage() == null ? c.getClass().getSimpleName() : c.getMessage();
    }

    private void rejectIfPresent(UUID surveyKey) {
        Object existing = em.createNativeQuery("SELECT id, name FROM survey.surveys WHERE survey_key = ?1")
                .setParameter(1, surveyKey).getResultStream().findFirst().orElse(null);
        if (existing instanceof Object[] row) {
            throw new DefinitionException("This survey is already open in Author as '" + row[1] + "' (id " + row[0]
                    + "). Open it from the Surveys list instead of importing it again.");
        }
    }

    private long nextval(String sequence) {
        return ((Number) em.createNativeQuery("SELECT nextval('" + sequence + "')").getSingleResult()).longValue();
    }

    private Long durableIdOf(String durableColumn, String table, long surrogateId) {
        return ((Number) em.createNativeQuery("SELECT " + durableColumn + " FROM " + table + " WHERE id = ?1")
                .setParameter(1, surrogateId).getSingleResult()).longValue();
    }

    private Long insertSurvey(String[] f, UUID surveyKey) {
        long newId = nextval("survey.surveys_seq");
        int nextDisplayOrder = ((Number) em.createNativeQuery(
                "SELECT COALESCE(MAX(display_order), 0) + 1 FROM survey.surveys").getSingleResult()).intValue();
        Query q = em.createNativeQuery("""
                INSERT INTO survey.surveys
                    (id, survey_key, name, display_order, title, description, initial_display_key, post_survey_url,
                     published_by, published_comment)
                VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10)
                """);
        q.setParameter(1, newId);
        q.setParameter(2, surveyKey);
        q.setParameter(3, nullIfEmpty(f[2]));
        q.setParameter(4, nextDisplayOrder);
        q.setParameter(5, nullIfEmpty(f[4]));
        q.setParameter(6, nullIfEmpty(f[5]));
        q.setParameter(7, rebaseDisplayKey(f[6], newId));
        q.setParameter(8, nullIfEmpty(f[7]));
        q.setParameter(9, nullIfEmpty(f[8]));
        q.setParameter(10, nullIfEmpty(f[9]));
        q.executeUpdate();
        return newId;
    }

    private Long insertSelectGroup(String[] f, Long surveyId) {
        long newId = nextval("survey.select_groups_seq");
        String dataType = nullIfEmpty(f[4]);
        Query q = em.createNativeQuery("""
                INSERT INTO survey.select_groups (id, survey_id, select_group_key, name, description, data_type)
                VALUES (?1, ?2, ?3, ?4, ?5, ?6)
                """);
        q.setParameter(1, newId);
        q.setParameter(2, surveyId);
        q.setParameter(3, parseKeyOrMint(f[1], "element_key"));
        q.setParameter(4, nullIfEmpty(f[2]));
        q.setParameter(5, nullIfEmpty(f[3]));
        q.setParameter(6, dataType != null ? dataType : "Text");
        q.executeUpdate();
        return durableIdOf("select_group_id", "survey.select_groups", newId);
    }

    private void insertSelectItem(String[] f, Long surveyId, Map<Long, Long> selectGroupIdMap) {
        Long groupId = resolveRequired(parseLongOrNull(f[2]), selectGroupIdMap, "select_group (for select_item)");
        Query q = em.createNativeQuery("""
                INSERT INTO survey.select_items
                    (id, survey_id, select_item_key, select_group_id, display_text, display_order, coded_value)
                VALUES (nextval('survey.select_items_seq'), ?1, ?2, ?3, ?4, ?5, ?6)
                """);
        q.setParameter(1, surveyId);
        q.setParameter(2, parseKeyOrMint(f[1], "element_key"));
        q.setParameter(3, groupId);
        q.setParameter(4, nullIfEmpty(f[3]));
        q.setParameter(5, parseIntOrNull(f[4]));
        q.setParameter(6, nullIfEmpty(f[5]));
        q.executeUpdate();
    }

    private Long insertStep(String[] f, Long surveyId) {
        long newId = nextval("survey.steps_seq");
        String name = nullIfEmpty(f[3]);
        String dimensionName = nullIfEmpty(f[4]);
        Query q = em.createNativeQuery("""
                INSERT INTO survey.steps (id, survey_id, step_key, display_order, name, dimension_name, description)
                VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7)
                """);
        q.setParameter(1, newId);
        q.setParameter(2, surveyId);
        q.setParameter(3, parseKeyOrMint(f[1], "element_key"));
        q.setParameter(4, parseDecimalOrNull(f[2]));
        q.setParameter(5, name);
        q.setParameter(6, dimensionName != null ? dimensionName : (name != null ? name : ""));
        q.setParameter(7, nullIfEmpty(f[5]));
        q.executeUpdate();
        return durableIdOf("step_id", "survey.steps", newId);
    }

    private Long insertSection(String[] f, Long surveyId) {
        long newId = nextval("survey.sections_seq");
        String name = nullIfEmpty(f[3]);
        String dimensionName = nullIfEmpty(f[4]);
        Query q = em.createNativeQuery("""
                INSERT INTO survey.sections (id, survey_id, section_key, display_order, name, dimension_name, description)
                VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7)
                """);
        q.setParameter(1, newId);
        q.setParameter(2, surveyId);
        q.setParameter(3, parseKeyOrMint(f[1], "element_key"));
        q.setParameter(4, parseDecimalOrNull(f[2]));
        q.setParameter(5, name);
        q.setParameter(6, dimensionName != null ? dimensionName : (name != null ? name : ""));
        q.setParameter(7, nullIfEmpty(f[5]));
        q.executeUpdate();
        return durableIdOf("section_id", "survey.sections", newId);
    }

    private Long insertStepsSection(String[] f, Long surveyId, Map<Long, Long> stepIdMap, Map<Long, Long> sectionIdMap) {
        Long stepId = resolveRequired(parseLongOrNull(f[2]), stepIdMap, "step (for steps_sections)");
        Long sectionId = resolveRequired(parseLongOrNull(f[4]), sectionIdMap, "section (for steps_sections)");
        long newId = nextval("survey.steps_sections_seq");
        Query q = em.createNativeQuery("""
                INSERT INTO survey.steps_sections
                    (id, survey_id, steps_sections_key, step_id, step_display_order, section_id, section_display_order, display_key)
                VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8)
                """);
        q.setParameter(1, newId);
        q.setParameter(2, surveyId);
        q.setParameter(3, parseKeyOrMint(f[1], "element_key"));
        q.setParameter(4, stepId);
        q.setParameter(5, parseDecimalOrNull(f[3]));
        q.setParameter(6, sectionId);
        q.setParameter(7, parseDecimalOrNull(f[5]));
        q.setParameter(8, rebaseDisplayKey(f[6], surveyId));
        q.executeUpdate();
        return durableIdOf("steps_sections_id", "survey.steps_sections", newId);
    }

    private Long insertQuestion(String[] f, Long surveyId, Map<Long, Long> selectGroupIdMap) {
        Long selectGroupId = resolveNullable(parseLongOrNull(f[10]), selectGroupIdMap, "select_group (for question)");
        long newId = nextval("survey.questions_seq");
        Query q = em.createNativeQuery("""
                INSERT INTO survey.questions
                    (id, survey_id, question_key, type_id, text, short_text, tool_tip, required,
                     min_value, max_value, validation_text, select_group_id, mask, placeholder, default_value, variant)
                VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12, ?13, ?14, ?15, ?16)
                """);
        q.setParameter(1, newId);
        q.setParameter(2, surveyId);
        q.setParameter(3, parseKeyOrMint(f[1], "element_key"));
        q.setParameter(4, parseIntOrNull(f[2]));
        q.setParameter(5, nullIfEmpty(f[3]));
        q.setParameter(6, nullIfEmpty(f[4]));
        q.setParameter(7, nullIfEmpty(f[5]));
        q.setParameter(8, parseBooleanOrFalse(f[6]));
        q.setParameter(9, parseIntOrNull(f[7]));
        q.setParameter(10, parseIntOrNull(f[8]));
        q.setParameter(11, nullIfEmpty(f[9]));
        q.setParameter(12, selectGroupId);
        q.setParameter(13, nullIfEmpty(f[11]));
        q.setParameter(14, nullIfEmpty(f[12]));
        q.setParameter(15, nullIfEmpty(f[13]));
        q.setParameter(16, nullIfEmpty(f[14]));
        q.executeUpdate();
        return durableIdOf("question_id", "survey.questions", newId);
    }

    private Long insertSectionsQuestion(String[] f, Long surveyId, Map<Long, Long> questionIdMap, Map<Long, Long> sectionIdMap) {
        Long questionId = resolveRequired(parseLongOrNull(f[2]), questionIdMap, "question (for sections_questions)");
        Long sectionId = resolveRequired(parseLongOrNull(f[3]), sectionIdMap, "section (for sections_questions)");
        long newId = nextval("survey.sections_questions_seq");
        Query q = em.createNativeQuery("""
                INSERT INTO survey.sections_questions
                    (id, survey_id, sections_question_key, question_id, section_id, display_order)
                VALUES (?1, ?2, ?3, ?4, ?5, ?6)
                """);
        q.setParameter(1, newId);
        q.setParameter(2, surveyId);
        q.setParameter(3, parseKeyOrMint(f[1], "element_key"));
        q.setParameter(4, questionId);
        q.setParameter(5, sectionId);
        q.setParameter(6, parseDecimalOrNull(f[4]));
        q.executeUpdate();
        return durableIdOf("sections_question_id", "survey.sections_questions", newId);
    }

    private void insertRelationship(String[] f, Long surveyId, Map<Long, Long> stepIdMap,
                                    Map<Long, Long> sectionsQuestionIdMap, Map<Long, Long> stepsSectionIdMap) {
        Query q = em.createNativeQuery("""
                INSERT INTO survey.relationships
                    (id, survey_id, relationship_key, upstream_step_id, upstream_sq_id, downstream_step_id,
                     downstream_ss_id, downstream_sq_id, operator_id, action_id, description,
                     token, reference_value, default_upstream_value, override_upstream_value)
                VALUES (nextval('survey.relationships_seq'), ?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12, ?13, ?14)
                """);
        q.setParameter(1, surveyId);
        q.setParameter(2, parseKeyOrMint(f[1], "element_key"));
        q.setParameter(3, resolveNullable(parseLongOrNull(f[2]), stepIdMap, "step (upstream)"));
        q.setParameter(4, resolveRequired(parseLongOrNull(f[3]), sectionsQuestionIdMap, "sections_question (upstream_sq)"));
        q.setParameter(5, resolveNullable(parseLongOrNull(f[4]), stepIdMap, "step (downstream)"));
        q.setParameter(6, resolveNullable(parseLongOrNull(f[5]), stepsSectionIdMap, "steps_section (downstream_ss)"));
        q.setParameter(7, resolveNullable(parseLongOrNull(f[6]), sectionsQuestionIdMap, "sections_question (downstream_sq)"));
        q.setParameter(8, parseIntOrNull(f[7]));
        q.setParameter(9, parseIntOrNull(f[8]));
        q.setParameter(10, nullIfEmpty(f[9]));
        q.setParameter(11, nullIfEmpty(f[10]));
        q.setParameter(12, nullIfEmpty(f[11]));
        q.setParameter(13, nullIfEmpty(f[12]));
        q.setParameter(14, nullIfEmpty(f[13]));
        q.executeUpdate();
    }

    private void insertReport(String[] f, Long surveyId) {
        requireFields("reports", f);
        Query q = em.createNativeQuery("""
                INSERT INTO survey.reports (id, survey_id, report_key, name, description, url, display_order)
                VALUES (nextval('survey.reports_seq'), ?1, ?2, ?3, ?4, ?5, ?6)
                """);
        q.setParameter(1, surveyId);
        q.setParameter(2, parseKeyOrMint(f[1], "element_key"));
        q.setParameter(3, nullIfEmpty(f[2]));
        q.setParameter(4, nullIfEmpty(f[3]));
        q.setParameter(5, nullIfEmpty(f[4]));
        q.setParameter(6, parseIntOrNull(f[5]));
        q.executeUpdate();
    }

    private void insertPostSurveyAction(String[] f, Long surveyId) {
        requireFields("post_survey_actions", f);
        Query q = em.createNativeQuery("""
                INSERT INTO survey.post_survey_actions
                    (id, survey_id, post_survey_action_key, name, description, url, execution_order)
                VALUES (nextval('survey.post_survey_actions_seq'), ?1, ?2, ?3, ?4, ?5, ?6)
                """);
        q.setParameter(1, surveyId);
        q.setParameter(2, parseKeyOrMint(f[1], "element_key"));
        q.setParameter(3, nullIfEmpty(f[2]));
        q.setParameter(4, nullIfEmpty(f[3]));
        q.setParameter(5, nullIfEmpty(f[4]));
        q.setParameter(6, parseIntOrNull(f[5]));
        q.executeUpdate();
    }

    private Long insertOrReuseDimension(String[] f) {
        requireFields("dimensions", f);
        String name = nullIfEmpty(f[2]);
        Object existing = em.createNativeQuery("SELECT id FROM survey.dimensions WHERE name = ?1")
                .setParameter(1, name).getResultStream().findFirst().orElse(null);
        if (existing != null) {
            return ((Number) existing).longValue();
        }
        long newId = nextval("survey.dimensions_seq");
        Query q = em.createNativeQuery("INSERT INTO survey.dimensions (id, dimension_key, name) VALUES (?1, ?2, ?3)");
        q.setParameter(1, newId);
        q.setParameter(2, parseKeyOrMint(f[1], "element_key"));
        q.setParameter(3, name);
        q.executeUpdate();
        return newId;
    }

    private Long insertOrReuseOntology(String[] f, Long surveyId, Map<Long, Long> dimensionIdMap) {
        requireFields("ontology", f);
        String name = nullIfEmpty(f[2]);
        String tag = nullIfEmpty(f[3]);
        Object existing = em.createNativeQuery("SELECT id FROM survey.ontology WHERE name = ?1 AND tag = ?2")
                .setParameter(1, name).setParameter(2, tag).getResultStream().findFirst().orElse(null);
        if (existing != null) {
            return ((Number) existing).longValue();
        }
        long newId = nextval("survey.ontology_seq");
        Query q = em.createNativeQuery("""
                INSERT INTO survey.ontology (id, survey_id, ontology_key, name, tag, dimension)
                VALUES (?1, ?2, ?3, ?4, ?5, ?6)
                """);
        q.setParameter(1, newId);
        q.setParameter(2, surveyId);
        q.setParameter(3, parseKeyOrMint(f[1], "element_key"));
        q.setParameter(4, name);
        q.setParameter(5, tag);
        q.setParameter(6, resolveNullable(parseLongOrNull(f[4]), dimensionIdMap, "dimension (ontology)"));
        q.executeUpdate();
        return newId;
    }

    private void insertMetadata(String[] f, Long surveyId, Map<Long, Long> stepsSectionIdMap, Map<Long, Long> questionIdMap,
                                Map<Long, Long> sectionsQuestionIdMap, Map<Long, Long> ontologyIdMap) {
        requireFields("metadata", f);
        Query q = em.createNativeQuery("""
                INSERT INTO survey.metadata
                    (id, survey_id, metadata_key, steps_sections_id, question_id, sections_question_id, ontology_id, value)
                VALUES (nextval('survey.metadata_seq'), ?1, ?2, ?3, ?4, ?5, ?6, ?7)
                """);
        q.setParameter(1, surveyId);
        q.setParameter(2, parseKeyOrMint(f[1], "element_key"));
        q.setParameter(3, resolveNullable(parseLongOrNull(f[2]), stepsSectionIdMap, "steps_section (metadata)"));
        q.setParameter(4, resolveNullable(parseLongOrNull(f[3]), questionIdMap, "question (metadata)"));
        q.setParameter(5, resolveNullable(parseLongOrNull(f[4]), sectionsQuestionIdMap, "sections_question (metadata)"));
        q.setParameter(6, resolveRequired(parseLongOrNull(f[5]), ontologyIdMap, "ontology (metadata)"));
        q.setParameter(7, nullIfEmpty(f[6]));
        q.executeUpdate();
    }
}
