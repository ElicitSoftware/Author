package com.elicitsoftware.model;

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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.AssertTrue;

import java.util.UUID;

/**
 * A conditional-logic rule ({@code survey.relationships}): an upstream question/answer drives a
 * SHOW, REPEAT or TEXT action on exactly one downstream step, section assignment or question
 * assignment. All five cross-references are durable keys. {@code token} is the question-text
 * placeholder name used by TEXT actions (never the respondent's access code).
 */
@Entity
@Table(name = "relationships", schema = "survey")
public class Relationship extends StructuralElement {

    @Id
    @SequenceGenerator(name = "RELATIONSHIPS_ID_GENERATOR", schema = "survey", sequenceName = "relationships_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "RELATIONSHIPS_ID_GENERATOR")
    @Column(name = "id", nullable = false)
    public Integer id;

    @Column(name = "upstream_step_id")
    public Integer upstreamStepId;

    @Column(name = "upstream_step_version", nullable = false, updatable = false)
    public Integer upstreamStepVersion = 0;

    @Column(name = "upstream_sq_id", nullable = false)
    public Integer upstreamSqId;

    @Column(name = "upstream_sq_version", nullable = false, updatable = false)
    public Integer upstreamSqVersion = 0;

    @Column(name = "downstream_step_id")
    public Integer downstreamStepId;

    @Column(name = "downstream_step_version", nullable = false, updatable = false)
    public Integer downstreamStepVersion = 0;

    @Column(name = "downstream_ss_id")
    public Integer downstreamSsId;

    @Column(name = "downstream_ss_version", nullable = false, updatable = false)
    public Integer downstreamSsVersion = 0;

    @Column(name = "downstream_sq_id")
    public Integer downstreamSqId;

    @Column(name = "downstream_sq_version", nullable = false, updatable = false)
    public Integer downstreamSqVersion = 0;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "operator_id", nullable = false)
    public OperatorType operatorType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "action_id", nullable = false)
    public ActionType actionType;

    @Column(name = "description", length = 255)
    public String description;

    @Column(name = "token", length = 10)
    public String token;

    @Column(name = "reference_value", length = 255)
    public String referenceValue;

    @Column(name = "default_upstream_value", length = 255)
    public String defaultUpstreamValue;

    @Column(name = "override_upstream_value", length = 255)
    public String overrideUpstreamValue;

    @Column(name = "relationship_id", nullable = false)
    public Integer relationshipId;

    @Column(name = "relationship_key", nullable = false, updatable = false)
    public UUID relationshipKey;

    /** Mirrors the table's {@code downstream_ck}: exactly one downstream target. */
    @AssertTrue(message = "A relationship must target exactly one of: step, section assignment, question assignment")
    public boolean hasExactlyOneDownstream() {
        int n = (downstreamStepId != null ? 1 : 0) + (downstreamSsId != null ? 1 : 0) + (downstreamSqId != null ? 1 : 0);
        return n == 1;
    }

    @Override public Integer getId() { return id; }
    @Override public Integer getDurableId() { return relationshipId; }
    @Override public void setDurableId(Integer durableId) { this.relationshipId = durableId; }
    @Override public UUID getElementKey() { return relationshipKey; }
    @Override public void setElementKey(UUID elementKey) { this.relationshipKey = elementKey; }
    @Override public String durableSequence() { return "survey.relationships_durable_seq"; }
}
