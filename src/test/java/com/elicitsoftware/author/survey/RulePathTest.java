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

import com.elicitsoftware.author.i18n.Translations;
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
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * UC-019 BR-001 (a downstream target is an outer-to-inner path) and UC-007 BR-013 (the sentence a
 * broken path is reported with), on a hand-built board.
 *
 * <pre>
 *   step 1 "Intro"    : section 10 "About you" (q 100, q 101) ; section 11 "Family" (q 110)
 *   step 2 "Details"  : section 12 "Children"  (q 120)
 *   step 3 "Wrap up"  : section 11 "Family" again (mount 1004)
 * </pre>
 */
class RulePathTest {

    private static final String SUBJECT = "The rule";

    private final List<Step> steps = new ArrayList<>();
    private final List<StepsSections> mounts = new ArrayList<>();
    private final List<SectionsQuestion> assignments = new ArrayList<>();

    RulePathTest() {
        step(1, "Intro");
        step(2, "Details");
        step(3, "Wrap up");
        mount(1000, 1, 10, 1);
        mount(1001, 1, 11, 2);
        mount(1002, 2, 12, 1);
        mount(1004, 3, 11, 1);
        assign(100, 10, 1);
        assign(101, 10, 2);
        assign(110, 11, 1);
        assign(120, 12, 1);
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
                Map.of(10, "About you", 11, "Family", 12, "Children"),
                Map.of(100, "Name", 101, "Age", 110, "Children?", 120, "Child name"));
    }

    private static Relationship rule(int upstreamSq, Integer step, Integer ss, Integer sq) {
        Relationship r = new Relationship();
        r.upstreamSqId = upstreamSq;
        r.upstreamStepId = 1;
        r.downstreamStepId = step;
        r.downstreamSsId = ss;
        r.downstreamSqId = sq;
        return r;
    }

    private static void assertPath(Relationship r, Integer step, Integer ss, Integer sq) {
        assertEquals(step, r.downstreamStepId, "downstream step");
        assertEquals(ss, r.downstreamSsId, "downstream mount");
        assertEquals(sq, r.downstreamSqId, "downstream assignment");
    }

    // --- complete -------------------------------------------------------------------------------

    @Test
    void complete_questionInUpstreamSection_dropsThePathAndTheUpstreamStep() {
        Relationship r = rule(100, 1, 1000, 101);
        RulePath.complete(r, board());
        assertPath(r, null, null, 101);
        assertNull(r.upstreamStepId, "a same-section rule fires from every mount");
    }

    @Test
    void complete_questionInASectionMountedOnce_takesThatMountAndItsStep() {
        Relationship r = rule(100, null, null, 120);
        RulePath.complete(r, board());
        assertPath(r, 2, 1002, 120);
        assertEquals(1, r.upstreamStepId, "the upstream step is kept for a rule that leaves the section");
    }

    @Test
    void complete_questionInASectionMountedTwice_leavesThePlaceOpen() {
        Relationship r = rule(100, null, null, 110);
        RulePath.complete(r, board());
        assertPath(r, null, null, 110);
    }

    @Test
    void complete_namedStep_picksTheMountInThatStep() {
        Relationship r = rule(100, 3, null, 110);
        RulePath.complete(r, board());
        assertPath(r, 3, 1004, 110);
    }

    @Test
    void complete_namedStepWithoutTheSection_leavesThePlaceOpen() {
        Relationship r = rule(100, 2, null, 110);
        RulePath.complete(r, board());
        assertPath(r, 2, null, 110);
    }

    @Test
    void complete_namedMount_isKeptAndFillsItsStep() {
        Relationship r = rule(100, null, 1001, 110);
        RulePath.complete(r, board());
        assertPath(r, 1, 1001, 110);
    }

    @Test
    void complete_sectionTarget_fillsItsStep() {
        Relationship r = rule(100, null, 1002, null);
        RulePath.complete(r, board());
        assertPath(r, 2, 1002, null);
    }

    @Test
    void complete_unknownUpstream_stillPlacesTheTarget() {
        Relationship r = rule(999, null, null, 120);
        RulePath.complete(r, board());
        assertPath(r, 2, 1002, 120);
    }

    @Test
    void complete_unknownTargetOrMount_changesNothing() {
        Relationship unknownQuestion = rule(100, null, null, 999);
        RulePath.complete(unknownQuestion, board());
        assertPath(unknownQuestion, null, null, 999);

        Relationship unknownMount = rule(100, null, 9999, null);
        RulePath.complete(unknownMount, board());
        assertPath(unknownMount, null, 9999, null);
    }

    @Test
    void complete_stepTarget_changesNothing() {
        Relationship r = rule(100, 2, null, null);
        RulePath.complete(r, board());
        assertPath(r, 2, null, null);
    }

    // --- problem --------------------------------------------------------------------------------

    private Optional<String> problem(int upstreamSq, Integer step, Integer ss, Integer sq) {
        return RulePath.problem(rule(upstreamSq, step, ss, sq), board(), SUBJECT);
    }

    @Test
    void problem_noTarget() {
        assertEquals(Optional.of(Translations.get("validation.rule.noTarget", SUBJECT)),
                problem(100, null, null, null));
    }

    @Test
    void problem_wholePaths_holdTogether() {
        assertEquals(Optional.empty(), problem(100, 2, null, null), "a step");
        assertEquals(Optional.empty(), problem(100, 2, 1002, null), "a section in its step");
        assertEquals(Optional.empty(), problem(100, null, 1002, null), "a section with no step named");
        assertEquals(Optional.empty(), problem(100, 2, 1002, 120), "a question in its section");
        assertEquals(Optional.empty(), problem(100, null, null, 101), "a question in the upstream's own section");
    }

    @Test
    void problem_sectionOutsideTheNamedStep() {
        assertEquals(Optional.of(Translations.get("validation.rule.sectionNotInStep", SUBJECT, "Children", "Intro")),
                problem(100, 1, 1002, null));
    }

    @Test
    void problem_questionOutsideTheNamedSection() {
        assertEquals(Optional.of(Translations.get("validation.rule.questionNotInSection", SUBJECT, "Children?", "About you")),
                problem(100, 1, 1000, 110));
    }

    @Test
    void problem_questionInAnotherSectionWithNoMount() {
        assertEquals(Optional.of(Translations.get("validation.rule.questionWithoutPlace", SUBJECT, "Child name")),
                problem(100, null, null, 120));
        assertEquals(Optional.of(Translations.get("validation.rule.questionWithoutPlace", SUBJECT, "Child name")),
                problem(100, 2, null, 120), "a step alone does not place a question");
    }

    @Test
    void problem_questionInOwnSectionThatNamesAStep() {
        assertEquals(Optional.of(Translations.get("validation.rule.questionWithoutPlace", SUBJECT, "Age")),
                problem(100, 1, null, 101));
    }

    @Test
    void problem_unknownUpstream_cannotClaimTheOwnSection() {
        assertEquals(Optional.of(Translations.get("validation.rule.questionWithoutPlace", SUBJECT, "Age")),
                problem(999, null, null, 101));
    }

    @Test
    void problem_missingElements_areLeftToTheReferentialChecks() {
        assertEquals(Optional.empty(), problem(100, 1, 9999, 999));
    }
}
