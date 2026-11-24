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
package com.elicitsoftware.author.survey;

import com.elicitsoftware.author.AuthoringException;
import com.elicitsoftware.author.definition.ValidationIssue;
import com.elicitsoftware.author.survey.ReportMarkService.Mark;
import com.elicitsoftware.model.Metadata.ElementRef;
import com.elicitsoftware.model.ReportItem;
import com.elicitsoftware.model.ReportItem.ReportType;
import com.elicitsoftware.model.Survey;
import com.elicitsoftware.test.PostgresTestResource;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** UC-049 Mark Elements for a Report. */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class ReportMarkServiceTest {

    @Inject
    ReportMarkService service;

    @Inject
    SurveyStructureQuery structure;

    @Inject
    EntityManager em;

    private Survey fixture() {
        return Survey.find("name", "AuthorFixture").firstResult();
    }

    private StructureNode node(Survey s, StructureNode.Kind kind, String label) {
        return structure.tree(s.id, false).all().stream()
                .filter(n -> n.kind() == kind && n.label().equals(label)).findFirst().orElseThrow();
    }

    @Test
    void fixtureMarksResolveToElementsAndReports() {
        List<Mark> marks = service.marks(fixture().id);
        assertEquals(2, marks.size());
        // Ordered by report type as Admin writes the file: ADMINISTRATOR before RESPONDENT.
        Mark respondent = marks.get(1);
        assertEquals(ReportType.RESPONDENT, respondent.item().reportType, "ordered by report");
        assertEquals(ElementRef.QUESTION, respondent.ref(), "BR-002: question-wide");
        assertEquals("FixtureQ4", respondent.elementLabel());
        assertEquals("Your age", respondent.shownAs(), "BR-006: the label replaces the text");
        assertFalse(respondent.dangling());
        Mark admin = marks.get(0);
        assertEquals(ReportType.ADMINISTRATOR, admin.item().reportType);
        assertEquals(ElementRef.STEPS_SECTIONS, admin.ref());
        assertEquals("Fixture Section B", admin.elementLabel());
        assertEquals("Fixture Section B", admin.shownAs(), "no label: the element's own text");
        assertTrue(admin.where().contains("Step One"), admin.where());
        assertEquals(1, service.marks(fixture().id, ReportType.RESPONDENT).size());
    }

    @Test
    void marksForANodeCoverBothScopesOfAQuestion() {
        Survey s = fixture();
        StructureNode q4 = node(s, StructureNode.Kind.QUESTION, "FixtureQ4");
        assertEquals(1, service.marksFor(s.id, q4).size(), "a question-wide mark lands on the placement");
        assertEquals(1, service.marksFor(s.id, StructureNode.Kind.QUESTION, q4.durableId()).size());
        assertEquals(0, service.marksFor(s.id, node(s, StructureNode.Kind.QUESTION, "FixtureQ2")).size());
    }

    @Test
    void markable_refusesStepsAndValuelessQuestions() {
        // UC-049 BR-003
        assertFalse(ReportMarkService.markable(new StructureNode(StructureNode.Kind.STEP, 1, null, "s", null, null, null, false, null)));
        assertFalse(ReportMarkService.markable(new StructureNode(StructureNode.Kind.QUESTION, 1, 1, "q", "HTML", null, null, false, null)));
        assertFalse(ReportMarkService.markable(new StructureNode(StructureNode.Kind.QUESTION, 1, 1, "q", "PASSWORD", null, null, false, null)));
        assertTrue(ReportMarkService.markable(new StructureNode(StructureNode.Kind.QUESTION, 1, 1, "q", "TEXT", null, null, false, null)));
        assertTrue(ReportMarkService.markable(new StructureNode(StructureNode.Kind.SECTION, 1, 1, "s", null, null, null, false, null)));
    }

    @Test
    @TestTransaction
    void mark_appendsPerReportAndRefusesDuplicatesAndHtml() {
        Survey s = fixture();
        StructureNode q2 = node(s, StructureNode.Kind.QUESTION, "FixtureQ2");
        ReportItem i = service.mark(s.id, ElementRef.SECTIONS_QUESTION, q2.assignmentId(), ReportType.RESPONDENT, "  Your name ");
        assertNotNull(i.reportItemKey, "C-017: key minted");
        assertEquals(2, i.displayOrder, "step 4: at the end of the respondent report");
        assertEquals("Your name", i.label);
        assertEquals(q2.assignmentId(), i.sectionsQuestionId);
        ReportItem admin = service.mark(s.id, ElementRef.SECTIONS_QUESTION, q2.assignmentId(), ReportType.ADMINISTRATOR, null);
        assertEquals(2, admin.displayOrder, "BR-004: each report has its own order");
        assertNull(admin.label);

        assertEquals("report", assertThrows(AuthoringException.class,
                () -> service.mark(s.id, ElementRef.SECTIONS_QUESTION, q2.assignmentId(), ReportType.RESPONDENT, null)).field(), "A1");
        StructureNode html = node(s, StructureNode.Kind.QUESTION, "FixtureQ1");
        assertEquals("HTML", html.detail());
        assertEquals("scope", assertThrows(AuthoringException.class,
                () -> service.mark(s.id, ElementRef.QUESTION, html.durableId(), ReportType.RESPONDENT, null)).field(), "A2 / BR-003");
        assertEquals("scope", assertThrows(AuthoringException.class,
                () -> service.mark(s.id, ElementRef.QUESTION, 999_999, ReportType.RESPONDENT, null)).field(), "element gone");
        assertEquals("label", assertThrows(AuthoringException.class,
                () -> service.mark(s.id, ElementRef.QUESTION, q2.durableId(), ReportType.RESPONDENT, "x".repeat(256))).field(), "BR-006");
    }

    @Test
    @TestTransaction
    void relabel_unmark_reorder() {
        Survey s = fixture();
        StructureNode q2 = node(s, StructureNode.Kind.QUESTION, "FixtureQ2");
        StructureNode q3 = node(s, StructureNode.Kind.QUESTION, "FixtureQ3");
        ReportItem first = service.marks(s.id, ReportType.RESPONDENT).get(0).item();
        ReportItem second = service.mark(s.id, ElementRef.QUESTION, q2.durableId(), ReportType.RESPONDENT, null);
        ReportItem third = service.mark(s.id, ElementRef.QUESTION, q3.durableId(), ReportType.RESPONDENT, "Children");

        assertEquals("Name", service.relabel(s.id, second.id, "Name").label, "step 6");
        assertNull(service.relabel(s.id, second.id, " ").label, "blank means the element's own text");

        service.reorder(s.id, ReportType.RESPONDENT, List.of(third.id));
        List<Integer> order = service.marks(s.id, ReportType.RESPONDENT).stream().map(m -> m.item().id).toList();
        assertEquals(List.of(third.id, first.id, second.id), order, "BR-004: the named mark first, the rest in their old order");
        assertEquals(1, service.marks(s.id, ReportType.ADMINISTRATOR).get(0).item().displayOrder, "the other report is untouched");

        service.unmark(s.id, first.id);
        List<Mark> left = service.marks(s.id, ReportType.RESPONDENT);
        assertEquals(List.of(third.id, second.id), left.stream().map(m -> m.item().id).toList());
        assertEquals(List.of(1, 2), left.stream().map(m -> m.item().displayOrder).toList(), "BR-005: renumbered");
        assertThrows(AuthoringException.class, () -> service.unmark(s.id, first.id));
    }

    @Test
    @TestTransaction
    void problems_warnAboutAMarkOnARemovedElement() {
        Survey s = fixture();
        assertTrue(service.problems(s.id).isEmpty(), "the fixture is clean");
        em.createNativeQuery("INSERT INTO survey.report_items (id, survey_id, report_type, question_id, display_order) "
                + "VALUES (nextval('survey.report_items_seq'), ?1, 'RESPONDENT', 999999, 9)")
                .setParameter(1, s.id).executeUpdate();
        List<ValidationIssue> problems = service.problems(s.id);
        assertEquals(1, problems.size(), "A3");
        assertFalse(problems.get(0).isError(), "a warning, never an error");
        assertEquals(ReportMarkService.KIND, problems.get(0).kind());
        assertTrue(problems.get(0).message().contains("#999999"), problems.get(0).message());
    }
}
