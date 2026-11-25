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

import com.elicitsoftware.model.OperatorType;
import com.elicitsoftware.model.QuestionType;
import com.elicitsoftware.model.Relationship;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

/**
 * What a Repeat rule builds, decided from the question it reads (UC-019 BR-007). Mirrors the
 * Survey runtime's {@code RepeatSource} (Survey UC-002 BR-012): a question answered with a number
 * repeats its downstream 1 to N times, and one answered by selecting several items of a list
 * repeats it once per selected item, each instance belonging to its item.
 */
public final class RepeatSource {

    /** The question types answered by selecting several items of a list. */
    private static final Set<String> SELECTION_TYPES = Set.of("MULTI_SELECT", "CHECKBOX_GROUP");

    /**
     * The operators that can never hold for a selection, which is stored as the selected items'
     * coded values joined by commas: neither a boolean nor a number. {@code LESS THAN} is not a
     * seeded operator, and is listed because the runtime evaluates it the same way.
     */
    private static final Set<String> NEVER_HOLD_FOR_A_SELECTION = Set.of("BOOLEAN", "GREATER THAN", "LESS THAN");

    private RepeatSource() { }

    /**
     * Whether a Repeat rule reading a question of this type repeats once per selected item.
     *
     * @param type the question's type, or {@code null}
     * @return {@code true} for a multi-select or checkbox group; {@code false} otherwise
     */
    public static boolean perItem(QuestionType type) {
        return type != null && SELECTION_TYPES.contains(type.name);
    }

    /**
     * {@link #perItem(QuestionType)} by type name, for the places that hold only the name.
     *
     * @param typeName the question type's name, or {@code null}
     * @return {@code true} for {@code MULTI_SELECT} or {@code CHECKBOX_GROUP}; {@code false} otherwise
     */
    public static boolean perItem(String typeName) {
        return typeName != null && SELECTION_TYPES.contains(typeName);
    }

    /**
     * Whether a rule reading a selection with this operator could never fire (UC-019 BR-007).
     *
     * @param operator the rule's operator, or {@code null}
     * @return {@code true} for {@code BOOLEAN}, {@code GREATER THAN} or {@code LESS THAN};
     *         {@code false} otherwise
     */
    public static boolean neverHoldsForASelection(OperatorType operator) {
        return operator != null && NEVER_HOLD_FOR_A_SELECTION.contains(operator.name);
    }

    /**
     * Whether a rule's downstream lies in an instance that a Repeat rule reading the same question
     * builds: the Repeat rule itself, or a rule aimed at the section or question it repeats. Inside
     * such an instance the rule's token holds the item the instance belongs to (UC-019 BR-008).
     *
     * @param rule  the rule carrying the token
     * @param rules the survey's current rules
     * @return {@code true} when a Repeat rule reading the same question repeats the rule's
     *         downstream question, or the section it targets
     */
    public static boolean reachesRepeatedInstance(Relationship rule, Collection<Relationship> rules) {
        for (Relationship repeat : rules) {
            if (repeat.actionType == null || !"REPEAT".equals(repeat.actionType.name)
                    || !Objects.equals(repeat.upstreamSqId, rule.upstreamSqId)) {
                continue;
            }
            if (repeat.downstreamSqId != null) {
                if (repeat.downstreamSqId.equals(rule.downstreamSqId)) {
                    return true;
                }
            } else if (repeat.downstreamSsId != null && repeat.downstreamSsId.equals(rule.downstreamSsId)) {
                return true;
            }
        }
        return false;
    }
}
