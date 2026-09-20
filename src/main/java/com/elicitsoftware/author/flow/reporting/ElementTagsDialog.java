package com.elicitsoftware.author.flow.reporting;

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

import com.elicitsoftware.author.AuthoringException;
import com.elicitsoftware.author.survey.ReportingNames;
import com.elicitsoftware.author.survey.ReportingService;
import com.elicitsoftware.author.survey.ReportingService.Assignment;
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
 * UC-021 step 1 from the designer: the reporting tags one board node carries (both scopes for a
 * question, the mount for a section) with Add, Edit and Remove.
 */
public class ElementTagsDialog extends Dialog {

    public final Grid<Assignment> grid = new Grid<>(Assignment.class, false);
    final Button add = new Button("Add tag…");
    final Button close = new Button("Close");
    final Paragraph empty = new Paragraph("No reporting tags yet. Without a tag, answers to this element are not reported.");

    private final ReportingService service;
    private final Survey survey;
    private final StructureNode node;
    private final Runnable onChanged;

    public ElementTagsDialog(ReportingService service, Survey survey, StructureNode node, Runnable onChanged) {
        this.service = service;
        this.survey = survey;
        this.node = node;
        this.onChanged = onChanged;
        setHeaderTitle("Reporting tags — " + node.label());
        setModal(true);
        setWidth("56rem");

        grid.addColumn(a -> a.metadata().ontology == null ? "" : a.metadata().ontology.tag).setHeader("Tag").setAutoWidth(true);
        grid.addColumn(Assignment::scopeLabel).setHeader("Applies to").setAutoWidth(true);
        grid.addColumn(Assignment::valueLabel).setHeader("Value").setAutoWidth(true);
        grid.addColumn(a -> a.metadata().ontology == null ? "" : ReportingNames.factColumn(a.metadata().ontology.tag))
                .setHeader("Report column").setAutoWidth(true);
        grid.addComponentColumn(a -> {
            Button edit = new Button("Edit…", e -> edit(a));
            edit.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            Button remove = new Button("Remove", e -> confirmRemove(a));
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
        new ConfirmDialog("Remove reporting tag", "Remove tag '" + tag + "' from " + node.label() + "? Answers already recorded at "
                + "deployed sites stop being classified by it once the survey is exported and applied.", "Remove", e -> {
            try {
                service.unassign(survey.id, a.metadata().id);
                Notification.show("Removed tag '" + tag + "'", 3000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                changed();
            } catch (AuthoringException ae) {
                Notification.show(ae.getMessage(), 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        }, "Cancel", e -> { }).open();
    }

    private void changed() {
        refresh();
        onChanged.run();
    }
}
