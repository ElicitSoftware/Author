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

/**
 * Links one structural element (by durable key: a step/section assignment, a question, or a
 * section/question assignment) to an ontology tag ({@code survey.metadata}). Type 1: reassigning a
 * question's tag is one of the two retroactive edits (see {@code docs/scd-contract.md}).
 */
@Entity
@Table(name = "metadata", schema = "survey")
public class Metadata extends PanacheEntityBase {

    @Id
    @SequenceGenerator(name = "METADATA_ID_GENERATOR", schema = "survey", sequenceName = "metadata_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "METADATA_ID_GENERATOR")
    @Column(name = "id", nullable = false)
    public Integer id;

    @Column(name = "survey_id", nullable = false)
    public Integer surveyId;

    @Column(name = "steps_sections_id")
    public Integer stepsSectionsId;

    @Column(name = "question_id")
    public Integer questionId;

    @Column(name = "sections_question_id")
    public Integer sectionsQuestionId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ontology_id", nullable = false)
    public Ontology ontology;

    @Column(name = "value", length = 255)
    public String value;
}
