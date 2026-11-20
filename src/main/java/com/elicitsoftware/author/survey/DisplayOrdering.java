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

import com.elicitsoftware.model.SectionsQuestion;
import com.elicitsoftware.model.Step;
import com.elicitsoftware.model.StepsSections;
import com.elicitsoftware.model.StructuralElement;
import com.elicitsoftware.model.Survey;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Keeps display orders contiguous and display keys derived (UC-016 BR-001). The Survey runtime
 * addresses a section assignment by {@code survey-stepOrder-0000-sectionOrder-0000-0000-0000}
 * with 4-digit integer components, so positions are always renumbered 1..n and keys recomputed.
 * <p>
 * Keys and step orders are unique among current rows, so a reorder is written in two phases:
 * park every touched row on a value that cannot collide, flush, then write the final values.
 * All methods expect to run inside the caller's transaction.
 */
@ApplicationScoped
public class DisplayOrdering {

    public static final String DISPLAY_KEY_FORMAT = "%04d-%04d-0000-%04d-0000-0000-0000";

    @Inject
    EntityManager em;

    /**
     * Formats a section assignment's display key ({@link #DISPLAY_KEY_FORMAT}).
     *
     * @param surveyId     the survey id
     * @param stepOrder    the step's position, from 1
     * @param sectionOrder the section's position within the step, from 1
     * @return the display key
     */
    public static String displayKey(int surveyId, int stepOrder, int sectionOrder) {
        return String.format(DISPLAY_KEY_FORMAT, surveyId, stepOrder, sectionOrder);
    }

    /**
     * A 34-character key no real assignment can carry, unique per row, used while renumbering.
     *
     * @param ss the assignment to park
     * @return {@code TMP-} followed by the assignment's id zero-padded to 30 digits
     */
    public static String parkedKey(StepsSections ss) {
        return String.format("TMP-%030d", ss.stepsSectionsId);
    }

    /**
     * A step order no real step can carry, unique per row, used while renumbering.
     *
     * @param step the step to park
     * @return the negated durable id of the step
     */
    public static BigDecimal parkedOrder(Step step) {
        return BigDecimal.valueOf(-step.stepId);
    }

    // ---- reads -----------------------------------------------------------------------------

    /**
     * Returns the survey's current steps in display order.
     *
     * @param surveyId the survey
     * @return the current steps, ordered by display order and then durable id
     */
    public List<Step> currentSteps(int surveyId) {
        return Step.list("surveyId = ?1 and effectiveTo = ?2 order by displayOrder, stepId", surveyId, StructuralElement.SENTINEL);
    }

    /**
     * Returns a step's current section assignments in display order.
     *
     * @param surveyId the survey
     * @param stepId   the step's durable id
     * @return the current assignments, ordered by section display order and then id
     */
    public List<StepsSections> currentLane(int surveyId, int stepId) {
        return StepsSections.list("surveyId = ?1 and stepId = ?2 and effectiveTo = ?3 order by sectionDisplayOrder, stepsSectionsId",
                surveyId, stepId, StructuralElement.SENTINEL);
    }

    /**
     * Returns a section's current question assignments in display order.
     *
     * @param surveyId  the survey
     * @param sectionId the section's durable id
     * @return the current assignments, ordered by display order and then id
     */
    public List<SectionsQuestion> currentQuestions(int surveyId, int sectionId) {
        return SectionsQuestion.list("surveyId = ?1 and sectionId = ?2 and effectiveTo = ?3 order by displayOrder, sectionsQuestionId",
                surveyId, sectionId, StructuralElement.SENTINEL);
    }

    // ---- writes ----------------------------------------------------------------------------

    /**
     * Renumber the steps in their current order and recompute every lane.
     *
     * @param surveyId the survey
     */
    public void renumberSurvey(int surveyId) {
        renumberSurvey(surveyId, currentSteps(surveyId));
    }

    /**
     * Renumber the steps to {@code orderedSteps} (1..n) and recompute every lane's keys, then
     * {@linkplain #syncInitialDisplayKey sync the entry point}.
     *
     * @param surveyId     the survey
     * @param orderedSteps the survey's current steps in their new order
     */
    public void renumberSurvey(int surveyId, List<Step> orderedSteps) {
        Map<Integer, List<StepsSections>> lanes = new LinkedHashMap<>();
        for (Step s : orderedSteps) {
            s.displayOrder = parkedOrder(s);
            List<StepsSections> lane = currentLane(surveyId, s.stepId);
            lane.forEach(ss -> ss.displayKey = parkedKey(ss));
            lanes.put(s.stepId, lane);
        }
        em.flush();
        int stepOrder = 0;
        for (Step s : orderedSteps) {
            stepOrder++;
            s.displayOrder = BigDecimal.valueOf(stepOrder);
            writeLane(surveyId, stepOrder, lanes.get(s.stepId));
        }
        em.flush();
        syncInitialDisplayKey(surveyId);
    }

    /**
     * Renumber one lane to {@code ordered} (1..n), keeping the step where it is, then
     * {@linkplain #syncInitialDisplayKey sync the entry point}.
     *
     * @param surveyId the survey
     * @param step     the step whose lane it is; its display order is used as is
     * @param ordered  the step's current assignments in their new order
     */
    public void renumberLane(int surveyId, Step step, List<StepsSections> ordered) {
        ordered.forEach(ss -> ss.displayKey = parkedKey(ss));
        em.flush();
        writeLane(surveyId, step.displayOrder.intValue(), ordered);
        em.flush();
        syncInitialDisplayKey(surveyId);
    }

    /**
     * Renumber one section's question assignments to {@code ordered} (1..n).
     *
     * @param ordered the section's current question assignments in their new order
     */
    public void renumberSection(List<SectionsQuestion> ordered) {
        int order = 0;
        for (SectionsQuestion sq : ordered) {
            order++;
            sq.displayOrder = BigDecimal.valueOf(order);
        }
        em.flush();
    }

    /**
     * UC-016 BR-002: the survey's entry point must name a current assignment; when it does not
     * (or is unset) it follows the lowest current key. Nothing changes when the survey does not
     * exist or has no current assignment.
     *
     * @param surveyId the survey
     */
    public void syncInitialDisplayKey(int surveyId) {
        Survey survey = Survey.findById(surveyId);
        if (survey == null) {
            return;
        }
        List<String> keys = StepsSections.<StepsSections>list("surveyId = ?1 and effectiveTo = ?2", surveyId, StructuralElement.SENTINEL)
                .stream().map(ss -> ss.displayKey).sorted().toList();
        if (keys.isEmpty()) {
            return;
        }
        if (survey.initialDisplayKey == null || !keys.contains(survey.initialDisplayKey)) {
            survey.initialDisplayKey = keys.get(0);
            em.flush();
        }
    }

    private static void writeLane(int surveyId, int stepOrder, List<StepsSections> lane) {
        int sectionOrder = 0;
        for (StepsSections ss : lane) {
            sectionOrder++;
            ss.stepDisplayOrder = BigDecimal.valueOf(stepOrder);
            ss.sectionDisplayOrder = BigDecimal.valueOf(sectionOrder);
            ss.displayKey = displayKey(surveyId, stepOrder, sectionOrder);
        }
    }
}
