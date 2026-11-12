/*-
 * ***LICENSE_START***
 * Elicit Author
 * %%
 * Copyright (C) 2025 - 2026 The Regents of the University of Michigan - Rogel Cancer Center
 * %%
 * PolyForm Noncommercial License 1.0.0
 * <https://polyformproject.org/licenses/noncommercial/1.0.0>
 * ***LICENSE_END***
 */
package com.elicitsoftware.model;

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
 * One post-survey action a survey's site calls when a respondent finishes (UC-052): a Type 1 row
 * of {@code survey.post_survey_actions}, edited in place and matched at a site by its key.
 */
@Entity
@Table(name = "post_survey_actions", schema = "survey")
public class PostSurveyAction extends PanacheEntityBase {

    @Id
    @SequenceGenerator(name = "POST_SURVEY_ACTION_ID_GENERATOR", schema = "survey", sequenceName = "post_survey_actions_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "POST_SURVEY_ACTION_ID_GENERATOR")
    @Column(name = "id", nullable = false)
    public Integer id;

    @Column(name = "survey_id", nullable = false)
    public Integer surveyId;

    @Column(name = "post_survey_action_key", nullable = false, updatable = false)
    public UUID postSurveyActionKey;

    /** A note for authors and site operators; never shown to a respondent, never translated (UC-052 BR-001). */
    @Column(name = "name", nullable = false, length = 255)
    public String name;

    /** Required (UC-052 BR-003). */
    @Column(name = "description", nullable = false, length = 255)
    public String description;

    /** The absolute http(s) address Survey calls (UC-052 BR-004). */
    @Column(name = "url", length = 255)
    public String url;

    /** Position in the order Survey calls the survey's actions, 1..n (UC-052 BR-005). */
    @Column(name = "execution_order", nullable = false)
    public Integer executionOrder;

    @PrePersist
    void ensureKey() {
        if (postSurveyActionKey == null) {
            postSurveyActionKey = UUID.randomUUID();
        }
    }
}
