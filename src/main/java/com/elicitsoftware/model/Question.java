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
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * A respondent-facing question ({@code survey.questions}). Placement into sections goes through
 * {@link SectionsQuestion}; the select group is referenced by its durable key only, because the
 * durable column is not unique across versions at a deployed site.
 */
@Entity
@Table(name = "questions", schema = "survey")
public class Question extends StructuralElement {

    @Id
    @SequenceGenerator(name = "QUESTIONS_ID_GENERATOR", schema = "survey", sequenceName = "questions_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "QUESTIONS_ID_GENERATOR")
    @Column(name = "id", nullable = false)
    public Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "type_id", nullable = false)
    public QuestionType questionType;

    @Column(name = "text", nullable = false, length = 8000)
    public String text;

    @Column(name = "short_text", length = 100)
    public String shortText;

    @Column(name = "tool_tip", length = 255)
    public String toolTip;

    @Column(name = "required", nullable = false)
    public boolean required = false;

    @Column(name = "min_value")
    public Integer minValue;

    @Column(name = "max_value")
    public Integer maxValue;

    @Column(name = "validation_text", length = 255)
    public String validationText;

    /** Durable key of the backing select group, if any. */
    @Column(name = "select_group_id")
    public Integer selectGroupId;

    /** FK companion pinned to 0 by a CHECK constraint; an existence check, never a version pin. */
    @Column(name = "select_group_version", nullable = false, updatable = false)
    public Integer selectGroupVersion = 0;

    @Column(name = "mask", length = 255)
    public String mask;

    @Column(name = "placeholder", length = 255)
    public String placeholder;

    @Column(name = "default_value", length = 255)
    public String defaultValue;

    @Column(name = "variant", length = 255)
    public String variant;

    @Column(name = "question_id", nullable = false)
    public Integer questionId;

    @Column(name = "question_key", nullable = false, updatable = false)
    public UUID questionKey;

    @Override public Integer getId() { return id; }
    @Override public Integer getDurableId() { return questionId; }
    @Override public void setDurableId(Integer durableId) { this.questionId = durableId; }
    @Override public UUID getElementKey() { return questionKey; }
    @Override public void setElementKey(UUID elementKey) { this.questionKey = elementKey; }
    @Override public String durableSequence() { return "survey.questions_durable_seq"; }
}
