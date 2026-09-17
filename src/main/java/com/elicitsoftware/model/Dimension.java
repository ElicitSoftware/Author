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
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * A reporting dimension category ({@code survey.dimensions}); platform-wide, reused by name across
 * surveys. The {@code dimension_key} element key arrives with Survey V015 and is mapped once the
 * schema mirror carries it.
 */
@Entity
@Table(name = "dimensions", schema = "survey")
public class Dimension extends PanacheEntityBase {

    @Id
    @SequenceGenerator(name = "DIMENSIONS_ID_GENERATOR", schema = "survey", sequenceName = "dimensions_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "DIMENSIONS_ID_GENERATOR")
    @Column(name = "id", nullable = false)
    public Integer id;

    @Column(name = "name", length = 50)
    public String name;
}
