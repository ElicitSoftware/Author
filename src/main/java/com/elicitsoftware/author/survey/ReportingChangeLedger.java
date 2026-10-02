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

    public record ReportingChange(String kind, Integer durableId, String messageKey, List<Object> params) {

        public String message() {
            return Translations.get(messageKey, params.toArray());
        }
    }

    private final Map<Integer, List<ReportingChange>> bySurvey = new ConcurrentHashMap<>();

    public void record(int surveyId, String kind, Integer durableId, String messageKey, Object... params) {
        bySurvey.computeIfAbsent(surveyId, k -> new CopyOnWriteArrayList<>())
                .add(new ReportingChange(kind, durableId, messageKey, List.of(params)));
    }

    public List<ReportingChange> since(int surveyId) {
        return List.copyOf(bySurvey.getOrDefault(surveyId, List.of()));
    }

    public void clear(int surveyId) {
        bySurvey.remove(surveyId);
    }
}
