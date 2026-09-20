package com.elicitsoftware.author.flow.reporting;

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

import com.elicitsoftware.author.flow.SurveyEditorView;
import com.elicitsoftware.author.survey.ReportingService.Assignment;
import com.elicitsoftware.model.Survey;
import com.elicitsoftware.test.PostgresTestResource;
import com.vaadin.browserless.quarkus.QuarkusBrowserlessTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.HasText;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** UC-021..UC-024 on the Reporting page. */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class ReportingViewTest extends QuarkusBrowserlessTest {

    private ReportingView open() {
        Survey s = Survey.find("name", "AuthorFixture").firstResult();
        return navigate(ReportingView.class, Map.of(SurveyEditorView.SURVEY_ID, String.valueOf(s.id)));
    }

    private static String text(Component c) {
        return flatten(c).filter(HasText.class::isInstance).map(x -> ((HasText) x).getText()).collect(Collectors.joining(" "));
    }

    private static Stream<Component> flatten(Component c) {
        return Stream.concat(Stream.of(c), c.getChildren().flatMap(ReportingViewTest::flatten));
    }

    @Test
    @TestSecurity(user = "reporting.author", roles = {"elicit_author"})
    void showsTagsAssignmentsAndTheImpactPreview() {
        ReportingView view = open();
        assertEquals(3, view.tags.getGenericDataView().getItems().count(), "UC-023: the fixture's three tags");
        assertEquals(3, view.assignments.getGenericDataView().getItems().count(), "UC-021: one assignment per scope");
        String impact = text(view.impactPanel);
        assertTrue(impact.contains("dim_fixture_age"), "UC-024 step 2: shared dimension table");
        assertTrue(impact.contains("dim_has_children") && impact.contains("has_children_key"), "UC-024 step 2: own table and column");
        assertFalse(impact.contains("Error:"), "the fixture is clean: " + impact);
        assertEquals(3, view.usesByTag.size(), "every fixture tag is assigned once");
    }

    @Test
    @TestSecurity(user = "reporting.author", roles = {"elicit_author"})
    void selectingATagFiltersTheAssignments() {
        ReportingView view = open();
        view.tags.select(view.tags.getGenericDataView().getItems().filter(o -> o.tag.equals("Gender")).findFirst().orElseThrow());
        assertEquals(1, view.assignments.getGenericDataView().getItems().count());
        Assignment only = view.assignments.getGenericDataView().getItems().findFirst().orElseThrow();
        assertEquals("Female", only.valueLabel(), "UC-021 BR-003: a fixed value");
        view.tags.deselectAll();
        assertEquals(3, view.assignments.getGenericDataView().getItems().count());
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "reporting.author", roles = {"elicit_author"})
    void newTagDialogPreviewsNamesAndRefusesAHyphenWithoutADimension() {
        ReportingView view = open();
        test(view.newTag).click();
        TagDialog dialog = $(TagDialog.class).single();
        assertTrue(dialog.isOpened());
        test(dialog.tag).setValue("Vital Status");
        assertTrue(dialog.preview.getText().contains("dim_vital_status"), "UC-024: preview of the own table");
        assertTrue(dialog.preview.getText().contains("vital_status_key"));
        test(dialog.tag).setValue("Non-Melanoma");
        test(dialog.save).click();
        assertTrue(dialog.isOpened(), "UC-023 A3: refused");
        assertTrue(dialog.tag.isInvalid());
        test(dialog.tag).setValue("Vital Status");
        test(dialog.save).click();
        assertFalse(dialog.isOpened(), "UC-023 step 5: created");
        assertEquals(4, view.tags.getGenericDataView().getItems().count());
        assertTrue(text(view.impactPanel).contains("not assigned"), "UC-024 step 3: a tag without assignments is a warning");
    }

    @Test
    @TestSecurity(user = "reporting.author", roles = {"elicit_author"})
    void editingAnAssignmentGatesSaveBehindTheRetroactiveWarning() {
        ReportingView view = open();
        Assignment gender = view.allAssignments.stream().filter(a -> a.metadata().ontology.tag.equals("Gender")).findFirst().orElseThrow();
        TagAssignmentDialog dialog = new TagAssignmentDialog(view.reporting, view.survey, null, null, gender, m -> { });
        // Opened by hand rather than through a click, so attach it explicitly for the browserless wrappers.
        UI.getCurrent().add(dialog);
        dialog.open();
        assertFalse(dialog.warningShown(), "UC-022 A1: nothing changed yet");
        assertTrue(dialog.save.isEnabled());
        test(dialog.value).setValue("Male");
        assertTrue(dialog.warningShown(), "UC-022 step 4: warning shown");
        assertFalse(dialog.save.isEnabled(), "UC-022 step 4: Save waits for the acknowledgement");
        test(dialog.acknowledge).click();
        assertTrue(dialog.save.isEnabled());
        dialog.close();
    }

    @Test
    @TestSecurity(user = "reporting.admin", roles = {"elicit_admin"})
    void adminRoleAloneCannotOpenTheReportingPage() {
        assertThrows(RuntimeException.class, this::open, "UC-002 BR-001");
    }
}
