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

import com.elicitsoftware.author.survey.RuleService;
import com.elicitsoftware.author.survey.StructureNode;
import com.elicitsoftware.author.survey.SurveyStructureQuery;
import com.elicitsoftware.model.ActionType;
import com.elicitsoftware.model.OperatorType;
import com.elicitsoftware.model.Relationship;
import com.elicitsoftware.model.SectionsQuestion;
import com.elicitsoftware.model.Step;
import com.elicitsoftware.model.StructuralElement;
import com.elicitsoftware.model.Survey;
import io.quarkus.narayana.jta.QuarkusTransaction;
import com.elicitsoftware.test.PostgresTestResource;
import com.vaadin.browserless.quarkus.QuarkusBrowserlessTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.RouterLink;
import java.util.Locale;
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

    @Inject
    RuleService rules;

    /**
     * The fixture's step two has no rule of its own, which UC-007 BR-012 reports as an error. This
     * gives it the usual one -- Field exists on the welcome → Show step two -- in its own transaction,
     * since a browserless view reads committed state, and returns the rule for {@link #unbuild}.
     */
    private Integer buildStepTwo() {
        return QuarkusTransaction.requiringNew().call(() -> {
            Survey s = Survey.find("name", "AuthorFixture").firstResult();
            Relationship draft = new Relationship();
            draft.upstreamSqId = SectionsQuestion.<SectionsQuestion>find("questionId = (select q.questionId from Question q where q.shortText = ?1 and q.surveyId = ?2 and q.effectiveTo = ?3) and effectiveTo = ?3",
                    "FixtureQ1", s.id, StructuralElement.SENTINEL).firstResult().sectionsQuestionId;
            draft.upstreamStepId = step(s, "Fixture Step One");
            draft.downstreamStepId = step(s, "Fixture Step Two");
            draft.operatorType = OperatorType.find("name", "FIELD_EXIST").firstResult();
            draft.actionType = ActionType.find("name", "SHOW").firstResult();
            return rules.create(s.id, draft).relationshipId;
        });
    }

    private static Integer step(Survey s, String name) {
        return Step.<Step>find("surveyId = ?1 and name = ?2 and effectiveTo = ?3", s.id, name, StructuralElement.SENTINEL).firstResult().stepId;
    }

    /** Removes the rule outright, so no other test sees the fixture with a second rule. */
    private void unbuild(Integer relationshipId) {
        QuarkusTransaction.requiringNew().run(() -> em.createNativeQuery("DELETE FROM survey.relationships WHERE relationship_id = ?1")
                .setParameter(1, relationshipId).executeUpdate());
    }

    private SurveyEditorView open(String name) {
        Survey s = Survey.find("name", name).firstResult();
        return navigate(SurveyEditorView.class, Map.of(SurveyEditorView.SURVEY_ID, String.valueOf(s.id)));
    }

    @Test
    @TestSecurity(user = "editor.author", roles = {"elicit_author"})
    void showsSurveyAndValidationForTheFixture() {
        Integer rule = buildStepTwo();
        try {
            SurveyEditorView view = open("AuthorFixture");
            assertEquals("Author Fixture", view.heading.getText());
            assertTrue(view.summary.getText().contains("AuthorFixture"));
            assertTrue(view.issues.stream().noneMatch(i -> i.isError()), view.issues.toString());
            assertTrue(view.exportButton.isEnabled());
        } finally {
            unbuild(rule);
        }
    }

    /**
     * UC-007 BR-013 / FR-066: a finding is worded in the language of the author reading it. The
     * fixture's step two has no rule (BR-012), so the panel has that error to say in Spanish.
     */
    @Test
    @TestSecurity(user = "editor.author", roles = {"elicit_author"})
    void findingsAreInTheAuthorsLanguage() {
        Locale before = UI.getCurrent().getLocale();
        UI.getCurrent().getSession().setLocale(Locale.forLanguageTag("es-419"));
        UI.getCurrent().setLocale(Locale.forLanguageTag("es-419"));
        try {
            SurveyEditorView view = open("AuthorFixture");
            assertTrue(view.issues.stream().anyMatch(i -> i.isError()
                            && i.message().startsWith("El paso 'Fixture Step Two' no tiene una regla Mostrar o Repetir.")),
                    view.issues.toString());
            assertTrue(view.issues.stream().noneMatch(i -> i.message().contains("Step 'Fixture Step Two'")),
                    "no English finding is left beside it: " + view.issues);
        } finally {
            UI.getCurrent().getSession().setLocale(before);
            UI.getCurrent().setLocale(before);
        }
    }

    @Test
    @TestSecurity(user = "editor.author", roles = {"elicit_author"})
    void exportDialogOpensWithReleaseNote() {
        Integer rule = buildStepTwo();
        try {
            SurveyEditorView view = open("AuthorFixture");
            test(view.exportButton).click();
            ExportDialog dialog = find(ExportDialog.class).single();
            assertTrue(dialog.isOpened());
            assertTrue(dialog.export.isEnabled(), "no errors on the fixture, export allowed");

            test(dialog.export).click();
            assertTrue(dialog.releaseNote.isInvalid(), "UC-008 step 3: a release note is required");
            assertTrue(dialog.outcome.getChildren().findAny().isEmpty(), "nothing exported without a note");
        } finally {
            unbuild(rule);
        }
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
        com.elicitsoftware.author.flow.reporting.ReportingView reporting = find(com.elicitsoftware.author.flow.reporting.ReportingView.class).single();
        assertEquals(view.survey.id, reporting.survey.id, "UC-023 step 1: the survey's Reporting page");
    }

    @Test
    @TestSecurity(user = "editor.author", roles = {"elicit_author"})
    void reportsButtonOpensTheReportsPage() {
        SurveyEditorView view = open("AuthorFixture");
        test(view.reports).click();
        com.elicitsoftware.author.flow.reports.ReportsView reports = $(com.elicitsoftware.author.flow.reports.ReportsView.class).single();
        assertEquals(view.survey.id, reports.survey.id, "UC-048 step 1: the survey's Reports page");
    }

    @Test
    @TestSecurity(user = "editor.author", roles = {"elicit_author"})
    void postSurveyActionsButtonOpensThePostSurveyActionsPage() {
        SurveyEditorView view = open("AuthorFixture");
        test(view.postSurveyActions).click();
        com.elicitsoftware.author.flow.actions.PostSurveyActionsView actions =
                $(com.elicitsoftware.author.flow.actions.PostSurveyActionsView.class).single();
        assertEquals(view.survey.id, actions.survey.id, "UC-052 step 1: the survey's Post-survey actions page");
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
                .filter(l -> l.getText().contains("is empty"))
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
