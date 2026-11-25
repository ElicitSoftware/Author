/*-
 * ***LICENSE_START***
 * Elicit Author
 * %%
 * Copyright (C) 2025 - 2026 Elicit Software LLC
 * %%
 * PolyForm Noncommercial License 1.0.0
 * <https://polyformproject.org/licenses/noncommercial/1.0.0>
 * ***LICENSE_END***
 */
package com.elicitsoftware.author.flow.reports;

import com.elicitsoftware.author.AuthoringException;
import com.elicitsoftware.author.survey.ReportMarkService;
import com.elicitsoftware.author.survey.ReportMarkService.Mark;
import com.elicitsoftware.author.survey.StructureNode;
import com.elicitsoftware.model.Survey;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import java.util.List;

/**
 * UC-049 step 1 from the designer: the report marks one board node carries (both scopes for a
 * question, the mount for a section) with Add, Edit label and Unmark.
 */
public class ElementMarksDialog extends Dialog {

    public final Grid<Mark> grid = new Grid<>(Mark.class, false);
    final Button add = new Button();
    final Button close = new Button();
    final Paragraph empty = new Paragraph();

    private final ReportMarkService service;
    private final Survey survey;
    private final StructureNode node;
    private final Runnable onChanged;

    /**
     * Builds the dialog for one node and lists its marks, without opening it. Add is disabled for a
     * question that cannot be marked (UC-049 A2).
     *
     * @param service   reads, adds, relabels and removes the marks
     * @param survey    the survey the node belongs to
     * @param node      the board node whose marks are shown
     * @param onChanged called after a mark is added, relabeled or removed
     */
    public ElementMarksDialog(ReportMarkService service, Survey survey, StructureNode node, Runnable onChanged) {
        this.service = service;
        this.survey = survey;
        this.node = node;
        this.onChanged = onChanged;
        add.setText(getTranslation("elementMarksDialog.addMark"));
        close.setText(getTranslation("common.close"));
        empty.setText(ReportMarkService.markable(node)
                ? getTranslation("elementMarksDialog.empty")
                : getTranslation("reportMarkDialog.notMarkable", node.detail()));
        setHeaderTitle(getTranslation("elementMarksDialog.title", node.label()));
        setModal(true);
        setWidth("56rem");

        grid.getElement().setAttribute("data-i18n-content", ""); // labels are authored
        grid.addColumn(m -> ReportMarkService.reportLabel(m.item().reportType)).setHeader(getTranslation("elementMarksDialog.grid.report")).setAutoWidth(true);
        grid.addColumn(Mark::scopeLabel).setHeader(getTranslation("elementMarksDialog.grid.appliesTo")).setAutoWidth(true);
        grid.addColumn(Mark::shownAs).setHeader(getTranslation("elementMarksDialog.grid.label")).setAutoWidth(true);
        grid.addComponentColumn(m -> {
            Button edit = new Button(getTranslation("reportsView.editLabel"), e -> new ReportMarkDialog(service, survey, null, null, m, saved -> changed()).open());
            edit.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            Button unmark = new Button(getTranslation("reportsView.unmark"), e -> confirmUnmark(m));
            unmark.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            return new HorizontalLayout(edit, unmark);
        }).setHeader("").setAutoWidth(true);
        grid.setAllRowsVisible(true);
        add(grid, empty);
        refresh();

        add.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        // UC-049 A2: an HTML or PASSWORD question offers no mark.
        add.setEnabled(ReportMarkService.markable(node));
        add.addClickListener(e -> new ReportMarkDialog(service, survey, null, node, null, saved -> changed()).open());
        close.addClickListener(e -> close());
        getFooter().add(close, add);
    }

    void refresh() {
        List<Mark> rows = service.marksFor(survey.id, node);
        grid.setItems(rows);
        grid.setVisible(!rows.isEmpty());
        empty.setVisible(rows.isEmpty());
    }

    private void confirmUnmark(Mark m) {
        new ConfirmDialog(getTranslation("reportsView.unmark.header"),
                getTranslation("reportsView.unmark.text", node.label(), ReportMarkService.reportLabel(m.item().reportType)),
                getTranslation("reportsView.unmark"), e -> {
            try {
                service.unmark(survey.id, m.item().id);
                Notification.show(getTranslation("reportsView.unmarked", node.label()), 3000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                changed();
            } catch (AuthoringException ae) {
                Notification.show(ae.getMessage(), 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        }, getTranslation("common.cancel"), e -> { }).open();
    }

    private void changed() {
        refresh();
        onChanged.run();
    }
}
