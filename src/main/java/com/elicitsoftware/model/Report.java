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
 * One report service a survey offers its respondents ({@code survey.reports}, UC-048): the
 * built-in respondent report every new survey is seeded with, or a custom service such as
 * FHHS's. Type 1: no versions, removal is a delete, and a site deletes the row whose key the next
 * export no longer lists (UC-048 BR-006). {@code report_key} is minted once (C-017). The built-in
 * row is distinguished only by its address (UC-048 BR-002).
 */
@Entity
@Table(name = "reports", schema = "survey")
public class Report extends PanacheEntityBase {

    @Id
    @SequenceGenerator(name = "REPORT_ID_GENERATOR", schema = "survey", sequenceName = "reports_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "REPORT_ID_GENERATOR")
    @Column(name = "id", nullable = false)
    public Integer id;

    @Column(name = "survey_id", nullable = false)
    public Integer surveyId;

    @Column(name = "report_key", nullable = false, updatable = false)
    public UUID reportKey;

    /** Heads the report's card at a site; translatable (UC-043). */
    @Column(name = "name", nullable = false, length = 255)
    public String name;

    /** Shown with the card; translatable. */
    @Column(name = "description", nullable = false, length = 255)
    public String description;

    /** A path under the site's Survey module, or an absolute http(s) URL (UC-048 BR-004). */
    @Column(name = "url", length = 255)
    public String url;

    /** Position on the respondent's report page, 1..n (UC-048 BR-005). */
    @Column(name = "display_order", nullable = false)
    public Integer displayOrder;

    @PrePersist
    void ensureKey() {
        if (reportKey == null) {
            reportKey = UUID.randomUUID();
        }
    }
}
