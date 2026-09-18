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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

/** Assignment of a question into a section ({@code survey.sections_questions}); durable references. */
@Entity
@Table(name = "sections_questions", schema = "survey")
public class SectionsQuestion extends StructuralElement {

    @Id
    @SequenceGenerator(name = "SECTIONS_QUESTIONS_ID_GENERATOR", schema = "survey", sequenceName = "sections_questions_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SECTIONS_QUESTIONS_ID_GENERATOR")
    @Column(name = "id", nullable = false)
    public Integer id;

    @Column(name = "question_id", nullable = false)
    public Integer questionId;

    @Column(name = "question_version", nullable = false, updatable = false)
    public Integer questionVersion = 0;

    @Column(name = "section_id", nullable = false)
    public Integer sectionId;

    @Column(name = "section_version", nullable = false, updatable = false)
    public Integer sectionVersion = 0;

    @Column(name = "display_order", nullable = false)
    public BigDecimal displayOrder;

    @Column(name = "sections_question_id", nullable = false)
    public Integer sectionsQuestionId;

    @Column(name = "sections_question_key", nullable = false, updatable = false)
    public UUID sectionsQuestionKey;

    @Override public Integer getId() { return id; }
    @Override public Integer getDurableId() { return sectionsQuestionId; }
    @Override public void setDurableId(Integer durableId) { this.sectionsQuestionId = durableId; }
    @Override public UUID getElementKey() { return sectionsQuestionKey; }
    @Override public void setElementKey(UUID elementKey) { this.sectionsQuestionKey = elementKey; }
    @Override public String durableSequence() { return "survey.sections_questions_durable_seq"; }
}
