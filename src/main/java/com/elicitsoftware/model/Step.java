package com.elicitsoftware.model;

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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

/** A top-level navigational unit of a survey ({@code survey.steps}). */
@Entity
@Table(name = "steps", schema = "survey")
public class Step extends StructuralElement {

    @Id
    @SequenceGenerator(name = "STEP_ID_GENERATOR", schema = "survey", sequenceName = "steps_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "STEP_ID_GENERATOR")
    @Column(name = "id", nullable = false)
    public Integer id;

    /** NUMERIC so a step can be inserted between two neighbors without renumbering. */
    @Column(name = "display_order", nullable = false)
    public BigDecimal displayOrder;

    @Column(name = "name", length = 255)
    public String name;

    @Column(name = "dimension_name", nullable = false, length = 50)
    public String dimensionName;

    @Column(name = "description", length = 255)
    public String description;

    @Column(name = "step_id", nullable = false)
    public Integer stepId;

    @Column(name = "step_key", nullable = false, updatable = false)
    public UUID stepKey;

    @Override public Integer getId() { return id; }
    @Override public Integer getDurableId() { return stepId; }
    @Override public void setDurableId(Integer durableId) { this.stepId = durableId; }
    @Override public UUID getElementKey() { return stepKey; }
    @Override public void setElementKey(UUID elementKey) { this.stepKey = elementKey; }
    @Override public String durableSequence() { return "survey.steps_durable_seq"; }
}
