package com.elicitsoftware.author.flow.reporting;

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

import com.elicitsoftware.author.AuthoringException;
import com.elicitsoftware.author.survey.ReportingNames;
import com.elicitsoftware.author.survey.ReportingService;
import com.elicitsoftware.author.survey.ReportingService.Assignment;
import com.elicitsoftware.author.survey.StructureNode;
import com.elicitsoftware.model.Survey;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.ModalityMode;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import java.util.List;

/**
 * UC-021 step 1 from the designer: the reporting tags one board node carries (both scopes for a
 * question, the mount for a section) with Add, Edit and Remove.
 */
public class ElementTagsDialog extends Dialog {

    public final Grid<Assignment> grid = new Grid<>(Assignment.class, false);
    final Button add = new Button();
    final Button close = new Button();
    final Paragraph empty = new Paragraph();

    private final ReportingService service;
    private final Survey survey;
    private final StructureNode node;
    private final Runnable onChanged;

    /**
     * Builds the dialog and lists the node's tag assignments; it is not opened here.
     *
     * @param service   reads and changes the assignments
     * @param survey    the survey the node belongs to
     * @param node      the board node whose tags are shown
     * @param onChanged run after every add, edit or removal
     */
    public ElementTagsDialog(ReportingService service, Survey survey, StructureNode node, Runnable onChanged) {
        this.service = service;
        this.survey = survey;
        this.node = node;
        this.onChanged = onChanged;
        add.setText(getTranslation("elementTagsDialog.addTag"));
        close.setText(getTranslation("common.close"));
        empty.setText(getTranslation("elementTagsDialog.empty"));
        setHeaderTitle(getTranslation("elementTagsDialog.title", node.label()));
        setModality(ModalityMode.STRICT);
        setWidth("56rem");

        grid.getElement().setAttribute("data-i18n-content", ""); // tag names and values are authored
        grid.addColumn(a -> a.metadata().ontology == null ? "" : a.metadata().ontology.tag).setHeader(getTranslation("elementTagsDialog.grid.tag")).setAutoWidth(true);
        grid.addColumn(Assignment::scopeLabel).setHeader(getTranslation("elementTagsDialog.grid.appliesTo")).setAutoWidth(true);
        grid.addColumn(Assignment::valueLabel).setHeader(getTranslation("elementTagsDialog.grid.value")).setAutoWidth(true);
        grid.addColumn(a -> a.metadata().ontology == null ? "" : ReportingNames.factColumn(a.metadata().ontology.tag))
                .setHeader(getTranslation("elementTagsDialog.grid.reportColumn")).setAutoWidth(true);
        grid.addComponentColumn(a -> {
            Button edit = new Button(getTranslation("elementTagsDialog.edit"), e -> edit(a));
            edit.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            Button remove = new Button(getTranslation("common.remove"), e -> confirmRemove(a));
            remove.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            return new HorizontalLayout(edit, remove);
        }).setHeader("").setAutoWidth(true);
        grid.setAllRowsVisible(true);
        add(grid, empty);
        refresh();

        add.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        add.addClickListener(e -> new TagAssignmentDialog(service, survey, null, node, null, saved -> changed()).open());
        close.addClickListener(e -> close());
        getFooter().add(close, add);
    }

    void refresh() {
        List<Assignment> rows = service.assignmentsFor(survey.id, node);
        grid.setItems(rows);
        grid.setVisible(!rows.isEmpty());
        empty.setVisible(rows.isEmpty());
    }

    private void edit(Assignment a) {
        new TagAssignmentDialog(service, survey, null, null, a, saved -> changed()).open();
    }

    private void confirmRemove(Assignment a) {
        String tag = a.metadata().ontology == null ? "?" : a.metadata().ontology.tag;
        new ConfirmDialog(getTranslation("elementTagsDialog.removeAssignment.header"),
                getTranslation("elementTagsDialog.removeAssignment.text", tag, node.label()), getTranslation("common.remove"), e -> {
            try {
                service.unassign(survey.id, a.metadata().id);
                Notification.show(getTranslation("elementTagsDialog.removedTag", tag), 3000, Notification.Position.BOTTOM_START)
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
