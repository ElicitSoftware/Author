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

/** Platform-wide vocabulary row ({@code survey.question_types}); read-only for Author. */
@Entity
@Table(name = "question_types", schema = "survey")
public class QuestionType extends PanacheEntityBase {

    @Id
    @SequenceGenerator(name = "QUESTION_TYPES_ID_GENERATOR", schema = "survey", sequenceName = "question_types_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "QUESTION_TYPES_ID_GENERATOR")
    @Column(name = "id", nullable = false)
    public Integer id;

    @Column(name = "name", length = 255)
    public String name;

    @Column(name = "description", length = 255)
    public String description;

    @Column(name = "data_type", length = 255)
    public String dataType;

    @Override
    public String toString() {
        return name;
    }
}
