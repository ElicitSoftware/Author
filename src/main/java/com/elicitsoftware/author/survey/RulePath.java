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

import com.elicitsoftware.model.Relationship;
import com.elicitsoftware.model.SectionsQuestion;
import com.elicitsoftware.model.StepsSections;

import java.util.List;
import java.util.Optional;

/**
 * UC-019 BR-001: a rule's downstream target is a path the Survey runtime walks from the outside in —
 * {@code downstream_step_id} names the step, {@code downstream_ss_id} the section mounted in it,
 * {@code downstream_sq_id} the question assigned to that section. The innermost element is what the
 * rule acts on; the outer ones say where the runtime places it. A question in the upstream question's
 * own section needs no path: the runtime shows it on the page being answered, whichever mount that is.
 *
 * <p>{@link #complete} fills in what a chosen target implies, so the designer and the dialog can name
 * a target by its innermost element; {@link #problem} reports a path that does not hold together.</p>
 */
public final class RulePath {

    private RulePath() {
    }

    /**
     * Fills in the outer elements a target implies: a section's step; for a question outside the
     * upstream question's section, its mount when only one fits. A question in the upstream's own
     * section keeps no path — even when the caller named the mount it was chosen in — and such a
     * rule names no upstream step: it fires from every mount. (The runtime's login-time query hides
     * every question of a section or step a rule names downstream, so a same-section rule stored
     * with a path would blank the section for a fresh respondent.)
     */
    public static void complete(Relationship r, RuleOrdering.Board board) {
        if (r.downstreamSqId != null) {
            SectionsQuestion target = board.assignment(r.downstreamSqId).orElse(null);
            SectionsQuestion origin = board.assignment(r.upstreamSqId).orElse(null);
            if (target != null && origin != null && target.sectionId.equals(origin.sectionId)) {
                r.downstreamSsId = null;
                r.downstreamStepId = null;
                r.upstreamStepId = null;
            } else if (target != null && r.downstreamSsId == null) {
                List<StepsSections> mounts = board.mountsOf(target.sectionId).stream()
                        .filter(m -> r.downstreamStepId == null || r.downstreamStepId.equals(m.stepId)).toList();
                if (mounts.size() == 1) {
                    r.downstreamSsId = mounts.get(0).stepsSectionsId;
                }
            }
        }
        if (r.downstreamSsId != null && r.downstreamStepId == null) {
            board.mount(r.downstreamSsId).ifPresent(m -> r.downstreamStepId = m.stepId);
        }
    }

    /**
     * Why the path does not hold together, as a predicate on "the rule": no target at all, a section
     * outside the named step, a question outside the named section, or a question in another section
     * that names no mount. Missing elements are not reported here; the referential checks do that.
     */
    public static Optional<String> problem(Relationship r, RuleOrdering.Board board) {
        if (r.downstreamStepId == null && r.downstreamSsId == null && r.downstreamSqId == null) {
            return Optional.of("must target a step, a section or a question");
        }
        StepsSections mount = board.mount(r.downstreamSsId).orElse(null);
        SectionsQuestion target = board.assignment(r.downstreamSqId).orElse(null);
        if (mount != null && r.downstreamStepId != null && !mount.stepId.equals(r.downstreamStepId)) {
            return Optional.of("targets section " + board.sectionLabel(mount.sectionId) + " through step "
                    + board.stepLabel(r.downstreamStepId) + ", which does not hold that section");
        }
        if (target != null && mount != null && !target.sectionId.equals(mount.sectionId)) {
            return Optional.of("targets question " + board.questionLabel(target.questionId) + " through section "
                    + board.sectionLabel(mount.sectionId) + ", which does not hold that question");
        }
        if (target != null && r.downstreamSsId == null) {
            SectionsQuestion origin = board.assignment(r.upstreamSqId).orElse(null);
            boolean ownSection = origin != null && origin.sectionId.equals(target.sectionId);
            if (!ownSection || r.downstreamStepId != null) {
                return Optional.of("targets question " + board.questionLabel(target.questionId)
                        + " outside the upstream question's section without naming the step and section it is shown in");
            }
        }
        return Optional.empty();
    }
}
