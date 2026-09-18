package com.elicitsoftware.author.definition;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Checks a survey's current elements before export (UC-007): dangling durable references,
 * relationships without exactly one downstream target, choice questions without options,
 * empty steps and sections, duplicate ordering. Errors block export; warnings do not.
 * Read-only (UC-007 BR-004).
 */
@ApplicationScoped
public class ExportValidation {

    private static final Set<String> CHOICE_TYPES = Set.of("RADIO", "COMBOBOX", "MULTI_SELECT", "CHECKBOX_GROUP");

    public List<ValidationIssue> validate(Survey survey) {
        int sid = survey.id;
        List<ValidationIssue> issues = new ArrayList<>();

        Map<Integer, Step> steps = current(Step.list("surveyId", sid), s -> s.stepId);
        Map<Integer, Section> sections = current(Section.list("surveyId", sid), s -> s.sectionId);
        Map<Integer, Question> questions = current(Question.list("surveyId", sid), q -> q.questionId);
        Map<Integer, SelectGroup> groups = current(SelectGroup.list("surveyId", sid), g -> g.selectGroupId);
        List<SelectItem> items = SelectItem.<SelectItem>list("surveyId", sid).stream().filter(StructuralElement::isCurrent).toList();
        Map<Integer, StepsSections> stepsSections = current(StepsSections.list("surveyId", sid), ss -> ss.stepsSectionsId);
        Map<Integer, SectionsQuestion> sectionsQuestions = current(SectionsQuestion.list("surveyId", sid), sq -> sq.sectionsQuestionId);
        List<Relationship> relationships = Relationship.<Relationship>list("surveyId", sid).stream().filter(StructuralElement::isCurrent).toList();

        if (steps.isEmpty()) {
            issues.add(ValidationIssue.warning("surveys", sid, "The survey has no steps"));
        }

        Set<Integer> stepsWithSections = new HashSet<>();
        Set<Integer> sectionsWithQuestions = new HashSet<>();
        Set<Integer> groupsWithItems = new HashSet<>();
        items.forEach(i -> groupsWithItems.add(i.selectGroupId));

        for (StepsSections ss : stepsSections.values()) {
            if (!steps.containsKey(ss.stepId)) {
                issues.add(ValidationIssue.error("steps_sections", ss.stepsSectionsId, "Section assignment refers to a removed or missing step"));
            }
            if (!sections.containsKey(ss.sectionId)) {
                issues.add(ValidationIssue.error("steps_sections", ss.stepsSectionsId, "Section assignment refers to a removed or missing section"));
            }
            stepsWithSections.add(ss.stepId);
        }
        for (SectionsQuestion sq : sectionsQuestions.values()) {
            if (!questions.containsKey(sq.questionId)) {
                issues.add(ValidationIssue.error("sections_questions", sq.sectionsQuestionId, "Question assignment refers to a removed or missing question"));
            }
            if (!sections.containsKey(sq.sectionId)) {
                issues.add(ValidationIssue.error("sections_questions", sq.sectionsQuestionId, "Question assignment refers to a removed or missing section"));
            }
            sectionsWithQuestions.add(sq.sectionId);
        }
        for (Question q : questions.values()) {
            String type = q.questionType == null ? "" : q.questionType.name;
            if (q.selectGroupId != null && !groups.containsKey(q.selectGroupId)) {
                issues.add(ValidationIssue.error("questions", q.questionId, label(q) + " refers to a removed or missing list"));
            } else if (CHOICE_TYPES.contains(type)) {
                if (q.selectGroupId == null) {
                    issues.add(ValidationIssue.error("questions", q.questionId, label(q) + " is a " + type + " question without a list of options"));
                } else if (!groupsWithItems.contains(q.selectGroupId)) {
                    issues.add(ValidationIssue.error("questions", q.questionId, label(q) + " uses a list that has no options"));
                }
            }
            if (q.text == null || q.text.isBlank()) {
                issues.add(ValidationIssue.error("questions", q.questionId, label(q) + " has no text"));
            }
        }
        for (SelectItem i : items) {
            if (!groups.containsKey(i.selectGroupId)) {
                issues.add(ValidationIssue.error("select_items", i.selectItemId, "Option '" + i.displayText + "' belongs to a removed or missing list"));
            }
        }
        for (Relationship r : relationships) {
            String rule = "Rule '" + r.description + "'";
            if (!r.hasExactlyOneDownstream()) {
                issues.add(ValidationIssue.error("relationships", r.relationshipId, rule + " must target exactly one step, section or question"));
            }
            if (!sectionsQuestions.containsKey(r.upstreamSqId)) {
                issues.add(ValidationIssue.error("relationships", r.relationshipId, rule + " reads from a removed or missing question assignment"));
            }
            if (r.upstreamStepId != null && !steps.containsKey(r.upstreamStepId)) {
                issues.add(ValidationIssue.error("relationships", r.relationshipId, rule + " reads from a removed or missing step"));
            }
            if (r.downstreamStepId != null && !steps.containsKey(r.downstreamStepId)) {
                issues.add(ValidationIssue.error("relationships", r.relationshipId, rule + " targets a removed or missing step"));
            }
            if (r.downstreamSsId != null && !stepsSections.containsKey(r.downstreamSsId)) {
                issues.add(ValidationIssue.error("relationships", r.relationshipId, rule + " targets a removed or missing section assignment"));
            }
            if (r.downstreamSqId != null && !sectionsQuestions.containsKey(r.downstreamSqId)) {
                issues.add(ValidationIssue.error("relationships", r.relationshipId, rule + " targets a removed or missing question assignment"));
            }
            if (r.operatorType == null || r.actionType == null) {
                issues.add(ValidationIssue.error("relationships", r.relationshipId, rule + " has no operator or action"));
            }
        }
        for (Step s : steps.values()) {
            if (!stepsWithSections.contains(s.stepId)) {
                issues.add(ValidationIssue.warning("steps", s.stepId, "Step '" + s.name + "' has no sections"));
            }
        }
        for (Section s : sections.values()) {
            if (!sectionsWithQuestions.contains(s.sectionId)) {
                issues.add(ValidationIssue.warning("sections", s.sectionId, "Section '" + s.name + "' has no questions"));
            }
        }
        duplicateOrders(issues, "steps", steps.values().stream().map(s -> Map.entry(s.stepId, "step:" + s.displayOrder)).toList());
        duplicateOrders(issues, "steps_sections", stepsSections.values().stream()
                .map(ss -> Map.entry(ss.stepsSectionsId, ss.stepId + ":" + ss.sectionDisplayOrder)).toList());
        duplicateOrders(issues, "sections_questions", sectionsQuestions.values().stream()
                .map(sq -> Map.entry(sq.sectionsQuestionId, sq.sectionId + ":" + sq.displayOrder)).toList());
        return issues;
    }

    private static void duplicateOrders(List<ValidationIssue> issues, String kind, List<Map.Entry<Integer, String>> keyed) {
        Map<String, Integer> seen = new HashMap<>();
        for (Map.Entry<Integer, String> e : keyed) {
            Integer first = seen.putIfAbsent(e.getValue(), e.getKey());
            if (first != null) {
                issues.add(ValidationIssue.warning(kind, e.getKey(), "Two elements share the same position ("
                        + e.getValue().replaceFirst("^[^:]*:", "order ") + ")"));
            }
        }
    }

    private static <E extends StructuralElement> Map<Integer, E> current(List<E> rows, Function<E, Integer> durable) {
        Map<Integer, E> out = new HashMap<>();
        for (E e : rows) {
            if (e.isCurrent()) {
                out.put(durable.apply(e), e);
            }
        }
        return out;
    }

    private static String label(Question q) {
        return "Question '" + (q.shortText != null && !q.shortText.isBlank() ? q.shortText : "#" + q.questionId) + "'";
    }
}
