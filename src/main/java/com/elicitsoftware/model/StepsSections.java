package com.elicitsoftware.model;

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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Assignment of a section into a step ({@code survey.steps_sections}). {@code step_id} and
 * {@code section_id} are durable keys. The display key's leading component is Author's local
 * survey id; a deployed site rebases it on import.
 */
@Entity
@Table(name = "steps_sections", schema = "survey")
public class StepsSections extends StructuralElement {

    @Id
    @SequenceGenerator(name = "STEPS_SECTIONS_ID_GENERATOR", schema = "survey", sequenceName = "steps_sections_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "STEPS_SECTIONS_ID_GENERATOR")
    @Column(name = "id", nullable = false)
    public Integer id;

    @Column(name = "step_id", nullable = false)
    public Integer stepId;

    @Column(name = "step_version", nullable = false, updatable = false)
    public Integer stepVersion = 0;

    @Column(name = "step_display_order", nullable = false)
    public BigDecimal stepDisplayOrder;

    @Column(name = "section_id", nullable = false)
    public Integer sectionId;

    @Column(name = "section_version", nullable = false, updatable = false)
    public Integer sectionVersion = 0;

    @Column(name = "section_display_order", nullable = false)
    public BigDecimal sectionDisplayOrder;

    @Column(name = "display_key", nullable = false, length = 34)
    public String displayKey;

    @Column(name = "steps_sections_id", nullable = false)
    public Integer stepsSectionsId;

    @Column(name = "steps_sections_key", nullable = false, updatable = false)
    public UUID stepsSectionsKey;

    @Override public Integer getId() { return id; }
    @Override public Integer getDurableId() { return stepsSectionsId; }
    @Override public void setDurableId(Integer durableId) { this.stepsSectionsId = durableId; }
    @Override public UUID getElementKey() { return stepsSectionsKey; }
    @Override public void setElementKey(UUID elementKey) { this.stepsSectionsKey = elementKey; }
    @Override public String durableSequence() { return "survey.steps_sections_durable_seq"; }
}
