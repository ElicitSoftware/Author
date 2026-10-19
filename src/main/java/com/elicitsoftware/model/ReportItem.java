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

import com.elicitsoftware.model.Metadata.ElementRef;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * A report mark ({@code survey.report_items}, Survey V023, UC-049): one structural element, named
 * by durable id exactly as {@link Metadata} names it, marked for the respondent report or the
 * administrator report, with an order within that report and an optional label that replaces the
 * element's text. Type 1: unmarking deletes the row, a label change updates it in place
 * (UC-049 BR-005); {@code report_item_key} is minted once (C-017). The schema enforces that exactly
 * one element column is set.
 */
@Entity
@Table(name = "report_items", schema = "survey")
public class ReportItem extends PanacheEntityBase {

    /** Which default report a mark belongs to (UC-049 BR-004). */
    public enum ReportType {
        RESPONDENT,
        ADMINISTRATOR
    }

    @Id
    @SequenceGenerator(name = "REPORT_ITEM_ID_GENERATOR", schema = "survey", sequenceName = "report_items_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "REPORT_ITEM_ID_GENERATOR")
    @Column(name = "id", nullable = false)
    public Integer id;

    @Column(name = "survey_id", nullable = false)
    public Integer surveyId;

    @Column(name = "report_item_key", nullable = false, updatable = false)
    public UUID reportItemKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 13)
    public ReportType reportType;

    @Column(name = "steps_sections_id")
    public Integer stepsSectionsId;

    @Column(name = "question_id")
    public Integer questionId;

    @Column(name = "sections_question_id")
    public Integer sectionsQuestionId;

    /** Replaces the element's own text on the report; translatable (UC-049 BR-006). */
    @Column(name = "label", length = 255)
    public String label;

    /** Position within its report, 1..n (UC-049 BR-004). */
    @Column(name = "display_order", nullable = false)
    public Integer displayOrder;

    /** The element column set on this row, or {@code null} when none or several are set. */
    public ElementRef elementRef() {
        int set = (stepsSectionsId != null ? 1 : 0) + (questionId != null ? 1 : 0) + (sectionsQuestionId != null ? 1 : 0);
        if (set != 1) {
            return null;
        }
        return stepsSectionsId != null ? ElementRef.STEPS_SECTIONS : questionId != null ? ElementRef.QUESTION : ElementRef.SECTIONS_QUESTION;
    }

    /** The durable id in whichever element column is set, or {@code null}. */
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
        if (reportItemKey == null) {
            reportItemKey = UUID.randomUUID();
        }
        if (displayOrder == null) {
            displayOrder = 0;
        }
    }
}
