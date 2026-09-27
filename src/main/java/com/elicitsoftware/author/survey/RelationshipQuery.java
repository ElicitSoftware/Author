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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Read model for the rule arrows (UC-020): one edge per relationship, anchored on the DOM ids
 * the board gives its nodes ({@code step-N}, {@code ss-N}, {@code sq-N}). Because a question
 * row is only rendered while its card is open, each end also carries the ids of the cards that
 * hold it, for the overlay to fall back on (UC-020 step 3). BR-001: derived, never stored — and so
 * is the {@code invalid} mark of a rule whose target is not after its origin (UC-033 BR-003).
 */
@ApplicationScoped
public class RelationshipQuery {

    public record RuleEdge(int relationshipId, UUID key, String source, List<String> sourceFallbacks,
                           String target, List<String> targetFallbacks, String action, String operator,
                           String label, String condition, String description, boolean retired,
                           boolean invalid, String problem) { }

    public List<RuleEdge> edges(int surveyId, boolean includeRetired) {
        // Names, so the arrow can read "Show <section>", "Repeat <question>", "Replace {TOKEN}".
        Map<Integer, String> stepName = new HashMap<>();
        List<Step> steps = Step.list("surveyId = ?1 and effectiveTo = ?2", surveyId, StructuralElement.SENTINEL);
        for (Step s : steps) {
            stepName.put(s.stepId, s.name);
        }
        Map<Integer, String> sectionName = new HashMap<>();
        for (Section s : Section.<Section>list("surveyId = ?1 and effectiveTo = ?2", surveyId, StructuralElement.SENTINEL)) {
            sectionName.put(s.sectionId, s.name);
        }
        Map<Integer, String> questionName = new HashMap<>();
        for (Question q : Question.<Question>list("surveyId = ?1 and effectiveTo = ?2", surveyId, StructuralElement.SENTINEL)) {
            questionName.put(q.questionId, q.shortText != null && !q.shortText.isBlank() ? q.shortText : q.text);
        }

        Map<Integer, List<String>> cardsHoldingQuestionAssignment = new HashMap<>();
        Map<Integer, List<StepsSections>> mountsBySection = new HashMap<>();
        Map<Integer, Integer> sectionIdByAssignment = new HashMap<>();  // ss id -> section durable id
        Map<Integer, Integer> stepIdByAssignment = new HashMap<>();     // ss id -> step durable id
        Map<Integer, Integer> questionIdByAssignment = new HashMap<>(); // sq id -> question durable id
        List<StepsSections> mounts = StepsSections.list("surveyId = ?1 and effectiveTo = ?2", surveyId, StructuralElement.SENTINEL);
        List<SectionsQuestion> assignments = SectionsQuestion.list("surveyId = ?1 and effectiveTo = ?2", surveyId, StructuralElement.SENTINEL);
        for (StepsSections ss : mounts) {
            mountsBySection.computeIfAbsent(ss.sectionId, k -> new ArrayList<>()).add(ss);
            sectionIdByAssignment.put(ss.stepsSectionsId, ss.sectionId);
            stepIdByAssignment.put(ss.stepsSectionsId, ss.stepId);
        }
        for (SectionsQuestion sq : assignments) {
            List<String> cards = new ArrayList<>();
            mountsBySection.getOrDefault(sq.sectionId, List.of()).forEach(ss -> cards.add("ss-" + ss.stepsSectionsId));
            cardsHoldingQuestionAssignment.put(sq.sectionsQuestionId, cards);
            questionIdByAssignment.put(sq.sectionsQuestionId, sq.questionId);
        }

        RuleOrdering.Board board = new RuleOrdering.Board(steps, mounts, assignments, sectionName, questionName);
        List<RuleEdge> edges = new ArrayList<>();
        for (Relationship r : Relationship.<Relationship>list("surveyId", surveyId)) {
            if (!includeRetired && !r.isCurrent()) {
                continue;
            }
            String target;
            String targetName;
            List<String> targetFallbacks;
            // UC-019 BR-001: the arrow lands on the innermost element of the downstream path.
            if (r.downstreamSqId != null) {
                target = "sq-" + r.downstreamSqId;
                targetName = questionName.get(questionIdByAssignment.get(r.downstreamSqId));
                Integer stepId = stepIdByAssignment.get(r.downstreamSsId);
                targetFallbacks = r.downstreamSsId != null && stepId != null
                        ? List.of("ss-" + r.downstreamSsId, "step-" + stepId)
                        : collapseFallbacks(cardsHoldingQuestionAssignment.get(r.downstreamSqId), stepIdByAssignment);
            } else if (r.downstreamSsId != null) {
                target = "ss-" + r.downstreamSsId;
                targetName = sectionName.get(sectionIdByAssignment.get(r.downstreamSsId));
                Integer stepId = stepIdByAssignment.get(r.downstreamSsId);
                targetFallbacks = stepId == null ? List.of() : List.of("step-" + stepId);
            } else {
                target = "step-" + r.downstreamStepId;
                targetName = stepName.get(r.downstreamStepId);
                targetFallbacks = List.of();
            }
            String action = r.actionType == null ? "" : r.actionType.name;
            String operator = r.operatorType == null ? "" : r.operatorType.name;
            List<String> sourceFallbacks = collapseFallbacks(cardsHoldingQuestionAssignment.get(r.upstreamSqId), stepIdByAssignment);
            // UC-033: retired rules are never validated; a current rule that points backwards is marked with its reason.
            Optional<String> problem = r.isCurrent() ? board.problem(r) : Optional.empty();
            edges.add(new RuleEdge(r.relationshipId, r.relationshipKey,
                    "sq-" + r.upstreamSqId, sourceFallbacks,
                    target, targetFallbacks, action, operator, label(r, targetName), condition(r), r.description, !r.isCurrent(),
                    problem.isPresent(), problem.map(p -> "Points backwards: " + p).orElse(null)));
        }
        edges.sort(Comparator.comparing(RuleEdge::relationshipId));
        return edges;
    }

    /**
     * Where the overlay anchors an arrow whose endpoint is hidden: the section cards that hold the
     * question, then the steps that hold those cards — so a rule to a question inside a collapsed
     * step still draws to the visible step box (UC-020 step 3).
     */
    private static List<String> collapseFallbacks(List<String> cards, Map<Integer, Integer> stepIdByAssignment) {
        if (cards == null || cards.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> fallbacks = new LinkedHashSet<>(cards);
        for (String card : cards) {
            Integer ssId = Integer.valueOf(card.substring("ss-".length()));
            Integer stepId = stepIdByAssignment.get(ssId);
            if (stepId != null) {
                fallbacks.add("step-" + stepId);
            }
        }
        return new ArrayList<>(fallbacks);
    }

    /** The arrow label reads what the rule does: {@code "Show <target>"}, {@code "Repeat <target>"}, {@code "Replace {TOKEN}"}. */
    public static String label(Relationship r, String targetName) {
        String name = targetName == null || targetName.isBlank() ? "target" : targetName;
        String action = r.actionType == null ? "" : r.actionType.name;
        return switch (action) {
            case "SHOW" -> "Show " + name;
            case "REPEAT" -> "Repeat " + name;
            case "TEXT" -> "Replace " + (r.token != null && !r.token.isBlank() ? Tokens.reference(r.token) : name);
            default -> (action.isBlank() ? "Rule" : action) + " " + name;
        };
    }

    /** UC-019 BR-003: the condition "(operator) value" and the token the rule carries, if any — shown in the tooltip. */
    public static String condition(Relationship r) {
        String op = r.operatorType == null ? "?" : switch (r.operatorType.name) {
            case "BOOLEAN" -> "is true";
            case "GREATER THAN", "GREATER_THAN" -> ">";
            case "LESS THAN", "LESS_THAN" -> "<";
            case "EQUAL" -> "=";
            case "NOT_EQUAL", "NOT EQUAL" -> "≠";
            case "FIELD_EXIST", "FIELD EXIST" -> "exists";
            case "CONTAINS" -> "∋";
            default -> r.operatorType.symbol != null && !r.operatorType.symbol.isBlank() ? r.operatorType.symbol : r.operatorType.name;
        };
        boolean unary = "is true".equals(op) || "exists".equals(op);
        String value = unary || r.referenceValue == null || r.referenceValue.isBlank() ? "" : " " + r.referenceValue;
        String token = r.token != null && !r.token.isBlank() ? " " + Tokens.reference(r.token) : "";
        return op + value + token;
    }
}
