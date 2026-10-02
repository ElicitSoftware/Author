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

import com.elicitsoftware.author.i18n.Translations;
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
 * stores nothing (BR-003). The target is the innermost element of the rule's downstream path
 * (UC-019 BR-001): a question is compared in the mount its path names; a section in its own mount;
 * a step as a whole. A rule fires from every mount of its upstream section unless it names its
 * upstream step, so every firing mount is checked (BR-004) — except that a downstream question in
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

        public Optional<StepsSections> mount(Integer stepsSectionsId) {
            return Optional.ofNullable(stepsSectionsId == null ? null : mounts.get(stepsSectionsId));
        }

        public Optional<SectionsQuestion> assignment(Integer sectionsQuestionId) {
            return Optional.ofNullable(sectionsQuestionId == null ? null : assignments.get(sectionsQuestionId));
        }

        public Optional<Step> step(Integer stepId) {
            return Optional.ofNullable(stepId == null ? null : steps.get(stepId));
        }

        /** The current mounts of a section, in no particular order. */
        public List<StepsSections> mountsOf(Integer sectionId) {
            return mountsBySection.getOrDefault(sectionId, List.of());
        }

        /** The section's name as a finding prints it; the language file supplies the quotes. */
        public String sectionLabel(Integer sectionId) {
            return nameOrUnknown(sectionName.get(sectionId));
        }

        public String questionLabel(Integer questionId) {
            return nameOrUnknown(questionName.get(questionId));
        }

        public String stepLabel(Integer stepId) {
            Step step = steps.get(stepId);
            return nameOrUnknown(step == null ? null : step.name);
        }

        /**
         * Empty when the rule points forward from every mount it fires from; otherwise the reason, in
         * the reader's language (UC-007 BR-013), such as {@code "downstream question 'Consent' in
         * step 'Intro' › 'About you' must come after upstream question 'How many' in step 'Intro' ›
         * 'Family'"}, naming the first backward pairing found.
         */
        public Optional<String> problem(Relationship r) {
            SectionsQuestion origin = r.upstreamSqId == null ? null : assignments.get(r.upstreamSqId);
            if (origin == null) {
                return Optional.empty();
            }
            for (StepsSections originMount : firingMounts(r, origin)) {
                Position from = new Position(stepOrder(originMount), originMount.sectionDisplayOrder, origin.displayOrder);
                String readQuestion = questionLabel(origin.questionId);
                String readStep = stepLabel(originMount.stepId);
                String readSection = sectionLabel(originMount.sectionId);
                if (r.downstreamSqId != null) {
                    SectionsQuestion target = assignments.get(r.downstreamSqId);
                    if (target == null) {
                        return Optional.empty();
                    }
                    for (StepsSections targetMount : targetMounts(r, origin, originMount, target)) {
                        if (!new Position(stepOrder(targetMount), targetMount.sectionDisplayOrder, target.displayOrder).after(from)) {
                            return Optional.of(Translations.get("validation.rule.backwards.question",
                                    questionLabel(target.questionId), stepLabel(targetMount.stepId),
                                    sectionLabel(targetMount.sectionId), readQuestion, readStep, readSection));
                        }
                    }
                } else if (r.downstreamSsId != null) {
                    StepsSections target = mounts.get(r.downstreamSsId);
                    if (target != null && !new Position(stepOrder(target), target.sectionDisplayOrder, WHOLE).after(from)) {
                        return Optional.of(Translations.get("validation.rule.backwards.section",
                                sectionLabel(target.sectionId), stepLabel(target.stepId), readQuestion, readStep, readSection));
                    }
                } else if (r.downstreamStepId != null) {
                    Step target = steps.get(r.downstreamStepId);
                    if (target != null && !new Position(target.displayOrder, WHOLE, WHOLE).after(from)) {
                        return Optional.of(Translations.get("validation.rule.backwards.step",
                                nameOrUnknown(target.name), readQuestion, readStep, readSection));
                    }
                }
            }
            return Optional.empty();
        }

        /**
         * The mounts the rule fires from (BR-004): the one in its upstream step when it names one and that
         * step still mounts the section, otherwise every mount of the upstream question's section.
         */
        private List<StepsSections> firingMounts(Relationship r, SectionsQuestion origin) {
            List<StepsSections> all = mountsBySection.getOrDefault(origin.sectionId, List.of());
            if (r.upstreamStepId == null) {
                return all;
            }
            List<StepsSections> named = all.stream().filter(m -> r.upstreamStepId.equals(m.stepId)).toList();
            return named.isEmpty() ? all : named;
        }

        /**
         * Where a downstream question is compared: the mount its path names (the section, else any mount of
         * the section in the named step); with no path, the origin's own mount when the target shares the
         * origin's section, else every mount of the target's section.
         */
        private List<StepsSections> targetMounts(Relationship r, SectionsQuestion origin, StepsSections originMount, SectionsQuestion target) {
            if (r.downstreamSsId != null && mounts.containsKey(r.downstreamSsId)) {
                return List.of(mounts.get(r.downstreamSsId));
            }
            List<StepsSections> all = mountsBySection.getOrDefault(target.sectionId, List.of());
            if (r.downstreamStepId != null) {
                List<StepsSections> inStep = all.stream().filter(m -> r.downstreamStepId.equals(m.stepId)).toList();
                if (!inStep.isEmpty()) {
                    return inStep;
                }
            }
            if (target.sectionId.equals(origin.sectionId)) {
                return all.stream().filter(m -> m.stepsSectionsId.equals(originMount.stepsSectionsId)).toList();
            }
            return all;
        }

        /** The step's own order is the source of truth; the mount's copy stands in for a dangling step. */
        private BigDecimal stepOrder(StepsSections mount) {
            Step step = steps.get(mount.stepId);
            return step != null && step.displayOrder != null ? step.displayOrder : mount.stepDisplayOrder;
        }

        private static String nameOrUnknown(String name) {
            return name == null || name.isBlank() ? "?" : name;
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
