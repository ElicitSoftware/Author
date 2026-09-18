package com.elicitsoftware.author.survey;

/*-
 * ***LICENSE_START***
 * Elicit Author
 * %%
 * Copyright (C) 2026 The Regents of the University of Michigan - Rogel Cancer Center
 * %%
 * PolyForm Noncommercial License 1.0.0
 * <https://polyformproject.org/licenses/noncommercial/1.0.0>
 * ***LICENSE_END***
 */

import com.elicitsoftware.author.AuthoringException;
import com.elicitsoftware.author.ConstraintViolationTranslator;
import com.elicitsoftware.model.Question;
import com.elicitsoftware.model.Relationship;
import com.elicitsoftware.model.Section;
import com.elicitsoftware.model.SectionsQuestion;
import com.elicitsoftware.model.SelectGroup;
import com.elicitsoftware.model.SelectItem;
import com.elicitsoftware.model.Step;
import com.elicitsoftware.model.StepsSections;
import com.elicitsoftware.model.StructuralElement;
import com.elicitsoftware.model.Survey;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Working-copy operations on structural elements. Create and update are plain inserts/updates
 * (SCD columns untouched); removal is retirement with cascade (UC-009); restore reopens the
 * validity window (UC-010); purge deletes physically and is allowed only for surveys never
 * exported (UC-009 A3 / BR-005).
 */
@ApplicationScoped
public class ElementService {

    /** What a removal would take along, so the UI can confirm it (UC-009 step 3). */
    public record RemovalPlan(StructureNode.Kind kind, Integer durableId, String label, List<String> dependents, List<String> blockers) {
        public boolean blocked() {
            return !blockers.isEmpty();
        }
    }

    @Inject
    EntityManager em;

    // ---- create / update -------------------------------------------------------------------

    @Transactional
    public <E extends StructuralElement> E create(E element) {
        if (element.surveyId == null) {
            throw new AuthoringException("An element must belong to a survey");
        }
        element.setDurableId(((Number) em.createNativeQuery("SELECT nextval('" + element.durableSequence() + "')")
                .getSingleResult()).intValue());
        if (element.getElementKey() == null) {
            element.setElementKey(UUID.randomUUID());
        }
        element.version = 0;
        element.effectiveFrom = StructuralElement.EPOCH;
        element.effectiveTo = StructuralElement.SENTINEL;
        try {
            element.persist();
            em.flush();
        } catch (RuntimeException e) {
            throw ConstraintViolationTranslator.translate(e);
        }
        return element;
    }

    /** Flushes in-place edits of a managed working copy; the SCD columns and keys are not touched. */
    @Transactional
    public <E extends StructuralElement> E update(E element) {
        try {
            E managed = em.merge(element);
            em.flush();
            return managed;
        } catch (RuntimeException e) {
            throw ConstraintViolationTranslator.translate(e);
        }
    }

    // ---- lookups --------------------------------------------------------------------------

    public Optional<Step> step(int surveyId, int durableId) {
        return Step.find("surveyId = ?1 and stepId = ?2", surveyId, durableId).firstResultOptional();
    }

    public Optional<Section> section(int surveyId, int durableId) {
        return Section.find("surveyId = ?1 and sectionId = ?2", surveyId, durableId).firstResultOptional();
    }

    public Optional<Question> question(int surveyId, int durableId) {
        return Question.find("surveyId = ?1 and questionId = ?2", surveyId, durableId).firstResultOptional();
    }

    public Optional<SelectGroup> selectGroup(int surveyId, int durableId) {
        return SelectGroup.find("surveyId = ?1 and selectGroupId = ?2", surveyId, durableId).firstResultOptional();
    }

    public Optional<SelectItem> selectItem(int surveyId, int durableId) {
        return SelectItem.find("surveyId = ?1 and selectItemId = ?2", surveyId, durableId).firstResultOptional();
    }

    // ---- remove (retire) ------------------------------------------------------------------

    /** Computes dependents and blockers without changing anything. */
    public RemovalPlan planRemoval(int surveyId, StructureNode.Kind kind, int durableId) {
        List<String> dependents = new ArrayList<>();
        List<String> blockers = new ArrayList<>();
        String label;
        switch (kind) {
            case QUESTION -> {
                Question q = question(surveyId, durableId).orElseThrow(() -> new AuthoringException("Question not found"));
                label = "Question '" + q.shortText + "'";
                List<SectionsQuestion> sqs = currentAssignmentsOfQuestion(surveyId, durableId);
                sqs.forEach(sq -> dependents.add("assignment of the question to section " + sq.sectionId));
                relationshipsTouching(surveyId, sqs.stream().map(sq -> sq.sectionsQuestionId).toList(), null, null)
                        .forEach(r -> dependents.add("rule '" + r.description + "'"));
            }
            case SECTION -> {
                Section s = section(surveyId, durableId).orElseThrow(() -> new AuthoringException("Section not found"));
                label = "Section '" + s.name + "'";
                List<StepsSections> sss = currentStepAssignmentsOfSection(surveyId, durableId);
                sss.forEach(ss -> dependents.add("assignment of the section to step " + ss.stepId));
                List<SectionsQuestion> sqs = currentQuestionAssignmentsOfSection(surveyId, durableId);
                sqs.forEach(sq -> dependents.add("assignment of question " + sq.questionId + " to the section"));
                relationshipsTouching(surveyId, sqs.stream().map(sq -> sq.sectionsQuestionId).toList(),
                        sss.stream().map(ss -> ss.stepsSectionsId).toList(), null)
                        .forEach(r -> dependents.add("rule '" + r.description + "'"));
            }
            case STEP -> {
                Step st = step(surveyId, durableId).orElseThrow(() -> new AuthoringException("Step not found"));
                label = "Step '" + st.name + "'";
                List<StepsSections> sss = currentSectionAssignmentsOfStep(surveyId, durableId);
                sss.forEach(ss -> dependents.add("assignment of section " + ss.sectionId + " to the step"));
                relationshipsTouching(surveyId, List.of(), sss.stream().map(ss -> ss.stepsSectionsId).toList(), durableId)
                        .forEach(r -> dependents.add("rule '" + r.description + "'"));
            }
            default -> throw new AuthoringException("Unsupported element kind " + kind);
        }
        return new RemovalPlan(kind, durableId, label, dependents, blockers);
    }

    /** UC-009: retire the element and its dependents at one instant. */
    @Transactional
    public RemovalPlan remove(int surveyId, StructureNode.Kind kind, int durableId) {
        RemovalPlan plan = planRemoval(surveyId, kind, durableId);
        if (plan.blocked()) {
            throw new AuthoringException("Cannot remove " + plan.label() + ": " + String.join("; ", plan.blockers()));
        }
        OffsetDateTime now = OffsetDateTime.now();
        switch (kind) {
            case QUESTION -> {
                List<SectionsQuestion> sqs = currentAssignmentsOfQuestion(surveyId, durableId);
                relationshipsTouching(surveyId, sqs.stream().map(sq -> sq.sectionsQuestionId).toList(), null, null).forEach(r -> r.effectiveTo = now);
                sqs.forEach(sq -> sq.effectiveTo = now);
                question(surveyId, durableId).ifPresent(q -> q.effectiveTo = now);
            }
            case SECTION -> {
                List<StepsSections> sss = currentStepAssignmentsOfSection(surveyId, durableId);
                List<SectionsQuestion> sqs = currentQuestionAssignmentsOfSection(surveyId, durableId);
                relationshipsTouching(surveyId, sqs.stream().map(sq -> sq.sectionsQuestionId).toList(),
                        sss.stream().map(ss -> ss.stepsSectionsId).toList(), null).forEach(r -> r.effectiveTo = now);
                sqs.forEach(sq -> sq.effectiveTo = now);
                sss.forEach(ss -> ss.effectiveTo = now);
                section(surveyId, durableId).ifPresent(s -> s.effectiveTo = now);
            }
            case STEP -> {
                List<StepsSections> sss = currentSectionAssignmentsOfStep(surveyId, durableId);
                relationshipsTouching(surveyId, List.of(), sss.stream().map(ss -> ss.stepsSectionsId).toList(), durableId).forEach(r -> r.effectiveTo = now);
                sss.forEach(ss -> ss.effectiveTo = now);
                step(surveyId, durableId).ifPresent(s -> s.effectiveTo = now);
            }
            default -> throw new AuthoringException("Unsupported element kind " + kind);
        }
        em.flush();
        return plan;
    }

    /** Retire a select group (refused while a current question uses it) and its items. */
    @Transactional
    public void removeSelectGroup(int surveyId, int durableId) {
        long users = Question.count("surveyId = ?1 and selectGroupId = ?2 and effectiveTo = ?3", surveyId, durableId, StructuralElement.SENTINEL);
        if (users > 0) {
            throw new AuthoringException("This list is still used by " + users + " question(s); change or remove them first");
        }
        OffsetDateTime now = OffsetDateTime.now();
        SelectItem.<SelectItem>list("surveyId = ?1 and selectGroupId = ?2 and effectiveTo = ?3", surveyId, durableId, StructuralElement.SENTINEL)
                .forEach(i -> i.effectiveTo = now);
        selectGroup(surveyId, durableId).ifPresent(g -> g.effectiveTo = now);
        em.flush();
    }

    @Transactional
    public void removeSelectItem(int surveyId, int durableId) {
        selectItem(surveyId, durableId).orElseThrow(() -> new AuthoringException("Option not found")).effectiveTo = OffsetDateTime.now();
        em.flush();
    }

    // ---- restore --------------------------------------------------------------------------

    /**
     * UC-010: reopen the element and the dependents retired with it (same closing instant).
     * Parents must be current (BR-002).
     */
    @Transactional
    public void restore(int surveyId, StructureNode.Kind kind, int durableId) {
        StructuralElement element = switch (kind) {
            case QUESTION -> question(surveyId, durableId).orElse(null);
            case SECTION -> section(surveyId, durableId).orElse(null);
            case STEP -> step(surveyId, durableId).orElse(null);
        };
        if (element == null) {
            throw new AuthoringException("Element not found");
        }
        if (element.isCurrent()) {
            return;
        }
        OffsetDateTime closedAt = element.effectiveTo;
        element.effectiveTo = StructuralElement.SENTINEL;
        switch (kind) {
            case QUESTION -> SectionsQuestion.<SectionsQuestion>list("surveyId = ?1 and questionId = ?2 and effectiveTo = ?3", surveyId, durableId, closedAt)
                    .forEach(sq -> {
                        if (section(surveyId, sq.sectionId).map(StructuralElement::isCurrent).orElse(false)) {
                            sq.effectiveTo = StructuralElement.SENTINEL;
                            reopenRelationships(surveyId, sq.sectionsQuestionId, null, closedAt);
                        } else {
                            throw new AuthoringException("Restore the section this question belonged to first");
                        }
                    });
            case SECTION -> {
                StepsSections.<StepsSections>list("surveyId = ?1 and sectionId = ?2 and effectiveTo = ?3", surveyId, durableId, closedAt).forEach(ss -> {
                    if (!step(surveyId, ss.stepId).map(StructuralElement::isCurrent).orElse(false)) {
                        throw new AuthoringException("Restore the step this section belonged to first");
                    }
                    ss.effectiveTo = StructuralElement.SENTINEL;
                    reopenRelationships(surveyId, null, ss.stepsSectionsId, closedAt);
                });
                SectionsQuestion.<SectionsQuestion>list("surveyId = ?1 and sectionId = ?2 and effectiveTo = ?3", surveyId, durableId, closedAt).forEach(sq -> {
                    sq.effectiveTo = StructuralElement.SENTINEL;
                    reopenRelationships(surveyId, sq.sectionsQuestionId, null, closedAt);
                });
            }
            case STEP -> StepsSections.<StepsSections>list("surveyId = ?1 and stepId = ?2 and effectiveTo = ?3", surveyId, durableId, closedAt).forEach(ss -> {
                ss.effectiveTo = StructuralElement.SENTINEL;
                reopenRelationships(surveyId, null, ss.stepsSectionsId, closedAt);
            });
        }
        em.flush();
    }

    // ---- purge ---------------------------------------------------------------------------

    /** UC-009 A3: physically delete every retired row of a survey that has never been exported. */
    @Transactional
    public int purgeRemoved(int surveyId) {
        Survey s = Survey.findById(surveyId);
        if (s == null) {
            throw new AuthoringException("Survey not found");
        }
        if (s.publishedBy != null && s.publishedComment != null) {
            throw new AuthoringException("This survey has been exported; removed elements are kept so sites can retire them");
        }
        int n = 0;
        for (String table : List.of("relationships", "sections_questions", "steps_sections", "questions", "select_items", "select_groups", "sections", "steps")) {
            n += em.createNativeQuery("DELETE FROM survey." + table + " WHERE survey_id = ?1 AND effective_to <> '9999-12-31 23:59:59+00'")
                    .setParameter(1, surveyId).executeUpdate();
        }
        em.flush();
        return n;
    }

    // ---- helpers ---------------------------------------------------------------------------

    private List<SectionsQuestion> currentAssignmentsOfQuestion(int surveyId, int questionId) {
        return SectionsQuestion.list("surveyId = ?1 and questionId = ?2 and effectiveTo = ?3", surveyId, questionId, StructuralElement.SENTINEL);
    }

    private List<SectionsQuestion> currentQuestionAssignmentsOfSection(int surveyId, int sectionId) {
        return SectionsQuestion.list("surveyId = ?1 and sectionId = ?2 and effectiveTo = ?3", surveyId, sectionId, StructuralElement.SENTINEL);
    }

    private List<StepsSections> currentStepAssignmentsOfSection(int surveyId, int sectionId) {
        return StepsSections.list("surveyId = ?1 and sectionId = ?2 and effectiveTo = ?3", surveyId, sectionId, StructuralElement.SENTINEL);
    }

    private List<StepsSections> currentSectionAssignmentsOfStep(int surveyId, int stepId) {
        return StepsSections.list("surveyId = ?1 and stepId = ?2 and effectiveTo = ?3", surveyId, stepId, StructuralElement.SENTINEL);
    }

    private List<Relationship> relationshipsTouching(int surveyId, List<Integer> sqIds, List<Integer> ssIds, Integer stepId) {
        List<Relationship> out = new ArrayList<>();
        for (Relationship r : Relationship.<Relationship>list("surveyId = ?1 and effectiveTo = ?2", surveyId, StructuralElement.SENTINEL)) {
            boolean hit = (sqIds != null && (sqIds.contains(r.upstreamSqId) || sqIds.contains(r.downstreamSqId)))
                    || (ssIds != null && ssIds.contains(r.downstreamSsId))
                    || (stepId != null && (stepId.equals(r.upstreamStepId) || stepId.equals(r.downstreamStepId)));
            if (hit) {
                out.add(r);
            }
        }
        return out;
    }

    private void reopenRelationships(int surveyId, Integer sqId, Integer ssId, OffsetDateTime closedAt) {
        for (Relationship r : Relationship.<Relationship>list("surveyId = ?1 and effectiveTo = ?2", surveyId, closedAt)) {
            boolean hit = (sqId != null && (sqId.equals(r.upstreamSqId) || sqId.equals(r.downstreamSqId)))
                    || (ssId != null && ssId.equals(r.downstreamSsId));
            if (hit) {
                r.effectiveTo = StructuralElement.SENTINEL;
            }
        }
    }
}
