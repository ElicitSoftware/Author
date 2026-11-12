/*-
 * ***LICENSE_START***
 * Elicit Author
 * %%
 * Copyright (C) 2025 - 2026 The Regents of the University of Michigan - Rogel Cancer Center
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

    @Inject
    EntityManager em;

    public List<PostSurveyAction> actions(int surveyId) {
        return PostSurveyAction.list("surveyId", Sort.by("executionOrder").and("id"), surveyId);
    }

    public Optional<PostSurveyAction> find(int surveyId, int actionId) {
        return PostSurveyAction.<PostSurveyAction>find("surveyId = ?1 and id = ?2", surveyId, actionId).firstResultOptional();
    }

    /** UC-052 steps 3-4: a new action at the end of the order. */
    @Transactional
    public PostSurveyAction add(int surveyId, String name, String description, String url) {
        PostSurveyAction a = new PostSurveyAction();
        a.surveyId = surveyId;
        a.name = validName(surveyId, name, null);
        a.description = validDescription(description);
        a.url = validUrl(url);
        a.executionOrder = actions(surveyId).size() + 1;
        try {
            a.persist();
            em.flush();
        } catch (RuntimeException e) {
            throw ConstraintViolationTranslator.translate(e);
        }
        return a;
    }

    /** UC-052 step 6, BR-007: in place, under the same key. */
    @Transactional
    public PostSurveyAction update(int surveyId, int actionId, String name, String description, String url) {
        PostSurveyAction a = action(surveyId, actionId);
        a.name = validName(surveyId, name, actionId);
        a.description = validDescription(description);
        a.url = validUrl(url);
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

    /** UC-052 step 5, BR-005: the given ids take positions 1..n; actions not named keep their relative order after them. */
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
