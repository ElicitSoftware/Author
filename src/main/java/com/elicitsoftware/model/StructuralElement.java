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

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Common shape of the eight Kimball Type 2 structural tables ({@code steps}, {@code sections},
 * {@code questions}, {@code select_groups}, {@code select_items}, {@code steps_sections},
 * {@code sections_questions}, {@code relationships}).
 *
 * <p>In Author's working database every element has exactly one row: {@code version = 0},
 * {@code effective_from} at the epoch and {@code effective_to} at the {@link #SENTINEL} while the
 * element is current. Removing an element (UC-009) closes {@code effective_to}; restoring it
 * (UC-010) reopens it. Version rows are created only by Admin's update service at a deployed
 * site, so Author never writes {@code version}.</p>
 *
 * <p>Three identities live on every row (see {@code docs/scd-contract.md}): the surrogate
 * {@code id} of this physical row, the durable {@code {entity}_id} shared by every version of the
 * element inside one database, and the {@link #getElementKey() element key}, the UUID that is the
 * same for this element in every deployment and is what the definition file carries.</p>
 */
@MappedSuperclass
public abstract class StructuralElement extends PanacheEntityBase {

    /** {@code effective_from} of a row that has "existed since forever" (the schema default). */
    public static final OffsetDateTime EPOCH = OffsetDateTime.parse("1970-01-01T00:00:00Z");

    /** {@code effective_to} of a current row (the schema default). */
    public static final OffsetDateTime SENTINEL = OffsetDateTime.parse("9999-12-31T23:59:59Z");

    @Column(name = "survey_id", nullable = false)
    public Integer surveyId;

    @Column(name = "version", nullable = false)
    public Integer version = 0;

    @Column(name = "effective_from")
    public OffsetDateTime effectiveFrom = EPOCH;

    @Column(name = "effective_to")
    public OffsetDateTime effectiveTo = SENTINEL;

    @Column(name = "published_by")
    public String publishedBy;

    @Column(name = "published_comment")
    public String publishedComment;

    /** Surrogate primary key of this physical row. */
    public abstract Integer getId();

    /** Durable key ({@code question_id}, {@code step_id}, ...): stable across versions within one database. */
    public abstract Integer getDurableId();

    public abstract void setDurableId(Integer durableId);

    /** Element key: cross-deployment identity, minted once and never regenerated. */
    public abstract UUID getElementKey();

    public abstract void setElementKey(UUID elementKey);

    /** Fully qualified name of the sequence that mints durable keys for this table. */
    public abstract String durableSequence();

    public boolean isCurrent() {
        return SENTINEL.equals(effectiveTo);
    }

    public boolean isRetired() {
        return effectiveTo != null && !isCurrent();
    }

    @PrePersist
    void ensureElementKey() {
        if (getElementKey() == null) {
            setElementKey(UUID.randomUUID());
        }
    }
}
