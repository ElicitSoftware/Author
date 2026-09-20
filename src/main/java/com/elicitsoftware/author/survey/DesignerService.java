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

import com.elicitsoftware.author.AuthoringException;
import com.elicitsoftware.author.ConstraintViolationTranslator;
import com.elicitsoftware.author.definition.ExportValidation;
import com.elicitsoftware.model.Question;
import com.elicitsoftware.model.Relationship;
import com.elicitsoftware.model.Section;
import com.elicitsoftware.model.SectionsQuestion;
import com.elicitsoftware.model.Step;
import com.elicitsoftware.model.StepsSections;
import com.elicitsoftware.model.StructuralElement;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Write side of the designer board: create, place, copy, reorder, rename and remove steps,
 * sections and questions (UC-011, UC-015, UC-016, UC-018). Every operation takes a
 * {@code position} — the 0-based index the element should occupy among its current siblings
 * after the operation (clamped to the end) — and leaves the container renumbered 1..n with
 * derived display keys ({@link DisplayOrdering}).
 */
@ApplicationScoped
public class DesignerService {

    @Inject
    EntityManager em;

    @Inject
    ElementService elements;

    @Inject
    DisplayOrdering ordering;

    @Inject
    RenameLedger renames;

    // ---- create ----------------------------------------------------------------------------

    /** UC-016 / UC-015 A4 for steps: a new step placed at {@code position} among the current steps. */
    @Transactional
    public Step createStep(int surveyId, Step draft, int position) {
        validate(draft);
        List<Step> steps = ordering.currentSteps(surveyId);
        Step step = new Step();
        step.surveyId = surveyId;
        copy(draft, step);
        step.displayOrder = steps.stream().map(s -> s.displayOrder).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO).add(BigDecimal.ONE);
        elements.create(step);
        steps.add(Math.min(Math.max(position, 0), steps.size()), step);
        ordering.renumberSurvey(surveyId, steps);
        return step;
    }

    /** UC-015 A4: a new section mounted in {@code stepId} at {@code position}. */
    @Transactional
    public Section createSectionIn(int surveyId, int stepId, Section draft, int position) {
        validate(draft);
        Step step = currentStep(surveyId, stepId);
        Section section = new Section();
        section.surveyId = surveyId;
        copy(draft, section);
        section.displayOrder = nextSectionOrder(surveyId);
        elements.create(section);
        mount(surveyId, step, section.sectionId, position);
        return section;
    }

    /** UC-011: a new question assigned to {@code sectionId} at {@code position}. */
    @Transactional
    public Question createQuestionIn(int surveyId, int sectionId, Question draft, int position) {
        validate(draft);
        currentSection(surveyId, sectionId);
        Question question = new Question();
        question.surveyId = surveyId;
        copy(draft, question);
        elements.create(question);
        assign(surveyId, sectionId, question.questionId, position);
        return question;
    }

    // ---- place -----------------------------------------------------------------------------

    /**
     * UC-015 main flow (BR-001, BR-002): a brand-new section in {@code targetStepId} carrying
     * the source's name, dimension name and description and the same questions, no rules.
     */
    @Transactional
    public Section duplicateSectionInto(int surveyId, int sourceSectionId, int targetStepId, int position) {
        Section source = currentSection(surveyId, sourceSectionId);
        Section copy = new Section();
        copy.name = source.name;
        copy.dimensionName = source.dimensionName;
        copy.description = source.description;
        Section created = createSectionIn(surveyId, targetStepId, copy, position);
        int order = 0;
        for (SectionsQuestion sq : ordering.currentQuestions(surveyId, sourceSectionId)) {
            SectionsQuestion assignment = new SectionsQuestion();
            assignment.surveyId = surveyId;
            assignment.sectionId = created.sectionId;
            assignment.questionId = sq.questionId;
            assignment.displayOrder = BigDecimal.valueOf(++order);
            elements.create(assignment);
        }
        return created;
    }

    /** UC-015 A1 (BR-003): mount an existing section in another step; once per step (A2). */
    @Transactional
    public StepsSections mountSection(int surveyId, int sectionId, int targetStepId, int position) {
        currentSection(surveyId, sectionId);
        Step step = currentStep(surveyId, targetStepId);
        if (ordering.currentLane(surveyId, targetStepId).stream().anyMatch(ss -> ss.sectionId == sectionId)) {
            throw new AuthoringException("Step '" + step.name + "' already contains this section; a section can be mounted once per step");
        }
        return mount(surveyId, step, sectionId, position);
    }

    /** UC-011 A4 (BR-001): a second assignment of the same question in another section. */
    @Transactional
    public SectionsQuestion assignQuestionInto(int surveyId, int questionId, int targetSectionId, int position) {
        currentQuestion(surveyId, questionId);
        Section section = currentSection(surveyId, targetSectionId);
        if (ordering.currentQuestions(surveyId, targetSectionId).stream().anyMatch(sq -> sq.questionId == questionId)) {
            throw new AuthoringException("Section '" + section.name + "' already contains this question");
        }
        return assign(surveyId, targetSectionId, questionId, position);
    }

    /** UC-015 A3: take a section out of one step; its last mount retires the section too. */
    @Transactional
    public void retireSectionAssignment(int surveyId, int stepsSectionsId) {
        StepsSections ss = StepsSections.<StepsSections>find("surveyId = ?1 and stepsSectionsId = ?2 and effectiveTo = ?3",
                surveyId, stepsSectionsId, StructuralElement.SENTINEL).firstResultOptional()
                .orElseThrow(() -> new AuthoringException("Section assignment not found"));
        Step step = currentStep(surveyId, ss.stepId);
        if (elements.currentStepAssignmentsOfSection(surveyId, ss.sectionId).size() == 1) {
            // Last mount: retire the section with its assignments and rules at one instant, so that
            // UC-010 restore of the section brings this mount back with it.
            elements.remove(surveyId, StructureNode.Kind.SECTION, ss.sectionId);
        } else {
            OffsetDateTime now = OffsetDateTime.now();
            elements.relationshipsTouching(surveyId, List.of(), List.of(stepsSectionsId), null).forEach(r -> r.effectiveTo = now);
            ss.effectiveTo = now;
            em.flush();
        }
        ordering.renumberLane(surveyId, step, ordering.currentLane(surveyId, step.stepId));
    }

    /** UC-011 A5: take a question out of one section; its last assignment retires the question too. */
    @Transactional
    public void retireQuestionAssignment(int surveyId, int sectionsQuestionId) {
        SectionsQuestion sq = SectionsQuestion.<SectionsQuestion>find("surveyId = ?1 and sectionsQuestionId = ?2 and effectiveTo = ?3",
                surveyId, sectionsQuestionId, StructuralElement.SENTINEL).firstResultOptional()
                .orElseThrow(() -> new AuthoringException("Question assignment not found"));
        if (elements.currentAssignmentsOfQuestion(surveyId, sq.questionId).size() == 1) {
            // Last assignment: retire the question with its assignment and rules at one instant, so
            // that UC-010 restore of the question brings the assignment back with it.
            elements.remove(surveyId, StructureNode.Kind.QUESTION, sq.questionId);
        } else {
            OffsetDateTime now = OffsetDateTime.now();
            elements.relationshipsTouching(surveyId, List.of(sectionsQuestionId), List.of(), null).forEach(r -> r.effectiveTo = now);
            sq.effectiveTo = now;
            em.flush();
        }
        ordering.renumberSection(ordering.currentQuestions(surveyId, sq.sectionId));
    }

    /** Remove a step (UC-009 cascade) and renumber the survey; the board's Remove step. */
    @Transactional
    public ElementService.RemovalPlan removeStep(int surveyId, int stepId) {
        ElementService.RemovalPlan plan = elements.remove(surveyId, StructureNode.Kind.STEP, stepId);
        ordering.renumberSurvey(surveyId);
        return plan;
    }

    /** Remove a section everywhere it is mounted (UC-009 cascade) and renumber; also the Undo of a copy. */
    @Transactional
    public ElementService.RemovalPlan removeSection(int surveyId, int sectionId) {
        ElementService.RemovalPlan plan = elements.remove(surveyId, StructureNode.Kind.SECTION, sectionId);
        ordering.renumberSurvey(surveyId);
        return plan;
    }

    // ---- reorder (UC-016) ------------------------------------------------------------------

    @Transactional
    public void moveStep(int surveyId, int stepId, int newIndex) {
        List<Step> steps = ordering.currentSteps(surveyId);
        Step step = steps.stream().filter(s -> s.stepId == stepId).findFirst()
                .orElseThrow(() -> new AuthoringException("Step not found"));
        if (!reposition(steps, step, newIndex)) {
            return;
        }
        ordering.renumberSurvey(surveyId, steps);
    }

    @Transactional
    public void moveSectionWithinStep(int surveyId, int stepsSectionsId, int newIndex) {
        StepsSections ss = StepsSections.<StepsSections>find("surveyId = ?1 and stepsSectionsId = ?2 and effectiveTo = ?3",
                surveyId, stepsSectionsId, StructuralElement.SENTINEL).firstResultOptional()
                .orElseThrow(() -> new AuthoringException("Section assignment not found"));
        Step step = currentStep(surveyId, ss.stepId);
        List<StepsSections> lane = ordering.currentLane(surveyId, step.stepId);
        StepsSections managed = lane.stream().filter(x -> Objects.equals(x.stepsSectionsId, stepsSectionsId)).findFirst().orElseThrow();
        if (!reposition(lane, managed, newIndex)) {
            return;
        }
        ordering.renumberLane(surveyId, step, lane);
    }

    @Transactional
    public void moveQuestionWithinSection(int surveyId, int sectionsQuestionId, int newIndex) {
        SectionsQuestion sq = SectionsQuestion.<SectionsQuestion>find("surveyId = ?1 and sectionsQuestionId = ?2 and effectiveTo = ?3",
                surveyId, sectionsQuestionId, StructuralElement.SENTINEL).firstResultOptional()
                .orElseThrow(() -> new AuthoringException("Question assignment not found"));
        List<SectionsQuestion> rows = ordering.currentQuestions(surveyId, sq.sectionId);
        SectionsQuestion managed = rows.stream().filter(x -> Objects.equals(x.sectionsQuestionId, sectionsQuestionId)).findFirst().orElseThrow();
        if (!reposition(rows, managed, newIndex)) {
            return;
        }
        ordering.renumberSection(rows);
    }

    // ---- edit ------------------------------------------------------------------------------

    /** UC-018: name, dimension name and description; a changed name is recorded for the export warning. */
    @Transactional
    public Step rename(int surveyId, Step edited) {
        validate(edited);
        Step step = currentStep(surveyId, edited.stepId);
        if (!Objects.equals(step.name, edited.name)) {
            renames.record(surveyId, "steps", step.stepId, step.name, edited.name);
        }
        copy(edited, step);
        return flushTranslated(step);
    }

    @Transactional
    public Section rename(int surveyId, Section edited) {
        validate(edited);
        Section section = currentSection(surveyId, edited.sectionId);
        if (!Objects.equals(section.name, edited.name)) {
            renames.record(surveyId, "sections", section.sectionId, section.name, edited.name);
        }
        copy(edited, section);
        return flushTranslated(section);
    }

    /** UC-014 steps 6-7 / BR-001: give a choice question its list of options. */
    @Transactional
    public Question assignList(int surveyId, int questionId, int selectGroupId) {
        Question question = currentQuestion(surveyId, questionId);
        String type = question.questionType == null ? "" : question.questionType.name;
        if (!com.elicitsoftware.author.definition.ExportValidation.CHOICE_TYPES.contains(type)) {
            throw new AuthoringException("A " + (type.isEmpty() ? "question of this type" : type + " question") + " does not take a list of options");
        }
        elements.selectGroup(surveyId, selectGroupId).filter(StructuralElement::isCurrent)
                .orElseThrow(() -> new AuthoringException("The list no longer exists"));
        question.selectGroupId = selectGroupId;
        return flushTranslated(question);
    }

    /** UC-011 A2: edit a question in place (same key, same durable id). */
    @Transactional
    public Question update(int surveyId, Question edited) {
        validate(edited);
        Question question = currentQuestion(surveyId, edited.questionId);
        copy(edited, question);
        return flushTranslated(question);
    }

    // ---- lookups ---------------------------------------------------------------------------

    public Step currentStep(int surveyId, int stepId) {
        return elements.step(surveyId, stepId).filter(StructuralElement::isCurrent)
                .orElseThrow(() -> new AuthoringException("Step not found or removed"));
    }

    public Section currentSection(int surveyId, int sectionId) {
        return elements.section(surveyId, sectionId).filter(StructuralElement::isCurrent)
                .orElseThrow(() -> new AuthoringException("Section not found or removed"));
    }

    public Question currentQuestion(int surveyId, int questionId) {
        return elements.question(surveyId, questionId).filter(StructuralElement::isCurrent)
                .orElseThrow(() -> new AuthoringException("Question not found or removed"));
    }

    /** Current rules that read from or target the given assignments or step, for badges and menus. */
    public List<Relationship> rulesTouching(int surveyId, List<Integer> sqIds, List<Integer> ssIds, Integer stepId) {
        return elements.relationshipsTouching(surveyId, sqIds, ssIds, stepId);
    }

    // ---- internals -------------------------------------------------------------------------

    private StepsSections mount(int surveyId, Step step, int sectionId, int position) {
        List<StepsSections> lane = ordering.currentLane(surveyId, step.stepId);
        StepsSections ss = new StepsSections();
        ss.surveyId = surveyId;
        ss.stepId = step.stepId;
        ss.sectionId = sectionId;
        ss.stepDisplayOrder = step.displayOrder;
        int next = lane.stream().map(x -> x.sectionDisplayOrder.intValue()).max(Integer::compareTo).orElse(0) + 1;
        ss.sectionDisplayOrder = BigDecimal.valueOf(next);
        ss.displayKey = DisplayOrdering.displayKey(surveyId, step.displayOrder.intValue(), next);
        elements.create(ss);
        lane.add(Math.min(Math.max(position, 0), lane.size()), ss);
        ordering.renumberLane(surveyId, step, lane);
        return ss;
    }

    private SectionsQuestion assign(int surveyId, int sectionId, int questionId, int position) {
        List<SectionsQuestion> rows = ordering.currentQuestions(surveyId, sectionId);
        SectionsQuestion sq = new SectionsQuestion();
        sq.surveyId = surveyId;
        sq.sectionId = sectionId;
        sq.questionId = questionId;
        sq.displayOrder = BigDecimal.valueOf(rows.stream().map(x -> x.displayOrder.intValue()).max(Integer::compareTo).orElse(0) + 1);
        elements.create(sq);
        rows.add(Math.min(Math.max(position, 0), rows.size()), sq);
        ordering.renumberSection(rows);
        return sq;
    }

    /** Moves {@code item} to {@code newIndex} in {@code list}; false when it already sits there (UC-016 A2). */
    private static <T> boolean reposition(List<T> list, T item, int newIndex) {
        int from = list.indexOf(item);
        int to = Math.min(Math.max(newIndex, 0), list.size() - 1);
        if (from == to) {
            return false;
        }
        list.remove(from);
        list.add(to, item);
        return true;
    }

    private BigDecimal nextSectionOrder(int surveyId) {
        return Section.<Section>list("surveyId = ?1 and effectiveTo = ?2", surveyId, StructuralElement.SENTINEL).stream()
                .map(s -> s.displayOrder).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO).add(BigDecimal.ONE);
    }

    private <E extends StructuralElement> E flushTranslated(E element) {
        try {
            em.flush();
        } catch (RuntimeException e) {
            throw ConstraintViolationTranslator.translate(e);
        }
        return element;
    }

    private static void validate(Step step) {
        if (step.name == null || step.name.isBlank()) {
            throw new AuthoringException("A step needs a name", "name");
        }
        if (step.name.length() > 255) {
            throw new AuthoringException("Step names are at most 255 characters", "name");
        }
    }

    private static void validate(Section section) {
        if (section.name == null || section.name.isBlank()) {
            throw new AuthoringException("A section needs a name", "name");
        }
        if (section.name.length() > 255) {
            throw new AuthoringException("Section names are at most 255 characters", "name");
        }
    }

    private static void validate(Question question) {
        if (question.questionType == null) {
            throw new AuthoringException("Choose a question type", "questionType");
        }
        if (question.text == null || question.text.isBlank()) {
            throw new AuthoringException("A question needs its text", "text");
        }
        if (ExportValidation.CHOICE_TYPES.contains(question.questionType.name) && question.selectGroupId == null) {
            throw new AuthoringException("A " + question.questionType.name + " question needs a list of options", "selectGroupId");
        }
        if (question.minValue != null && question.maxValue != null && question.minValue > question.maxValue) {
            throw new AuthoringException("The minimum cannot exceed the maximum", "minValue");
        }
    }

    private static void copy(Step from, Step to) {
        to.name = from.name.trim();
        to.dimensionName = from.dimensionName == null || from.dimensionName.isBlank() ? to.name : from.dimensionName.trim();
        to.description = from.description;
    }

    private static void copy(Section from, Section to) {
        to.name = from.name.trim();
        to.dimensionName = from.dimensionName == null || from.dimensionName.isBlank() ? to.name : from.dimensionName.trim();
        to.description = from.description;
    }

    private static void copy(Question from, Question to) {
        to.questionType = from.questionType;
        to.text = from.text;
        to.shortText = from.shortText;
        to.toolTip = from.toolTip;
        to.required = from.required;
        to.minValue = from.minValue;
        to.maxValue = from.maxValue;
        to.validationText = from.validationText;
        to.selectGroupId = from.selectGroupId;
        to.mask = from.mask;
        to.placeholder = from.placeholder;
        to.defaultValue = from.defaultValue;
        to.variant = from.variant;
    }

    /** Rules touching a section's assignments, used by the board for badges; package-visible for tests. */
    List<Integer> assignmentIdsOf(List<SectionsQuestion> rows) {
        List<Integer> ids = new ArrayList<>();
        rows.forEach(sq -> ids.add(sq.sectionsQuestionId));
        return ids;
    }
}
