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
import com.elicitsoftware.author.i18n.Translations;
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
    void create_rejectsANameOrTitleThatIsNotOneLine() {
        for (String control : new String[]{"\n", "\r", "\t", "\u0000"}) {
            Survey named = draft("UC003 two" + control + "lines");
            named.title = "UC003 title";
            AuthoringException ex = assertThrows(AuthoringException.class, () -> service.create(named));
            assertEquals("name", ex.field(), "UC-003 BR-005");
            assertEquals(Translations.get("error.name.singleLine"), ex.getMessage());

            Survey titled = draft("UC003 single line");
            titled.title = "Two" + control + "lines";
            ex = assertThrows(AuthoringException.class, () -> service.create(titled));
            assertEquals("title", ex.field(), "UC-003 BR-005");
            assertEquals(Translations.get("error.survey.titleSingleLine"), ex.getMessage());
        }
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "author.two", roles = {"elicit_author"})
    void updateMetadata_rejectsANameOrTitleThatIsNotOneLine() {
        Survey created = service.create(draft("UC004 single line"));

        Survey named = draft("UC004 two\nlines");
        AuthoringException ex = assertThrows(AuthoringException.class, () -> service.updateMetadata(created.id, named));
        assertEquals("name", ex.field(), "UC-004 BR-004");

        Survey titled = draft("UC004 single line");
        titled.title = "Two\r\nlines";
        ex = assertThrows(AuthoringException.class, () -> service.updateMetadata(created.id, titled));
        assertEquals("title", ex.field(), "UC-004 BR-004");
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
        // The curated set (author.content.languages=en,ar,es-419) has only two non-base tags, so
        // this can no longer show a third tag arriving alongside two untouched ones -- one
        // untouched tag is all the curated set leaves room for.
        Survey s = withLanguages("UC044 add", "en", "ar");

        Survey updated = service.publishLanguage(s.id, "es-419");

        assertEquals("ar,es-419", updated.contentLanguages, "ordered and de-duplicated as stored");
        assertEquals("author.one", updated.publishedBy, "UC-003 BR-004: last editor recorded");
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "author.one", roles = {"elicit_author"})
    void publishLanguage_isTheFirstLanguageOfASurveyThatPublishedNone() {
        Survey s = withLanguages("UC044 first", "en", null);
        assertEquals("ar", service.publishLanguage(s.id, "ar").contentLanguages);
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "author.one", roles = {"elicit_author"})
    void publishLanguage_storesTheTagCanonically() {
        Survey s = withLanguages("UC044 canon", "en", null);
        // Underscore form on purpose: translations_es_419.properties is how the bundle spells it,
        // and canonical() accepts that spelling too (LanguageTags.canonical).
        assertEquals("es-419", service.publishLanguage(s.id, "  ES_419  ").contentLanguages,
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
        Survey s = withLanguages("UC044 dupe", "en", "ar");
        AuthoringException e = assertThrows(AuthoringException.class,
                () -> service.publishLanguage(s.id, "AR"));
        assertEquals("contentLanguages", e.field());
        assertEquals("ar", Survey.<Survey>findById(s.id).contentLanguages, "set unchanged");
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "author.one", roles = {"elicit_author"})
    void publishLanguage_refusesATagElicitDoesNotCarry() {
        // UC-044 BR-004: only the curated set (author.content.languages) may be published -- a
        // language the shipped chrome does not carry is one Survey's ContentTranslator silently
        // skips, so publishing it would reach no respondent anywhere.
        Survey s = withLanguages("UC044 uncurated", "en", "ar");

        AuthoringException e = assertThrows(AuthoringException.class,
                () -> service.publishLanguage(s.id, "fr"));

        assertEquals("contentLanguages", e.field());
        assertTrue(e.getMessage().contains("ElicitSoftware"), "points the author at who can add it");
        assertEquals("ar", Survey.<Survey>findById(s.id).contentLanguages, "set unchanged");
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "author.one", roles = {"elicit_author"})
    void publishLanguage_refusesWhatIsNotALanguage() {
        Survey s = withLanguages("UC044 junk", "en", null);
        // Not fr_CA any more: the underscore spelling is now read rather than refused, so it
        // would be turned away by the not-carried check instead and this test would pass without
        // exercising what it names. LanguageTagsTest covers that spelling directly.
        for (String bad : new String[]{"zzz", "français", "", "   "}) {
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
        // silently truncated. The curated set has only two non-base tags, so this set can no
        // longer be built through publishLanguage (validate() would refuse every one of these
        // pt-XX tags as not curated); it is written straight onto the already-managed entity
        // instead, standing in for a set an import could have written before languages were
        // curated -- validate()'s already-published exemption is exactly what lets such a survey
        // still be edited, and publishLanguage never re-validates what it did not add.
        List<String> many = new ArrayList<>();
        for (char a = 'A'; many.size() < 42; a++) {
            for (char b = 'A'; b <= 'Z' && many.size() < 42; b++) {
                many.add("pt-" + a + b);
            }
        }
        Survey s = withLanguages("UC044 full", "en", null);
        s.contentLanguages = String.join(",", many);
        assertEquals(251, s.contentLanguages.length(), "the set is full");

        AuthoringException e = assertThrows(AuthoringException.class,
                () -> service.publishLanguage(s.id, "es-419"));
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

    // ---- UC-044 BR-004: create() / updateMetadata() check the curated set too ----

    @Test
    @TestTransaction
    @TestSecurity(user = "author.one", roles = {"elicit_author"})
    void create_refusesAContentLanguageElicitDoesNotCarry() {
        // UC-044 BR-004: a set handed to create() -- the tests' way of seeding one -- passes the
        // same curated check as publishLanguage.
        Survey d = draft("UC044 metadata refuse");
        d.baseLanguage = "en";
        d.contentLanguages = "fr";

        AuthoringException e = assertThrows(AuthoringException.class, () -> service.create(d));

        assertEquals("contentLanguages", e.field());
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "author.one", roles = {"elicit_author"})
    void updateMetadata_leavesThePublishedSetAsStored() {
        // UC-004 BR-005: the details form no longer carries the set, so whatever the edited bean
        // holds -- nothing, or a stale copy -- must not overwrite what the Translations page set.
        Survey s = withLanguages("UC004 BR-005", "en", "ar");

        Survey edit = draft("UC004 BR-005 renamed");
        edit.id = s.id;
        edit.contentLanguages = null;
        assertEquals("ar", service.updateMetadata(s.id, edit).contentLanguages);

        edit.contentLanguages = "fr";
        assertEquals("ar", service.updateMetadata(s.id, edit).contentLanguages, "not checked, not stored");
    }

    // ---- UC-044 A2: withdrawing a content language from the Translations page ----

    @Test
    @TestTransaction
    @TestSecurity(user = "author.two", roles = {"elicit_author"})
    void withdrawLanguage_removesTheTagAndKeepsTheRest() {
        Survey s = withLanguages("UC044 withdraw", "en", "ar,es-419");

        Survey updated = service.withdrawLanguage(s.id, "ES_419");

        assertEquals("ar", updated.contentLanguages, "matched in canonical form");
        assertEquals("author.two", updated.publishedBy, "UC-003 BR-004: last editor recorded");
        assertNull(service.withdrawLanguage(s.id, "ar").contentLanguages, "an empty set is stored as null");
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "author.one", roles = {"elicit_author"})
    void withdrawLanguage_takesOutATagElicitDoesNotCarry() {
        // UC-044 BR-004: the one thing an author may do with an uncurated tag is remove it. Seeded
        // on the column, standing in for an import; publishLanguage would refuse "fr".
        Survey s = withLanguages("UC044 withdraw legacy", "en", "ar");
        s.contentLanguages = "ar,fr";

        assertEquals("ar", service.withdrawLanguage(s.id, "fr").contentLanguages);
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "author.one", roles = {"elicit_author"})
    void withdrawLanguage_refusesALanguageTheSurveyDoesNotPublish() {
        Survey s = withLanguages("UC044 withdraw missing", "en", "ar");

        AuthoringException e = assertThrows(AuthoringException.class, () -> service.withdrawLanguage(s.id, "es-419"));

        assertEquals("contentLanguages", e.field());
        assertEquals("ar", Survey.<Survey>findById(s.id).contentLanguages, "set unchanged");
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
