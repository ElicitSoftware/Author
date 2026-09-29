package com.elicitsoftware.author.survey;

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

import com.elicitsoftware.author.AuthoringException;
import com.elicitsoftware.model.Survey;
import com.elicitsoftware.test.PostgresTestResource;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** UC-003 Create Survey, UC-004 Edit Survey Metadata, UC-008 step 4 (export attribution). */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class SurveyServiceTest {

    @Inject
    SurveyService service;

    private static Survey draft(String name) {
        Survey s = new Survey();
        s.name = name;
        s.title = name + " title";
        s.description = "  described  ";
        return s;
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "author.one", roles = {"elicit_author"})
    void create_mintsKeyAppendsOrderAndRecordsAuthor() {
        int before = service.nextDisplayOrder();
        Survey created = service.create(draft("UC003 created"));

        assertNotNull(created.id);
        assertNotNull(created.surveyKey, "UC-003 BR-001: key minted at creation");
        assertEquals(before, created.displayOrder, "UC-003 BR-003: appended after existing surveys");
        assertEquals("author.one", created.publishedBy, "UC-003 BR-004: last editor recorded");
        assertEquals("described", created.description, "whitespace trimmed");
        assertNull(created.publishedComment);
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "author.one", roles = {"elicit_author"})
    void create_keepsASuppliedKeyVerbatim() {
        UUID key = UUID.randomUUID();
        Survey d = draft("UC003 keyed");
        d.surveyKey = key;
        assertEquals(key, service.create(d).surveyKey);
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "author.one", roles = {"elicit_author"})
    void create_rejectsMissingNameWithFieldMessage() {
        Survey d = draft("x");
        d.name = "   ";
        AuthoringException ex = assertThrows(AuthoringException.class, () -> service.create(d));
        assertEquals("name", ex.field());
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "author.one", roles = {"elicit_author"})
    void create_rejectsDuplicateNameAsFieldError() {
        AuthoringException ex = assertThrows(AuthoringException.class, () -> service.create(draft("AuthorFixture")));
        assertEquals("name", ex.field(), "UC-003 A2 / BR-002: name unique within Author");
        assertTrue(ex.getMessage().toLowerCase().contains("name"));
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "author.two", roles = {"elicit_author"})
    void updateMetadata_editsInPlaceAndKeepsKeyAndOrder() {
        Survey created = service.create(draft("UC004 before"));
        UUID key = created.surveyKey;
        int order = created.displayOrder;

        Survey edit = draft("UC004 after");
        edit.postSurveyUrl = "https://example.org/done";
        Survey updated = service.updateMetadata(created.id, edit);

        assertEquals(created.id, updated.id);
        assertEquals(key, updated.surveyKey, "UC-004 BR-002: key immutable");
        assertEquals(order, updated.displayOrder, "list position untouched");
        assertEquals("UC004 after", updated.name);
        assertEquals("https://example.org/done", updated.postSurveyUrl);
        assertEquals("author.two", updated.publishedBy);
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "author.one", roles = {"elicit_author"})
    void updateMetadata_unknownSurveyIsReported() {
        assertThrows(AuthoringException.class, () -> service.updateMetadata(-1, draft("nope")));
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "exporter", roles = {"elicit_author"})
    void stampExport_recordsAuthorAndReleaseNote() {
        Survey created = service.create(draft("UC008 stamp"));
        Survey stamped = service.stampExport(created.id, "  first release  ");
        assertEquals("exporter", stamped.publishedBy, "UC-008 BR-003 / NFR-009");
        assertEquals("first release", stamped.publishedComment);
        assertNull(service.stampExport(created.id, " ").publishedComment, "blank note stored as null");
    }

    @Test
    void list_isOrderedByDisplayOrderAndContainsFixtures() {
        var names = service.list().stream().map(s -> s.name).toList();
        assertTrue(names.indexOf("AuthorFixture") < names.indexOf("AuthorFixtureEmpty"));
    }

    // ---- UC-044: publishing a content language from the Translations page ----

    private Survey withLanguages(String name, String base, String published) {
        Survey d = draft(name);
        d.baseLanguage = base;
        d.contentLanguages = published;
        return service.create(d);
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "author.one", roles = {"elicit_author"})
    void publishLanguage_addsTheTagAndLeavesTheRestOfTheSetAlone() {
        Survey s = withLanguages("UC044 add", "en", "ar,es-419");

        Survey updated = service.publishLanguage(s.id, "fr");

        assertEquals("ar,es-419,fr", updated.contentLanguages, "ordered and de-duplicated as stored");
        assertEquals("author.one", updated.publishedBy, "UC-003 BR-004: last editor recorded");
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "author.one", roles = {"elicit_author"})
    void publishLanguage_isTheFirstLanguageOfASurveyThatPublishedNone() {
        Survey s = withLanguages("UC044 first", "en", null);
        assertEquals("fr", service.publishLanguage(s.id, "fr").contentLanguages);
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "author.one", roles = {"elicit_author"})
    void publishLanguage_storesTheTagCanonically() {
        Survey s = withLanguages("UC044 canon", "en", null);
        assertEquals("pt-BR", service.publishLanguage(s.id, "  PT-br  ").contentLanguages,
                "UC-044 BR-004: stored in the form the chrome mount uses");
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "author.one", roles = {"elicit_author"})
    void publishLanguage_refusesTheBaseLanguage() {
        Survey s = withLanguages("UC044 base", "en", null);
        AuthoringException e = assertThrows(AuthoringException.class,
                () -> service.publishLanguage(s.id, "en"));
        assertEquals("contentLanguages", e.field());
        assertNull(Survey.<Survey>findById(s.id).contentLanguages, "nothing stored");
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "author.one", roles = {"elicit_author"})
    void publishLanguage_refusesTheBaseLanguageHoweverItWasStored() {
        // copyMetadata only trims the base language, so "EN" is storable; the comparison
        // canonicalizes both sides rather than assuming a form.
        Survey s = withLanguages("UC044 base case", "EN", null);
        assertThrows(AuthoringException.class, () -> service.publishLanguage(s.id, "en"));
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "author.one", roles = {"elicit_author"})
    void publishLanguage_refusesATagItAlreadyPublishes() {
        Survey s = withLanguages("UC044 dupe", "en", "fr");
        AuthoringException e = assertThrows(AuthoringException.class,
                () -> service.publishLanguage(s.id, "FR"));
        assertEquals("contentLanguages", e.field());
        assertEquals("fr", Survey.<Survey>findById(s.id).contentLanguages, "set unchanged");
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "author.one", roles = {"elicit_author"})
    void publishLanguage_refusesWhatIsNotALanguage() {
        Survey s = withLanguages("UC044 junk", "en", null);
        for (String bad : new String[]{"zzz", "fr_CA", "français", "", "   "}) {
            AuthoringException e = assertThrows(AuthoringException.class,
                    () -> service.publishLanguage(s.id, bad), "should refuse '" + bad + "'");
            assertEquals("contentLanguages", e.field());
        }
        assertNull(Survey.<Survey>findById(s.id).contentLanguages);
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "author.one", roles = {"elicit_author"})
    void publishLanguage_refusesASetThatWouldNotFitTheColumn() {
        // content_languages is varchar(255). Forty-two five-character tags store as 251 characters
        // (5 each plus 41 commas), so one more tag would not fit and must be refused rather than
        // silently truncated.
        List<String> many = new ArrayList<>();
        for (char a = 'A'; many.size() < 42; a++) {
            for (char b = 'A'; b <= 'Z' && many.size() < 42; b++) {
                many.add("pt-" + a + b);
            }
        }
        Survey s = withLanguages("UC044 full", "en", String.join(",", many));
        assertEquals(251, Survey.<Survey>findById(s.id).contentLanguages.length(), "the set is full");

        AuthoringException e = assertThrows(AuthoringException.class,
                () -> service.publishLanguage(s.id, "zh-Hans"));
        assertEquals("contentLanguages", e.field());
        assertEquals(251, Survey.<Survey>findById(s.id).contentLanguages.length(), "set unchanged");
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "author.one", roles = {"elicit_author"})
    void publishLanguage_unknownSurveyIsReported() {
        AuthoringException e = assertThrows(AuthoringException.class,
                () -> service.publishLanguage(-1, "fr"));
        assertNull(e.field(), "not a field problem");
    }

    @Test
    void normalizeLanguages_trimsDeduplicatesAndOrders() {
        assertEquals("ar,es-419,fr", SurveyService.normalizeLanguages(" fr , ar ,es-419, fr "));
        assertNull(SurveyService.normalizeLanguages("  "));
        assertNull(SurveyService.normalizeLanguages(null));
        assertEquals("en,fr", SurveyService.normalizeLanguages("en,fr"),
                "it does not strip the base language, and its javadoc no longer claims to");
    }
}
