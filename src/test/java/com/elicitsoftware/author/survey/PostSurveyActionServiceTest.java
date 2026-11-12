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
package com.elicitsoftware.author.survey;

import com.elicitsoftware.author.AuthoringException;
import com.elicitsoftware.model.PostSurveyAction;
import com.elicitsoftware.model.Survey;
import com.elicitsoftware.test.PostgresTestResource;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** UC-052 Manage Post-Survey Actions. */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class PostSurveyActionServiceTest {

    @Inject
    PostSurveyActionService service;

    @Inject
    SurveyService surveys;

    @Inject
    EntityManager em;

    private Survey fixture() {
        return Survey.find("name", "AuthorFixture").firstResult();
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "psa.author", roles = {"elicit_author"})
    void create_seedsNoAction() {
        Survey draft = new Survey();
        draft.name = "PsaSeed " + UUID.randomUUID();
        draft.title = "PSA seed";
        Survey s = surveys.create(draft);
        assertTrue(service.actions(s.id).isEmpty(), "UC-052 BR-008: a new survey starts with no actions");
    }

    @Test
    @TestTransaction
    void add_appendsInOrderAndValidates() {
        int sid = fixture().id;
        int before = service.actions(sid).size();
        PostSurveyAction a = service.add(sid, " File the history ", " Uploads the PDF. ", "http://fhhs:8080/familyhistory/generate");
        assertEquals("File the history", a.name, "trimmed");
        assertEquals("Uploads the PDF.", a.description, "trimmed");
        assertEquals(before + 1, a.executionOrder, "UC-052 step 4: at the end");
        assertNotNull(a.postSurveyActionKey, "BR-007: the key is minted");

        assertEquals("name", assertThrows(AuthoringException.class,
                () -> service.add(sid, "File the history", "d", "https://x.example/a")).field(), "A1: name taken");
        assertEquals("name", assertThrows(AuthoringException.class, () -> service.add(sid, " ", "d", "https://x.example/a")).field());
        assertEquals("description", assertThrows(AuthoringException.class,
                () -> service.add(sid, "Other", " ", "https://x.example/a")).field(), "BR-003: required");
        assertEquals("description", assertThrows(AuthoringException.class,
                () -> service.add(sid, "Long", "d".repeat(256), "https://x.example/a")).field(), "BR-003: 255 characters");
        assertEquals("url", assertThrows(AuthoringException.class, () -> service.add(sid, "Other", "d", "")).field(), "A2");
        assertEquals("url", assertThrows(AuthoringException.class,
                () -> service.add(sid, "Other", "d", "/api/reports/respondent-summary")).field(), "BR-004: no path form");
        assertEquals("url", assertThrows(AuthoringException.class, () -> service.add(sid, "Other", "d", "ftp://x")).field(), "BR-004");
        assertEquals("url", assertThrows(AuthoringException.class,
                () -> service.add(sid, "Other", "d", "https://x.example/" + "a".repeat(240))).field(), "BR-004: 255 characters");
        assertEquals("https://x.example/other", service.add(sid, "Other", "kept", " https://x.example/other ").url);
    }

    @Test
    @TestTransaction
    void update_keepsTheKey_reorder_renumbers() {
        int sid = fixture().id;
        PostSurveyAction first = service.add(sid, "First", "d", "https://x.example/1");
        PostSurveyAction second = service.add(sid, "Second", "d", "https://x.example/2");
        UUID key = first.postSurveyActionKey;

        PostSurveyAction edited = service.update(sid, first.id, "First renamed", "changed", "https://x.example/1b");
        assertEquals(key, edited.postSurveyActionKey, "UC-052 step 6 / BR-007: in place, same key");
        assertEquals("name", assertThrows(AuthoringException.class,
                () -> service.update(sid, first.id, "Second", "d", "https://x.example/1")).field(), "A1 on edit");

        service.reorder(sid, List.of(second.id));
        List<PostSurveyAction> rows = service.actions(sid);
        assertEquals(second.id, rows.get(0).id, "BR-005: named ids first, the rest follow");
        assertEquals(List.of(1, 2), rows.stream().limit(2).map(r -> r.executionOrder).toList());
    }

    /** BR-006: removal deletes a preview respondent's delivery record for the action first, then renumbers. */
    @Test
    @TestTransaction
    void remove_deletesTheActionAndItsPreviewDeliveryRecords() {
        int sid = fixture().id;
        PostSurveyAction first = service.add(sid, "Notify", "d", "https://x.example/notify");
        PostSurveyAction second = service.add(sid, "File", "d", "https://x.example/file");
        em.createNativeQuery("INSERT INTO survey.respondents (id, survey_id, access_code) "
                        + "VALUES (nextval('survey.respondents_seq'), ?1, 'psa-preview-1')")
                .setParameter(1, sid).executeUpdate();
        em.createNativeQuery("INSERT INTO survey.respondent_psa (id, respondent_id, post_survey_action_id, status) "
                        + "SELECT nextval('survey.respondent_psa_seq'), r.id, ?2, 'COMPLETED' "
                        + "FROM survey.respondents r WHERE r.survey_id = ?1 AND r.access_code = 'psa-preview-1'")
                .setParameter(1, sid).setParameter(2, first.id).executeUpdate();

        service.remove(sid, first.id);

        assertTrue(service.find(sid, first.id).isEmpty());
        assertEquals(0L, ((Number) em.createNativeQuery(
                        "SELECT count(*) FROM survey.respondent_psa WHERE post_survey_action_id = ?1")
                .setParameter(1, first.id).getSingleResult()).longValue(), "its delivery records go with it");
        assertEquals(second.id, service.actions(sid).stream().filter(a -> a.id.equals(second.id)).findFirst().orElseThrow().id);
        List<Integer> orders = service.actions(sid).stream().map(a -> a.executionOrder).toList();
        assertEquals(java.util.stream.IntStream.rangeClosed(1, orders.size()).boxed().toList(), orders, "renumbered 1..n");
        assertThrows(AuthoringException.class, () -> service.remove(sid, first.id));
    }
}
