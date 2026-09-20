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

/** A grouping of questions within a step ({@code survey.sections}). */
@Entity
@Table(name = "sections", schema = "survey")
public class Section extends StructuralElement {

    @Id
    @SequenceGenerator(name = "SECTIONS_ID_GENERATOR", schema = "survey", sequenceName = "sections_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SECTIONS_ID_GENERATOR")
    @Column(name = "id", nullable = false)
    public Integer id;

    @Column(name = "display_order", nullable = false)
    public BigDecimal displayOrder;

    @Column(name = "name", length = 255)
    public String name;

    @Column(name = "dimension_name", nullable = false, length = 50)
    public String dimensionName;

    @Column(name = "description", length = 255)
    public String description;

    @Column(name = "section_id", nullable = false)
    public Integer sectionId;

    @Column(name = "section_key", nullable = false, updatable = false)
    public UUID sectionKey;

    @Override public Integer getId() { return id; }
    @Override public Integer getDurableId() { return sectionId; }
    @Override public void setDurableId(Integer durableId) { this.sectionId = durableId; }
    @Override public UUID getElementKey() { return sectionKey; }
    @Override public void setElementKey(UUID elementKey) { this.sectionKey = elementKey; }
    @Override public String durableSequence() { return "survey.sections_durable_seq"; }
}
