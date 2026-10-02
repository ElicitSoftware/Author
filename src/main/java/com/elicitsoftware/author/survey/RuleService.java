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
import com.elicitsoftware.author.AuthoringException;
import com.elicitsoftware.author.ConstraintViolationTranslator;
import com.elicitsoftware.model.Question;
import com.elicitsoftware.model.QuestionType;
import com.elicitsoftware.model.Relationship;
import com.elicitsoftware.model.SectionsQuestion;
import com.elicitsoftware.model.Step;
import com.elicitsoftware.model.StepsSections;
import com.elicitsoftware.model.StructuralElement;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Write side of rules (UC-019): create, edit, re-point, retire and restore relationships with
 * the runtime's constraints enforced up front — a downstream path that holds together (BR-001,
 * completed from the chosen target by {@link RulePath}), a question assignment upstream (BR-002),
 * a token for Text (any action may carry one, BR-005), a token that is a name a text could write
 * between angle brackets (A2), no Repeat onto a step (A3), an operator that can hold when a Repeat
 * reads a multi-select (A6), and a target that comes after the question the rule reads from
 * (UC-033 A1).
 */
@ApplicationScoped
public class RuleService {

    /** Which end of an arrow is being moved (UC-019 A5). */
    public enum End { SOURCE, TARGET }

    @Inject
    EntityManager em;

    @Inject
    ElementService elements;

    @Inject
    RuleOrdering ordering;

    public Optional<Relationship> current(int surveyId, int relationshipId) {
        return Relationship.find("surveyId = ?1 and relationshipId = ?2 and effectiveTo = ?3",
                surveyId, relationshipId, StructuralElement.SENTINEL).firstResultOptional();
    }

    public Optional<Relationship> any(int surveyId, int relationshipId) {
        return Relationship.find("surveyId = ?1 and relationshipId = ?2", surveyId, relationshipId).firstResultOptional();
    }

    @Transactional
    public Relationship create(int surveyId, Relationship draft) {
        Relationship r = new Relationship();
        r.surveyId = surveyId;
        copy(draft, r);
        validate(surveyId, r);
        return elements.create(r);
    }

    @Transactional
    public Relationship update(int surveyId, Relationship edited) {
        Relationship r = current(surveyId, edited.relationshipId).orElseThrow(() -> new AuthoringException(Translations.get("error.rule.gone")));
        validate(surveyId, edited); // validate the draft first so a refused edit leaves the managed row untouched
        copy(edited, r);
        return flushTranslated(r);
    }

    /** UC-019 A5: move one end to the element named by a board anchor ({@code step-N}, {@code ss-N}, {@code sq-N}). */
    @Transactional
    public Relationship repoint(int surveyId, int relationshipId, End end, String anchor) {
        return repoint(surveyId, relationshipId, end, anchor, null);
    }

    /**
     * UC-019 A5 with the card the anchor was dropped in ({@code ss-N}, or null when unknown): for a
     * question that is how the rule learns which mount it fires from or targets when the section is
     * mounted in several steps.
     */
    @Transactional
    public Relationship repoint(int surveyId, int relationshipId, End end, String anchor, String mountAnchor) {
        Relationship managed = current(surveyId, relationshipId).orElseThrow(() -> new AuthoringException(Translations.get("error.rule.gone")));
        Relationship r = new Relationship(); // work on a copy: a refused re-point must not dirty the managed row
        copy(managed, r);
        String[] parts = anchor == null ? new String[0] : anchor.split("-", 2);
        if (parts.length != 2) {
            throw new AuthoringException(Translations.get("error.rule.dropOnElement"));
        }
        int id;
        try {
            id = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            throw new AuthoringException(Translations.get("error.rule.dropOnElement"));
        }
        Integer mountId = mountAnchor != null && mountAnchor.startsWith("ss-") ? parseOrNull(mountAnchor.substring(3)) : null;
        if (end == End.SOURCE) {
            if (!"sq".equals(parts[0])) {
                throw new AuthoringException(Translations.get("error.rule.readsFromQuestion"), "upstreamSqId");
            }
            r.upstreamSqId = id;
            r.upstreamStepId = mountId != null ? stepOfMount(surveyId, mountId).orElse(null) : stepOfQuestionAssignment(surveyId, id).orElse(null);
        } else {
            r.downstreamStepId = null;
            r.downstreamSsId = null;
            r.downstreamSqId = null;
            switch (parts[0]) {
                case "step" -> r.downstreamStepId = id;
                case "ss" -> r.downstreamSsId = id;
                case "sq" -> {
                    r.downstreamSqId = id;
                    r.downstreamSsId = mountId;
                }
                default -> throw new AuthoringException(Translations.get("error.rule.dropOnElement"));
            }
        }
        validate(surveyId, r);
        copy(r, managed);
        return flushTranslated(managed);
    }

    private static Integer parseOrNull(String s) {
        try {
            return Integer.valueOf(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Transactional
    public void retire(int surveyId, int relationshipId) {
        Relationship r = current(surveyId, relationshipId).orElseThrow(() -> new AuthoringException(Translations.get("error.rule.gone")));
        r.effectiveTo = OffsetDateTime.now();
        em.flush();
    }

    @Transactional
    public void restore(int surveyId, int relationshipId) {
        Relationship r = any(surveyId, relationshipId).orElseThrow(() -> new AuthoringException(Translations.get("error.rule.gone")));
        if (r.isCurrent()) {
            return;
        }
        validate(surveyId, r); // before touching the row: a refused restore leaves it retired (UC-033 A2)
        r.effectiveTo = StructuralElement.SENTINEL;
        em.flush();
    }

    /** The step a section mount sits in. */
    public Optional<Integer> stepOfMount(int surveyId, int stepsSectionsId) {
        return StepsSections.<StepsSections>find("surveyId = ?1 and stepsSectionsId = ?2 and effectiveTo = ?3",
                surveyId, stepsSectionsId, StructuralElement.SENTINEL).firstResultOptional().map(ss -> ss.stepId);
    }

    /** The step a question assignment sits in, through its section's first current mount. */
    public Optional<Integer> stepOfQuestionAssignment(int surveyId, int sectionsQuestionId) {
        Optional<SectionsQuestion> sq = SectionsQuestion.<SectionsQuestion>find("surveyId = ?1 and sectionsQuestionId = ?2 and effectiveTo = ?3",
                surveyId, sectionsQuestionId, StructuralElement.SENTINEL).firstResultOptional();
        if (sq.isEmpty()) {
            return Optional.empty();
        }
        Optional<StepsSections> mount = StepsSections.<StepsSections>find("surveyId = ?1 and sectionId = ?2 and effectiveTo = ?3 order by displayKey",
                surveyId, sq.get().sectionId, StructuralElement.SENTINEL).firstResultOptional();
        return mount.map(ss -> ss.stepId);
    }

    // ---- internals -------------------------------------------------------------------------

    /** The rule being saved, as the subject of the sentences a validation finding shares with it (UC-007 BR-013). */
    private static String theRule() {
        return Translations.get("validation.where.theRule");
    }

    void validate(int surveyId, Relationship r) {
        if (r.upstreamSqId == null) {
            throw new AuthoringException(Translations.get("error.rule.upstreamRequired"), "upstreamSqId");
        }
        if (SectionsQuestion.count("surveyId = ?1 and sectionsQuestionId = ?2 and effectiveTo = ?3", surveyId, r.upstreamSqId, StructuralElement.SENTINEL) == 0) {
            throw new AuthoringException(Translations.get("validation.rule.readsRemovedQuestion", theRule()), "upstreamSqId");
        }
        RuleOrdering.Board board = ordering.load(surveyId);
        RulePath.complete(r, board); // UC-019 BR-001: the chosen target implies its step and section
        if (r.downstreamStepId != null && board.step(r.downstreamStepId).isEmpty()) {
            throw new AuthoringException(Translations.get("validation.rule.targetsRemovedStep", theRule()), "downstream");
        }
        if (r.downstreamSsId != null && board.mount(r.downstreamSsId).isEmpty()) {
            throw new AuthoringException(Translations.get("validation.rule.targetsRemovedSection", theRule()), "downstream");
        }
        if (r.downstreamSqId != null && board.assignment(r.downstreamSqId).isEmpty()) {
            throw new AuthoringException(Translations.get("validation.rule.targetsRemovedQuestion", theRule()), "downstream");
        }
        RulePath.problem(r, board, theRule()).ifPresent(problem -> {
            throw new AuthoringException(problem, "downstream");
        });
        if (r.downstreamSqId != null && r.downstreamSqId.equals(r.upstreamSqId)) {
            throw new AuthoringException(Translations.get("error.rule.selfTarget"), "downstream");
        }
        // UC-033 A1 / A2: the target must come after the origin, for every mount of both (BR-001, BR-004).
        board.problem(r).ifPresent(problem -> {
            throw new AuthoringException(Translations.get("validation.rule.backwards", theRule(), problem), "downstream");
        });
        if (r.operatorType == null) {
            throw new AuthoringException(Translations.get("error.rule.operatorRequired"), "operatorType");
        }
        if (r.actionType == null) {
            throw new AuthoringException(Translations.get("error.rule.actionRequired"), "actionType");
        }
        // UC-019 BR-005: a rule of any action may carry a token; A2: a Text rule must.
        if (r.token != null && r.token.isBlank()) {
            r.token = null;
        }
        if ("TEXT".equals(r.actionType.name) && r.token == null) {
            throw new AuthoringException(Translations.get("error.rule.textNeedsToken"), "token");
        }
        if (r.token != null) {
            // FR-012: the token is the name alone. The brackets belong to the text that names it, and
            // the runtime keys a text's values on this column verbatim, so a token stored <S1> is
            // asked for under a name it does not have and fills nothing (Author#21). An author who
            // types the reference, or types the name in lower case, is saved from it; measured after
            // that, so the limit counts the name and not brackets that are about to be dropped.
            r.token = Tokens.canonical(r.token);
            if (r.token.length() > Tokens.MAX_LENGTH) {
                throw new AuthoringException(Translations.get("error.rule.tokenTooLong", String.valueOf(Tokens.MAX_LENGTH)), "token");
            }
            if (!Tokens.TOKEN_NAME.matcher(r.token).matches()) {
                throw new AuthoringException(Translations.get("error.rule.tokenInvalid", r.token), "token");
            }
            // UC-019 BR-006: a token holds an answer or the rule's own value. A display-only question
            // has no answer to give, so a rule on one fills nothing unless it carries a value.
            if (!upstreamCollectsAnswer(surveyId, r.upstreamSqId)
                    && (r.defaultUpstreamValue == null || r.defaultUpstreamValue.isBlank())) {
                throw new AuthoringException(Translations.get("error.rule.displayOnlySource", upstreamTypeName(surveyId, r.upstreamSqId), "{" + Tokens.reference(r.token) + "}"), "upstreamSqId");
            }
        }
        if ("REPEAT".equals(r.actionType.name) && r.target() == Relationship.Target.STEP) {
            throw new AuthoringException(Translations.get("error.rule.repeatNotStep"), "downstream");
        }
        // UC-019 A6 / BR-007: a Repeat reading a multi-select repeats once per selected item, and only
        // while its own condition holds. A numeric or boolean comparison never holds for a selection.
        if ("REPEAT".equals(r.actionType.name) && RepeatSource.perItem(upstreamType(surveyId, r.upstreamSqId))
                && RepeatSource.neverHoldsForASelection(r.operatorType)) {
            throw new AuthoringException(Translations.get("error.rule.repeatSelectionOperator", r.operatorType.name), "operatorType");
        }
        if (r.upstreamStepId != null && Step.count("surveyId = ?1 and stepId = ?2 and effectiveTo = ?3", surveyId, r.upstreamStepId, StructuralElement.SENTINEL) == 0) {
            throw new AuthoringException(Translations.get("validation.rule.readsRemovedStep", theRule()), "upstreamStepId");
        }
        if (r.description == null || r.description.isBlank()) {
            r.description = RelationshipQuery.condition(r);
        }
    }

    /**
     * Whether the question a rule reads collects an answer at all, so a token on that rule can be
     * filled from one (UC-019 BR-006). True when the upstream cannot be resolved: a missing
     * assignment is reported by its own check, and this one should not double up on it.
     */
    public boolean upstreamCollectsAnswer(int surveyId, Integer upstreamSqId) {
        QuestionType type = upstreamType(surveyId, upstreamSqId);
        return type == null || Tokens.collectsAnswer(type);
    }

    /** The upstream question's type name for a message, or "display-only" when it cannot be resolved. */
    private String upstreamTypeName(int surveyId, Integer upstreamSqId) {
        QuestionType type = upstreamType(surveyId, upstreamSqId);
        return type == null || type.name == null ? Translations.get("error.rule.displayOnlyType") : type.name;
    }

    private QuestionType upstreamType(int surveyId, Integer upstreamSqId) {
        if (upstreamSqId == null) {
            return null;
        }
        Optional<SectionsQuestion> sq = SectionsQuestion.find(
                "surveyId = ?1 and sectionsQuestionId = ?2 and effectiveTo = ?3",
                surveyId, upstreamSqId, StructuralElement.SENTINEL).firstResultOptional();
        if (sq.isEmpty()) {
            return null;
        }
        Optional<Question> question = Question.find("surveyId = ?1 and questionId = ?2 and effectiveTo = ?3",
                surveyId, sq.get().questionId, StructuralElement.SENTINEL).firstResultOptional();
        return question.map(q -> q.questionType).orElse(null);
    }

    private static void copy(Relationship from, Relationship to) {
        to.upstreamStepId = from.upstreamStepId;
        to.upstreamSqId = from.upstreamSqId;
        to.downstreamStepId = from.downstreamStepId;
        to.downstreamSsId = from.downstreamSsId;
        to.downstreamSqId = from.downstreamSqId;
        to.operatorType = from.operatorType;
        to.actionType = from.actionType;
        to.description = from.description;
        to.token = from.token;
        to.referenceValue = from.referenceValue;
        to.defaultUpstreamValue = from.defaultUpstreamValue;
        to.overrideUpstreamValue = from.overrideUpstreamValue;
    }

    private Relationship flushTranslated(Relationship r) {
        try {
            em.flush();
        } catch (RuntimeException e) {
            throw ConstraintViolationTranslator.translate(e);
        }
        return r;
    }
}
