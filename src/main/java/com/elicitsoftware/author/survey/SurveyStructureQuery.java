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

import com.elicitsoftware.model.Question;
import com.elicitsoftware.model.Section;
import com.elicitsoftware.model.SectionsQuestion;
import com.elicitsoftware.model.Step;
import com.elicitsoftware.model.StepsSections;
import com.elicitsoftware.model.StructuralElement;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Read model for the structure tree (UC-006): one query per table, labels resolved through
 * durable-id maps, steps → sections → questions in display order. Rows are all working copies,
 * so a durable id maps to exactly one row; retired rows are included only on request.
 */
@ApplicationScoped
public class SurveyStructureQuery {

    /** Tree as an ordered map: root nodes to their children (children to grandchildren). */
    public record Tree(List<StructureNode> roots, Map<StructureNode, List<StructureNode>> children) {

        public List<StructureNode> childrenOf(StructureNode node) {
            return children.getOrDefault(node, List.of());
        }

        public List<StructureNode> all() {
            List<StructureNode> out = new ArrayList<>();
            for (StructureNode r : roots) {
                out.add(r);
                for (StructureNode s : childrenOf(r)) {
                    out.add(s);
                    out.addAll(childrenOf(s));
                }
            }
            return out;
        }
    }

    public Tree tree(int surveyId, boolean includeRetired) {
        Map<Integer, Step> steps = byDurable(Step.list("surveyId", surveyId), s -> s.stepId, includeRetired);
        Map<Integer, Section> sections = byDurable(Section.list("surveyId", surveyId), s -> s.sectionId, includeRetired);
        Map<Integer, Question> questions = byDurable(Question.list("surveyId", surveyId), q -> q.questionId, includeRetired);
        List<StepsSections> stepsSections = StepsSections.<StepsSections>list("surveyId", surveyId).stream()
                .filter(ss -> includeRetired || ss.isCurrent()).toList();
        List<SectionsQuestion> sectionsQuestions = SectionsQuestion.<SectionsQuestion>list("surveyId", surveyId).stream()
                .filter(sq -> includeRetired || sq.isCurrent()).toList();

        List<StructureNode> roots = new ArrayList<>();
        Map<StructureNode, List<StructureNode>> children = new LinkedHashMap<>();
        Map<Integer, StructureNode> stepNodes = new HashMap<>();
        Map<Integer, StructureNode> sectionNodesByAssignment = new HashMap<>();

        steps.values().stream().sorted(Comparator.comparing((Step s) -> s.displayOrder)).forEach(s -> {
            StructureNode n = new StructureNode(StructureNode.Kind.STEP, s.stepId, null, s.name,
                    s.description, s.displayOrder, s.stepKey, s.isRetired(), null);
            roots.add(n);
            stepNodes.put(s.stepId, n);
            children.put(n, new ArrayList<>());
        });
        stepsSections.stream().sorted(Comparator.comparing((StepsSections ss) -> ss.stepDisplayOrder).thenComparing(ss -> ss.sectionDisplayOrder))
                .forEach(ss -> {
                    StructureNode stepNode = stepNodes.get(ss.stepId);
                    Section section = sections.get(ss.sectionId);
                    if (stepNode == null || section == null) {
                        return; // dangling assignment: reported by validation, not drawn
                    }
                    StructureNode n = new StructureNode(StructureNode.Kind.SECTION, section.sectionId, ss.stepsSectionsId,
                            section.name, ss.displayKey, ss.sectionDisplayOrder, section.sectionKey,
                            ss.isRetired() || section.isRetired(), stepNode);
                    children.get(stepNode).add(n);
                    children.put(n, new ArrayList<>());
                    sectionNodesByAssignment.put(ss.sectionId, n);
                });
        sectionsQuestions.stream().sorted(Comparator.comparing((SectionsQuestion sq) -> sq.displayOrder)).forEach(sq -> {
            StructureNode sectionNode = sectionNodesByAssignment.get(sq.sectionId);
            Question q = questions.get(sq.questionId);
            if (sectionNode == null || q == null) {
                return;
            }
            String label = q.shortText != null && !q.shortText.isBlank() ? q.shortText : plain(q.text);
            String detail = q.questionType == null ? "" : q.questionType.name;
            StructureNode n = new StructureNode(StructureNode.Kind.QUESTION, q.questionId, sq.sectionsQuestionId,
                    label, detail, sq.displayOrder, q.questionKey, sq.isRetired() || q.isRetired(), sectionNode);
            children.get(sectionNode).add(n);
        });
        return new Tree(roots, children);
    }

    private static <E extends StructuralElement> Map<Integer, E> byDurable(List<E> rows, Function<E, Integer> durable, boolean includeRetired) {
        Map<Integer, E> out = new HashMap<>();
        for (E e : rows) {
            if (includeRetired || e.isCurrent()) {
                out.put(durable.apply(e), e);
            }
        }
        return out;
    }

    static String plain(String html) {
        if (html == null) {
            return "";
        }
        String text = html.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
        return text.length() > 60 ? text.substring(0, 57) + "…" : text;
    }

    public static BigDecimal order(StructureNode n) {
        return n.order() == null ? BigDecimal.ZERO : n.order();
    }
}
