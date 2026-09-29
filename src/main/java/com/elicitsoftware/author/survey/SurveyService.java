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
    CurrentUser currentUser;

    @Inject
    RenameLedger renames;

    @Inject
    ReportingChangeLedger reportingChanges;

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
        return s;
    }

    /** UC-004: in-place update; key and list position untouched (BR-001, BR-002). */
    @Transactional
    public Survey updateMetadata(int id, Survey edited) {
        validate(edited);
        Survey s = Survey.findById(id);
        if (s == null) {
            throw new AuthoringException("Survey " + id + " no longer exists");
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
            throw new AuthoringException("Survey " + surveyId + " no longer exists");
        }
        String canonical = LanguageTags.canonical(tag);
        if (canonical == null) {
            throw new AuthoringException("'" + (tag == null ? "" : tag.strip())
                    + "' is not a language tag. Use a tag such as fr or es-419.", "contentLanguages");
        }
        if (canonical.equals(LanguageTags.canonical(s.baseLanguage))) {
            throw new AuthoringException(canonical
                    + " is the language this survey is written in; it needs no translation.",
                    "contentLanguages");
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
            throw new AuthoringException(canonical
                    + " is already one of this survey's published languages.", "contentLanguages");
        }
        published.add(canonical);
        String set = normalizeLanguages(String.join(",", published));
        // content_languages is varchar(255). Twenty tags is about seventy characters, so this is a
        // guard against a silent truncation rather than a limit anyone meets.
        if (set != null && set.length() > 255) {
            throw new AuthoringException(
                    "This survey already publishes as many languages as it can hold.", "contentLanguages");
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
            throw new AuthoringException("Survey " + id + " no longer exists");
        }
        String by = currentUser.name();
        if (by == null || by.isBlank()) {
            throw new AuthoringException("An export must be attributed to a signed-in author");
        }
        s.publishedBy = by;
        s.publishedComment = releaseNote == null || releaseNote.isBlank() ? null : releaseNote.trim();
        renames.clear(id);
        reportingChanges.clear(id);
        em.flush();
        return s;
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

    private static void validate(Survey s) {
        if (s.name == null || s.name.isBlank()) {
            throw new AuthoringException("Name is required", "name");
        }
        if (s.name.length() > 255) {
            throw new AuthoringException("Name must be 255 characters or fewer", "name");
        }
        if (s.title == null || s.title.isBlank()) {
            throw new AuthoringException("Title is required", "title");
        }
        if (s.title.length() > 255) {
            throw new AuthoringException("Title must be 255 characters or fewer", "title");
        }
        if (s.description != null && s.description.length() > 2000) {
            throw new AuthoringException("Description must be 2000 characters or fewer", "description");
        }
        if (s.initialDisplayKey != null && s.initialDisplayKey.length() > 255) {
            throw new AuthoringException("Initial display key must be 255 characters or fewer", "initialDisplayKey");
        }
        if (s.postSurveyUrl != null && s.postSurveyUrl.length() > 255) {
            throw new AuthoringException("Post-survey address must be 255 characters or fewer", "postSurveyUrl");
        }
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
