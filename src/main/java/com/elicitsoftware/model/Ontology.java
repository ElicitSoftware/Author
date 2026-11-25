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
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * A survey-scoped reporting tag ({@code survey.ontology}). The tag names a column of the site's
 * {@code surveyreport.fact_sections}; with a {@link Dimension} its values share that dimension's
 * table, without one it gets a table of its own (UC-023). {@code name} is a namespace — Author
 * writes the survey's name — that the ETL never reads; {@code (name, tag)} is unique across the
 * database. Type 1: {@code ontology_key} is minted once.
 */
@Entity
@Table(name = "ontology", schema = "survey")
public class Ontology extends PanacheEntityBase {

    @Id
    @SequenceGenerator(name = "ONTOLOGY_ID_GENERATOR", schema = "survey", sequenceName = "ontology_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ONTOLOGY_ID_GENERATOR")
    @Column(name = "id", nullable = false)
    public Integer id;

    @Column(name = "survey_id", nullable = false)
    public Integer surveyId;

    @Column(name = "ontology_key", nullable = false, updatable = false)
    public UUID ontologyKey;

    @Column(name = "name", nullable = false, length = 255)
    public String name;

    @Column(name = "tag", nullable = false, length = 255)
    public String tag;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "dimension")
    public Dimension dimension;

    @PrePersist
    void ensureKey() {
        if (ontologyKey == null) {
            ontologyKey = UUID.randomUUID();
        }
    }
}
