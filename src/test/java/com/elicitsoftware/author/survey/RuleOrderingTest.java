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
import com.elicitsoftware.model.Step;
import com.elicitsoftware.model.StepsSections;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC-033 BR-001 / BR-004: the pure position comparison, on a hand-built board.
 *
 * <pre>
 *   step 1 "Intro"    : section 10 "About you" (q 100, q 101) ; section 11 "Family" (q 110, q 111)
 *   step 2 "Details"  : section 12 "Children"  (q 120)
 *   step 3 "Wrap up"  : section 13 "Consent"   (q 130)
 * </pre>
 */
class RuleOrderingTest {

    private final List<Step> steps = new ArrayList<>();
    private final List<StepsSections> mounts = new ArrayList<>();
    private final List<SectionsQuestion> assignments = new ArrayList<>();

    RuleOrderingTest() {
        step(1, "Intro");
        step(2, "Details");
        step(3, "Wrap up");
        mount(1000, 1, 10, 1);
        mount(1001, 1, 11, 2);
        mount(1002, 2, 12, 1);
        mount(1003, 3, 13, 1);
        assign(100, 10, 1);
        assign(101, 10, 2);
        assign(110, 11, 1);
        assign(111, 11, 2);
        assign(120, 12, 1);
        assign(130, 13, 1);
    }

    private void step(int id, String name) {
        Step s = new Step();
        s.stepId = id;
        s.displayOrder = BigDecimal.valueOf(id);
        s.name = name;
        steps.add(s);
    }

    private void mount(int id, int stepId, int sectionId, int order) {
        StepsSections ss = new StepsSections();
        ss.stepsSectionsId = id;
        ss.stepId = stepId;
        ss.sectionId = sectionId;
        ss.stepDisplayOrder = BigDecimal.valueOf(stepId);
        ss.sectionDisplayOrder = BigDecimal.valueOf(order);
        mounts.add(ss);
    }

    private void assign(int id, int sectionId, int order) {
        SectionsQuestion sq = new SectionsQuestion();
        sq.sectionsQuestionId = id;
        sq.sectionId = sectionId;
        sq.questionId = id;
        sq.displayOrder = BigDecimal.valueOf(order);
        assignments.add(sq);
    }

    private RuleOrdering.Board board() {
        return new RuleOrdering.Board(steps, mounts, assignments,
                Map.of(10, "About you", 11, "Family", 12, "Children", 13, "Consent"),
                Map.of(100, "Name", 101, "Age", 110, "Children?", 111, "How many", 120, "Child name", 130, "Agree"));
    }

    private static Relationship rule(int upstreamSq, Integer step, Integer ss, Integer sq) {
        Relationship r = new Relationship();
        r.upstreamSqId = upstreamSq;
        r.downstreamStepId = step;
        r.downstreamSsId = ss;
        r.downstreamSqId = sq;
        return r;
    }

    private static void forward(Optional<String> problem) {
        assertTrue(problem.isEmpty(), () -> "expected forward but got: " + problem.get());
    }

    private static void backward(Optional<String> problem) {
        assertTrue(problem.isPresent(), "expected a backward rule");
        assertTrue(problem.get().contains("is not after"), problem.get());
    }

    @Test
    void stepTarget_mustBeALaterStep() {
        RuleOrdering.Board b = board();
        forward(b.problem(rule(110, 2, null, null)));   // Family (step 1) -> step 2
        forward(b.problem(rule(110, 3, null, null)));
        backward(b.problem(rule(110, 1, null, null)));  // the origin's own step
        backward(b.problem(rule(120, 1, null, null)));  // an earlier step
    }

    @Test
    void sectionTarget_mustBeALaterSection_laterInTheSameStepCounts() {
        RuleOrdering.Board b = board();
        forward(b.problem(rule(100, null, 1001, null)));  // About you -> Family, same step, later lane position
        forward(b.problem(rule(100, null, 1002, null)));  // later step
        backward(b.problem(rule(110, null, 1000, null))); // Family -> About you, earlier in the same step
        backward(b.problem(rule(110, null, 1001, null))); // the origin's own section
        backward(b.problem(rule(120, null, 1001, null))); // an earlier step
    }

    @Test
    void questionTarget_mustBeALaterQuestion_laterInTheSameSectionCounts() {
        RuleOrdering.Board b = board();
        forward(b.problem(rule(110, null, null, 111)));  // Children? -> How many, same section
        forward(b.problem(rule(101, null, null, 110)));  // later section, same step
        forward(b.problem(rule(111, null, null, 130)));  // later step
        backward(b.problem(rule(111, null, null, 110))); // earlier question in the same section
        backward(b.problem(rule(110, null, null, 101))); // earlier section
        backward(b.problem(rule(120, null, null, 111))); // earlier step
    }

    @Test
    void reason_namesBothEndsWithTheirPlaces() {
        String p = board().problem(rule(111, null, null, 100)).orElseThrow();
        assertEquals("downstream question 'Name' in step 'Intro' › 'About you' is not after "
                + "upstream question 'How many' in step 'Intro' › 'Family'", p);
        assertEquals("downstream step 'Intro' is not after upstream question 'Child name' in step 'Details' › 'Children'",
                board().problem(rule(120, 1, null, null)).orElseThrow());
        assertEquals("downstream section 'About you' in step 'Intro' is not after upstream question 'Children?' in step 'Intro' › 'Family'",
                board().problem(rule(110, null, 1000, null)).orElseThrow());
    }

    @Test
    void multiMount_everyPairingMustBeForward() {
        // Mount Family (section 11) again in step 3, after Consent: the origin now also sits at (3, 2).
        mount(1004, 3, 11, 2);
        RuleOrdering.Board b = board();
        forward(b.problem(rule(110, null, null, 111)));  // same section: compared within each mount, so forward
        backward(b.problem(rule(111, null, null, 110))); // and still backward when the order is wrong
        backward(b.problem(rule(110, null, null, 120))); // fine from the step-1 mount, backward from the step-3 mount (BR-004)
        backward(b.problem(rule(110, null, null, 130))); // Consent (3,1) is before the (3,2) mount
        // An upstream step hint does not narrow the check: the rule fires from whichever mount is answered.
        Relationship hinted = rule(110, null, null, 120);
        hinted.upstreamStepId = 1;
        backward(b.problem(hinted));
    }

    @Test
    void multiMount_targetInAMultiMountedSectionIsCheckedForEveryMount() {
        mount(1004, 1, 13, 3); // Consent also mounted in step 1 after Family
        RuleOrdering.Board b = board();
        backward(b.problem(rule(120, null, null, 130))); // step-2 origin, step-1 mount of Consent is before it
        forward(b.problem(rule(110, null, null, 130)));  // Family (1,2) is before both Consent mounts (1,3) and (3,1)
    }

    @Test
    void missingElements_areNotThisChecksProblem() {
        RuleOrdering.Board b = board();
        forward(b.problem(rule(999, 2, null, null)));     // unknown origin
        forward(b.problem(rule(110, 99, null, null)));    // unknown step
        forward(b.problem(rule(110, null, 9999, null)));  // unknown mount
        forward(b.problem(rule(110, null, null, 9999)));  // unknown assignment
        Relationship none = rule(110, null, null, null);
        forward(b.problem(none));
    }
}
