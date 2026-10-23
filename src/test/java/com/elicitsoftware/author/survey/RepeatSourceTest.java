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

import com.elicitsoftware.model.ActionType;
import com.elicitsoftware.model.OperatorType;
import com.elicitsoftware.model.QuestionType;
import com.elicitsoftware.model.Relationship;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** UC-019 BR-007 / BR-008: what a Repeat rule builds from the question it reads. */
class RepeatSourceTest {

    private static QuestionType type(String name) {
        QuestionType t = new QuestionType();
        t.name = name;
        return t;
    }

    private static OperatorType operator(String name) {
        OperatorType o = new OperatorType();
        o.name = name;
        return o;
    }

    private static Relationship rule(String action, int upstreamSq, Integer ss, Integer sq) {
        Relationship r = new Relationship();
        if (action != null) {
            r.actionType = new ActionType();
            r.actionType.name = action;
        }
        r.upstreamSqId = upstreamSq;
        r.downstreamSsId = ss;
        r.downstreamSqId = sq;
        return r;
    }

    @Test
    void perItem_onlyForSelectionTypes() {
        assertTrue(RepeatSource.perItem(type("MULTI_SELECT")));
        assertTrue(RepeatSource.perItem(type("CHECKBOX_GROUP")));
        assertFalse(RepeatSource.perItem(type("INTEGER")));
        assertFalse(RepeatSource.perItem((QuestionType) null));

        assertTrue(RepeatSource.perItem("MULTI_SELECT"));
        assertTrue(RepeatSource.perItem("CHECKBOX_GROUP"));
        assertFalse(RepeatSource.perItem("COMBOBOX"));
        assertFalse(RepeatSource.perItem((String) null));
    }

    @Test
    void neverHoldsForASelection_booleanAndNumericOperators() {
        assertTrue(RepeatSource.neverHoldsForASelection(operator("BOOLEAN")));
        assertTrue(RepeatSource.neverHoldsForASelection(operator("GREATER THAN")));
        assertTrue(RepeatSource.neverHoldsForASelection(operator("LESS THAN")));
        assertFalse(RepeatSource.neverHoldsForASelection(operator("CONTAINS")));
        assertFalse(RepeatSource.neverHoldsForASelection(null));
    }

    @Test
    void reachesRepeatedInstance_questionAndSectionRepeats() {
        Relationship repeatQuestion = rule("REPEAT", 100, null, 120);
        Relationship repeatSection = rule("REPEAT", 100, 1002, null);
        Relationship show = rule("SHOW", 100, null, 130);

        assertTrue(RepeatSource.reachesRepeatedInstance(rule("SHOW", 100, null, 120), List.of(repeatQuestion)));
        assertTrue(RepeatSource.reachesRepeatedInstance(rule("SHOW", 100, 1002, null), List.of(repeatSection)));
        assertFalse(RepeatSource.reachesRepeatedInstance(rule("SHOW", 101, null, 120), List.of(repeatQuestion)),
                "a Repeat reading another question");
        assertFalse(RepeatSource.reachesRepeatedInstance(rule("SHOW", 100, 1002, null), List.of(repeatQuestion)),
                "a question Repeat does not reach its section");
        assertFalse(RepeatSource.reachesRepeatedInstance(rule("SHOW", 100, null, 130), List.of(show, rule(null, 100, null, 130))),
                "only Repeat rules build instances");
    }
}
