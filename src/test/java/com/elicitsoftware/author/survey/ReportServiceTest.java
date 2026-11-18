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
package com.elicitsoftware.author.survey;

import com.elicitsoftware.author.AuthoringException;
import com.elicitsoftware.model.Report;
import com.elicitsoftware.model.Survey;
import com.elicitsoftware.test.PostgresTestResource;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** UC-048 Manage Report Services. */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class ReportServiceTest {

    @Inject
    ReportService service;

    @Inject
    SurveyService surveys;

    private Survey fixture() {
        return Survey.find("name", "AuthorFixture").firstResult();
    }

    @Test
    void fixtureListsTheBuiltInReportFirst() {
        List<Report> rows = service.reports(fixture().id);
        assertEquals(1, rows.size());
        assertTrue(ReportService.isBuiltIn(rows.get(0)), "UC-048 BR-002: built-in is the address");
        assertEquals(1, rows.get(0).displayOrder);
        assertNotNull(rows.get(0).reportKey, "C-017: the key is minted");
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "report.author", roles = {"elicit_author"})
    void create_seedsTheBuiltInRespondentReport() {
        Survey draft = new Survey();
        draft.name = "ReportSeed " + UUID.randomUUID();
        draft.title = "Report seed";
        Survey s = surveys.create(draft);
        List<Report> rows = service.reports(s.id);
        assertEquals(1, rows.size(), "UC-048 BR-001: every new survey starts with the built-in report");
        assertEquals(ReportService.BUILT_IN_RESPONDENT_PATH, rows.get(0).url);
        assertEquals(ReportService.BUILT_IN_RESPONDENT_NAME, rows.get(0).name);
        assertEquals(rows.get(0).id, service.seedBuiltIn(s.id).id, "seeding twice adds nothing");
        assertEquals(1, service.reports(s.id).size());
    }

    @Test
    @TestTransaction
    void add_appendsInOrderAndValidates() {
        int sid = fixture().id;
        Report custom = service.add(sid, " Pedigree ", "A drawing of the family.", "http://host.docker.internal:8082/pedigree/report");
        assertEquals("Pedigree", custom.name, "trimmed");
        assertEquals(2, custom.displayOrder, "UC-048 step 4: at the end");
        assertFalse(ReportService.isBuiltIn(custom));
        assertEquals("name", assertThrows(AuthoringException.class,
                () -> service.add(sid, "Pedigree", "again", "/x")).field(), "A2: name taken");
        assertEquals("name", assertThrows(AuthoringException.class, () -> service.add(sid, " ", "d", "/x")).field());
        assertEquals("description", assertThrows(AuthoringException.class, () -> service.add(sid, "Other", "", "/x")).field());
        assertEquals("url", assertThrows(AuthoringException.class, () -> service.add(sid, "Other", "d", "ftp://x")).field(), "BR-004");
        assertEquals("url", assertThrows(AuthoringException.class, () -> service.add(sid, "Other", "d", "relative/path")).field(), "BR-004");
        assertEquals("url", assertThrows(AuthoringException.class, () -> service.add(sid, "Other", "d", "//host/path")).field(), "BR-004");
        assertEquals("/api/reports/other", service.add(sid, "Other", "d", " /api/reports/other ").url, "BR-004: a path");
    }

    @Test
    @TestTransaction
    void update_keepsTheKey_remove_renumbers_reorder_renumbers() {
        int sid = fixture().id;
        Report builtIn = service.reports(sid).get(0);
        Report second = service.add(sid, "Second", "d", "/api/reports/second");
        Report third = service.add(sid, "Third", "d", "https://example.org/third");
        UUID key = second.reportKey;

        Report edited = service.update(sid, second.id, "Second renamed", "changed", "/api/reports/second-2");
        assertEquals(key, edited.reportKey, "UC-048 step 6 / BR-006: in place, same key");
        assertEquals(2, edited.displayOrder);
        assertEquals("name", assertThrows(AuthoringException.class,
                () -> service.update(sid, second.id, "Third", "d", "/x")).field(), "A2 on edit");

        service.reorder(sid, List.of(third.id, builtIn.id));
        List<Report> rows = service.reports(sid);
        assertEquals(List.of(third.id, builtIn.id, second.id), rows.stream().map(r -> r.id).toList(), "BR-005: named ids first, the rest follow");
        assertEquals(List.of(1, 2, 3), rows.stream().map(r -> r.displayOrder).toList());

        service.remove(sid, builtIn.id);
        rows = service.reports(sid);
        assertEquals(List.of(third.id, second.id), rows.stream().map(r -> r.id).toList(), "A3: the built-in row goes like any other");
        assertEquals(List.of(1, 2), rows.stream().map(r -> r.displayOrder).toList(), "BR-006: renumbered");
        assertThrows(AuthoringException.class, () -> service.remove(sid, builtIn.id));
    }
}
