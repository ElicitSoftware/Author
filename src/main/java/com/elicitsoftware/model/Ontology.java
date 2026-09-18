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
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/** A survey-scoped tag that maps survey elements onto a reporting dimension ({@code survey.ontology}). */
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

    @Column(name = "name", nullable = false, length = 255)
    public String name;

    @Column(name = "tag", nullable = false, length = 255)
    public String tag;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "dimension")
    public Dimension dimension;
}
