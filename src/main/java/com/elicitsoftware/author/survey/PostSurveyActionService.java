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
import com.elicitsoftware.model.PostSurveyAction;
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
 * The post-survey actions a survey's site calls when a respondent finishes (UC-052):
 * {@code survey.post_survey_actions} rows, a Type 1 table edited in place, ordered as Survey calls
 * them. A new survey starts with none (BR-008).
 */
@ApplicationScoped
public class PostSurveyActionService {

    private static final Pattern ABSOLUTE_URL = Pattern.compile("^https?://\\S+$");

    /** UC-052 BR-009: the methods a site's Survey calls an action with. */
    public static final List<String> METHODS = List.of("POST", "GET", "PUT");

    /** UC-052 BR-010: the placeholders an address may hold. */
    public static final String RESPONDENT_ID = "{respondentId}";
    public static final String EXTERNAL_ID = "{externalId}";

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{[^}]*}");

    @Inject
    EntityManager em;

    /**
     * Returns the survey's post-survey actions in the order Survey calls them.
     *
     * @param surveyId the survey
     * @return the actions by execution order, then id; empty when the survey has none
     */
    public List<PostSurveyAction> actions(int surveyId) {
        return PostSurveyAction.list("surveyId", Sort.by("executionOrder").and("id"), surveyId);
    }

    /**
     * Finds one of the survey's post-survey actions.
     *
     * @param surveyId the survey
     * @param actionId the action's id
     * @return the action, or empty when the survey has no action with that id
     */
    public Optional<PostSurveyAction> find(int surveyId, int actionId) {
        return PostSurveyAction.<PostSurveyAction>find("surveyId = ?1 and id = ?2", surveyId, actionId).firstResultOptional();
    }

    /**
     * UC-052 steps 3-4: a new action at the end of the order. Name, description and address are
     * stripped; a blank method means POST.
     *
     * @param surveyId        the survey the action belongs to
     * @param name            the action's name, unique within the survey (BR-002)
     * @param description     what the action does (BR-003)
     * @param method          POST, GET or PUT, in any case (BR-009)
     * @param url             the absolute http(s) address Survey calls (BR-004, BR-010, BR-011)
     * @param sendsExternalId whether the call carries the respondent's External ID
     * @return the saved action
     * @throws AuthoringException when a value is invalid or the save violates a recognized constraint
     */
    @Transactional
    public PostSurveyAction add(int surveyId, String name, String description, String method, String url, boolean sendsExternalId) {
        PostSurveyAction a = new PostSurveyAction();
        a.surveyId = surveyId;
        a.name = validName(surveyId, name, null);
        a.description = validDescription(description);
        a.method = validMethod(method);
        a.url = validUrl(url, sendsExternalId);
        a.sendsExternalId = sendsExternalId;
        a.executionOrder = actions(surveyId).size() + 1;
        try {
            a.persist();
            em.flush();
        } catch (RuntimeException e) {
            throw ConstraintViolationTranslator.translate(e);
        }
        return a;
    }

    /**
     * UC-052 step 6, BR-007: in place, under the same key. Values are validated as in
     * {@link #add(int, String, String, String, String, boolean)}.
     *
     * @param surveyId        the survey the action belongs to
     * @param actionId        the action's id
     * @param name            the new name, unique within the survey (BR-002)
     * @param description     the new description (BR-003)
     * @param method          POST, GET or PUT, in any case; blank means POST (BR-009)
     * @param url             the new absolute http(s) address (BR-004, BR-010, BR-011)
     * @param sendsExternalId whether the call carries the respondent's External ID
     * @return the updated action
     * @throws AuthoringException when the action is gone, a value is invalid, or the save violates
     *                            a recognized constraint
     */
    @Transactional
    public PostSurveyAction update(int surveyId, int actionId, String name, String description, String method, String url,
                                   boolean sendsExternalId) {
        PostSurveyAction a = action(surveyId, actionId);
        a.name = validName(surveyId, name, actionId);
        a.description = validDescription(description);
        a.method = validMethod(method);
        a.url = validUrl(url, sendsExternalId);
        a.sendsExternalId = sendsExternalId;
        try {
            em.flush();
        } catch (RuntimeException e) {
            throw ConstraintViolationTranslator.translate(e);
        }
        return a;
    }

    /**
     * UC-052 step 7, BR-006: delete and renumber. A preview respondent who finished in the preview
     * Survey has a delivery record for the action, which references it, so those go first.
     *
     * @param surveyId the survey the action belongs to
     * @param actionId the action's id
     * @throws AuthoringException when the survey has no action with that id
     */
    @Transactional
    public void remove(int surveyId, int actionId) {
        PostSurveyAction a = action(surveyId, actionId);
        em.createNativeQuery("DELETE FROM survey.respondent_psa WHERE post_survey_action_id = ?1")
                .setParameter(1, a.id).executeUpdate();
        a.delete();
        em.flush();
        renumber(surveyId);
    }

    /**
     * UC-052 step 5, BR-005: the given ids take positions 1..n; actions not named keep their relative order after them.
     *
     * @param surveyId   the survey whose actions are reordered
     * @param idsInOrder action ids in their new order; an id the survey does not have is ignored
     */
    @Transactional
    public void reorder(int surveyId, List<Integer> idsInOrder) {
        List<PostSurveyAction> all = actions(surveyId);
        int next = 1;
        for (Integer id : idsInOrder) {
            for (PostSurveyAction a : all) {
                if (Objects.equals(a.id, id)) {
                    a.executionOrder = next++;
                }
            }
        }
        for (PostSurveyAction a : all) {
            if (!idsInOrder.contains(a.id)) {
                a.executionOrder = next++;
            }
        }
        em.flush();
    }

    private void renumber(int surveyId) {
        int next = 1;
        for (PostSurveyAction a : actions(surveyId)) {
            a.executionOrder = next++;
        }
        em.flush();
    }

    private PostSurveyAction action(int surveyId, int actionId) {
        return find(surveyId, actionId).orElseThrow(() -> new AuthoringException(Translations.get("error.postSurveyAction.gone")));
    }

    /** UC-052 BR-002. */
    private String validName(int surveyId, String name, Integer selfId) {
        String clean = name == null ? "" : name.strip();
        if (clean.isEmpty()) {
            throw new AuthoringException(Translations.get("error.postSurveyAction.nameRequired"), "name");
        }
        if (clean.length() > 255) {
            throw new AuthoringException(Translations.get("error.postSurveyAction.nameTooLong"), "name");
        }
        for (PostSurveyAction other : actions(surveyId)) {
            if (!Objects.equals(other.id, selfId) && clean.equals(other.name)) {
                throw new AuthoringException(Translations.get("error.postSurveyAction.nameTaken", clean), "name");
            }
        }
        return clean;
    }

    /** UC-052 BR-003. */
    private static String validDescription(String description) {
        String clean = description == null ? "" : description.strip();
        if (clean.isEmpty()) {
            throw new AuthoringException(Translations.get("error.postSurveyAction.descriptionRequired"), "description");
        }
        if (clean.length() > 255) {
            throw new AuthoringException(Translations.get("error.postSurveyAction.descriptionTooLong"), "description");
        }
        return clean;
    }

    /** UC-052 BR-009: POST unless the author chose GET or PUT. */
    static String validMethod(String method) {
        String clean = method == null || method.isBlank() ? "POST" : method.strip().toUpperCase();
        if (!METHODS.contains(clean)) {
            throw new AuthoringException(Translations.get("error.postSurveyAction.methodInvalid", clean), "method");
        }
        return clean;
    }

    /**
     * UC-052 BR-004, BR-010, BR-011: a complete http(s) address whose only placeholders are
     * {@code {respondentId}} and, on an action that sends the External ID, {@code {externalId}};
     * an action that sends the External ID must use https.
     */
    static String validUrl(String url, boolean sendsExternalId) {
        String clean = validUrl(url);
        var m = PLACEHOLDER.matcher(clean);
        while (m.find()) {
            String found = m.group();
            if (found.equals(EXTERNAL_ID) && !sendsExternalId) {
                throw new AuthoringException(Translations.get("error.postSurveyAction.externalIdPlaceholderNeedsFlag"), "url");
            }
            if (!found.equals(RESPONDENT_ID) && !found.equals(EXTERNAL_ID)) {
                throw new AuthoringException(Translations.get("error.postSurveyAction.placeholderUnknown", found), "url");
            }
        }
        if (sendsExternalId && !clean.regionMatches(true, 0, "https://", 0, 8)) {
            throw new AuthoringException(Translations.get("error.postSurveyAction.externalIdNeedsHttps"), "url");
        }
        return clean;
    }

    /** UC-052 BR-004: an absolute http(s) address, at most 255 characters; no path form. */
    static String validUrl(String url) {
        String clean = url == null ? "" : url.strip();
        if (clean.isEmpty()) {
            throw new AuthoringException(Translations.get("error.postSurveyAction.urlRequired"), "url");
        }
        if (clean.length() > 255) {
            throw new AuthoringException(Translations.get("error.postSurveyAction.urlTooLong"), "url");
        }
        if (!ABSOLUTE_URL.matcher(clean).matches()) {
            throw new AuthoringException(Translations.get("error.postSurveyAction.urlInvalid"), "url");
        }
        return clean;
    }
}
