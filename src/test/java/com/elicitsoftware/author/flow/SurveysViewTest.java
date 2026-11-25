package com.elicitsoftware.author.flow;

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

import com.elicitsoftware.author.survey.SurveyService;
import com.elicitsoftware.model.Survey;
import com.elicitsoftware.test.PostgresTestResource;
import com.vaadin.browserless.quarkus.QuarkusBrowserlessTest;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.textfield.TextField;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** UC-003 / UC-004 through the Surveys view. */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class SurveysViewTest extends QuarkusBrowserlessTest {

    @Inject
    EntityManager em;

    @Inject
    SurveyService surveys;

    @Test
    @TestSecurity(user = "view.author", roles = {"elicit_author"})
    void listsFixtureSurveys() {
        SurveysView view = navigate(SurveysView.class);
        var names = view.grid.getGenericDataView().getItems().map(s -> s.name).toList();
        assertTrue(names.contains("AuthorFixture"));
        assertTrue(names.contains("AuthorFixtureEmpty"));
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "view.author", roles = {"elicit_author"})
    void newSurveyDialogValidatesAndCreates() {
        SurveysView view = navigate(SurveysView.class);
        test(view.newSurvey).click();
        SurveyMetadataDialog dialog = find(SurveyMetadataDialog.class).single();
        assertFalse(dialog.baseLanguage.isVisible(), "UC-003 BR-006: no language is asked for");

        // Empty submit: binder blocks, dialog stays open.
        test(dialog.save).click();
        assertTrue(dialog.isOpened(), "required fields block the save");
        assertTrue(dialog.name.isInvalid());

        test(dialog.name).setValue("UC003 via view");
        test(dialog.title).setValue("Created through the dialog");
        test(dialog.save).click();

        assertFalse(dialog.isOpened(), "dialog closes after a successful save");
        Survey created = Survey.find("name", "UC003 via view").firstResult();
        assertEquals("view.author", created.publishedBy);
        assertEquals("en", created.baseLanguage, "UC-003 BR-006: written in en");
        assertNull(created.contentLanguages, "UC-003 BR-006: published in nothing else");
        assertTrue(view.grid.getGenericDataView().getItems().anyMatch(s -> s.id.equals(created.id)));
    }

    private Survey publishing(String name, String published) {
        Survey bean = new Survey();
        bean.name = name;
        bean.title = name;
        bean.contentLanguages = published;
        return surveys.create(bean);
    }

    private SurveyMetadataDialog editDialog(Survey s) {
        SurveyEditorView editor = navigate(SurveyEditorView.class,
                Map.of(SurveyEditorView.SURVEY_ID, String.valueOf(s.id)));
        test(editor.editDetails).click();
        return find(SurveyMetadataDialog.class).single();
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "view.author", roles = {"elicit_author"})
    void editShowsTheBaseLanguageAndKeepsThePublishedSet() {
        Survey s = publishing("UC004 keeps set", "ar");
        SurveyMetadataDialog dialog = editDialog(s);
        assertTrue(dialog.baseLanguage.isVisible(), "UC-004 step 2");
        assertEquals("en", dialog.baseLanguage.getValue());

        test(dialog.title).setValue("Retitled");
        test(dialog.save).click();

        assertNull(dialog.baseLanguageWarning, "UC-004 A4: the base language was not changed");
        assertFalse(dialog.isOpened());
        Survey stored = Survey.findById(s.id);
        assertEquals("Retitled", stored.title);
        assertEquals("ar", stored.contentLanguages, "UC-004 BR-005: the published set is left as stored");
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "view.author", roles = {"elicit_author"})
    void changingTheBaseLanguageWarnsBeforeSaving() {
        Survey s = publishing("UC004 A4", null);
        SurveyMetadataDialog dialog = editDialog(s);

        test(dialog.baseLanguage).setValue("es");
        test(dialog.save).click();

        assertTrue(dialog.baseLanguageWarning.isOpened(), "UC-044 A3: warned first");
        assertTrue(dialog.isOpened(), "the details stay open behind the warning");
        assertEquals("en", Survey.<Survey>findById(s.id).baseLanguage, "nothing stored before confirming");

        ConfirmDialog warning = dialog.baseLanguageWarning;
        ComponentUtil.fireEvent(warning, new ConfirmDialog.ConfirmEvent(warning, true));

        assertFalse(dialog.isOpened());
        assertEquals("es", Survey.<Survey>findById(s.id).baseLanguage);
    }

    @Test
    void retypingTheBaseLanguageIsNotAChange() {
        assertFalse(SurveyMetadataDialog.baseLanguageChanged("en", "EN"), "compared in canonical form");
        assertFalse(SurveyMetadataDialog.baseLanguageChanged("en", " "), "blank is stored as en");
        assertFalse(SurveyMetadataDialog.baseLanguageChanged(null, "en"));
        assertTrue(SurveyMetadataDialog.baseLanguageChanged("en", "es-419"));
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "view.author", roles = {"elicit_author"})
    void duplicateNameIsShownOnTheNameField() {
        SurveysView view = navigate(SurveysView.class);
        test(view.newSurvey).click();
        SurveyMetadataDialog dialog = find(SurveyMetadataDialog.class).single();
        test(dialog.name).setValue("AuthorFixture");
        test(dialog.title).setValue("dup");
        test(dialog.save).click();
        assertTrue(dialog.isOpened());
        TextField name = dialog.name;
        assertTrue(name.isInvalid(), "UC-003 A2: name already in use");
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "view.author", roles = {"elicit_author"})
    void deleteIsGuardedByTypingTheNameAndOffersABackup() {
        SurveysView view = navigate(SurveysView.class);
        Survey empty = Survey.find("name", "AuthorFixtureEmpty").firstResult();
        view.confirmDelete(empty);
        DeleteSurveyDialog dialog = find(DeleteSurveyDialog.class).single();

        assertFalse(dialog.delete.isEnabled(), "UC-047 BR-002: disabled until the name is typed");
        assertFalse(dialog.previewAnswers.isVisible(), "UC-047 A4: nothing said of a preview never taken");
        test(dialog.confirmName).setValue("AuthorFixture");
        assertFalse(dialog.delete.isEnabled(), "a different survey's name does not unlock it");

        // UC-047 step 4: the backup is a download link, offered before anything is typed.
        assertTrue(dialog.backup.getHref() != null && !dialog.backup.getHref().isBlank());

        test(dialog.confirmName).setValue("AuthorFixtureEmpty");
        assertTrue(dialog.delete.isEnabled());
        test(dialog.delete).click();

        assertFalse(dialog.isOpened());
        assertTrue(Survey.findByIdOptional(empty.id).isEmpty());
        assertFalse(view.grid.getGenericDataView().getItems().anyMatch(s -> s.id.equals(empty.id)));
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "view.author", roles = {"elicit_author"})
    void deleteSaysHowManyPreviewRespondentsGoAndRemovesThem() {
        SurveysView view = navigate(SurveysView.class);
        Survey empty = Survey.find("name", "AuthorFixtureEmpty").firstResult();
        em.createNativeQuery("""
                        INSERT INTO survey.respondents (id, survey_id, access_code)
                        VALUES (nextval('survey.respondents_seq'), ?1, 'UC047')
                        """)
                .setParameter(1, empty.id).executeUpdate();
        view.confirmDelete(empty);
        DeleteSurveyDialog dialog = find(DeleteSurveyDialog.class).single();

        // UC-047 A4 / BR-004: stated before the name is typed, and no longer a refusal.
        assertTrue(dialog.previewAnswers.isVisible());
        assertTrue(dialog.previewAnswers.getText().contains("1"), dialog.previewAnswers.getText());

        test(dialog.confirmName).setValue("AuthorFixtureEmpty");
        test(dialog.delete).click();

        assertFalse(dialog.isOpened(), "the delete goes through");
        assertTrue(Survey.findByIdOptional(empty.id).isEmpty());
    }

    @Test
    @TestSecurity(user = "view.nobody", roles = {})
    void requiresAuthorRole() {
        // UC-002 A1: without elicit_author the route is not reachable; the access checker
        // reroutes to the access-denied error view instead of rendering SurveysView.
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> navigate(SurveysView.class));
    }
}
