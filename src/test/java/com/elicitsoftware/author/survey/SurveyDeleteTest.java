package com.elicitsoftware.author.survey;

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

import com.elicitsoftware.author.AuthoringException;
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
            "translations", "metadata", "ontology", "relationships", "sections_questions", "steps_sections",
            "questions", "select_items", "select_groups", "sections", "steps", "reports", "post_survey_actions");

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

    private long rows(String table, int surveyId) {
        return ((Number) em.createNativeQuery("SELECT count(*) FROM survey." + table + " WHERE survey_id = ?1")
                .setParameter(1, surveyId).getSingleResult()).longValue();
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "author.one", roles = {"elicit_author"})
    void delete_removesTheSurveyAndEveryRowItOwns() {
        int id = fixture("AuthorFixture").id;
        assertTrue(rows("questions", id) > 0, "the fixture has structure to delete");

        service.delete(id);

        for (String table : OWNED) {
            assertEquals(0, rows(table, id), "UC-047 BR-001: nothing left in " + table);
        }
        assertTrue(service.find(id).isEmpty());
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
    void delete_refusesASurveyWithRespondents() {
        Survey s = fixture("AuthorFixtureEmpty");
        em.createNativeQuery("""
                        INSERT INTO survey.respondents (id, survey_id, access_code)
                        VALUES ((SELECT coalesce(max(id), 0) + 1 FROM survey.respondents), ?1, 'UC047')
                        """)
                .setParameter(1, s.id).executeUpdate();

        AuthoringException ex = assertThrows(AuthoringException.class, () -> service.delete(s.id));
        assertTrue(ex.getMessage().contains("respondents"), "UC-047 BR-004: " + ex.getMessage());
        assertTrue(service.find(s.id).isPresent());
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
