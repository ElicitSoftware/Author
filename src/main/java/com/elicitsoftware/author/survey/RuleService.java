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

import com.elicitsoftware.author.AuthoringException;
import com.elicitsoftware.author.ConstraintViolationTranslator;
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
 * the runtime's constraints enforced up front — one downstream target (BR-001), a question
 * assignment upstream (BR-002), a token for Text, no Repeat onto a step (A3).
 */
@ApplicationScoped
public class RuleService {

    /** Which end of an arrow is being moved (UC-019 A5). */
    public enum End { SOURCE, TARGET }

    @Inject
    EntityManager em;

    @Inject
    ElementService elements;

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
        Relationship r = current(surveyId, edited.relationshipId).orElseThrow(() -> new AuthoringException("Rule not found or removed"));
        validate(surveyId, edited); // validate the draft first so a refused edit leaves the managed row untouched
        copy(edited, r);
        return flushTranslated(r);
    }

    /** UC-019 A5: move one end to the element named by a board anchor ({@code step-N}, {@code ss-N}, {@code sq-N}). */
    @Transactional
    public Relationship repoint(int surveyId, int relationshipId, End end, String anchor) {
        Relationship managed = current(surveyId, relationshipId).orElseThrow(() -> new AuthoringException("Rule not found or removed"));
        Relationship r = new Relationship(); // work on a copy: a refused re-point must not dirty the managed row
        copy(managed, r);
        String[] parts = anchor == null ? new String[0] : anchor.split("-", 2);
        if (parts.length != 2) {
            throw new AuthoringException("Drop the arrow on a step, a section or a question");
        }
        int id;
        try {
            id = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            throw new AuthoringException("Drop the arrow on a step, a section or a question");
        }
        if (end == End.SOURCE) {
            if (!"sq".equals(parts[0])) {
                throw new AuthoringException("A rule reads from a question", "upstreamSqId");
            }
            r.upstreamSqId = id;
            r.upstreamStepId = stepOfQuestionAssignment(surveyId, id).orElse(null);
        } else {
            r.downstreamStepId = null;
            r.downstreamSsId = null;
            r.downstreamSqId = null;
            switch (parts[0]) {
                case "step" -> r.downstreamStepId = id;
                case "ss" -> r.downstreamSsId = id;
                case "sq" -> r.downstreamSqId = id;
                default -> throw new AuthoringException("Drop the arrow on a step, a section or a question");
            }
        }
        validate(surveyId, r);
        copy(r, managed);
        return flushTranslated(managed);
    }

    @Transactional
    public void retire(int surveyId, int relationshipId) {
        Relationship r = current(surveyId, relationshipId).orElseThrow(() -> new AuthoringException("Rule not found or removed"));
        r.effectiveTo = OffsetDateTime.now();
        em.flush();
    }

    @Transactional
    public void restore(int surveyId, int relationshipId) {
        Relationship r = any(surveyId, relationshipId).orElseThrow(() -> new AuthoringException("Rule not found"));
        if (r.isCurrent()) {
            return;
        }
        r.effectiveTo = StructuralElement.SENTINEL;
        validate(surveyId, r);
        em.flush();
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

    void validate(int surveyId, Relationship r) {
        if (r.upstreamSqId == null) {
            throw new AuthoringException("Choose the question the rule reads from", "upstreamSqId");
        }
        if (SectionsQuestion.count("surveyId = ?1 and sectionsQuestionId = ?2 and effectiveTo = ?3", surveyId, r.upstreamSqId, StructuralElement.SENTINEL) == 0) {
            throw new AuthoringException("The rule reads from a removed or missing question assignment", "upstreamSqId");
        }
        if (!r.hasExactlyOneDownstream()) {
            throw new AuthoringException("A rule targets exactly one step, section or question", "downstream");
        }
        if (r.downstreamStepId != null && Step.count("surveyId = ?1 and stepId = ?2 and effectiveTo = ?3", surveyId, r.downstreamStepId, StructuralElement.SENTINEL) == 0) {
            throw new AuthoringException("The rule targets a removed or missing step", "downstream");
        }
        if (r.downstreamSsId != null && StepsSections.count("surveyId = ?1 and stepsSectionsId = ?2 and effectiveTo = ?3", surveyId, r.downstreamSsId, StructuralElement.SENTINEL) == 0) {
            throw new AuthoringException("The rule targets a removed or missing section assignment", "downstream");
        }
        if (r.downstreamSqId != null && SectionsQuestion.count("surveyId = ?1 and sectionsQuestionId = ?2 and effectiveTo = ?3", surveyId, r.downstreamSqId, StructuralElement.SENTINEL) == 0) {
            throw new AuthoringException("The rule targets a removed or missing question assignment", "downstream");
        }
        if (r.downstreamSqId != null && r.downstreamSqId.equals(r.upstreamSqId)) {
            throw new AuthoringException("A rule cannot target the question it reads from", "downstream");
        }
        if (r.operatorType == null) {
            throw new AuthoringException("Choose an operator", "operatorType");
        }
        if (r.actionType == null) {
            throw new AuthoringException("Choose an action", "actionType");
        }
        if ("TEXT".equals(r.actionType.name)) {
            if (r.token == null || r.token.isBlank()) {
                throw new AuthoringException("A Text rule needs the token it replaces", "token");
            }
            if (r.token.trim().length() > 10) {
                throw new AuthoringException("Token names are at most 10 characters", "token");
            }
            r.token = r.token.trim();
        }
        if ("REPEAT".equals(r.actionType.name) && r.downstreamStepId != null) {
            throw new AuthoringException("Repeat applies to a section or a question, not a step", "downstream");
        }
        if (r.upstreamStepId != null && Step.count("surveyId = ?1 and stepId = ?2 and effectiveTo = ?3", surveyId, r.upstreamStepId, StructuralElement.SENTINEL) == 0) {
            throw new AuthoringException("The rule's upstream step is removed or missing", "upstreamStepId");
        }
        if (r.description == null || r.description.isBlank()) {
            r.description = RelationshipQuery.label(r);
        }
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
