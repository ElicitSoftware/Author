package com.elicitsoftware.author.survey;

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

import com.elicitsoftware.author.definition.ElicitFormat;
import com.elicitsoftware.author.definition.ExportResult;
import com.elicitsoftware.author.definition.ImportResult;
import com.elicitsoftware.author.definition.SurveyDefinitionExporter;
import com.elicitsoftware.author.definition.SurveyDefinitionImporter;
import com.elicitsoftware.model.Survey;
import com.elicitsoftware.test.PostgresTestResource;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** UC-047 Delete Survey. */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class SurveyDeleteTest {

    private static final List<String> OWNED = List.of(
            "translations", "report_items", "metadata", "ontology", "relationships", "sections_questions", "steps_sections",
            "questions", "select_items", "select_groups", "sections", "steps", "reports", "post_survey_actions");

    private static final List<String> PREVIEW = List.of("dependents", "respondent_psa", "answers", "respondents");

    @Inject
    SurveyService service;

    @Inject
    SurveyDefinitionExporter exporter;

    @Inject
    SurveyDefinitionImporter importer;

    @Inject
    EntityManager em;

    private static Survey fixture(String name) {
        return Survey.<Survey>find("name", name).firstResult();
    }

    /** A stand-in for the preview Survey (UC-047 BR-008): records the keys it was asked to drop, or refuses. */
    private static final class PreviewSurveyStub extends PreviewSurveyClient {
        final List<UUID> dropped = new java.util.ArrayList<>();
        final boolean refuse;

        PreviewSurveyStub(boolean refuse) {
            this.refuse = refuse;
        }

        @Override
        public void dropReportingSchema(UUID surveyKey) {
            if (refuse) {
                throw new com.elicitsoftware.author.AuthoringException("preview down");
            }
            dropped.add(surveyKey);
        }
    }

    private PreviewSurveyStub previewSurvey(boolean refuse) {
        PreviewSurveyStub stub = new PreviewSurveyStub(refuse);
        io.quarkus.test.junit.QuarkusMock.installMockForType(stub, PreviewSurveyClient.class);
        return stub;
    }

    /** Pretends the preview Survey built the survey's reporting schema (Survey UC-008 BR-006). */
    private void builtInPreview(int surveyId, String schema) {
        em.createNativeQuery("UPDATE survey.surveys SET report_schema = ?2 WHERE id = ?1")
                .setParameter(1, surveyId).setParameter(2, schema).executeUpdate();
        em.clear();
    }

    private long rows(String table, int surveyId) {
        return ((Number) em.createNativeQuery("SELECT count(*) FROM survey." + table + " WHERE survey_id = ?1")
                .setParameter(1, surveyId).getSingleResult()).longValue();
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "author.one", roles = {"elicit_author"})
    void delete_removesTheSurveyAndEveryRowItOwns() {
        PreviewSurveyStub preview = previewSurvey(false);
        int id = fixture("AuthorFixture").id;
        assertTrue(rows("questions", id) > 0, "the fixture has structure to delete");

        service.delete(id);

        for (String table : OWNED) {
            assertEquals(0, rows(table, id), "UC-047 BR-001: nothing left in " + table);
        }
        assertTrue(service.find(id).isEmpty());
        assertTrue(preview.dropped.isEmpty(), "UC-047 BR-008: a survey never built in the preview makes no call");
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "author.one", roles = {"elicit_author"})
    void delete_dropsThePreviewsReportingSchemaFirst() {
        PreviewSurveyStub preview = previewSurvey(false);
        Survey s = fixture("AuthorFixture");
        int id = s.id;
        UUID key = s.surveyKey;
        builtInPreview(id, "report_authorfixture");

        service.delete(id);

        assertEquals(List.of(key), preview.dropped, "UC-047 BR-008: the preview Survey was asked to drop the schema, by the survey's key");
        assertTrue(service.find(id).isEmpty());
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "author.one", roles = {"elicit_author"})
    void delete_isRefusedWhenThePreviewCannotDropTheSchema() {
        previewSurvey(true);
        int id = fixture("AuthorFixture").id;
        builtInPreview(id, "report_authorfixture");
        long questions = rows("questions", id);

        assertThrows(com.elicitsoftware.author.AuthoringException.class, () -> service.delete(id));

        assertEquals(questions, rows("questions", id), "UC-047 A7: the survey is unchanged");
        assertTrue(service.find(id).isPresent());
        assertEquals("report_authorfixture", em.createNativeQuery("SELECT report_schema FROM survey.surveys WHERE id = ?1")
                .setParameter(1, id).getSingleResult(), "UC-047 A7: the schema name is unchanged too");
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "author.one", roles = {"elicit_author"})
    void delete_handsASharedTagToTheSurveyStillUsingIt() {
        int id = fixture("AuthorFixture").id;
        int other = fixture("AuthorFixtureEmpty").id;
        Number tagId = (Number) em.createNativeQuery("SELECT min(id) FROM survey.ontology WHERE survey_id = ?1")
                .setParameter(1, id).getSingleResult();
        // As the importer leaves it when a second survey's file names a tag the first one owns.
        em.createNativeQuery("""
                        INSERT INTO survey.metadata (id, survey_id, metadata_key, question_id, ontology_id)
                        VALUES (nextval('survey.metadata_seq'), ?1, ?2, 1, ?3)
                        """)
                .setParameter(1, other).setParameter(2, UUID.randomUUID()).setParameter(3, tagId).executeUpdate();

        service.delete(id);

        Number owner = (Number) em.createNativeQuery("SELECT survey_id FROM survey.ontology WHERE id = ?1")
                .setParameter(1, tagId).getSingleResult();
        assertEquals(other, owner.intValue(), "UC-047 BR-005: the tag survives, owned by the survey using it");
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "author.one", roles = {"elicit_author"})
    void delete_removesWhatThePreviewRecorded() {
        int id = fixture("AuthorFixture").id;
        int previewed = previewRun(id, "UC047");
        int elsewhere = previewRun(fixture("AuthorFixtureEmpty").id, "UC047-OTHER");
        assertEquals(1, service.previewRespondents(id), "UC-047 A4: the confirmation states this count");

        service.delete(id);

        for (String table : PREVIEW) {
            assertEquals(0, rowsOf(table, previewed), "UC-047 BR-004: nothing left in " + table);
        }
        assertEquals(0, service.previewRespondents(id));
        assertTrue(service.find(id).isEmpty());
        assertEquals(2, rowsOf("answers", elsewhere), "another survey's preview answers are not touched");
    }

    /**
     * A walk through the preview as the Survey app leaves it: a respondent, two answers, the
     * dependency a rule recorded between them (where the survey has a rule) and a post-survey
     * action record.
     *
     * @return the respondent's id
     */
    private int previewRun(int surveyId, String accessCode) {
        int respondent = next("respondents_seq");
        em.createNativeQuery("INSERT INTO survey.respondents (id, survey_id, access_code) VALUES (?1, ?2, ?3)")
                .setParameter(1, respondent).setParameter(2, surveyId).setParameter(3, accessCode).executeUpdate();
        int upstream = next("answers_seq");
        int downstream = next("answers_seq");
        for (int answer : List.of(upstream, downstream)) {
            em.createNativeQuery("""
                            INSERT INTO survey.answers (id, survey_id, respondent_id, question_id, display_key, display_text)
                            VALUES (?1, ?2, ?3, (SELECT min(id) FROM survey.questions WHERE survey_id = ?2), ?4, 'UC-047')
                            """)
                    .setParameter(1, answer).setParameter(2, surveyId).setParameter(3, respondent)
                    .setParameter(4, "UC047-" + answer).executeUpdate();
        }
        em.createNativeQuery("""
                        INSERT INTO survey.dependents (id, respondent_id, upstream_id, downstream_id, relationship_id)
                        SELECT nextval('survey.dependents_seq'), ?1, ?2, ?3, min(id)
                          FROM survey.relationships WHERE survey_id = ?4 HAVING min(id) IS NOT NULL
                        """)
                .setParameter(1, respondent).setParameter(2, upstream).setParameter(3, downstream)
                .setParameter(4, surveyId).executeUpdate();
        int action = next("post_survey_actions_seq");
        em.createNativeQuery("""
                        INSERT INTO survey.post_survey_actions (id, survey_id, name, description, url, execution_order)
                        VALUES (?1, ?2, 'UC-047', 'UC-047', 'http://localhost/uc047', 1)
                        """)
                .setParameter(1, action).setParameter(2, surveyId).executeUpdate();
        em.createNativeQuery("""
                        INSERT INTO survey.respondent_psa (id, respondent_id, post_survey_action_id, status)
                        VALUES (nextval('survey.respondent_psa_seq'), ?1, ?2, 'PENDING')
                        """)
                .setParameter(1, respondent).setParameter(2, action).executeUpdate();
        return respondent;
    }

    private int next(String sequence) {
        return ((Number) em.createNativeQuery("SELECT nextval('survey." + sequence + "')").getSingleResult()).intValue();
    }

    private long rowsOf(String table, int respondentId) {
        String column = table.equals("respondents") ? "id" : "respondent_id";
        return ((Number) em.createNativeQuery("SELECT count(*) FROM survey." + table + " WHERE " + column + " = ?1")
                .setParameter(1, respondentId).getSingleResult()).longValue();
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "author.one", roles = {"elicit_author"})
    void anExportTakenFirstImportsBackUnderTheSameKey() {
        // The empty fixture: export validates, and an export is all this needs.
        Survey s = fixture("AuthorFixtureEmpty");
        UUID key = s.surveyKey;
        ExportResult file = exporter.export(service.stampExport(s.id, "before UC-047 delete"));

        service.delete(s.id);
        assertFalse(service.findByKey(key).isPresent());

        // UC-047 BR-003: once deleted the key is free again, so the file is accepted as it stands.
        ImportResult restored = importer.importFile(new ByteArrayInputStream(file.bytes()));
        assertEquals(key, restored.surveyKey());
        assertEquals("AuthorFixtureEmpty", restored.surveyName());
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "author.one", roles = {"elicit_author"})
    void aBackupKeepsASurveyThatCannotBeExported() {
        Survey s = fixture("AuthorFixture");
        UUID key = s.surveyKey;
        String note = s.publishedComment;
        long current = rows("questions", s.id) - retiredIn(s.id);

        ExportResult file = exporter.backup(s);

        // UC-047 BR-006: draft marker, so Admin refuses it; no export is recorded.
        String text = new String(file.bytes(), StandardCharsets.UTF_8);
        assertTrue(text.startsWith("# " + ElicitFormat.DRAFT_FORMAT_VERSION + "\n"));
        assertFalse(text.contains(ElicitFormat.FORMAT_VERSION));
        assertTrue(file.fileName().endsWith("_backup" + ElicitFormat.FILE_EXTENSION));
        assertEquals(note, service.find(s.id).orElseThrow().publishedComment);

        service.delete(s.id);
        ImportResult restored = importer.importFile(new ByteArrayInputStream(file.bytes()));
        assertEquals(key, restored.surveyKey());
        assertTrue(restored.draftFormat());
        assertEquals(current, rows("questions", restored.surveyId()) - retiredIn(restored.surveyId()),
                "every current question comes back");
    }

    private long retiredIn(int surveyId) {
        return ((Number) em.createNativeQuery(
                "SELECT count(*) FROM survey.questions WHERE survey_id = ?1 AND effective_to <> '9999-12-31 23:59:59+00'")
                .setParameter(1, surveyId).getSingleResult()).longValue();
    }

    @Test
    void isExported_followsTheStoredReleaseNote() {
        Survey s = new Survey();
        s.publishedBy = "author.one";
        assertFalse(SurveyService.isExported(s), "creating or editing records the author, not an export");
        s.publishedComment = "first release";
        assertTrue(SurveyService.isExported(s));
    }
}
