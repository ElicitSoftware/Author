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

import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Wording changes that have just invalidated a translation, waiting to be shown to the author
 * (FR-060, UC-043 A1).
 * <p>
 * An author who rewrites a question has to be told at that moment which languages it affects,
 * because a translation faithful to the old wording may now ask a different question, and the
 * respondents reading it will silently get the base text instead. The export warnings
 * ({@code ExportValidation}) are the second reminder; this is the first.
 * <p>
 * Shaped like {@link RenameLedger}, and for the same reason: the comparison happens in the service
 * that saves the element, but the message belongs on the screen that called it. Drained when read,
 * because it is a notification and not a record -- the durable version is the out-of-date status on
 * the Translations page.
 */
@ApplicationScoped
public class StaleTranslationLedger {

    private final Map<Integer, Deque<String>> bySurvey = new ConcurrentHashMap<>();

    /**
     * Records that one field's wording changed while translations of it existed.
     *
     * @param surveyId  the survey
     * @param what      what changed, for the author: "question", "step name", ...
     * @param languages the languages whose translations are now out of date
     */
    public void record(int surveyId, String what, List<String> languages) {
        if (languages.isEmpty()) {
            return;
        }
        bySurvey.computeIfAbsent(surveyId, k -> new ArrayDeque<>())
                .add(what + "|" + String.join(", ", languages) + "|" + languages.size());
    }

    /**
     * Everything recorded since the last read, and clears it.
     *
     * @param surveyId the survey
     * @return one entry per wording change: what changed, the languages, and how many
     */
    public List<String[]> drain(int surveyId) {
        Deque<String> pending = bySurvey.remove(surveyId);
        if (pending == null) {
            return List.of();
        }
        List<String[]> out = new ArrayList<>(pending.size());
        pending.forEach(entry -> out.add(entry.split("\\|", 3)));
        return out;
    }
}
