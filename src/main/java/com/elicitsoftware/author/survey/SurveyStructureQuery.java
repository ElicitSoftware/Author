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
import com.elicitsoftware.model.Relationship;
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
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Read model for the structure tree (UC-006): one query per table, labels resolved through
 * durable-id maps, steps → sections → questions in display order. Rows are all working copies,
 * so a durable id maps to exactly one row; retired rows are included only on request.
 */
@ApplicationScoped
public class SurveyStructureQuery {

    /**
     * Tree as an ordered map: root nodes to their children (children to grandchildren).
     *
     * @param roots    the step nodes in display order
     * @param children each step and section node's children in display order
     */
    public record Tree(List<StructureNode> roots, Map<StructureNode, List<StructureNode>> children) {

        /**
         * Returns a node's children in display order.
         *
         * @param node the parent node
         * @return its children; empty for a question or a node this tree does not hold
         */
        public List<StructureNode> childrenOf(StructureNode node) {
            return children.getOrDefault(node, List.of());
        }

        /**
         * Returns every node of the tree, each step followed by its sections, each section by its questions.
         *
         * @return a new list of all nodes in board order
         */
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

        /**
         * The node for an element, preferring the container the assignment names. A section mounted in
         * two steps, or a question assigned to two sections, is two nodes; with no assignment named —
         * or one this tree does not hold — the element's first place is returned.
         *
         * @param kind         the element's kind
         * @param durableId    the element's durable id
         * @param assignmentId the mount or assignment to prefer, may be null
         * @return the node, or null when the tree holds no node of that kind and durable id
         */
        public StructureNode find(StructureNode.Kind kind, Integer durableId, Integer assignmentId) {
            return all().stream()
                    .filter(n -> n.kind() == kind && Objects.equals(n.durableId(), durableId))
                    .filter(n -> assignmentId == null || Objects.equals(n.assignmentId(), assignmentId))
                    .findFirst()
                    .orElseGet(() -> assignmentId == null ? null : find(kind, durableId, null));
        }

        /**
         * The node an assignment created: the section in that step, or the question in that section.
         *
         * @param kind         {@link StructureNode.Kind#SECTION} for a mount, {@link StructureNode.Kind#QUESTION} for an assignment
         * @param assignmentId the mount or assignment id, may be null
         * @return the node, or null when the id is null or the tree holds no such node
         */
        public StructureNode findByAssignment(StructureNode.Kind kind, Integer assignmentId) {
            return assignmentId == null ? null : all().stream()
                    .filter(n -> n.kind() == kind && Objects.equals(n.assignmentId(), assignmentId))
                    .findFirst().orElse(null);
        }

        /**
         * The element a rule points at — the innermost of its downstream path, in the mount the path
         * names (UC-019 BR-001). A rule whose path no longer resolves has no node.
         *
         * @param r the rule, may be null
         * @return the node the rule targets, or null when the rule is null or its path does not resolve
         */
        public StructureNode target(Relationship r) {
            if (r == null) {
                return null;
            }
            if (r.downstreamSqId != null) {
                List<StructureNode> rows = all().stream().filter(n -> n.kind() == StructureNode.Kind.QUESTION
                        && Objects.equals(n.assignmentId(), r.downstreamSqId)).toList();
                return rows.stream().filter(n -> n.parent() != null && Objects.equals(n.parent().assignmentId(), r.downstreamSsId))
                        .findFirst().orElse(rows.isEmpty() ? null : rows.get(0));
            }
            if (r.downstreamSsId != null) {
                return findByAssignment(StructureNode.Kind.SECTION, r.downstreamSsId);
            }
            if (r.downstreamStepId != null) {
                return find(StructureNode.Kind.STEP, r.downstreamStepId, null);
            }
            return null;
        }
    }

    /**
     * Builds a survey's structure tree: steps, the sections mounted in them, and the questions
     * assigned to those sections, each level in display order. Mounts and assignments whose
     * element is missing are left out.
     *
     * @param surveyId       the survey's id
     * @param includeRetired whether retired rows are included, marked retired on their nodes
     * @return the tree; with no roots when the survey has no steps
     */
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
            String label = questionLabel(q);
            String detail = q.questionType == null ? "" : q.questionType.name;
            StructureNode n = new StructureNode(StructureNode.Kind.QUESTION, q.questionId, sq.sectionsQuestionId,
                    label, detail, sq.displayOrder, q.questionKey, sq.isRetired() || q.isRetired(), sectionNode);
            children.get(sectionNode).add(n);
        });
        return new Tree(roots, children);
    }

    /**
     * Current sections no step mounts (UC-015 A7). They are part of the survey and they export, but
     * the board draws only what a lane holds, so the Sections panel is where they are reachable
     * (UC-006 BR-003a). The nodes have no parent and no assignment, which is exactly what they lack.
     *
     * @param surveyId the survey's id
     * @return the unmounted sections in display order; empty when every section is mounted
     */
    public List<StructureNode> unplacedSections(int surveyId) {
        Set<Integer> mounted = StepsSections.<StepsSections>list("surveyId", surveyId).stream()
                .filter(StructuralElement::isCurrent).map(ss -> ss.sectionId).collect(Collectors.toSet());
        return Section.<Section>list("surveyId", surveyId).stream()
                .filter(StructuralElement::isCurrent)
                .filter(s -> !mounted.contains(s.sectionId))
                .sorted(Comparator.comparing((Section s) -> s.displayOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(s -> new StructureNode(StructureNode.Kind.SECTION, s.sectionId, null,
                        StructureNode.nameOr(s.name, s.sectionId), s.description, s.displayOrder, s.sectionKey, false, null))
                .toList();
    }

    /**
     * Current questions no section assigns (UC-011 A7); the Questions panel is where they are reachable.
     *
     * @param surveyId the survey's id
     * @return the unassigned questions as nodes with no parent, assignment or order; empty when there are none
     */
    public List<StructureNode> unplacedQuestions(int surveyId) {
        Set<Integer> assigned = SectionsQuestion.<SectionsQuestion>list("surveyId", surveyId).stream()
                .filter(StructuralElement::isCurrent).map(sq -> sq.questionId).collect(Collectors.toSet());
        return Question.<Question>list("surveyId", surveyId).stream()
                .filter(StructuralElement::isCurrent)
                .filter(q -> !assigned.contains(q.questionId))
                .map(q -> new StructureNode(StructureNode.Kind.QUESTION, q.questionId, null, questionLabel(q),
                        q.questionType == null ? "" : q.questionType.name, null, q.questionKey, false, null))
                .toList();
    }

    /** A question's short text, else its text with the markup taken out, else its durable id. */
    private static String questionLabel(Question q) {
        String label = q.shortText != null && !q.shortText.isBlank() ? q.shortText : plain(q.text);
        return StructureNode.nameOr(label, q.questionId);
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

    /** A placeholder as the author wrote it, {@code {phrase|default}} — the phrase holds the tokens. */
    private static final java.util.regex.Pattern PLACEHOLDER = java.util.regex.Pattern.compile("\\{[^{}]*}");

    /**
     * A question's text as a one-line label: markup removed, placeholders left as written.
     *
     * <p>A token is written {@code <NAME>} inside a placeholder's phrase (UC-045 BR-003), which is
     * indistinguishable from an HTML tag on its own — {@code <b>} matches {@link Tokens#TOKEN_REF}
     * just as well as {@code <NAME>} does. Stripping every {@code <…>} therefore erased the author's
     * tokens from the structure tree, the question row and the breadcrumb: "How old is
     * {@code {<NAME>|this person}}?" was shown as "How old is { |this person}?", leaving nothing on
     * screen to say the text was personalized at all.
     *
     * <p>Position is what tells the two apart, and it is the same rule the runtime applies: a token
     * means something only inside a placeholder, so markup is stripped outside the braces and
     * whatever is between them is kept verbatim.</p>
     */
    static String plain(String html) {
        if (html == null) {
            return "";
        }
        java.util.regex.Matcher placeholder = PLACEHOLDER.matcher(html);
        StringBuilder kept = new StringBuilder();
        int at = 0;
        while (placeholder.find()) {
            kept.append(stripMarkup(html.substring(at, placeholder.start()))).append(placeholder.group());
            at = placeholder.end();
        }
        kept.append(stripMarkup(html.substring(at)));
        String text = kept.toString().replaceAll("\\s+", " ").trim();
        return text.length() > 60 ? text.substring(0, 57) + "…" : text;
    }

    private static String stripMarkup(String html) {
        return html.replaceAll("<[^>]+>", " ");
    }

    /**
     * Returns a node's display order for sorting.
     *
     * @param n the node
     * @return its order, or zero when it has none
     */
    public static BigDecimal order(StructureNode n) {
        return n.order() == null ? BigDecimal.ZERO : n.order();
    }
}
