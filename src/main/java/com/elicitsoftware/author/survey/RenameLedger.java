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
 * Step and section renames since the last export (UC-018 step 6, UC-007 step 4). The working
 * copy keeps no history, so the ledger is what lets export validation repeat the retroactivity
 * warning; it is cleared when the survey is exported and does not survive a restart.
 *
 * <p>It keeps the two names, not a sentence: the warning is worded when it is shown, in the
 * language of whoever reads it (UC-007 BR-013).</p>
 */
@ApplicationScoped
public class RenameLedger {

    public record RenamedElement(String table, int durableId, String oldName, String newName) {

        public String message() {
            String key = "steps".equals(table) ? "validation.rename.step" : "validation.rename.section";
            return Translations.get(key, oldName, newName);
        }
    }

    private final Map<Integer, List<RenamedElement>> bySurvey = new ConcurrentHashMap<>();

    public void record(int surveyId, String table, int durableId, String oldName, String newName) {
        bySurvey.computeIfAbsent(surveyId, k -> new CopyOnWriteArrayList<>())
                .add(new RenamedElement(table, durableId, oldName, newName));
    }

    public List<RenamedElement> since(int surveyId) {
        return List.copyOf(bySurvey.getOrDefault(surveyId, List.of()));
    }

    public void clear(int surveyId) {
        bySurvey.remove(surveyId);
    }
}
