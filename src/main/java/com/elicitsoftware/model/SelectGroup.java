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

import java.util.UUID;

/** A named list of options for choice questions ({@code survey.select_groups}). */
@Entity
@Table(name = "select_groups", schema = "survey")
public class SelectGroup extends StructuralElement {

    @Id
    @SequenceGenerator(name = "SELECT_GROUPS_ID_GENERATOR", schema = "survey", sequenceName = "select_groups_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SELECT_GROUPS_ID_GENERATOR")
    @Column(name = "id", nullable = false)
    public Integer id;

    @Column(name = "name", length = 255)
    public String name;

    @Column(name = "description", length = 255)
    public String description;

    @Column(name = "data_type", nullable = false, length = 50)
    public String dataType = "Text";

    @Column(name = "select_group_id", nullable = false)
    public Integer selectGroupId;

    @Column(name = "select_group_key", nullable = false, updatable = false)
    public UUID selectGroupKey;

    @Override public Integer getId() { return id; }
    @Override public Integer getDurableId() { return selectGroupId; }
    @Override public void setDurableId(Integer durableId) { this.selectGroupId = durableId; }
    @Override public UUID getElementKey() { return selectGroupKey; }
    @Override public void setElementKey(UUID elementKey) { this.selectGroupKey = elementKey; }
    @Override public String durableSequence() { return "survey.select_groups_durable_seq"; }
}
