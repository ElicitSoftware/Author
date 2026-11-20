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
import com.elicitsoftware.model.SectionsQuestion;
import com.elicitsoftware.model.SelectItem;
import com.elicitsoftware.model.StepsSections;
import com.elicitsoftware.model.StructuralElement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * What the designer's previews substitute for a token, scoped the way the Survey runtime scopes it
 * (FR-013, UC-011 BR-004). The runtime builds a text's token values from the rules that target its
 * step, then the ones that target its section, then the ones that target the question itself, the
 * inner level overriding the outer — so a rule on a step reaches every section and question in it.
 * Which of the rule's two possible values lands in the slot is decided the way the runtime decides
 * it (Survey's {@code TokenSource.fill}): the sample answer stands in for the respondent's own
 * words and wins for a free-text or date source, while the rule's own constant wins for a coded,
 * numeric or boolean one, and is the only thing a rule reading a display-only question can write
 * (UC-011 BR-004, UC-019 BR-006). A rule with neither previews as the placeholder's default. One
 * case comes before all of that: in a section or question a Repeat rule builds once per selected
 * item of a multi-select, a rule reading that multi-select writes the item's text (UC-019 BR-008).
 * <p>
 * Each value carries where it came from, because the preview must treat the two differently:
 * a sample answer is escaped and never re-read for placeholders, a constant is neither
 * (see {@link com.elicitsoftware.author.flow.designer.PreviewFields}).
 */
public final class TokenSamples {

    /**
     * What a question offers a rule that reads it.
     *
     * @param value        the sample answer recorded on the question, may be null or blank
     * @param beatsConstant whether that answer outranks the rule's own constant, as a real answer
     *                      would at runtime: true when it is the respondent's own words or a moment
     *                      they chose, false when it is an option code or a bare figure that would
     *                      read as nonsense in a sentence. Mirrors {@code TokenSource.prefersAnswer}.
     * @param firstItem     for a question answered by selecting several items of a list, the text of
     *                      the list's first item, which is what a preview shows inside an instance a
     *                      Repeat rule builds per selected item (UC-019 BR-008); null otherwise
     */
    public record Sample(String value, boolean beatsConstant, String firstItem) {

        /**
         * A sample for a question that a Repeat rule cannot repeat per item: no first item.
         *
         * @param value         the sample answer recorded on the question, may be null or blank
         * @param beatsConstant whether that answer outranks the rule's own constant
         */
        public Sample(String value, boolean beatsConstant) {
            this(value, beatsConstant, null);
        }
    }

    /**
     * A place's token values and which of them the author wrote. Mirrors the runtime's
     * {@code QuestionManager.TokenValues}, whose {@code sources().keySet()} is this authored set.
     *
     * @param values   each token's value to preview, by token
     * @param authored the tokens whose value is a constant the author wrote rather than a sample answer
     */
    public record Values(Map<String, String> values, Set<String> authored) {

        public static final Values NONE = new Values(Map.of(), Set.of());
    }

    /**
     * One rule that carries a token: where it lands (innermost non-null of step, section mount,
     * question assignment) and what it writes — null when the rule has no default upstream value
     * and the question it reads has no sample answer, so the preview can only fall back to the
     * placeholder's default.
     *
     * @param token    the rule's token, trimmed
     * @param value    what the rule writes into the token, or null when it has nothing to write
     * @param authored whether {@code value} is the constant the author wrote on the rule rather
     *                 than a sample answer standing in for a respondent's
     * @param stepId   the rule's downstream step id, may be null
     * @param ssId     the rule's downstream section mount id, may be null
     * @param sqId     the rule's downstream question assignment id, may be null
     */
    public record Feed(String token, String value, boolean authored, Integer stepId, Integer ssId, Integer sqId) {

        boolean onStep(Integer step) {
            return sqId == null && ssId == null && stepId != null && stepId.equals(step);
        }

        boolean onSection(Integer ss) {
            return sqId == null && ssId != null && ssId.equals(ss);
        }

        boolean onQuestion(Integer sq) {
            return sqId != null && sqId.equals(sq);
        }
    }

    public static final TokenSamples NONE = new TokenSamples(List.of(), List.of(), List.of());

    private final List<Feed> feeds;
    private final List<StepsSections> mounts;
    private final List<SectionsQuestion> assignments;

    TokenSamples(List<Feed> feeds, Collection<StepsSections> mounts, Collection<SectionsQuestion> assignments) {
        this.feeds = List.copyOf(feeds);
        this.mounts = List.copyOf(mounts);
        this.assignments = List.copyOf(assignments);
    }

    /**
     * The survey's current rules, mounts and assignments, with each question's sample answer.
     *
     * @param surveyId the survey's id
     * @return the token samples built from the survey's current rows
     */
    public static TokenSamples load(int surveyId) {
        Map<Integer, String> firstItems = new HashMap<>();
        SelectItem.<SelectItem>list("surveyId = ?1 and effectiveTo = ?2 order by displayOrder", surveyId, StructuralElement.SENTINEL)
                .forEach(i -> firstItems.putIfAbsent(i.selectGroupId, i.displayText));
        return of(Relationship.list("surveyId = ?1 and effectiveTo = ?2", surveyId, StructuralElement.SENTINEL),
                StepsSections.list("surveyId = ?1 and effectiveTo = ?2", surveyId, StructuralElement.SENTINEL),
                SectionsQuestion.list("surveyId = ?1 and effectiveTo = ?2", surveyId, StructuralElement.SENTINEL),
                samplesOf(Question.list("surveyId = ?1 and effectiveTo = ?2", surveyId, StructuralElement.SENTINEL), firstItems));
    }

    /**
     * What every question that has an answer to give offers a rule reading it. A display-only
     * question collects none (UC-019 BR-006), so it is left out of the map entirely: a rule reading
     * one fills its token from the rule's own default upstream value or not at all, and where it has
     * none the preview must fall back to the placeholder's default -- which is what the respondent
     * would read. Previewing the sample there is what let a rule look as though it worked when it
     * could not (ElicitSoftware/Author#10). A missing entry is therefore the same case as the
     * runtime's {@code question == null || isDisplayOnly(type)} branch.
     *
     * @param questions the questions to sample
     * @return each answer-collecting question's sample by durable question id, with no first item
     */
    public static Map<Integer, Sample> samplesOf(Collection<Question> questions) {
        return samplesOf(questions, Map.of());
    }

    /**
     * {@link #samplesOf(Collection)}, with the first item of each list, by durable select group id,
     * for the questions a Repeat rule can repeat per selected item (UC-019 BR-008).
     *
     * @param questions  the questions to sample
     * @param firstItems the text of each list's first item, by durable select group id
     * @return each answer-collecting question's sample by durable question id
     */
    public static Map<Integer, Sample> samplesOf(Collection<Question> questions, Map<Integer, String> firstItems) {
        Map<Integer, Sample> samples = new HashMap<>();
        for (Question q : questions) {
            if (Tokens.collectsAnswer(q.questionType)) {
                String firstItem = RepeatSource.perItem(q.questionType) && q.selectGroupId != null
                        ? blankToNull(firstItems.get(q.selectGroupId)) : null;
                samples.put(q.questionId, new Sample(q.sample, Tokens.answerBeatsConstant(q.questionType, q.selectGroupId), firstItem));
            }
        }
        return samples;
    }

    /**
     * Builds the token samples from the given rows: one feed per rule that carries a token.
     *
     * @param rules       current rules; only the ones carrying a token matter, whatever their action
     *                    (the runtime fills a token from any rule that names it)
     * @param mounts      current step-to-section mounts, for scoping a rule on a step to the
     *                    sections and questions inside it
     * @param assignments current section-to-question assignments, which also say which question a
     *                    rule reads ({@code upstreamSqId} names the assignment, not the question)
     * @param samples     what each question offers by durable question id -- see
     *                    {@link #samplesOf(Collection)}
     * @return the token samples for those rows
     */
    public static TokenSamples of(Collection<Relationship> rules, Collection<StepsSections> mounts,
                                  Collection<SectionsQuestion> assignments, Map<Integer, Sample> samples) {
        Map<Integer, Integer> questionOfAssignment = new HashMap<>();
        assignments.forEach(sq -> questionOfAssignment.put(sq.sectionsQuestionId, sq.questionId));
        List<Feed> feeds = new ArrayList<>();
        for (Relationship r : rules) {
            if (r.token == null || r.token.isBlank()) {
                continue;
            }
            feeds.add(feed(r, samples.get(questionOfAssignment.get(r.upstreamSqId)),
                    RepeatSource.reachesRepeatedInstance(r, rules)));
        }
        return new TokenSamples(feeds, mounts, assignments);
    }

    /**
     * One rule's contribution, decided exactly as {@code TokenSource.fill} decides it at runtime.
     * The sample answer stands in for the respondent's: where a real answer would outrank the
     * rule's constant, the sample does, and where the constant would outrank the answer it still
     * does. Getting this backwards would not merely preview the wrong string -- it would label the
     * value's source wrongly, and the preview escapes and re-reads on that label.
     */
    private static Feed feed(Relationship r, Sample sample, boolean inRepeatedInstance) {
        String constant = blankToNull(r.defaultUpstreamValue);
        String answer = sample == null ? null : blankToNull(sample.value());
        String value;
        boolean authored;
        if (inRepeatedInstance && sample != null && sample.firstItem() != null) {
            // UC-019 BR-008: inside an instance built from a selected item the slot holds that
            // item's text, over the rule's constant. A preview shows one instance: the first item's.
            value = sample.firstItem();
            authored = false;
        } else if (sample != null && sample.beatsConstant() && answer != null) {
            value = answer;
            authored = false;
        } else if (constant != null) {
            value = constant;
            authored = true;
        } else {
            // Either a coded source whose sample is all there is, or nothing at all.
            value = answer;
            authored = false;
        }
        return new Feed(r.token.trim(), value, authored, r.downstreamStepId, r.downstreamSsId, r.downstreamSqId);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    /**
     * Token values for a text shown at one place on the board: the step's rules, overridden by the
     * section mount's, overridden by the question assignment's. Any of the ids may be null.
     *
     * @param stepId the step's id
     * @param ssId   the section mount's id
     * @param sqId   the question assignment's id
     * @return the tokens with a value to preview there, and which of them the author wrote
     */
    public Values forMount(Integer stepId, Integer ssId, Integer sqId) {
        Map<String, String> values = new HashMap<>();
        Set<String> authored = new HashSet<>();
        for (Feed f : feedsAt(stepId, ssId, sqId)) {
            if (f.value() == null) {
                continue;
            }
            values.put(f.token(), f.value());
            // An inner rule supplying a sample answer has to clear the flag an outer one set, or
            // the respondent's words would be spliced raw. QuestionManager.getKeyValues does the
            // same with sources().remove(key).
            if (f.authored()) {
                authored.add(f.token());
            } else {
                authored.remove(f.token());
            }
        }
        return new Values(values, authored);
    }

    /** The rules that land on that place, outermost first, so a later one overrides an earlier one. */
    private List<Feed> feedsAt(Integer stepId, Integer ssId, Integer sqId) {
        List<Feed> at = new ArrayList<>();
        feeds.stream().filter(f -> f.onStep(stepId)).forEach(at::add);
        feeds.stream().filter(f -> f.onSection(ssId)).forEach(at::add);
        feeds.stream().filter(f -> f.onQuestion(sqId)).forEach(at::add);
        return at;
    }

    /**
     * Token values for a question wherever it is assigned, for the question dialog, which edits the
     * question and not one placement of it: the first placement to fill a token wins.
     *
     * @param questionId the question's durable id
     * @return the tokens with a value to preview, and which of them the author wrote; empty when the
     *         question is not assigned in a mounted section
     */
    public Values forQuestion(Integer questionId) {
        Map<String, String> values = new HashMap<>();
        Set<String> authored = new HashSet<>();
        for (SectionsQuestion sq : assignments) {
            if (!Objects.equals(sq.questionId, questionId)) {
                continue;
            }
            for (StepsSections ss : mounts) {
                if (!Objects.equals(ss.sectionId, sq.sectionId)) {
                    continue;
                }
                Values at = forMount(ss.stepId, ss.stepsSectionsId, sq.sectionsQuestionId);
                at.values().forEach((token, value) -> {
                    // The flag has to follow the placement that supplied the value, not the last
                    // placement seen, or a token would be escaped by one placement's rule and
                    // spliced raw by another's.
                    if (values.putIfAbsent(token, value) == null && at.authored().contains(token)) {
                        authored.add(token);
                    }
                });
            }
        }
        return new Values(values, authored);
    }

    /**
     * The tokens the rules landing on that place carry, whether or not they have a value to preview.
     *
     * @param stepId the step's id, may be null
     * @param ssId   the section mount's id, may be null
     * @param sqId   the question assignment's id, may be null
     * @return the tokens as the rules carry them, trimmed
     */
    public Set<String> tokensAt(Integer stepId, Integer ssId, Integer sqId) {
        Set<String> tokens = new HashSet<>();
        feedsAt(stepId, ssId, sqId).forEach(f -> tokens.add(f.token()));
        return tokens;
    }

    /**
     * Whether the rules reaching that place fill the placeholder (UC-007 BR-005).
     *
     * <p>Every reference the phrase makes has to be reached, not just one of them. Filling a phrase
     * is all or nothing -- {@code QuestionManager.fillPhrase} abandons the whole placeholder the
     * moment one {@code <NAME>} in it has no value -- so {@code {<G1> and <S1>|you both}} with a rule
     * reaching only {@code G1} renders "you both" to the respondent, exactly as though nothing filled
     * it. Asking whether <em>any</em> reference was reached called that filled and let it export
     * silently.</p>
     *
     * <p>A phrase making no reference at all is not filled either: the runtime has nothing to
     * substitute and falls back the same way.</p>
     *
     * @param placeholder the placeholder found in a text
     * @param stepId      the step's id, may be null
     * @param ssId        the section mount's id, may be null
     * @param sqId        the question assignment's id, may be null
     * @return whether every reference the placeholder makes is carried by a rule reaching that place,
     *         compared without case; false when it makes none
     */
    public boolean fills(Tokens.Placeholder placeholder, Integer stepId, Integer ssId, Integer sqId) {
        Set<String> used = placeholder.tokens();
        if (used.isEmpty()) {
            return false;
        }
        Set<String> reaching = tokensAt(stepId, ssId, sqId);
        return used.stream().allMatch(name -> reaching.stream().anyMatch(token -> Tokens.bare(token).equalsIgnoreCase(name)));
    }
}
