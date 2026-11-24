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
package com.elicitsoftware.author.survey;

import com.elicitsoftware.author.AuthoringException;
import com.elicitsoftware.author.ConstraintViolationTranslator;
import com.elicitsoftware.author.i18n.Translations;
import com.elicitsoftware.model.Report;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * The report services a survey offers its respondents (UC-048): {@code survey.reports} rows, a
 * Type 1 table edited in place, ordered as the respondent's report page renders them, and seeded
 * with the built-in respondent report when a survey is created (BR-001).
 */
@ApplicationScoped
public class ReportService {

    /** Table name used in {@code ValidationIssue.kind()}. */
    public static final String KIND = "reports";

    /**
     * UC-048 BR-001, BR-002: the built-in respondent report is the row whose address is this path,
     * resolved by each site against its own Survey module (Admin UC-005 BR-023). It is the
     * service phase 4 of the umbrella's default reports plan adds to Survey.
     */
    public static final String BUILT_IN_RESPONDENT_PATH = "/api/reports/respondent-summary";
    public static final String BUILT_IN_RESPONDENT_NAME = "Summary of your answers";
    public static final String BUILT_IN_RESPONDENT_DESCRIPTION = "The answers you gave, as you finished the survey.";

    private static final Pattern ABSOLUTE_URL = Pattern.compile("^https?://\\S+$");

    @Inject
    EntityManager em;

    /**
     * The survey's reports in display order, ties broken by id.
     *
     * @param surveyId the survey
     * @return the reports; empty when the survey has none
     */
    public List<Report> reports(int surveyId) {
        return Report.list("surveyId", Sort.by("displayOrder").and("id"), surveyId);
    }

    /**
     * One report of the survey.
     *
     * @param surveyId the survey
     * @param reportId the report's id
     * @return the report, or empty when the survey has no report with that id
     */
    public Optional<Report> find(int surveyId, int reportId) {
        return Report.<Report>find("surveyId = ?1 and id = ?2", surveyId, reportId).firstResultOptional();
    }

    /**
     * UC-048 BR-002: built-in is the address, nothing else.
     *
     * @param r the report, may be null
     * @return true when its address is {@link #BUILT_IN_RESPONDENT_PATH}; false for null
     */
    public static boolean isBuiltIn(Report r) {
        return r != null && BUILT_IN_RESPONDENT_PATH.equals(r.url);
    }

    /**
     * UC-048 BR-001: a new survey starts with the built-in respondent report; a second call changes nothing.
     *
     * @param surveyId the survey
     * @return the survey's existing built-in report, or the one just added
     */
    @Transactional
    public Report seedBuiltIn(int surveyId) {
        for (Report r : reports(surveyId)) {
            if (isBuiltIn(r)) {
                return r;
            }
        }
        return add(surveyId, BUILT_IN_RESPONDENT_NAME, BUILT_IN_RESPONDENT_DESCRIPTION, BUILT_IN_RESPONDENT_PATH);
    }

    /**
     * UC-048 steps 3-4: a new row at the end of the order. Name, description and address are stored
     * stripped.
     *
     * @param surveyId    the survey
     * @param name        the report's name, unique within the survey
     * @param description the report's description
     * @param url         a path under Survey or an absolute http(s) URL
     * @return the persisted report
     * @throws AuthoringException when a field is missing, too long, a taken name or an invalid
     *                            address (BR-003, BR-004)
     */
    @Transactional
    public Report add(int surveyId, String name, String description, String url) {
        Report r = new Report();
        r.surveyId = surveyId;
        r.name = validName(surveyId, name, null);
        r.description = validDescription(description);
        r.url = validUrl(url);
        r.displayOrder = reports(surveyId).size() + 1;
        try {
            r.persist();
            em.flush();
        } catch (RuntimeException e) {
            throw ConstraintViolationTranslator.translate(e);
        }
        return r;
    }

    /**
     * UC-048 step 6: in place, under the same key.
     *
     * @param surveyId    the survey
     * @param reportId    the report's id
     * @param name        the new name, unique within the survey
     * @param description the new description
     * @param url         the new address: a path under Survey or an absolute http(s) URL
     * @return the updated report
     * @throws AuthoringException when the report is gone or a field fails validation (BR-003, BR-004)
     */
    @Transactional
    public Report update(int surveyId, int reportId, String name, String description, String url) {
        Report r = report(surveyId, reportId);
        r.name = validName(surveyId, name, reportId);
        r.description = validDescription(description);
        r.url = validUrl(url);
        try {
            em.flush();
        } catch (RuntimeException e) {
            throw ConstraintViolationTranslator.translate(e);
        }
        return r;
    }

    /**
     * UC-048 step 7, BR-006: delete and renumber.
     *
     * @param surveyId the survey
     * @param reportId the report's id
     * @throws AuthoringException when the report is gone
     */
    @Transactional
    public void remove(int surveyId, int reportId) {
        Report r = report(surveyId, reportId);
        r.delete();
        em.flush();
        renumber(surveyId);
    }

    /**
     * UC-048 step 5, BR-005: the given ids take positions 1..n; rows not named keep their relative order after them.
     *
     * @param surveyId   the survey
     * @param idsInOrder report ids in their new order; an id the survey does not have is skipped
     */
    @Transactional
    public void reorder(int surveyId, List<Integer> idsInOrder) {
        List<Report> all = reports(surveyId);
        int next = 1;
        for (Integer id : idsInOrder) {
            for (Report r : all) {
                if (Objects.equals(r.id, id)) {
                    r.displayOrder = next++;
                }
            }
        }
        for (Report r : all) {
            if (!idsInOrder.contains(r.id)) {
                r.displayOrder = next++;
            }
        }
        em.flush();
    }

    private void renumber(int surveyId) {
        int next = 1;
        for (Report r : reports(surveyId)) {
            r.displayOrder = next++;
        }
        em.flush();
    }

    private Report report(int surveyId, int reportId) {
        return find(surveyId, reportId).orElseThrow(() -> new AuthoringException(Translations.get("error.report.gone")));
    }

    /** UC-048 BR-003. */
    private String validName(int surveyId, String name, Integer selfId) {
        String clean = name == null ? "" : name.strip();
        if (clean.isEmpty()) {
            throw new AuthoringException(Translations.get("error.report.nameRequired"), "name");
        }
        if (clean.length() > 255) {
            throw new AuthoringException(Translations.get("error.report.nameTooLong"), "name");
        }
        for (Report other : reports(surveyId)) {
            if (!Objects.equals(other.id, selfId) && clean.equals(other.name)) {
                throw new AuthoringException(Translations.get("error.report.nameTaken", clean), "name");
            }
        }
        return clean;
    }

    /** UC-048 BR-003. */
    private static String validDescription(String description) {
        String clean = description == null ? "" : description.strip();
        if (clean.isEmpty()) {
            throw new AuthoringException(Translations.get("error.report.descriptionRequired"), "description");
        }
        if (clean.length() > 255) {
            throw new AuthoringException(Translations.get("error.report.descriptionTooLong"), "description");
        }
        return clean;
    }

    /** UC-048 BR-004: a path under Survey, or an absolute http(s) URL. */
    static String validUrl(String url) {
        String clean = url == null ? "" : url.strip();
        if (clean.isEmpty()) {
            throw new AuthoringException(Translations.get("error.report.urlRequired"), "url");
        }
        if (clean.length() > 255) {
            throw new AuthoringException(Translations.get("error.report.urlTooLong"), "url");
        }
        boolean path = clean.startsWith("/") && !clean.startsWith("//") && clean.chars().noneMatch(Character::isWhitespace);
        if (!path && !ABSOLUTE_URL.matcher(clean).matches()) {
            throw new AuthoringException(Translations.get("error.report.urlInvalid"), "url");
        }
        return clean;
    }
}
