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

import com.elicitsoftware.author.i18n.Translations;
import com.elicitsoftware.author.AuthoringException;
import com.elicitsoftware.author.ConstraintViolationTranslator;
import com.elicitsoftware.author.security.CurrentUser;
import com.elicitsoftware.model.Survey;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Survey records (UC-003 create, UC-004 edit metadata, UC-008 export attribution). {@code surveys}
 * is Type 1: edits are in place; the survey key is minted once and never changes.
 */
@ApplicationScoped
public class SurveyService {

    @Inject
    EntityManager em;

    @Inject
    ReportService reports;

    @Inject
    CurrentUser currentUser;

    @Inject
    RenameLedger renames;

    @Inject
    ReportingChangeLedger reportingChanges;

    @Inject
    PreviewSurveyClient previewSurvey;

    @Inject
    ContentLanguages contentLanguages;

    /**
     * The languages a survey may publish content in (UC-044 BR-004), for the controls that offer
     * them. Delegated so that a Vaadin dialog already holding this service need not also be handed
     * {@link ContentLanguages}.
     *
     * @return the curated tags, never empty
     */
    public List<String> supportedContentLanguages() {
        return contentLanguages.tags();
    }

    public List<Survey> list() {
        return Survey.listAll(Sort.by("displayOrder"));
    }

    public Optional<Survey> find(int id) {
        return Optional.ofNullable(Survey.findById(id));
    }

    public Optional<Survey> findByKey(UUID surveyKey) {
        return Survey.find("surveyKey", surveyKey).firstResultOptional();
    }

    /** UC-003: mint the key, append to the list order, record the author. */
    @Transactional
    public Survey create(Survey draft) {
        validate(draft);
        Survey s = new Survey();
        s.surveyKey = draft.surveyKey != null ? draft.surveyKey : UUID.randomUUID();
        copyMetadata(draft, s);
        s.displayOrder = nextDisplayOrder();
        s.publishedBy = currentUser.name();
        s.publishedComment = null;
        try {
            s.persist();
            em.flush();
        } catch (RuntimeException e) {
            throw ConstraintViolationTranslator.translate(e);
        }
        // UC-048 BR-001: every new survey starts with the built-in respondent report.
        reports.seedBuiltIn(s.id);
        return s;
    }

    /** UC-004: in-place update; key and list position untouched (BR-001, BR-002). */
    @Transactional
    public Survey updateMetadata(int id, Survey edited) {
        validate(edited);
        Survey s = Survey.findById(id);
        if (s == null) {
            throw new AuthoringException(Translations.get("error.survey.gone", String.valueOf(id)));
        }
        copyMetadata(edited, s);
        s.publishedBy = currentUser.name();
        try {
            em.flush();
        } catch (RuntimeException e) {
            throw ConstraintViolationTranslator.translate(e);
        }
        return s;
    }

    /**
     * UC-044 step 4: adds one language to the survey's published set, from the Translations page
     * (UC-043 step 2) rather than the metadata dialog. Idempotent in effect -- a tag already
     * published is refused rather than silently re-added, so the author is told why nothing
     * happened.
     * <p>
     * The tag is canonicalized before anything is compared or stored, which is also how the base
     * language is recognized however it was typed (the stored base language is only trimmed).
     *
     * @param surveyId the survey
     * @param tag      the language tag, as typed or as chosen
     * @return the survey, with the language in its published set
     * @throws AuthoringException if the tag is not a language, is the base language, or is already
     *                            published
     */
    @Transactional
    public Survey publishLanguage(int surveyId, String tag) {
        Survey s = Survey.findById(surveyId);
        if (s == null) {
            throw new AuthoringException(Translations.get("error.survey.gone", String.valueOf(surveyId)));
        }
        String canonical = LanguageTags.canonical(tag);
        if (canonical == null) {
            throw new AuthoringException(Translations.get("error.survey.notLanguageTag", tag == null ? "" : tag.strip()), "contentLanguages");
        }
        if (canonical.equals(LanguageTags.canonical(s.baseLanguage))) {
            throw new AuthoringException(Translations.get("error.survey.baseLanguage", canonical), "contentLanguages");
        }
        if (!contentLanguages.supports(canonical)) {
            throw new AuthoringException(unsupported(canonical), "contentLanguages");
        }
        List<String> published = new ArrayList<>();
        if (s.contentLanguages != null && !s.contentLanguages.isBlank()) {
            for (String existing : s.contentLanguages.split(",")) {
                String trimmed = existing.trim();
                if (!trimmed.isEmpty()) {
                    published.add(trimmed);
                }
            }
        }
        if (published.contains(canonical)) {
            throw new AuthoringException(Translations.get("error.survey.languagePublished", canonical), "contentLanguages");
        }
        published.add(canonical);
        String set = normalizeLanguages(String.join(",", published));
        // content_languages is varchar(255). Twenty tags is about seventy characters, so this is a
        // guard against a silent truncation rather than a limit anyone meets.
        if (set != null && set.length() > 255) {
            throw new AuthoringException(Translations.get("error.survey.languagesFull"), "contentLanguages");
        }
        s.contentLanguages = set;
        s.publishedBy = currentUser.name();
        em.flush();
        return s;
    }

    /** UC-008 step 4: record who exported and the release note. */
    @Transactional
    public Survey stampExport(int id, String releaseNote) {
        Survey s = Survey.findById(id);
        if (s == null) {
            throw new AuthoringException(Translations.get("error.survey.gone", String.valueOf(id)));
        }
        String by = currentUser.name();
        if (by == null || by.isBlank()) {
            throw new AuthoringException(Translations.get("error.survey.exportNeedsAuthor"));
        }
        s.publishedBy = by;
        s.publishedComment = releaseNote == null || releaseNote.isBlank() ? null : releaseNote.trim();
        renames.clear(id);
        reportingChanges.clear(id);
        em.flush();
        return s;
    }

    /**
     * Whether the survey has ever been exported. A stored release note is the mark (UC-008 step 3
     * makes it mandatory), the same test the purge guard applies (UC-009 A3).
     */
    public static boolean isExported(Survey s) {
        return s.publishedBy != null && s.publishedComment != null;
    }

    /**
     * Child tables in delete order: each table goes before the tables it references. Relationships
     * reference assignments and steps; assignments reference questions, sections and steps;
     * questions and items reference select groups. Metadata, report marks and translations carry
     * durable ids with no constraint, so they can go first.
     */
    private static final List<String> SURVEY_CHILD_TABLES = List.of(
            "translations", "report_items", "metadata", "ontology",
            "relationships", "sections_questions", "steps_sections", "questions",
            "select_items", "select_groups", "sections", "steps",
            "reports", "post_survey_actions");

    /**
     * What previewing the survey recorded, in delete order: dependents reference answers, and all
     * three reference the respondent. They go before {@link #SURVEY_CHILD_TABLES}, whose
     * relationships, questions, assignments and post-survey actions they reference in turn.
     */
    private static final List<String> RESPONDENT_CHILD_TABLES = List.of("dependents", "respondent_psa", "answers");

    /**
     * UC-047 A4: how many respondents previewing the survey has recorded in Author's database.
     * The confirmation states it, because they are deleted with the survey (BR-004).
     */
    public long previewRespondents(int id) {
        return ((Number) em.createNativeQuery("SELECT count(*) FROM survey.respondents WHERE survey_id = ?1")
                .setParameter(1, id).getSingleResult()).longValue();
    }

    /**
     * UC-047: physically deletes the survey and every row that belongs to it, retired rows
     * included. Unlike removal (UC-009), nothing is kept: the recovery path is an export taken
     * beforehand, which imports back under the same survey and element keys (UC-005).
     * <p>
     * Deleting the survey here never reaches a site. Sites that applied an export keep their copy;
     * re-importing the export is what lets Author update them again (BR-003). What it does reach,
     * first, is the preview Survey, which drops the survey's reporting schema in this database
     * (BR-008).
     *
     * @param id the survey
     * @throws AuthoringException when the survey is gone
     */
    @Transactional
    public void delete(int id) {
        Survey s = Survey.findById(id);
        if (s == null) {
            throw new AuthoringException(Translations.get("error.survey.gone", String.valueOf(id)));
        }
        // BR-008. The preview Survey's reporting schema for this survey goes first, through the
        // preview Survey, because this row holds the schema's only name. A refusal throws and
        // nothing below has run; a survey never built has no schema and makes no call.
        if (s.reportSchema != null) {
            previewSurvey.dropReportingSchema(s.surveyKey);
        }
        // BR-004. The only respondents in Author's database are the ones the preview recorded;
        // they mean nothing without the survey and go with it.
        for (String table : RESPONDENT_CHILD_TABLES) {
            em.createNativeQuery("DELETE FROM survey." + table
                            + " WHERE respondent_id IN (SELECT id FROM survey.respondents WHERE survey_id = ?1)")
                    .setParameter(1, id).executeUpdate();
        }
        em.createNativeQuery("DELETE FROM survey.respondents WHERE survey_id = ?1").setParameter(1, id).executeUpdate();
        // BR-005. The importer reuses a tag by (name, tag), so another survey's metadata may point at
        // a tag this survey owns. Hand such a tag to that survey rather than delete it from under it.
        em.createNativeQuery("""
                        UPDATE survey.ontology o
                           SET survey_id = (SELECT min(m.survey_id) FROM survey.metadata m
                                             WHERE m.ontology_id = o.id AND m.survey_id <> ?1)
                         WHERE o.survey_id = ?1
                           AND EXISTS (SELECT 1 FROM survey.metadata m WHERE m.ontology_id = o.id AND m.survey_id <> ?1)
                        """)
                .setParameter(1, id).executeUpdate();
        for (String table : SURVEY_CHILD_TABLES) {
            em.createNativeQuery("DELETE FROM survey." + table + " WHERE survey_id = ?1").setParameter(1, id).executeUpdate();
        }
        s.delete();
        renames.clear(id);
        reportingChanges.clear(id);
        em.flush();
    }

    int nextDisplayOrder() {
        Integer max = em.createQuery("select max(s.displayOrder) from Survey s", Integer.class).getSingleResult();
        return max == null ? 1 : max + 1;
    }

    private static void copyMetadata(Survey from, Survey to) {
        to.name = from.name.trim();
        to.title = from.title.trim();
        to.description = blankToNull(from.description);
        to.initialDisplayKey = blankToNull(from.initialDisplayKey);
        to.postSurveyUrl = blankToNull(from.postSurveyUrl);
        // UC-044. Withdrawing a language leaves its translations in place, closed by nothing:
        // they simply stop being published, and re-publishing brings them back.
        to.baseLanguage = from.baseLanguage == null || from.baseLanguage.isBlank() ? "en" : from.baseLanguage.trim();
        to.contentLanguages = normalizeLanguages(from.contentLanguages);
    }

    /**
     * The published set as stored: trimmed, de-duplicated and ordered.
     * <p>
     * It does <em>not</em> strip the base language, and has no way to -- it is handed the string
     * alone. Keeping the base language out of the set is the caller's job:
     * {@link #publishLanguage} refuses it, and {@link LanguageTags#offerable} never offers it. A
     * survey whose set already contains its own base language keeps it, and it is harmless: every
     * string in it falls back to that language anyway.
     */
    static String normalizeLanguages(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        java.util.TreeSet<String> tags = new java.util.TreeSet<>();
        for (String tag : raw.split(",")) {
            String trimmed = tag.trim();
            if (!trimmed.isEmpty()) {
                tags.add(trimmed);
            }
        }
        return tags.isEmpty() ? null : String.join(",", tags);
    }

    /**
     * A tag the applications do not carry, refused with the one route to getting one, in the
     * author's language like every other message this class raises (NFR-018).
     */
    private static String unsupported(String canonical) {
        return Translations.get("error.survey.languageUnsupported", canonical);
    }

    private void validate(Survey s) {
        if (s.name == null || s.name.isBlank()) {
            throw new AuthoringException(Translations.get("error.name.required"), "name");
        }
        if (s.name.length() > 255) {
            throw new AuthoringException(Translations.get("error.name.tooLong"), "name");
        }
        if (s.title == null || s.title.isBlank()) {
            throw new AuthoringException(Translations.get("error.survey.titleRequired"), "title");
        }
        if (s.title.length() > 255) {
            throw new AuthoringException(Translations.get("error.survey.titleTooLong"), "title");
        }
        if (s.description != null && s.description.length() > 2000) {
            throw new AuthoringException(Translations.get("error.survey.descriptionTooLong"), "description");
        }
        if (s.initialDisplayKey != null && s.initialDisplayKey.length() > 255) {
            throw new AuthoringException(Translations.get("error.survey.initialDisplayKeyTooLong"), "initialDisplayKey");
        }
        if (s.postSurveyUrl != null && s.postSurveyUrl.length() > 255) {
            throw new AuthoringException(Translations.get("error.survey.postSurveyUrlTooLong"), "postSurveyUrl");
        }
        // UC-044 BR-004. The metadata dialog is the other way into the published set, and until
        // now the only checked path was publishLanguage -- a tag typed here was stored unexamined.
        // A tag already on the survey is left alone: it can only have come from an imported
        // definition, and refusing to save the survey would leave the author unable to remove it.
        if (s.contentLanguages != null && !s.contentLanguages.isBlank()) {
            List<String> already = s.id == null ? List.of() : publishedTags(Survey.findById(s.id));
            for (String raw : s.contentLanguages.split(",")) {
                if (raw.isBlank()) {
                    continue;
                }
                String canonical = LanguageTags.canonical(raw);
                if (canonical == null) {
                    throw new AuthoringException(Translations.get("error.survey.notLanguageTag", raw.strip()), "contentLanguages");
                }
                if (!contentLanguages.supports(canonical) && !already.contains(canonical)) {
                    throw new AuthoringException(unsupported(canonical), "contentLanguages");
                }
            }
        }
    }

    /** The survey's published set as canonical tags; empty for a survey that has none. */
    private static List<String> publishedTags(Survey s) {
        if (s == null || s.contentLanguages == null || s.contentLanguages.isBlank()) {
            return List.of();
        }
        List<String> tags = new ArrayList<>();
        for (String raw : s.contentLanguages.split(",")) {
            String canonical = LanguageTags.canonical(raw);
            if (canonical != null) {
                tags.add(canonical);
            }
        }
        return tags;
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
