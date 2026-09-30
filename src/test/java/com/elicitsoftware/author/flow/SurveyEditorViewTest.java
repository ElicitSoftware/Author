package com.elicitsoftware.author.flow;

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

import com.elicitsoftware.author.survey.StructureNode;
import com.elicitsoftware.author.survey.SurveyStructureQuery;
import com.elicitsoftware.model.Survey;
import com.elicitsoftware.test.PostgresTestResource;
import com.vaadin.browserless.quarkus.QuarkusBrowserlessTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.RouterLink;
import java.util.Map;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** UC-006 entry, UC-007 panel and UC-008 dialog on the editor view. */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class SurveyEditorViewTest extends QuarkusBrowserlessTest {

    @Inject
    EntityManager em;

    @Inject
    SurveyStructureQuery structure;

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
    void reportingButtonOpensTheReportingPage() {
        SurveyEditorView view = open("AuthorFixture");
        test(view.reporting).click();
        com.elicitsoftware.author.flow.reporting.ReportingView reporting = $(com.elicitsoftware.author.flow.reporting.ReportingView.class).single();
        assertEquals(view.survey.id, reporting.survey.id, "UC-023 step 1: the survey's Reporting page");
    }

    /** UC-007 steps 5-6 / FR-061: a finding is a link to its element and says where that element sits. */
    @Test
    @TestTransaction
    @TestSecurity(user = "editor.author", roles = {"elicit_author"})
    void aFindingLinksToItsElementAndSaysWhereItSits() {
        Survey s = Survey.find("name", "AuthorFixture").firstResult();
        StructureNode sectionC = structure.tree(s.id, false).all().stream()
                .filter(n -> n.kind() == StructureNode.Kind.SECTION && "Fixture Section C".equals(n.label()))
                .findFirst().orElseThrow();
        em.createNativeQuery("UPDATE survey.sections_questions sq SET effective_to = now() FROM survey.questions q "
                        + "WHERE sq.question_id = q.question_id AND q.short_text = 'FixtureQ5' AND sq.survey_id = ?1")
                .setParameter(1, s.id).executeUpdate();
        em.flush();
        em.clear();

        SurveyEditorView view = open("AuthorFixture");
        RouterLink link = view.findings.getChildren().flatMap(Component::getChildren)
                .filter(RouterLink.class::isInstance).map(RouterLink.class::cast)
                .filter(l -> l.getText().contains("has no questions"))
                .findFirst().orElseThrow(() -> new AssertionError("no link for the empty section: " + view.issues));

        assertTrue(link.getHref().contains("/design"), link.getHref());
        assertEquals("section:" + sectionC.durableId(),
                link.getQueryParameters().orElseThrow().getSingleParameter("select").orElseThrow());
        assertTrue(view.findings.getChildren().flatMap(Component::getChildren)
                        .filter(Span.class::isInstance).map(Component::getElement)
                        .anyMatch(e -> e.getText().contains("Fixture Step Two")),
                "UC-007 BR-008: the finding names the step the section sits in");
    }

    @Test
    @TestSecurity(user = "editor.author", roles = {"elicit_author"})
    void aFindingWithNothingToOpenStaysPlainText() {
        SurveyEditorView view = open("AuthorFixtureEmpty");
        assertTrue(view.issues.stream().anyMatch(i -> "surveys".equals(i.kind())), view.issues.toString());
        assertTrue(view.findings.getChildren().flatMap(Component::getChildren)
                        .noneMatch(RouterLink.class::isInstance),
                "UC-007 BR-009: a survey-wide finding has no element to open");
    }

    @Test
    @TestSecurity(user = "editor.author", roles = {"elicit_author"})
    void unknownSurveyShowsNotFound() {
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> navigate(SurveyEditorView.class, Map.of(SurveyEditorView.SURVEY_ID, "999999")));
    }
}
