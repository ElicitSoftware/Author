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
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.util.Objects;
import java.util.UUID;

/**
 * A survey definition ({@code survey.surveys}). Type 1: edited in place, never versioned.
 * {@code survey_key} is the cross-deployment identity; {@code published_by}/{@code published_comment}
 * record the last export (UC-008).
 */
@Entity
@Table(name = "surveys", schema = "survey")
public class Survey extends PanacheEntityBase {

    @Id
    @SequenceGenerator(name = "SURVEY_ID_GENERATOR", schema = "survey", sequenceName = "surveys_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SURVEY_ID_GENERATOR")
    @Column(name = "id", nullable = false)
    public Integer id;

    @Column(name = "survey_key", nullable = false, unique = true, updatable = false)
    public UUID surveyKey;

    @Column(name = "name", length = 255)
    public String name;

    @Column(name = "display_order", nullable = false)
    public Integer displayOrder;

    @Column(name = "title", length = 255)
    public String title;

    @Column(name = "description", length = 2000)
    public String description;

    @Column(name = "initial_display_key", length = 255)
    public String initialDisplayKey;

    @Column(name = "post_survey_url", length = 255)
    public String postSurveyUrl;

    @Column(name = "published_by")
    public String publishedBy;

    @Column(name = "published_comment")
    public String publishedComment;

    @PrePersist
    void ensureSurveyKey() {
        if (surveyKey == null) {
            surveyKey = UUID.randomUUID();
        }
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Survey other && id != null && Objects.equals(id, other.id));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
