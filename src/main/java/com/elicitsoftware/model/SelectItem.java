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

/** One option of a select group ({@code survey.select_items}). */
@Entity
@Table(name = "select_items", schema = "survey")
public class SelectItem extends StructuralElement {

    @Id
    @SequenceGenerator(name = "SELECT_ITEMS_ID_GENERATOR", schema = "survey", sequenceName = "select_items_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SELECT_ITEMS_ID_GENERATOR")
    @Column(name = "id", nullable = false)
    public Integer id;

    /** Durable key of the owning select group. */
    @Column(name = "select_group_id", nullable = false)
    public Integer selectGroupId;

    @Column(name = "select_group_version", nullable = false, updatable = false)
    public Integer selectGroupVersion = 0;

    @Column(name = "display_text", length = 255)
    public String displayText;

    @Column(name = "display_order", nullable = false)
    public Integer displayOrder;

    @Column(name = "coded_value", length = 255)
    public String codedValue;

    @Column(name = "select_item_id", nullable = false)
    public Integer selectItemId;

    @Column(name = "select_item_key", nullable = false, updatable = false)
    public UUID selectItemKey;

    @Override public Integer getId() { return id; }
    @Override public Integer getDurableId() { return selectItemId; }
    @Override public void setDurableId(Integer durableId) { this.selectItemId = durableId; }
    @Override public UUID getElementKey() { return selectItemKey; }
    @Override public void setElementKey(UUID elementKey) { this.selectItemKey = elementKey; }
    @Override public String durableSequence() { return "survey.select_items_durable_seq"; }
}
