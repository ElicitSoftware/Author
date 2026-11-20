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
import com.vaadin.flow.component.ModalityMode;
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

    final TextField tag = new TextField();
    final ComboBox<String> dimension = new ComboBox<>();
    final TextField namespace = new TextField();
    final Span preview = new Span();
    final Div warning = new Div();
    final Checkbox acknowledge = new Checkbox();
    final Button save = new Button();
    final Button cancel = new Button();

    private final Map<String, HasValidation> fieldsByProperty = Map.of("tag", tag, "dimension", dimension);
    private final List<String> dimensionNames = new ArrayList<>();
    private final Ontology existing;
    private final boolean assigned;

    /**
     * Creates the dialog for creating a reporting tag or editing one.
     *
     * @param service  supplies the dimensions and assignment count, and saves the tag
     * @param survey   the survey the tag belongs to; its name is a new tag's namespace
     * @param existing the tag to edit, or {@code null} to create one
     * @param onSaved  receives the persisted tag
     */
    public TagDialog(ReportingService service, Survey survey, Ontology existing, Consumer<Ontology> onSaved) {
        this.existing = existing;
        boolean creating = existing == null;
        this.assigned = !creating && service.assignmentCount(existing.id) > 0;
        tag.setLabel(getTranslation("tagDialog.tag"));
        dimension.setLabel(getTranslation("tagDialog.dimension"));
        namespace.setLabel(getTranslation("tagDialog.namespace"));
        acknowledge.setLabel(getTranslation("tagDialog.acknowledge"));
        save.setText(getTranslation("common.save"));
        cancel.setText(getTranslation("common.cancel"));
        setHeaderTitle(creating ? getTranslation("tagDialog.title.new") : getTranslation("tagDialog.title.edit"));
        setModality(ModalityMode.STRICT);
        setWidth("36rem");

        tag.setRequiredIndicatorVisible(true);
        tag.setMaxLength(255);
        tag.setHelperText(getTranslation("tagDialog.tag.helper"));
        for (Dimension d : service.dimensions()) {
            if (d.name != null) {
                dimensionNames.add(d.name);
            }
        }
        dimension.setItems(dimensionNames);
        dimension.setAllowCustomValue(true);
        dimension.setClearButtonVisible(true);
        dimension.setPlaceholder(getTranslation("tagDialog.dimension.placeholder"));
        dimension.setHelperText(getTranslation("tagDialog.dimension.helper"));
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
        namespace.setHelperText(getTranslation("tagDialog.namespace.helper"));

        FieldHelp.help(tag, getTranslation("tagDialog.help.tag"));
        FieldHelp.help(dimension, getTranslation("tagDialog.help.dimension"));

        if (!creating) {
            tag.setValue(existing.tag);
            if (existing.dimension != null && existing.dimension.name != null) {
                dimension.setValue(existing.dimension.name);
            }
        }
        preview.addClassName("reporting-preview");
        warning.setText(getTranslation("tagDialog.warning"));
        warning.addClassName("rename-warning");
        warning.getElement().setAttribute("role", "alert");
        warning.setVisible(false);
        acknowledge.setVisible(false);
        tag.addValueChangeListener(e -> refresh());
        dimension.addValueChangeListener(e -> refresh());
        acknowledge.addValueChangeListener(e -> refresh());

        FormLayout form = new FormLayout(tag, dimension);
        Details advanced = new Details(getTranslation("tagDialog.advanced"), namespace);
        add(form, preview, advanced, warning, acknowledge);
        refresh();

        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.addClickListener(e -> {
            if (tag.getValue() == null || tag.getValue().isBlank()) {
                tag.setInvalid(true);
                tag.setErrorMessage(getTranslation("tagDialog.error.tagRequired"));
                return;
            }
            try {
                Ontology saved = creating
                        ? service.createTag(survey.id, tag.getValue(), dimension.getValue())
                        : service.updateTag(survey.id, existing.id, tag.getValue(), dimension.getValue());
                close();
                Notification.show(creating ? getTranslation("tagDialog.created", saved.tag) : getTranslation("tagDialog.updated", saved.tag),
                                3000, Notification.Position.BOTTOM_START)
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
            preview.setText(getTranslation("tagDialog.preview.none"));
        } else {
            preview.setText(getTranslation("tagDialog.preview.text", ReportingNames.dimTable(t, d), ReportingNames.factColumn(t)));
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
