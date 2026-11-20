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
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Reporting-tag changes made since the last export (UC-022 BR-001): reassigning, removing, or
 * redefining an assigned tag reclassifies answers already recorded at deployed sites, so
 * {@code ExportValidation} repeats the warning at export time (UC-007 step 4). Like
 * {@link RenameLedger}, kept in memory per survey and cleared when the export is stamped.
 *
 * <p>A change is kept as a {@code validation.tag.*} key and what it is filled with, so the warning
 * is worded when it is shown, in the language of whoever reads it (UC-007 BR-013).</p>
 */
@ApplicationScoped
public class ReportingChangeLedger {

    /**
     * One recorded reporting-tag change.
     *
     * @param kind       the table the changed row belongs to, such as {@code metadata}
     * @param durableId  the changed row's id in that table
     * @param messageKey the {@code validation.tag.*} key the warning is worded from
     * @param params     what the key is filled with
     */
    public record ReportingChange(String kind, Integer durableId, String messageKey, List<Object> params) {

        /**
         * The warning, worded in the current reader's language.
         *
         * @return the translated message
         */
        public String message() {
            return Translations.get(messageKey, params.toArray());
        }
    }

    private final Map<Integer, List<ReportingChange>> bySurvey = new ConcurrentHashMap<>();

    /**
     * Records a change for a survey.
     *
     * @param surveyId   the survey's id
     * @param kind       the table the changed row belongs to, such as {@code metadata}
     * @param durableId  the changed row's id in that table
     * @param messageKey the {@code validation.tag.*} key the warning is worded from
     * @param params     what the key is filled with; none may be {@code null}
     */
    public void record(int surveyId, String kind, Integer durableId, String messageKey, Object... params) {
        bySurvey.computeIfAbsent(surveyId, k -> new CopyOnWriteArrayList<>())
                .add(new ReportingChange(kind, durableId, messageKey, List.of(params)));
    }

    /**
     * The changes recorded for a survey since its last export.
     *
     * @param surveyId the survey's id
     * @return an immutable copy of the changes, in the order recorded; empty when there are none
     */
    public List<ReportingChange> since(int surveyId) {
        return List.copyOf(bySurvey.getOrDefault(surveyId, List.of()));
    }

    /**
     * Forgets a survey's changes, once its export is stamped.
     *
     * @param surveyId the survey's id
     */
    public void clear(int surveyId) {
        bySurvey.remove(surveyId);
    }
}
