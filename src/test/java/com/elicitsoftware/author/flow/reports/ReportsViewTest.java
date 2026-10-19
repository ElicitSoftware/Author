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
package com.elicitsoftware.author.flow.reports;

import com.elicitsoftware.author.flow.SurveyEditorView;
import com.elicitsoftware.author.survey.ReportMarkService.Mark;
import com.elicitsoftware.author.survey.ReportService;
import com.elicitsoftware.author.survey.StructureNode;
import com.elicitsoftware.model.Report;
import com.elicitsoftware.model.ReportItem.ReportType;
import com.elicitsoftware.model.Survey;
import com.elicitsoftware.test.PostgresTestResource;
import com.vaadin.browserless.quarkus.QuarkusBrowserlessTest;
import com.vaadin.flow.component.UI;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** UC-048 and UC-049 on the Reports page. */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class ReportsViewTest extends QuarkusBrowserlessTest {

    private ReportsView open() {
        Survey s = Survey.find("name", "AuthorFixture").firstResult();
        return navigate(ReportsView.class, Map.of(SurveyEditorView.SURVEY_ID, String.valueOf(s.id)));
    }

    private static List<Report> services(ReportsView view) {
        return view.services.getGenericDataView().getItems().toList();
    }

    private static List<Mark> marks(ReportsView view) {
        return view.marksGrid.getGenericDataView().getItems().toList();
    }

    @Test
    @TestSecurity(user = "reports.author", roles = {"elicit_author"})
    void showsTheBuiltInReportAndEachReportsMarks() {
        ReportsView view = open();
        List<Report> rows = services(view);
        assertEquals(1, rows.size(), "UC-048 A1: the fixture lists the built-in report");
        assertTrue(ReportService.isBuiltIn(rows.get(0)));
        assertTrue(view.services.isVisible());
        assertFalse(view.servicesEmpty.isVisible());

        assertTrue(view.preview.isVisible(), "UC-048 A5: the test profile sets author.preview.survey.url");
        assertTrue(view.preview.getHref().startsWith("http"), view.preview.getHref());
        assertEquals("_blank", view.preview.getTarget().orElse(""));
        assertEquals(ReportType.RESPONDENT, view.shownReport, "the Respondent tab opens first");
        List<Mark> respondent = marks(view);
        assertEquals(1, respondent.size());
        assertEquals("FixtureQ4", respondent.get(0).elementLabel());
        assertEquals("Your age", respondent.get(0).shownAs(), "UC-049 BR-006: the label is what the report shows");

        view.tabs.setSelectedTab(view.administratorTab);
        assertEquals(ReportType.ADMINISTRATOR, view.shownReport);
        List<Mark> administrator = marks(view);
        assertEquals(1, administrator.size(), "UC-049 BR-004: the other report's marks");
        assertEquals("Fixture Section B", administrator.get(0).elementLabel());
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "reports.author", roles = {"elicit_author"})
    void addServiceDialogAddsARowAndTheRowsCanBeReorderedAndRemoved() {
        ReportsView view = open();
        test(view.addService).click();
        ReportServiceDialog dialog = $(ReportServiceDialog.class).single();
        assertTrue(dialog.isOpened());
        test(dialog.name).setValue("Pedigree");
        test(dialog.description).setValue("The family tree.");
        test(dialog.url).setValue("ftp://nowhere");
        test(dialog.save).click();
        assertTrue(dialog.url.isInvalid(), "UC-048 BR-004: a path or an http(s) address");
        test(dialog.url).setValue("https://example.org/pedigree/report");
        test(dialog.save).click();
        assertFalse(dialog.isOpened());
        List<Report> rows = services(view);
        assertEquals(2, rows.size(), "UC-048 step 4");
        assertEquals("Pedigree", rows.get(1).name);

        view.moveService(rows.get(1), rows.get(0), true);
        rows = services(view);
        assertEquals("Pedigree", rows.get(0).name, "UC-048 step 5: dragged above the built-in report");
        assertEquals(List.of(1, 2), rows.stream().map(r -> r.displayOrder).toList());

        view.reports.remove(view.survey.id, rows.get(1).id);
        view.load();
        rows = services(view);
        assertEquals(1, rows.size(), "UC-048 A3: the built-in report goes like any other");
        assertFalse(ReportService.isBuiltIn(rows.get(0)));
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "reports.author", roles = {"elicit_author"})
    void markElementDialogMarksAndTheLabelCanBeEdited() {
        ReportsView view = open();
        test(view.markElement).click();
        ReportMarkDialog dialog = $(ReportMarkDialog.class).single();
        assertTrue(dialog.isOpened());
        assertTrue(dialog.element.getGenericDataView().getItems().noneMatch(n -> "FixtureQ1".equals(n.label())),
                "UC-049 BR-003: the HTML question is not offered");
        assertTrue(dialog.disclosureShown(), "UC-049 A4: the respondent report is as private as the access code");
        StructureNode q2 = dialog.element.getGenericDataView().getItems().filter(n -> "FixtureQ2".equals(n.label())).findFirst().orElseThrow();
        test(dialog.element).selectItem(dialog.element.getItemLabelGenerator().apply(q2));
        assertTrue(dialog.scope.isVisible(), "UC-049 BR-002: a question offers its scopes");
        test(dialog.report).selectItem(dialog.report.getItemLabelGenerator().apply(ReportType.ADMINISTRATOR));
        assertFalse(dialog.disclosureShown());
        test(dialog.label).setValue("Name");
        test(dialog.save).click();
        assertFalse(dialog.isOpened());

        view.tabs.setSelectedTab(view.administratorTab);
        List<Mark> rows = marks(view);
        assertEquals(2, rows.size(), "UC-049 step 4: at the end of the administrator report");
        assertEquals("Name", rows.get(1).shownAs());
        assertEquals(2, rows.get(1).item().displayOrder);

        ReportMarkDialog relabel = new ReportMarkDialog(view.marks, view.survey, null, null, rows.get(1), saved -> view.load());
        UI.getCurrent().add(relabel);
        relabel.open();
        assertFalse(relabel.scope.isVisible(), "UC-049 step 6: only the label is edited");
        assertTrue(relabel.report.isReadOnly());
        test(relabel.label).setValue("Full name");
        test(relabel.save).click();
        assertEquals("Full name", marks(view).get(1).shownAs());

        view.moveMark(marks(view).get(1), marks(view).get(0), true);
        rows = marks(view);
        assertEquals("Full name", rows.get(0).shownAs(), "UC-049 step 5: dragged first");
        assertEquals(List.of(1, 2), rows.stream().map(m -> m.item().displayOrder).toList());
        view.tabs.setSelectedTab(view.respondentTab);
        assertEquals(1, marks(view).size(), "UC-049 BR-004: the respondent report is untouched");
    }

    @Test
    @TestSecurity(user = "reports.admin", roles = {"elicit_admin"})
    void adminRoleAloneCannotOpenTheReportsPage() {
        assertThrows(RuntimeException.class, this::open, "UC-002 BR-001");
    }
}
