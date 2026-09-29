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
 * A rule's value is its default upstream value when it has one, else the sample answer recorded on
 * the question it reads (UC-011 BR-005); a rule with neither previews as the placeholder's default.
 * A rule reading a display-only question has no sample to fall back on, because such a question
 * collects nothing for a token to take (UC-019 BR-006).
 */
public final class TokenSamples {

    /**
     * One rule that carries a token: where it lands (innermost non-null of step, section mount,
     * question assignment) and what it writes — null when the rule has no default upstream value
     * and the question it reads has no sample answer, so the preview can only fall back to the
     * placeholder's default.
     */
    public record Feed(String token, String value, Integer stepId, Integer ssId, Integer sqId) {

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

    /** The survey's current rules, mounts and assignments, with each question's sample answer. */
    public static TokenSamples load(int surveyId) {
        return of(Relationship.list("surveyId = ?1 and effectiveTo = ?2", surveyId, StructuralElement.SENTINEL),
                StepsSections.list("surveyId = ?1 and effectiveTo = ?2", surveyId, StructuralElement.SENTINEL),
                SectionsQuestion.list("surveyId = ?1 and effectiveTo = ?2", surveyId, StructuralElement.SENTINEL),
                samplesOf(Question.list("surveyId = ?1 and effectiveTo = ?2", surveyId, StructuralElement.SENTINEL)));
    }

    /**
     * The sample answer of every question that has an answer to give. A display-only question
     * collects none (UC-019 BR-006), so its sample is not a value any token can take: a rule reading
     * one fills its token from the rule's own default upstream value or not at all, and where it has
     * none the preview must fall back to the placeholder's default -- which is what the respondent
     * would read. Previewing the sample there is what let a rule look as though it worked when it
     * could not (ElicitSoftware/Author#10).
     */
    public static Map<Integer, String> samplesOf(Collection<Question> questions) {
        Map<Integer, String> samples = new HashMap<>();
        for (Question q : questions) {
            if (Tokens.collectsAnswer(q.questionType)) {
                samples.put(q.questionId, q.sample);
            }
        }
        return samples;
    }

    /**
     * @param rules       current rules; only the ones carrying a token matter, whatever their action
     *                    (the runtime fills a token from any rule that names it)
     * @param samples     sample answer by durable question id, for the questions that collect an
     *                    answer -- see {@link #samplesOf(Collection)}
     */
    public static TokenSamples of(Collection<Relationship> rules, Collection<StepsSections> mounts,
                                  Collection<SectionsQuestion> assignments, Map<Integer, String> samples) {
        Map<Integer, Integer> questionOfAssignment = new HashMap<>();
        assignments.forEach(sq -> questionOfAssignment.put(sq.sectionsQuestionId, sq.questionId));
        List<Feed> feeds = new ArrayList<>();
        for (Relationship r : rules) {
            if (r.token == null || r.token.isBlank()) {
                continue;
            }
            String value = r.defaultUpstreamValue;
            if (value == null || value.isBlank()) {
                value = samples.get(questionOfAssignment.get(r.upstreamSqId));
            }
            feeds.add(new Feed(r.token.trim(), value == null || value.isBlank() ? null : value,
                    r.downstreamStepId, r.downstreamSsId, r.downstreamSqId));
        }
        return new TokenSamples(feeds, mounts, assignments);
    }

    /**
     * Token values for a text shown at one place on the board: the step's rules, overridden by the
     * section mount's, overridden by the question assignment's. Any of the ids may be null.
     */
    public Map<String, String> forMount(Integer stepId, Integer ssId, Integer sqId) {
        Map<String, String> values = new HashMap<>();
        for (Feed f : feedsAt(stepId, ssId, sqId)) {
            if (f.value() != null) {
                values.put(f.token(), f.value());
            }
        }
        return values;
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
     */
    public Map<String, String> forQuestion(Integer questionId) {
        Map<String, String> values = new HashMap<>();
        for (SectionsQuestion sq : assignments) {
            if (!Objects.equals(sq.questionId, questionId)) {
                continue;
            }
            for (StepsSections ss : mounts) {
                if (Objects.equals(ss.sectionId, sq.sectionId)) {
                    forMount(ss.stepId, ss.stepsSectionsId, sq.sectionsQuestionId).forEach(values::putIfAbsent);
                }
            }
        }
        return values;
    }

    /** The tokens the rules landing on that place carry, whether or not they have a value to preview. */
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
