package com.elicitsoftware.author.flow.translations;

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
import com.elicitsoftware.author.survey.SurveyService;
import com.elicitsoftware.author.survey.TranslationHandoff;
import com.elicitsoftware.author.survey.TranslationService.Status;
import com.elicitsoftware.model.Survey;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.elicitsoftware.test.PostgresTestResource;
import com.vaadin.browserless.quarkus.QuarkusBrowserlessTest;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC-043 step 2 on the Translations page: the picker offers languages the survey does not publish
 * yet, and choosing one publishes it (UC-044 step 4).
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class TranslationsViewTest extends QuarkusBrowserlessTest {

    @Inject
    SurveyService surveys;

    @Inject
    TranslationHandoff handoff;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Survey survey(String contentLanguages) {
        Survey bean = new Survey();
        bean.name = "TV " + UUID.randomUUID();
        bean.title = "Translations view fixture";
        bean.description = "Something for the grid to list.";
        bean.contentLanguages = contentLanguages;
        return surveys.create(bean);
    }

    private TranslationsView open(Survey s) {
        return navigate(TranslationsView.class,
                Map.of(SurveyEditorView.SURVEY_ID, String.valueOf(s.id)));
    }

    /** The full label as the dropdown sends it, e.g. {@code fr — French · not published yet}. */
    private String suggestionFor(TranslationsView view, String tag) {
        return test(view.language).getSuggestions().stream()
                .filter(label -> label.startsWith(tag + " "))
                .findFirst().orElseThrow(() -> new AssertionError(
                        tag + " is not offered; offered: " + test(view.language).getSuggestions()));
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "translations.author", roles = {"elicit_author"})
    void aSurveyThatPublishesNothingStillGetsAPicker() {
        TranslationsView view = open(survey(null));

        List<String> offered = test(view.language).getSuggestionItems();
        assertEquals(List.of("ar", "es-419"), offered,
                "the curated set, less the base language");
        assertFalse(offered.contains("en"), "UC-044 BR-005: the base language is not a target");
        assertFalse(offered.contains("fr"), "UC-044 BR-004: Elicit carries no French");

        assertEquals(view.getTranslation("translationsView.language.option", "ar", "Arabic"),
                suggestionFor(view, "ar"),
                "with nothing published, the 'not published yet' note would sit on every row alike");
        assertNull(view.language.getValue(), "nothing to preselect");
        assertEquals(view.getTranslation("translationsView.noLanguages"), view.counts.getText(),
                "UC-043 A4: an invitation rather than the dead end it replaced");
        assertFalse(view.request.isVisible(), "nothing to hand a translator yet");
        assertEquals(0, view.grid.getGenericDataView().getItems().count());
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "translations.author", roles = {"elicit_author"})
    void aPublishedLanguageIsPreselectedAndItsStringsListed() {
        TranslationsView view = open(survey("es-419"));

        assertEquals("es-419", view.language.getValue());
        assertTrue(view.request.isVisible());
        assertTrue(view.grid.getGenericDataView().getItems().count() > 0, "UC-043 step 3");
        assertTrue(suggestionFor(view, "es-419").contains("Spanish"),
                "named, not just tagged: " + suggestionFor(view, "es-419"));
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "translations.author", roles = {"elicit_author"})
    void choosingAnUnpublishedLanguagePublishesItAndShowsItsStrings() {
        Survey s = survey("ar");
        TranslationsView view = open(s);
        String spanish = suggestionFor(view, "es-419");
        assertEquals(view.getTranslation("translationsView.language.option.new", "es-419", "Spanish (Latin America)"), spanish,
                "offered, and marked as one the survey does not publish yet");
        assertEquals(view.getTranslation("translationsView.language.option", "ar", "Arabic"),
                suggestionFor(view, "ar"), "while a published one carries no note");

        test(view.language).selectItem(spanish);

        assertEquals("es-419", view.language.getValue());
        assertEquals("ar,es-419", Survey.<Survey>findById(s.id).contentLanguages,
                "UC-044 step 4: recorded with the survey, beside the one it already published");
        assertTrue(view.publishedTags.contains("es-419"));
        assertTrue(view.request.isVisible(), "UC-045 is reachable straight away");
        assertTrue(view.grid.getGenericDataView().getItems().count() > 0);
        assertEquals(view.getTranslation("translationsView.language.option", "es-419", "Spanish (Latin America)"),
                suggestionFor(view, "es-419"), "the label drops the 'not published yet' note");
    }

    /**
     * UC-046 through the page: the bytes the upload handler collects go straight to the import,
     * the outcome dialog opens and the grid reloads. The handler itself is Vaadin's in-memory one
     * and is not simulated here; what the view does with the file is.
     */
    @Test
    @TestTransaction
    @TestSecurity(user = "translations.author", roles = {"elicit_author"})
    void aReturnedFileIsImportedFromItsBytesAndTheGridReloaded() throws Exception {
        Survey s = survey("es-419");
        TranslationsView view = open(s);
        assertTrue(view.rows.stream().allMatch(r -> r.status() == Status.MISSING), "nothing translated yet");

        ObjectNode file = (ObjectNode) MAPPER.readTree(handoff.export(s, "es-419", false));
        for (JsonNode item : file.path("items")) {
            ((ObjectNode) item).put("translation", "ES: " + item.path("source").asText());
        }

        view.importReturned(MAPPER.writeValueAsBytes(file));

        assertTrue(find(TranslationImportResultDialog.class).single().isOpened(), "the outcome is reported");
        assertTrue(view.rows.stream().allMatch(r -> r.status() == Status.TRANSLATED),
                "the grid reloaded with the imported translations");
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "translations.author", roles = {"elicit_author"})
    void aTypedTagElicitDoesNotCarryIsRefused() {
        Survey s = survey("ar");
        TranslationsView view = open(s);
        assertFalse(test(view.language).getSuggestionItems().contains("fr"), "not one of the curated set");

        view.publish("  FR  ", "ar");

        assertEquals("ar", view.language.getValue(), "the previous selection is restored");
        assertEquals("ar", Survey.<Survey>findById(s.id).contentLanguages, "set unchanged");
        assertFalse(test(view.language).getSuggestionItems().contains("fr"),
                "custom values stay allowed only so a typed tag can be refused, not accepted");
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "translations.author", roles = {"elicit_author"})
    void aTypedTagThatIsNotALanguageIsRefusedAndNothingIsPublished() {
        Survey s = survey("ar");
        TranslationsView view = open(s);

        view.publish("zzz", "ar");

        assertEquals("ar", view.language.getValue(), "the previous language is restored");
        assertEquals("ar", Survey.<Survey>findById(s.id).contentLanguages, "set unchanged");
        assertFalse(test(view.language).getSuggestionItems().contains("zzz"));
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "translations.author", roles = {"elicit_author"})
    void theBaseLanguageIsRefusedWhenTyped() {
        Survey s = survey("ar");
        TranslationsView view = open(s);

        view.publish("en", "ar");

        assertEquals("ar", view.language.getValue());
        assertEquals("ar", Survey.<Survey>findById(s.id).contentLanguages);
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "translations.author", roles = {"elicit_author"})
    void typingALanguageTheSurveyAlreadyPublishesJustSelectsIt() {
        Survey s = survey("ar,es-419");
        TranslationsView view = open(s);

        view.publish("es-419", "ar");

        assertEquals("es-419", view.language.getValue());
        assertEquals("ar,es-419", Survey.<Survey>findById(s.id).contentLanguages, "set unchanged");
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "translations.author", roles = {"elicit_author"})
    void aPublishedTagElicitDoesNotCarryIsStillOfferedAsUnsupported() {
        Survey s = survey("ar");
        // Seeded on the column directly: publishLanguage would refuse "fr" (UC-044 BR-004), but an
        // imported definition, or a survey published before the set was curated, can already carry it.
        s.contentLanguages = "ar,fr";

        TranslationsView view = open(s);

        assertTrue(test(view.language).getSuggestionItems().contains("fr"),
                "UC-044 BR-004: kept in the list so it can be seen and taken back out");
        assertEquals(view.getTranslation("translationsView.language.option.unsupported", "fr", "French"),
                suggestionFor(view, "fr"),
                "checked before the published/new distinction, so an unsupported tag never reads as new");
    }

    @Test
    @TestSecurity(user = "translations.admin", roles = {"elicit_admin"})
    void adminRoleAloneCannotOpenTheTranslationsPage() {
        Survey fixture = Survey.find("name", "AuthorFixture").firstResult();
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> open(fixture), "UC-002 BR-001");
    }
}
