package com.elicitsoftware.author.survey;

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

    /**
     * One rename.
     *
     * @param table     the renamed element's table, {@code steps} or {@code sections}
     * @param durableId the renamed element's durable id
     * @param oldName   the name before the rename
     * @param newName   the name after it
     */
    public record RenamedElement(String table, int durableId, String oldName, String newName) {

        /**
         * The retroactivity warning for this rename, in the reader's language.
         *
         * @return the step wording for {@code steps}, the section wording for any other table
         */
        public String message() {
            String key = "steps".equals(table) ? "validation.rename.step" : "validation.rename.section";
            return Translations.get(key, oldName, newName);
        }
    }

    private final Map<Integer, List<RenamedElement>> bySurvey = new ConcurrentHashMap<>();

    /**
     * Records one rename.
     *
     * @param surveyId  the survey
     * @param table     the renamed element's table, {@code steps} or {@code sections}
     * @param durableId the renamed element's durable id
     * @param oldName   the name before the rename
     * @param newName   the name after it
     */
    public void record(int surveyId, String table, int durableId, String oldName, String newName) {
        bySurvey.computeIfAbsent(surveyId, k -> new CopyOnWriteArrayList<>())
                .add(new RenamedElement(table, durableId, oldName, newName));
    }

    /**
     * The survey's renames since its last export or since the application started.
     *
     * @param surveyId the survey
     * @return an unmodifiable copy, in the order recorded; empty when there are none
     */
    public List<RenamedElement> since(int surveyId) {
        return List.copyOf(bySurvey.getOrDefault(surveyId, List.of()));
    }

    /**
     * Forgets the survey's renames, as an export does.
     *
     * @param surveyId the survey
     */
    public void clear(int surveyId) {
        bySurvey.remove(surveyId);
    }
}
