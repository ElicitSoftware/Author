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
package com.elicitsoftware.author.survey;

import com.elicitsoftware.author.AuthoringException;
import com.elicitsoftware.author.ConstraintViolationTranslator;
import com.elicitsoftware.author.definition.ValidationIssue;
import com.elicitsoftware.author.i18n.Translations;
import com.elicitsoftware.author.survey.ReportingService.Located;
import com.elicitsoftware.author.survey.ReportingService.Locator;
import com.elicitsoftware.model.Metadata.ElementRef;
import com.elicitsoftware.model.ReportItem;
import com.elicitsoftware.model.ReportItem.ReportType;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Report marks (UC-049): which elements the respondent report echoes back and which the
 * administrator report monitors, as {@code survey.report_items} rows. A mark names its element by
 * durable id the way an analysis-tag assignment does, so the same locator resolves both.
 */
@ApplicationScoped
public class ReportMarkService {

    /** Table name used in {@link ValidationIssue#kind()}. */
    public static final String KIND = "report_items";

    /** UC-049 BR-003: question types that have no value to show back. */
    static final Set<String> UNMARKABLE_TYPES = Set.of("HTML", "PASSWORD");

    /**
     * One mark resolved against the structure: the element it names and where that element sits.
     *
     * @param item         the {@code survey.report_items} row
     * @param ref          the scope the row names its element by, or {@code null} when the row names none
     * @param elementLabel the element's own text, empty when the element is not found
     * @param where        where the element sits in the structure, empty when it is not found
     * @param dangling     whether the element is gone or retired (UC-049 A3)
     * @param node         the element's structure node, or {@code null} when it is not found
     */
    public record Mark(ReportItem item, ElementRef ref, String elementLabel, String where, boolean dangling, StructureNode node) {

        /**
         * Returns the translated name of the mark's scope, as for a tag assignment.
         *
         * @return the scope label, or the invalid-scope label when {@link #ref()} is {@code null}
         */
        public String scopeLabel() {
            return ReportingService.Assignment.scopeLabel(ref);
        }

        /**
         * What the report shows: the label when set, the element's own text otherwise.
         *
         * @return the mark's label, or {@link #elementLabel()} when the label is {@code null} or blank
         */
        public String shownAs() {
            return item.label == null || item.label.isBlank() ? elementLabel : item.label;
        }
    }

    @Inject
    EntityManager em;

    @Inject
    ReportingService reporting;

    /**
     * Returns every mark of a survey, resolved against its structure with retired elements included,
     * ordered by report, then display order, then id.
     *
     * @param surveyId the survey's id
     * @return the survey's marks; empty when it has none
     */
    public List<Mark> marks(int surveyId) {
        Locator locator = reporting.locator(surveyId);
        List<Mark> out = new ArrayList<>();
        for (ReportItem i : ReportItem.<ReportItem>list("surveyId", Sort.by("reportType").and("displayOrder").and("id"), surveyId)) {
            out.add(resolve(locator, i));
        }
        return out;
    }

    /**
     * Returns the marks of one report of a survey, in display order.
     *
     * @param surveyId the survey's id
     * @param type     the report
     * @return the marks on that report; empty when it has none
     */
    public List<Mark> marks(int surveyId, ReportType type) {
        return marks(surveyId).stream().filter(m -> m.item().reportType == type).toList();
    }

    /**
     * The marks that concern one board node: both scopes for a question, the mount for a section.
     *
     * @param surveyId the survey's id
     * @param node     the board node; {@code null} matches no mark
     * @return the matching marks; empty when there are none
     */
    public List<Mark> marksFor(int surveyId, StructureNode node) {
        return marks(surveyId).stream().filter(m -> concerns(m.item(), node)).toList();
    }

    /**
     * The marks that concern one element by kind and durable id (the element detail page).
     *
     * @param surveyId  the survey's id
     * @param kind      the element's kind
     * @param durableId the element's durable id
     * @return the marks whose resolved node has that kind and durable id; empty when there are none
     */
    public List<Mark> marksFor(int surveyId, StructureNode.Kind kind, int durableId) {
        return marks(surveyId).stream()
                .filter(m -> m.node() != null && m.node().kind() == kind && Objects.equals(m.node().durableId(), durableId))
                .toList();
    }

    /**
     * UC-049 BR-003: a step is never marked, nor a question with no value to show.
     *
     * @param node the structure node, may be {@code null}
     * @return {@code false} for {@code null}, a step, or a question of an unmarkable type; {@code true} otherwise
     */
    public static boolean markable(StructureNode node) {
        if (node == null || node.kind() == StructureNode.Kind.STEP) {
            return false;
        }
        return node.kind() != StructureNode.Kind.QUESTION || node.detail() == null || !UNMARKABLE_TYPES.contains(node.detail());
    }

    /**
     * UC-049 steps 3-4: one new mark at the end of its report's order.
     *
     * @param surveyId  the survey's id
     * @param ref       the scope the element is named by
     * @param durableId the element's id within that scope
     * @param type      the report the mark goes on
     * @param label     the optional label; blank means the element's own text (BR-006)
     * @return the persisted mark
     * @throws AuthoringException when the scope or report is missing, the element is gone, the element
     *                            cannot be marked, it is already marked on that report, the label is
     *                            too long, or the database refuses the row
     */
    @Transactional
    public ReportItem mark(int surveyId, ElementRef ref, int durableId, ReportType type, String label) {
        if (ref == null) {
            throw new AuthoringException(Translations.get("error.mark.scopeRequired"), "scope");
        }
        if (type == null) {
            throw new AuthoringException(Translations.get("error.mark.reportRequired"), "report");
        }
        Located located = reporting.locator(surveyId).locate(ref, durableId);
        if (located.node() == null) {
            throw new AuthoringException(Translations.get("error.mark.elementGone"), "scope");
        }
        if (!markable(located.node())) {
            throw new AuthoringException(Translations.get("error.mark.notMarkable", located.node().detail()), "scope");
        }
        for (ReportItem existing : ReportItem.<ReportItem>list("surveyId", surveyId)) {
            if (existing.reportType == type && existing.elementRef() == ref && Objects.equals(existing.elementId(), durableId)) {
                throw new AuthoringException(Translations.get("error.mark.alreadyMarked", reportLabel(type)), "report");
            }
        }
        ReportItem i = new ReportItem();
        i.surveyId = surveyId;
        i.reportType = type;
        i.label = validLabel(label);
        i.displayOrder = (int) ReportItem.count("surveyId = ?1 and reportType = ?2", surveyId, type) + 1;
        switch (ref) {
            case STEPS_SECTIONS -> i.stepsSectionsId = durableId;
            case QUESTION -> i.questionId = durableId;
            case SECTIONS_QUESTION -> i.sectionsQuestionId = durableId;
        }
        try {
            i.persist();
            em.flush();
        } catch (RuntimeException e) {
            throw ConstraintViolationTranslator.translate(e);
        }
        return i;
    }

    /**
     * UC-049 step 6: the label changes in place (BR-005).
     *
     * @param surveyId the survey's id
     * @param itemId   the mark's id
     * @param label    the new label; blank clears it
     * @return the updated mark
     * @throws AuthoringException when the mark is gone or the label is too long
     */
    @Transactional
    public ReportItem relabel(int surveyId, int itemId, String label) {
        ReportItem i = item(surveyId, itemId);
        i.label = validLabel(label);
        em.flush();
        return i;
    }

    /**
     * UC-049 step 7, BR-005: delete and renumber the report it was on.
     *
     * @param surveyId the survey's id
     * @param itemId   the mark's id
     * @throws AuthoringException when the mark is gone
     */
    @Transactional
    public void unmark(int surveyId, int itemId) {
        ReportItem i = item(surveyId, itemId);
        ReportType type = i.reportType;
        i.delete();
        em.flush();
        renumber(surveyId, type);
    }

    /**
     * UC-049 step 5, BR-004: the given ids take positions 1..n on their report; the rest follow in their old order.
     *
     * @param surveyId   the survey's id
     * @param type       the report to reorder
     * @param idsInOrder the mark ids in their new order; ids not on that report are ignored
     */
    @Transactional
    public void reorder(int surveyId, ReportType type, List<Integer> idsInOrder) {
        List<ReportItem> all = ReportItem.list("surveyId = ?1 and reportType = ?2", Sort.by("displayOrder").and("id"), surveyId, type);
        int next = 1;
        for (Integer id : idsInOrder) {
            for (ReportItem i : all) {
                if (Objects.equals(i.id, id)) {
                    i.displayOrder = next++;
                }
            }
        }
        for (ReportItem i : all) {
            if (!idsInOrder.contains(i.id)) {
                i.displayOrder = next++;
            }
        }
        em.flush();
    }

    /**
     * UC-049 A3: a mark on a removed element is an export warning, never an error.
     *
     * @param surveyId the survey's id
     * @return one warning per dangling mark; empty when there are none
     */
    public List<ValidationIssue> problems(int surveyId) {
        List<ValidationIssue> out = new ArrayList<>();
        for (Mark m : marks(surveyId)) {
            if (m.dangling()) {
                out.add(ValidationIssue.warning(KIND, m.item().id, Translations.get("validation.mark.removedElement",
                        reportLabel(m.item().reportType), m.elementLabel().isEmpty() ? "#" + m.item().elementId() : m.elementLabel())));
            }
        }
        return out;
    }

    /**
     * Returns the translated name of a report.
     *
     * @param type the report
     * @return the respondent report's name for {@link ReportType#RESPONDENT}, the administrator
     *         report's name for anything else, {@code null} included
     */
    public static String reportLabel(ReportType type) {
        return type == ReportType.RESPONDENT ? Translations.get("report.type.respondent") : Translations.get("report.type.administrator");
    }

    private void renumber(int surveyId, ReportType type) {
        int next = 1;
        for (ReportItem i : ReportItem.<ReportItem>list("surveyId = ?1 and reportType = ?2", Sort.by("displayOrder").and("id"), surveyId, type)) {
            i.displayOrder = next++;
        }
        em.flush();
    }

    private ReportItem item(int surveyId, int itemId) {
        return ReportItem.<ReportItem>find("surveyId = ?1 and id = ?2", surveyId, itemId).firstResultOptional()
                .orElseThrow(() -> new AuthoringException(Translations.get("error.mark.gone")));
    }

    /** UC-049 BR-006: optional, at most 255 characters; blank means the element's own text. */
    private static String validLabel(String label) {
        String clean = label == null ? "" : label.strip();
        if (clean.length() > 255) {
            throw new AuthoringException(Translations.get("error.mark.labelTooLong"), "label");
        }
        return clean.isEmpty() ? null : clean;
    }

    private static Mark resolve(Locator locator, ReportItem i) {
        ElementRef ref = i.elementRef();
        if (ref == null) {
            return new Mark(i, null, "", "", true, null);
        }
        Located l = locator.locate(ref, i.elementId());
        return new Mark(i, ref, l.label(), l.where(), l.dangling(), l.node());
    }

    private static boolean concerns(ReportItem i, StructureNode node) {
        ElementRef ref = i.elementRef();
        if (ref == null || node == null) {
            return false;
        }
        return switch (ref) {
            case QUESTION -> node.kind() == StructureNode.Kind.QUESTION && Objects.equals(node.durableId(), i.questionId);
            case SECTIONS_QUESTION -> node.kind() == StructureNode.Kind.QUESTION && Objects.equals(node.assignmentId(), i.sectionsQuestionId);
            case STEPS_SECTIONS -> node.kind() == StructureNode.Kind.SECTION && Objects.equals(node.assignmentId(), i.stepsSectionsId);
        };
    }
}
