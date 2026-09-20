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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * UC-033: a rule's target must come after the question it reads from. "After" is a comparison of
 * board positions {@code (step, section, question)} — the display orders UC-016 keeps contiguous —
 * compared step first (BR-001). The check is derived from current positions on every call and
 * stores nothing (BR-003); a section mounted in several steps is checked for every mount, of the
 * upstream question and of the downstream target (BR-004) — except that a downstream question in
 * the upstream question's own section is compared within the same mount, because the runtime shows
 * it on the page being answered. Missing elements are not this check's problem: the referential
 * checks in {@code RuleService.validate} and {@code ExportValidation} report those.
 */
@ApplicationScoped
public class RuleOrdering {

    private static final BigDecimal WHOLE = BigDecimal.ZERO;

    /** A board position; {@link #WHOLE} in a component means "the whole container". */
    public record Position(BigDecimal step, BigDecimal section, BigDecimal question) implements Comparable<Position> {

        @Override
        public int compareTo(Position o) {
            int c = step.compareTo(o.step);
            if (c == 0) {
                c = section.compareTo(o.section);
            }
            if (c == 0) {
                c = question.compareTo(o.question);
            }
            return c;
        }

        public boolean after(Position o) {
            return compareTo(o) > 0;
        }
    }

    /** A survey's current structure, loaded once and asked about many rules. */
    public static final class Board {

        private final Map<Integer, Step> steps = new HashMap<>();
        private final Map<Integer, StepsSections> mounts = new HashMap<>();
        private final Map<Integer, List<StepsSections>> mountsBySection = new HashMap<>();
        private final Map<Integer, SectionsQuestion> assignments = new HashMap<>();
        private final Map<Integer, String> sectionName;
        private final Map<Integer, String> questionName;

        public Board(Collection<Step> steps, Collection<StepsSections> mounts, Collection<SectionsQuestion> assignments,
                     Map<Integer, String> sectionName, Map<Integer, String> questionName) {
            steps.forEach(s -> this.steps.put(s.stepId, s));
            for (StepsSections ss : mounts) {
                this.mounts.put(ss.stepsSectionsId, ss);
                mountsBySection.computeIfAbsent(ss.sectionId, k -> new ArrayList<>()).add(ss);
            }
            assignments.forEach(sq -> this.assignments.put(sq.sectionsQuestionId, sq));
            this.sectionName = sectionName;
            this.questionName = questionName;
        }

        /**
         * Empty when the rule points forward for every mount pairing; otherwise a sentence such as
         * {@code "downstream question 'Consent' in step 'Intro' › 'About you' is not after upstream question
         * 'How many' in step 'Intro' › 'Family'"}, naming the first backward pairing found.
         */
        public Optional<String> problem(Relationship r) {
            SectionsQuestion origin = r.upstreamSqId == null ? null : assignments.get(r.upstreamSqId);
            if (origin == null) {
                return Optional.empty();
            }
            for (StepsSections originMount : mountsBySection.getOrDefault(origin.sectionId, List.of())) {
                Position from = new Position(stepOrder(originMount), originMount.sectionDisplayOrder, origin.displayOrder);
                String upstream = "upstream question " + quote(questionName.get(origin.questionId)) + inStep(originMount)
                        + " › " + quote(sectionName.get(originMount.sectionId));
                if (r.downstreamStepId != null) {
                    Step target = steps.get(r.downstreamStepId);
                    if (target != null && !new Position(target.displayOrder, WHOLE, WHOLE).after(from)) {
                        return Optional.of("downstream step " + quote(target.name) + " is not after " + upstream);
                    }
                } else if (r.downstreamSsId != null) {
                    StepsSections target = mounts.get(r.downstreamSsId);
                    if (target != null && !new Position(stepOrder(target), target.sectionDisplayOrder, WHOLE).after(from)) {
                        return Optional.of("downstream section " + quote(sectionName.get(target.sectionId)) + inStep(target) + " is not after " + upstream);
                    }
                } else if (r.downstreamSqId != null) {
                    SectionsQuestion target = assignments.get(r.downstreamSqId);
                    if (target == null) {
                        return Optional.empty();
                    }
                    boolean sameSection = target.sectionId.equals(origin.sectionId);
                    for (StepsSections targetMount : mountsBySection.getOrDefault(target.sectionId, List.of())) {
                        if (sameSection && !targetMount.stepsSectionsId.equals(originMount.stepsSectionsId)) {
                            continue; // shown on the page being answered: only the question order matters
                        }
                        if (!new Position(stepOrder(targetMount), targetMount.sectionDisplayOrder, target.displayOrder).after(from)) {
                            return Optional.of("downstream question " + quote(questionName.get(target.questionId)) + inStep(targetMount)
                                    + " › " + quote(sectionName.get(targetMount.sectionId)) + " is not after " + upstream);
                        }
                    }
                }
            }
            return Optional.empty();
        }

        /** The step's own order is the source of truth; the mount's copy stands in for a dangling step. */
        private BigDecimal stepOrder(StepsSections mount) {
            Step step = steps.get(mount.stepId);
            return step != null && step.displayOrder != null ? step.displayOrder : mount.stepDisplayOrder;
        }

        private String inStep(StepsSections mount) {
            Step step = steps.get(mount.stepId);
            return " in step " + quote(step == null ? null : step.name);
        }

        private static String quote(String name) {
            return "'" + (name == null || name.isBlank() ? "?" : name) + "'";
        }
    }

    /** The current board of a survey: one query per table. */
    public Board load(int surveyId) {
        Map<Integer, String> sectionName = new HashMap<>();
        for (Section s : Section.<Section>list("surveyId = ?1 and effectiveTo = ?2", surveyId, StructuralElement.SENTINEL)) {
            sectionName.put(s.sectionId, s.name);
        }
        Map<Integer, String> questionName = new HashMap<>();
        for (Question q : Question.<Question>list("surveyId = ?1 and effectiveTo = ?2", surveyId, StructuralElement.SENTINEL)) {
            questionName.put(q.questionId, questionLabel(q));
        }
        return new Board(
                Step.list("surveyId = ?1 and effectiveTo = ?2", surveyId, StructuralElement.SENTINEL),
                StepsSections.list("surveyId = ?1 and effectiveTo = ?2", surveyId, StructuralElement.SENTINEL),
                SectionsQuestion.list("surveyId = ?1 and effectiveTo = ?2", surveyId, StructuralElement.SENTINEL),
                sectionName, questionName);
    }

    /** How a question is named in a reason: its short text, else its text without markup. */
    public static String questionLabel(Question q) {
        return q.shortText != null && !q.shortText.isBlank() ? q.shortText : SurveyStructureQuery.plain(q.text);
    }
}
