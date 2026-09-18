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

import com.elicitsoftware.model.Relationship;
import com.elicitsoftware.model.SectionsQuestion;
import com.elicitsoftware.model.StepsSections;
import com.elicitsoftware.model.StructuralElement;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Read model for the rule arrows (UC-020): one edge per relationship, anchored on the DOM ids
 * the board gives its nodes ({@code step-N}, {@code ss-N}, {@code sq-N}). Because a question
 * row is only rendered while its card is open, each end also carries the ids of the cards that
 * hold it, for the overlay to fall back on (UC-020 step 3). BR-001: derived, never stored.
 */
@ApplicationScoped
public class RelationshipQuery {

    public record RuleEdge(int relationshipId, UUID key, String source, List<String> sourceFallbacks,
                           String target, List<String> targetFallbacks, String action, String operator,
                           String label, String description, boolean retired) { }

    public List<RuleEdge> edges(int surveyId, boolean includeRetired) {
        Map<Integer, List<String>> cardsHoldingQuestionAssignment = new HashMap<>();
        Map<Integer, List<StepsSections>> mountsBySection = new HashMap<>();
        for (StepsSections ss : StepsSections.<StepsSections>list("surveyId = ?1 and effectiveTo = ?2", surveyId, StructuralElement.SENTINEL)) {
            mountsBySection.computeIfAbsent(ss.sectionId, k -> new ArrayList<>()).add(ss);
        }
        for (SectionsQuestion sq : SectionsQuestion.<SectionsQuestion>list("surveyId = ?1 and effectiveTo = ?2", surveyId, StructuralElement.SENTINEL)) {
            List<String> cards = new ArrayList<>();
            mountsBySection.getOrDefault(sq.sectionId, List.of()).forEach(ss -> cards.add("ss-" + ss.stepsSectionsId));
            cardsHoldingQuestionAssignment.put(sq.sectionsQuestionId, cards);
        }
        List<RuleEdge> edges = new ArrayList<>();
        for (Relationship r : Relationship.<Relationship>list("surveyId", surveyId)) {
            if (!includeRetired && !r.isCurrent()) {
                continue;
            }
            String target;
            List<String> targetFallbacks = List.of();
            if (r.downstreamStepId != null) {
                target = "step-" + r.downstreamStepId;
            } else if (r.downstreamSsId != null) {
                target = "ss-" + r.downstreamSsId;
            } else {
                target = "sq-" + r.downstreamSqId;
                targetFallbacks = cardsHoldingQuestionAssignment.getOrDefault(r.downstreamSqId, List.of());
            }
            String action = r.actionType == null ? "" : r.actionType.name;
            String operator = r.operatorType == null ? "" : r.operatorType.name;
            edges.add(new RuleEdge(r.relationshipId, r.relationshipKey,
                    "sq-" + r.upstreamSqId, cardsHoldingQuestionAssignment.getOrDefault(r.upstreamSqId, List.of()),
                    target, targetFallbacks, action, operator, label(r), r.description, !r.isCurrent()));
        }
        edges.sort(Comparator.comparing(RuleEdge::relationshipId));
        return edges;
    }

    /** UC-019 BR-003: "(operator) value" and, for Text, the token — the arrow's label. */
    public static String label(Relationship r) {
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
        String token = r.actionType != null && "TEXT".equals(r.actionType.name) && r.token != null && !r.token.isBlank() ? " {" + r.token + "}" : "";
        return op + value + token;
    }
}
