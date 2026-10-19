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
import com.elicitsoftware.author.survey.Tokens;
import com.elicitsoftware.author.survey.TranslatableFields;
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

    @Inject
    com.elicitsoftware.author.survey.ReportService reports;

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
        boolean reportsHeaderSeen = false;
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
                    if (line.startsWith("# reports:")) {
                        // A backup (UC-047) counts its tables; a draft (UC-042) need not.
                        reportsHeaderSeen = true;
                    }
                    try {
                        OffsetDateTime parsed = ElicitFormat.parseRevisionHeader(line);
                        if (parsed != null) {
                            fileRevision = parsed;
                        }
                    } catch (IllegalArgumentException e) {
                        throw new DefinitionException(Translations.get("error.import.line", String.valueOf(lineNumber), e.getMessage()));
                    }
                    continue;
                }
                if (!versionValidated) {
                    throw new DefinitionException(Translations.get("error.import.notExport", ElicitFormat.FORMAT_VERSION));
                }
                int colon = line.indexOf(':');
                if (colon < 0) {
                    throw new DefinitionException(Translations.get("error.import.line", String.valueOf(lineNumber), Translations.get("error.import.noColon")));
                }
                String table = line.substring(0, colon).trim().toLowerCase();
                String[] fields = ElicitFormat.parseFields(line.substring(colon + 1).trim());
                try {
                    if (!"surveys".equals(table) && surveyId == null) {
                        throw new DefinitionException(Translations.get("error.import.recordBeforeSurvey", table));
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
                                throw new DefinitionException(Translations.get("error.import.multipleSurveys"));
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
                        case "report_items" -> insertReportItem(fields, surveyId, stepsSectionIdMap, questionIdMap, sectionsQuestionIdMap);
                        case "translations" -> insertTranslation(fields, surveyId);
                        default -> throw new DefinitionException(Translations.get("error.import.unknownRecord", table));
                    }
                    counts.merge(table, 1, Integer::sum);
                } catch (DefinitionException e) {
                    throw new DefinitionException(Translations.get("error.import.line", String.valueOf(lineNumber), e.getMessage()), e.details());
                } catch (RuntimeException e) {
                    Log.debugf(e, "Import failed at line %d", lineNumber);
                    throw new DefinitionException(Translations.get("error.import.line", String.valueOf(lineNumber), rootMessage(e)), e);
                }
            }
        } catch (IOException e) {
            throw new DefinitionException(Translations.get("error.import.readFailed", e.getMessage()), e);
        }
        if (!versionValidated) {
            throw new DefinitionException(Translations.get("error.import.notDefinition", ElicitFormat.FORMAT_VERSION, ElicitFormat.DRAFT_FORMAT_VERSION));
        }
        if (surveyId == null) {
            throw new DefinitionException(Translations.get("error.import.noSurveysRecord"));
        }
        em.flush();
        if (draftFormat && !reportsHeaderSeen && counts.getOrDefault("reports", 0) == 0) {
            // UC-048 BR-001: a draft is told to omit the reports table, so it starts like a survey
            // made from the blank form. A definition file, or a backup, is the whole list and
            // seeds nothing.
            reports.seedBuiltIn(surveyId.intValue());
            counts.put("reports", 1);
        }
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
            throw new DefinitionException(Translations.get("error.import.alreadyOpen", String.valueOf(row[1]), String.valueOf(row[0])));
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
                     published_by, published_comment, base_language, content_languages)
                VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12)
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
        // V019. A file written before the two fields existed leaves the survey on the column
        // default, 'en', and publishes nothing.
        String baseLanguage = f.length > 10 ? nullIfEmpty(f[10]) : null;
        q.setParameter(11, baseLanguage != null ? baseLanguage : "en");
        q.setParameter(12, f.length > 11 ? nullIfEmpty(f[11]) : null);
        q.executeUpdate();
        return newId;
    }

    /**
     * Fields: {@code translation_id|translation_key|element_type|element_key|field|language|value|
     * source_hash|version|effective_from|effective_to|published_by|published_comment}.
     * <p>
     * Both keys are parsed strictly rather than minted. A translation with no {@code element_key}
     * names nothing, and one with no {@code translation_key} cannot be matched at a site on the
     * next update, so either is a malformed record rather than something to default. The durable
     * {@code translation_id} in the file is a local link at the exporting instance and is
     * discarded; the sequence allocates a fresh one here.
     *
     * @param f        the record's fields
     * @param surveyId the survey being imported into
     */
    private void insertTranslation(String[] f, Long surveyId) {
        UUID translationKey = parseKeyStrict(f[1], "translation_key");
        UUID targetKey = parseKeyStrict(f[3], "element_key");
        String elementType = nullIfEmpty(f[2]);
        String field = nullIfEmpty(f[4]);
        if (!TranslatableFields.isTranslatable(elementType, field)) {
            throw new DefinitionException(Translations.get("error.import.translationNotTranslatable", elementType + "." + field));
        }
        String value = nullIfEmpty(f[6]);
        if (value == null) {
            throw new DefinitionException(Translations.get("error.import.translationEmpty", elementType + "." + field));
        }
        Query q = em.createNativeQuery("""
                INSERT INTO survey.translations
                    (id, survey_id, translation_key, element_type, element_key, field, language, value, source_hash)
                VALUES (nextval('survey.translations_seq'), ?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8)
                """);
        q.setParameter(1, surveyId);
        q.setParameter(2, translationKey);
        q.setParameter(3, elementType);
        q.setParameter(4, targetKey);
        q.setParameter(5, field);
        q.setParameter(6, nullIfEmpty(f[5]));
        q.setParameter(7, value);
        q.setParameter(8, nullIfEmpty(f[7]));
        q.executeUpdate();
    }

    /** A key the file must carry: unlike an element key on a structural row, this one is never minted. */
    private static UUID parseKeyStrict(String raw, String what) {
        String value = nullIfEmpty(raw);
        if (value == null) {
            throw new DefinitionException(Translations.get("error.import.translationMissing", what));
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException e) {
            throw new DefinitionException(Translations.get("error.import.invalidValue", what, value));
        }
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
        q.setParameter(11, importedToken(f[10]));
        q.setParameter(12, nullIfEmpty(f[11]));
        q.setParameter(13, nullIfEmpty(f[12]));
        q.setParameter(14, nullIfEmpty(f[13]));
        q.executeUpdate();
    }

    /**
     * The token a relationship carries, held to the same rule {@code RuleService} holds one an
     * author types (FR-012).
     *
     * <p>Until this check existed the field went straight into {@code survey.relationships.token},
     * which is {@code character varying(10)}, so an over-long name surfaced as the raw
     * {@code value too long for type character varying(10)} from Postgres -- a line number and a
     * column width, naming neither the token nor the limit. A definition drafted outside Author
     * (UC-042) is where that comes from: the editor cannot produce one.</p>
     *
     * <p>Canonicalized first, as the editor does, so a draft that writes the name in lower case or
     * as the reference {@code <DEST>} still imports and is stored the way Author stores it. The
     * runtime keys a text's values on this column verbatim, so a token stored with its brackets is
     * asked for under a name it does not have and fills nothing.</p>
     */
    private static String importedToken(String field) {
        String written = nullIfEmpty(field);
        if (written == null) {
            return null;
        }
        String token = Tokens.canonical(written);
        if (token.length() > Tokens.MAX_LENGTH) {
            throw new DefinitionException(Translations.get("error.import.tokenTooLong", token, String.valueOf(token.length()), String.valueOf(Tokens.MAX_LENGTH)));
        }
        if (!Tokens.TOKEN_NAME.matcher(token).matches()) {
            throw new DefinitionException(Translations.get("error.import.tokenInvalid", written));
        }
        return token;
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

    /**
     * UC-049, UC-005 BR-005: {@code id|report_item_key|report_type|steps_sections_id|question_id|
     * sections_question_id|label|display_order}, the element columns remapped like metadata's.
     */
    private void insertReportItem(String[] f, Long surveyId, Map<Long, Long> stepsSectionIdMap, Map<Long, Long> questionIdMap,
                                  Map<Long, Long> sectionsQuestionIdMap) {
        requireFields("report_items", f);
        Query q = em.createNativeQuery("""
                INSERT INTO survey.report_items
                    (id, survey_id, report_item_key, report_type, steps_sections_id, question_id, sections_question_id, label, display_order)
                VALUES (nextval('survey.report_items_seq'), ?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8)
                """);
        q.setParameter(1, surveyId);
        q.setParameter(2, parseKeyOrMint(f[1], "element_key"));
        q.setParameter(3, nullIfEmpty(f[2]));
        q.setParameter(4, resolveNullable(parseLongOrNull(f[3]), stepsSectionIdMap, "steps_section (report_items)"));
        q.setParameter(5, resolveNullable(parseLongOrNull(f[4]), questionIdMap, "question (report_items)"));
        q.setParameter(6, resolveNullable(parseLongOrNull(f[5]), sectionsQuestionIdMap, "sections_question (report_items)"));
        q.setParameter(7, nullIfEmpty(f[6]));
        Integer order = parseIntOrNull(f[7]);
        q.setParameter(8, order == null ? 0 : order);
        q.executeUpdate();
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
