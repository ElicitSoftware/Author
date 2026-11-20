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
 * Links one structural element (by durable key: a step/section assignment, a question, or a
 * section/question assignment) to an ontology tag ({@code survey.metadata}). Exactly one of the
 * three element columns is set (UC-021 BR-001; the schema's check constraint does not enforce it).
 * A null {@code value} means the respondent's answer is the reported value; a constant is stored
 * whenever the tagged element is answered. Type 1: reassigning a tag is one of the two retroactive
 * edits (see {@code docs/scd-contract.md}); {@code metadata_key} is minted once.
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

    @Column(name = "metadata_key", nullable = false, updatable = false)
    public UUID metadataKey;

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

    /** Which of the three element columns this row uses. */
    public enum ElementRef {
        /** {@code steps_sections_id}: a section as mounted in one step. */
        STEPS_SECTIONS,
        /** {@code question_id}: the question wherever it appears. */
        QUESTION,
        /** {@code sections_question_id}: the question only in one section. */
        SECTIONS_QUESTION
    }

    /**
     * The element column set on this row, or {@code null} when none or several are set.
     *
     * @return the column in use, or {@code null}
     */
    public ElementRef elementRef() {
        int set = (stepsSectionsId != null ? 1 : 0) + (questionId != null ? 1 : 0) + (sectionsQuestionId != null ? 1 : 0);
        if (set != 1) {
            return null;
        }
        return stepsSectionsId != null ? ElementRef.STEPS_SECTIONS : questionId != null ? ElementRef.QUESTION : ElementRef.SECTIONS_QUESTION;
    }

    /**
     * The durable id in whichever element column is set, or {@code null}.
     *
     * @return the id, or {@code null} when none or several columns are set
     */
    public Integer elementId() {
        ElementRef ref = elementRef();
        if (ref == null) {
            return null;
        }
        return switch (ref) {
            case STEPS_SECTIONS -> stepsSectionsId;
            case QUESTION -> questionId;
            case SECTIONS_QUESTION -> sectionsQuestionId;
        };
    }

    @PrePersist
    void ensureKey() {
        if (metadataKey == null) {
            metadataKey = UUID.randomUUID();
        }
    }
}
