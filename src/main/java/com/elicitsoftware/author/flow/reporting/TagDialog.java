package com.elicitsoftware.author.flow.reporting;

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

import com.elicitsoftware.author.AuthoringException;
import com.elicitsoftware.author.flow.designer.FieldHelp;
import com.elicitsoftware.author.survey.ReportingNames;
import com.elicitsoftware.author.survey.ReportingService;
import com.elicitsoftware.model.Dimension;
import com.elicitsoftware.model.Ontology;
import com.elicitsoftware.model.Survey;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.TextField;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * UC-023: create or edit a reporting tag. The dimension is picked from the platform vocabulary or
 * typed in to create one (BR-001); the preview shows the table and column the site's ETL will
 * derive (UC-024). Editing an assigned tag's name or dimension is retroactive (A4), so Save waits
 * for an acknowledgement exactly as a rename does (UC-018).
 */
public class TagDialog extends Dialog {

    final TextField tag = new TextField("Tag");
    final ComboBox<String> dimension = new ComboBox<>("Dimension");
    final TextField namespace = new TextField("Namespace");
    final Span preview = new Span();
    final Div warning = new Div();
    final Checkbox acknowledge = new Checkbox("I understand that answers already recorded at deployed sites are reclassified");
    final Button save = new Button("Save");
    final Button cancel = new Button("Cancel");

    private final Map<String, HasValidation> fieldsByProperty = Map.of("tag", tag, "dimension", dimension);
    private final List<String> dimensionNames = new ArrayList<>();
    private final Ontology existing;
    private final boolean assigned;

    /**
     * @param existing the tag to edit, or {@code null} to create one
     * @param onSaved  receives the persisted tag
     */
    public TagDialog(ReportingService service, Survey survey, Ontology existing, Consumer<Ontology> onSaved) {
        this.existing = existing;
        boolean creating = existing == null;
        this.assigned = !creating && service.assignmentCount(existing.id) > 0;
        setHeaderTitle(creating ? "New reporting tag" : "Edit reporting tag");
        setModal(true);
        setWidth("36rem");

        tag.setRequiredIndicatorVisible(true);
        tag.setMaxLength(255);
        tag.setHelperText("Letters, digits, spaces and hyphens. Becomes a column of the report.");
        for (Dimension d : service.dimensions()) {
            if (d.name != null) {
                dimensionNames.add(d.name);
            }
        }
        dimension.setItems(dimensionNames);
        dimension.setAllowCustomValue(true);
        dimension.setClearButtonVisible(true);
        dimension.setPlaceholder("None: the tag gets its own table");
        dimension.setHelperText("Type a new name to create a dimension. Letters, digits and underscores.");
        dimension.addCustomValueSetListener(e -> {
            String custom = e.getDetail() == null ? "" : e.getDetail().trim();
            if (custom.isEmpty()) {
                return;
            }
            if (dimensionNames.stream().noneMatch(n -> n.equalsIgnoreCase(custom))) {
                dimensionNames.add(custom);
                dimension.setItems(dimensionNames);
            }
            dimension.setValue(dimensionNames.stream().filter(n -> n.equalsIgnoreCase(custom)).findFirst().orElse(custom));
        });
        namespace.setReadOnly(true);
        namespace.setValue(creating ? survey.name : existing.name);
        namespace.setHelperText("Groups this survey's tags; a site keeps one row per namespace and tag");

        FieldHelp.help(tag, "The tag's name. Answers of the tagged elements are reported under a column named after it.");
        FieldHelp.help(dimension, "A shared category such as 'age' or 'cancer'. Tags on the same dimension share one table of values; "
                + "leave it empty to give this tag a table of its own.");

        if (!creating) {
            tag.setValue(existing.tag);
            if (existing.dimension != null && existing.dimension.name != null) {
                dimension.setValue(existing.dimension.name);
            }
        }
        preview.addClassName("reporting-preview");
        warning.setText("This tag is assigned. Changing its name or dimension is retroactive: answers already recorded at "
                + "deployed sites are reclassified, not only answers recorded from now on.");
        warning.addClassName("rename-warning");
        warning.getElement().setAttribute("role", "alert");
        warning.setVisible(false);
        acknowledge.setVisible(false);
        tag.addValueChangeListener(e -> refresh());
        dimension.addValueChangeListener(e -> refresh());
        acknowledge.addValueChangeListener(e -> refresh());

        FormLayout form = new FormLayout(tag, dimension);
        Details advanced = new Details("Advanced", namespace);
        add(form, preview, advanced, warning, acknowledge);
        refresh();

        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.addClickListener(e -> {
            if (tag.getValue() == null || tag.getValue().isBlank()) {
                tag.setInvalid(true);
                tag.setErrorMessage("Tag is required");
                return;
            }
            try {
                Ontology saved = creating
                        ? service.createTag(survey.id, tag.getValue(), dimension.getValue())
                        : service.updateTag(survey.id, existing.id, tag.getValue(), dimension.getValue());
                close();
                Notification.show((creating ? "Created tag '" : "Updated tag '") + saved.tag + "'", 3000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                onSaved.accept(saved);
            } catch (AuthoringException ae) {
                showError(ae);
            }
        });
        cancel.addClickListener(e -> close());
        getFooter().add(cancel, save);
    }

    /** Preview of the derived names (UC-024 BR-001) and the retroactivity guard (UC-023 A4). */
    void refresh() {
        String t = tag.getValue() == null ? "" : tag.getValue().trim();
        String d = dimension.getValue();
        tag.setInvalid(false);
        dimension.setInvalid(false);
        if (t.isEmpty()) {
            preview.setText("Choose a tag to see the table and column it produces.");
        } else {
            preview.setText("Values go to surveyreport." + ReportingNames.dimTable(t, d)
                    + " · report column " + ReportingNames.factColumn(t));
        }
        boolean changed = assigned && (!Objects.equals(existing.tag, t)
                || !Objects.equals(existing.dimension == null ? null : existing.dimension.name, d == null || d.isBlank() ? null : d));
        warning.setVisible(changed);
        acknowledge.setVisible(changed);
        if (!changed) {
            acknowledge.setValue(false);
        }
        save.setEnabled(!changed || Boolean.TRUE.equals(acknowledge.getValue()));
    }

    boolean warningShown() {
        return warning.isVisible();
    }

    private void showError(AuthoringException ae) {
        HasValidation field = ae.field() == null ? null : fieldsByProperty.get(ae.field());
        if (field != null) {
            field.setInvalid(true);
            field.setErrorMessage(ae.getMessage());
        } else {
            Notification.show(ae.getMessage(), 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}
