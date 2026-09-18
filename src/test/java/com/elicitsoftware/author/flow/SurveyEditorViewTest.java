package com.elicitsoftware.author.flow;

/*-
 * ***LICENSE_START***
 * Elicit Author
 * %%
 * Copyright (C) 2026 The Regents of the University of Michigan - Rogel Cancer Center
 * %%
 * PolyForm Noncommercial License 1.0.0
 * <https://polyformproject.org/licenses/noncommercial/1.0.0>
 * ***LICENSE_END***
 */

import com.elicitsoftware.model.Survey;
import com.elicitsoftware.test.PostgresTestResource;
import com.vaadin.browserless.quarkus.QuarkusBrowserlessTest;
import java.util.Map;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** UC-006 entry, UC-007 panel and UC-008 dialog on the editor view. */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class SurveyEditorViewTest extends QuarkusBrowserlessTest {

    private SurveyEditorView open(String name) {
        Survey s = Survey.find("name", name).firstResult();
        return navigate(SurveyEditorView.class, Map.of(SurveyEditorView.SURVEY_ID, String.valueOf(s.id)));
    }

    @Test
    @TestSecurity(user = "editor.author", roles = {"elicit_author"})
    void showsSurveyAndValidationForTheFixture() {
        SurveyEditorView view = open("AuthorFixture");
        assertEquals("Author Fixture", view.heading.getText());
        assertTrue(view.summary.getText().contains("AuthorFixture"));
        assertTrue(view.issues.stream().noneMatch(i -> i.isError()));
        assertTrue(view.exportButton.isEnabled());
    }

    @Test
    @TestSecurity(user = "editor.author", roles = {"elicit_author"})
    void exportDialogOpensWithReleaseNote() {
        SurveyEditorView view = open("AuthorFixture");
        test(view.exportButton).click();
        ExportDialog dialog = $(ExportDialog.class).single();
        assertTrue(dialog.isOpened());
        assertTrue(dialog.export.isEnabled(), "no errors on the fixture, export allowed");

        test(dialog.export).click();
        assertTrue(dialog.releaseNote.isInvalid(), "UC-008 step 3: a release note is required");
        assertTrue(dialog.outcome.getChildren().findAny().isEmpty(), "nothing exported without a note");
    }

    @Test
    @TestSecurity(user = "editor.author", roles = {"elicit_author"})
    void emptySurveyShowsWarningsButAllowsExport() {
        SurveyEditorView view = open("AuthorFixtureEmpty");
        assertTrue(view.issues.stream().anyMatch(i -> !i.isError()));
        assertTrue(view.exportButton.isEnabled());
    }

    @Test
    @TestSecurity(user = "editor.author", roles = {"elicit_author"})
    void unknownSurveyShowsNotFound() {
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> navigate(SurveyEditorView.class, Map.of(SurveyEditorView.SURVEY_ID, "999999")));
    }
}
