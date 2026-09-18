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

    /**
     * What a restore would bring back with the element (UC-010 step 3) and what stops it: a parent
     * that is itself still removed (UC-010 step 2 / BR-002).
     */
    public record RestorePlan(StructureNode.Kind kind, Integer durableId, String label, List<String> dependents, List<String> blockers) {
        public boolean blocked() {
            return !blockers.isEmpty();
        }
    }

    @Inject
    EntityManager em;

    @Inject
    DisplayOrdering ordering;

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
                sqs.forEach(sq -> dependents.add("assignment of the question to section " + sectionName(surveyId, sq.sectionId)));
                relationshipsTouching(surveyId, sqs.stream().map(sq -> sq.sectionsQuestionId).toList(), null, null)
                        .forEach(r -> dependents.add("rule '" + r.description + "'"));
            }
            case SECTION -> {
                Section s = section(surveyId, durableId).orElseThrow(() -> new AuthoringException("Section not found"));
                label = "Section '" + s.name + "'";
                List<StepsSections> sss = currentStepAssignmentsOfSection(surveyId, durableId);
                sss.forEach(ss -> dependents.add("assignment of the section to step " + stepName(surveyId, ss.stepId)));
                List<SectionsQuestion> sqs = currentQuestionAssignmentsOfSection(surveyId, durableId);
                sqs.forEach(sq -> dependents.add("assignment of question " + questionName(surveyId, sq.questionId) + " to the section"));
                relationshipsTouching(surveyId, sqs.stream().map(sq -> sq.sectionsQuestionId).toList(),
                        sss.stream().map(ss -> ss.stepsSectionsId).toList(), null)
                        .forEach(r -> dependents.add("rule '" + r.description + "'"));
            }
            case STEP -> {
                Step st = step(surveyId, durableId).orElseThrow(() -> new AuthoringException("Step not found"));
                label = "Step '" + st.name + "'";
                List<StepsSections> sss = currentSectionAssignmentsOfStep(surveyId, durableId);
                sss.forEach(ss -> dependents.add("assignment of section " + sectionName(surveyId, ss.sectionId) + " to the step"));
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

    /** Computes what a restore brings back and what blocks it, without changing anything. */
    public RestorePlan planRestore(int surveyId, StructureNode.Kind kind, int durableId) {
        StructuralElement element = find(surveyId, kind, durableId).orElseThrow(() -> new AuthoringException("Element not found"));
        String label = labelOf(kind, element);
        List<String> dependents = new ArrayList<>();
        List<String> blockers = new ArrayList<>();
        if (element.isCurrent()) {
            return new RestorePlan(kind, durableId, label, dependents, blockers);
        }
        OffsetDateTime closedAt = element.effectiveTo;
        switch (kind) {
            case QUESTION -> {
                List<SectionsQuestion> sqs = SectionsQuestion.list("surveyId = ?1 and questionId = ?2 and effectiveTo = ?3", surveyId, durableId, closedAt);
                for (SectionsQuestion sq : sqs) {
                    if (section(surveyId, sq.sectionId).map(StructuralElement::isCurrent).orElse(false)) {
                        dependents.add("assignment of the question to section " + sectionName(surveyId, sq.sectionId));
                    } else {
                        blockers.add("section " + sectionName(surveyId, sq.sectionId) + " is removed; restore it first");
                    }
                }
                relationshipsClosedAt(surveyId, sqs.stream().map(sq -> sq.sectionsQuestionId).toList(), List.of(), null, closedAt)
                        .forEach(r -> dependents.add("rule '" + r.description + "'"));
            }
            case SECTION -> {
                List<StepsSections> sss = StepsSections.list("surveyId = ?1 and sectionId = ?2 and effectiveTo = ?3", surveyId, durableId, closedAt);
                for (StepsSections ss : sss) {
                    if (step(surveyId, ss.stepId).map(StructuralElement::isCurrent).orElse(false)) {
                        dependents.add("assignment of the section to step " + stepName(surveyId, ss.stepId));
                    } else {
                        blockers.add("step " + stepName(surveyId, ss.stepId) + " is removed; restore it first");
                    }
                }
                List<SectionsQuestion> sqs = SectionsQuestion.list("surveyId = ?1 and sectionId = ?2 and effectiveTo = ?3", surveyId, durableId, closedAt);
                sqs.forEach(sq -> dependents.add("assignment of question " + questionName(surveyId, sq.questionId) + " to the section"));
                relationshipsClosedAt(surveyId, sqs.stream().map(sq -> sq.sectionsQuestionId).toList(),
                        sss.stream().map(ss -> ss.stepsSectionsId).toList(), null, closedAt)
                        .forEach(r -> dependents.add("rule '" + r.description + "'"));
            }
            case STEP -> {
                List<StepsSections> sss = StepsSections.list("surveyId = ?1 and stepId = ?2 and effectiveTo = ?3", surveyId, durableId, closedAt);
                sss.forEach(ss -> dependents.add("assignment of section " + sectionName(surveyId, ss.sectionId) + " to the step"));
                relationshipsClosedAt(surveyId, List.of(), sss.stream().map(ss -> ss.stepsSectionsId).toList(), durableId, closedAt)
                        .forEach(r -> dependents.add("rule '" + r.description + "'"));
            }
            default -> throw new AuthoringException("Unsupported element kind " + kind);
        }
        return new RestorePlan(kind, durableId, label, dependents, blockers);
    }

    /**
     * UC-010: reopen the element and the dependents retired with it (same closing instant).
     * Parents must be current (BR-002); {@link #planRestore} says which are not.
     */
    @Transactional
    public RestorePlan restore(int surveyId, StructureNode.Kind kind, int durableId) {
        RestorePlan plan = planRestore(surveyId, kind, durableId);
        if (plan.blocked()) {
            throw new AuthoringException("Cannot restore " + plan.label() + ": " + String.join("; ", plan.blockers()));
        }
        StructuralElement element = find(surveyId, kind, durableId).orElseThrow(() -> new AuthoringException("Element not found"));
        if (element.isCurrent()) {
            return plan;
        }
        OffsetDateTime closedAt = element.effectiveTo;
        element.effectiveTo = StructuralElement.SENTINEL;
        switch (kind) {
            case QUESTION -> {
                List<SectionsQuestion> sqs = SectionsQuestion.list("surveyId = ?1 and questionId = ?2 and effectiveTo = ?3", surveyId, durableId, closedAt);
                relationshipsClosedAt(surveyId, sqs.stream().map(sq -> sq.sectionsQuestionId).toList(), List.of(), null, closedAt)
                        .forEach(r -> r.effectiveTo = StructuralElement.SENTINEL);
                sqs.forEach(sq -> sq.effectiveTo = StructuralElement.SENTINEL);
            }
            case SECTION -> {
                List<StepsSections> sss = StepsSections.list("surveyId = ?1 and sectionId = ?2 and effectiveTo = ?3", surveyId, durableId, closedAt);
                List<SectionsQuestion> sqs = SectionsQuestion.list("surveyId = ?1 and sectionId = ?2 and effectiveTo = ?3", surveyId, durableId, closedAt);
                relationshipsClosedAt(surveyId, sqs.stream().map(sq -> sq.sectionsQuestionId).toList(),
                        sss.stream().map(ss -> ss.stepsSectionsId).toList(), null, closedAt)
                        .forEach(r -> r.effectiveTo = StructuralElement.SENTINEL);
                sqs.forEach(sq -> sq.effectiveTo = StructuralElement.SENTINEL);
                // A reopened assignment's key may now belong to a current one: park it, renumber below.
                sss.forEach(ss -> {
                    ss.effectiveTo = StructuralElement.SENTINEL;
                    ss.displayKey = DisplayOrdering.parkedKey(ss);
                });
            }
            case STEP -> {
                List<StepsSections> sss = StepsSections.list("surveyId = ?1 and stepId = ?2 and effectiveTo = ?3", surveyId, durableId, closedAt);
                relationshipsClosedAt(surveyId, List.of(), sss.stream().map(ss -> ss.stepsSectionsId).toList(), durableId, closedAt)
                        .forEach(r -> r.effectiveTo = StructuralElement.SENTINEL);
                sss.forEach(ss -> {
                    ss.effectiveTo = StructuralElement.SENTINEL;
                    ss.displayKey = DisplayOrdering.parkedKey(ss);
                });
                ((Step) element).displayOrder = DisplayOrdering.parkedOrder((Step) element);
            }
            default -> throw new AuthoringException("Unsupported element kind " + kind);
        }
        em.flush();
        if (kind != StructureNode.Kind.QUESTION) {
            // UC-016 BR-001: a restored step or section takes the next free position; keys follow.
            ordering.renumberSurvey(surveyId);
        }
        return plan;
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

    List<SectionsQuestion> currentAssignmentsOfQuestion(int surveyId, int questionId) {
        return SectionsQuestion.list("surveyId = ?1 and questionId = ?2 and effectiveTo = ?3", surveyId, questionId, StructuralElement.SENTINEL);
    }

    List<SectionsQuestion> currentQuestionAssignmentsOfSection(int surveyId, int sectionId) {
        return SectionsQuestion.list("surveyId = ?1 and sectionId = ?2 and effectiveTo = ?3", surveyId, sectionId, StructuralElement.SENTINEL);
    }

    List<StepsSections> currentStepAssignmentsOfSection(int surveyId, int sectionId) {
        return StepsSections.list("surveyId = ?1 and sectionId = ?2 and effectiveTo = ?3", surveyId, sectionId, StructuralElement.SENTINEL);
    }

    List<StepsSections> currentSectionAssignmentsOfStep(int surveyId, int stepId) {
        return StepsSections.list("surveyId = ?1 and stepId = ?2 and effectiveTo = ?3", surveyId, stepId, StructuralElement.SENTINEL);
    }

    List<Relationship> relationshipsTouching(int surveyId, List<Integer> sqIds, List<Integer> ssIds, Integer stepId) {
        List<Relationship> out = new ArrayList<>();
        for (Relationship r : Relationship.<Relationship>list("surveyId = ?1 and effectiveTo = ?2", surveyId, StructuralElement.SENTINEL)) {
            boolean hit = has(sqIds, r.upstreamSqId) || has(sqIds, r.downstreamSqId)
                    || has(ssIds, r.downstreamSsId)
                    || (stepId != null && (stepId.equals(r.upstreamStepId) || stepId.equals(r.downstreamStepId)));
            if (hit) {
                out.add(r);
            }
        }
        return out;
    }

    /** Rules closed at exactly {@code closedAt} that touch the given assignments or step. */
    private List<Relationship> relationshipsClosedAt(int surveyId, List<Integer> sqIds, List<Integer> ssIds, Integer stepId, OffsetDateTime closedAt) {
        List<Relationship> out = new ArrayList<>();
        for (Relationship r : Relationship.<Relationship>list("surveyId = ?1 and effectiveTo = ?2", surveyId, closedAt)) {
            boolean hit = has(sqIds, r.upstreamSqId) || has(sqIds, r.downstreamSqId)
                    || has(ssIds, r.downstreamSsId)
                    || (stepId != null && (stepId.equals(r.upstreamStepId) || stepId.equals(r.downstreamStepId)));
            if (hit) {
                out.add(r);
            }
        }
        return out;
    }

    /** Null-safe membership: a rule's optional references are null, and {@code List.of()} rejects {@code contains(null)}. */
    private static boolean has(List<Integer> ids, Integer id) {
        return ids != null && id != null && ids.contains(id);
    }

    private Optional<? extends StructuralElement> find(int surveyId, StructureNode.Kind kind, int durableId) {
        return switch (kind) {
            case QUESTION -> question(surveyId, durableId);
            case SECTION -> section(surveyId, durableId);
            case STEP -> step(surveyId, durableId);
        };
    }

    private static String labelOf(StructureNode.Kind kind, StructuralElement element) {
        return switch (kind) {
            case QUESTION -> "Question '" + ((Question) element).shortText + "'";
            case SECTION -> "Section '" + ((Section) element).name + "'";
            case STEP -> "Step '" + ((Step) element).name + "'";
        };
    }

    // Dependents are named for people (UC-009 step 3, UC-010 step 3); the id is the fallback only
    // when the referenced row is gone.
    private String sectionName(int surveyId, Integer sectionId) {
        return section(surveyId, sectionId).map(s -> "'" + s.name + "'").orElse("#" + sectionId);
    }

    private String stepName(int surveyId, Integer stepId) {
        return step(surveyId, stepId).map(s -> "'" + s.name + "'").orElse("#" + stepId);
    }

    private String questionName(int surveyId, Integer questionId) {
        return question(surveyId, questionId).map(q -> "'" + q.shortText + "'").orElse("#" + questionId);
    }
}
