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

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * A reporting dimension category ({@code survey.dimensions}); platform-wide, reused by name across
 * surveys. Its name becomes the reporting table {@code surveyreport.dim_<lower(name)>} that every
 * ontology tag on the dimension shares (UC-023 BR-001). Type 1: the {@code dimension_key} element
 * key (Survey V015) is minted once and never regenerated.
 */
@Entity
@Table(name = "dimensions", schema = "survey")
public class Dimension extends PanacheEntityBase {

    @Id
    @SequenceGenerator(name = "DIMENSIONS_ID_GENERATOR", schema = "survey", sequenceName = "dimensions_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "DIMENSIONS_ID_GENERATOR")
    @Column(name = "id", nullable = false)
    public Integer id;

    @Column(name = "dimension_key", nullable = false, updatable = false)
    public UUID dimensionKey;

    @Column(name = "name", length = 50)
    public String name;

    @PrePersist
    void ensureKey() {
        if (dimensionKey == null) {
            dimensionKey = UUID.randomUUID();
        }
    }
}
