package com.elicitsoftware.author.flow.designer;

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
import com.elicitsoftware.author.survey.ListService;
import com.elicitsoftware.author.survey.ListService.OptionDraft;
import com.elicitsoftware.model.SelectGroup;
import com.elicitsoftware.model.SelectItem;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * UC-014: create or edit a list of options. The group's fields bind to a working bean; the options
 * are rows of (display text, coded value) with Move up / Move down / Remove, saved as one whole.
 */
public class SelectGroupDialog extends Dialog {

    final TextField name = new TextField("Name");
    final TextField description = new TextField("Description");
    final TextField dataType = new TextField("Data type");
    final Div optionRows = new Div();
    final Span optionsError = new Span();
    final Button addOption = new Button("Add option", VaadinIcon.PLUS.create());
    final Button save = new Button("Save");
    final Button cancel = new Button("Cancel");
    final List<OptionRow> rows = new ArrayList<>();

    private final Binder<SelectGroup> binder = new Binder<>(SelectGroup.class);
    private final Map<String, HasValidation> fieldsByProperty = Map.of("name", name, "description", description, "dataType", dataType);

    /** One editable option. */
    final class OptionRow extends HorizontalLayout {
        final Integer selectItemId;
        final TextField displayText = new TextField();
        final TextField codedValue = new TextField();
        final Button up = new Button(VaadinIcon.ARROW_UP.create());
        final Button down = new Button(VaadinIcon.ARROW_DOWN.create());
        final Button remove = new Button(VaadinIcon.TRASH.create());

        OptionRow(Integer selectItemId, String text, String coded) {
            this.selectItemId = selectItemId;
            addClassName("designer-option-row");
            setAlignItems(Alignment.BASELINE);
            setWidthFull();
            displayText.setPlaceholder("Display text");
            displayText.setMaxLength(255);
            displayText.setValue(text == null ? "" : text);
            displayText.setAriaLabel("Option display text");
            codedValue.setPlaceholder("Coded value (defaults to the text)");
            codedValue.setMaxLength(255);
            codedValue.setValue(coded == null ? "" : coded);
            codedValue.setAriaLabel("Option coded value");
            for (Button b : List.of(up, down, remove)) {
                b.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY_INLINE);
            }
            up.setAriaLabel("Move option up");
            down.setAriaLabel("Move option down");
            remove.setAriaLabel("Remove option");
            remove.addThemeVariants(ButtonVariant.LUMO_ERROR);
            up.addClickListener(e -> move(this, -1));
            down.addClickListener(e -> move(this, 1));
            remove.addClickListener(e -> {
                rows.remove(this);
                renderRows();
            });
            add(displayText, codedValue, up, down, remove);
            expand(displayText, codedValue);
        }

        OptionDraft draft() {
            return new OptionDraft(selectItemId, displayText.getValue(), codedValue.getValue());
        }
    }

    /**
     * @param existing the list to edit, or {@code null} to create one
     * @param onSaved  receives the persisted list
     */
    public SelectGroupDialog(ListService service, int surveyId, SelectGroup existing, Consumer<SelectGroup> onSaved) {
        boolean creating = existing == null;
        setHeaderTitle(creating ? "New list of options" : "Edit list of options");
        setModal(true);
        setWidth("44rem");

        name.setRequiredIndicatorVisible(true);
        name.setMaxLength(255);
        description.setMaxLength(255);
        dataType.setMaxLength(50);
        dataType.setHelperText("Informational; 'Text' unless the site's reporting expects otherwise");
        FieldHelp.help(name, "The list's name, shown when picking options for a question. Unique within the survey.");
        FieldHelp.help(description, "An optional note for authors.");
        FieldHelp.help(dataType, "A label carried with the list; the Survey runtime does not interpret it.");

        binder.forField(name).asRequired("Name is required").bind(g -> g.name, (g, v) -> g.name = v);
        binder.forField(description).bind(g -> g.description, (g, v) -> g.description = v);
        binder.forField(dataType).bind(g -> g.dataType, (g, v) -> g.dataType = v);

        SelectGroup bean = new SelectGroup();
        if (!creating) {
            bean.selectGroupId = existing.selectGroupId;
            bean.surveyId = existing.surveyId;
            bean.name = existing.name;
            bean.description = existing.description;
            bean.dataType = existing.dataType;
            for (SelectItem i : service.options(surveyId, existing.selectGroupId)) {
                rows.add(new OptionRow(i.selectItemId, i.displayText, i.codedValue));
            }
        } else {
            bean.dataType = "Text";
        }
        binder.readBean(bean);

        optionRows.addClassName("designer-option-rows");
        optionsError.addClassName("designer-field-error");
        optionsError.setVisible(false);
        addOption.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        addOption.addClickListener(e -> {
            OptionRow row = new OptionRow(null, "", "");
            rows.add(row);
            renderRows();
            row.displayText.focus();
        });
        renderRows();

        FormLayout form = new FormLayout(name, dataType, description);
        form.setColspan(description, 2);
        Span optionsLabel = new Span("Options, in the order respondents see them");
        optionsLabel.addClassName("designer-palette-label");
        add(form, optionsLabel, optionRows, optionsError, addOption);

        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.addClickListener(e -> {
            try {
                binder.writeBean(bean);
            } catch (ValidationException ve) {
                return;
            }
            optionsError.setVisible(false);
            List<OptionDraft> drafts = rows.stream().map(OptionRow::draft).toList();
            try {
                SelectGroup saved = creating ? service.create(surveyId, bean, drafts) : service.update(surveyId, bean, drafts);
                close();
                Notification.show((creating ? "Created list '" : "Updated list '") + saved.name + "'", 3000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                onSaved.accept(saved);
            } catch (AuthoringException ae) {
                showError(ae);
            }
        });
        cancel.addClickListener(e -> close());
        getFooter().add(cancel, save);
    }

    private void move(OptionRow row, int delta) {
        int i = rows.indexOf(row);
        int j = i + delta;
        if (i < 0 || j < 0 || j >= rows.size()) {
            return;
        }
        rows.remove(i);
        rows.add(j, row);
        renderRows();
    }

    private void renderRows() {
        optionRows.removeAll();
        for (int i = 0; i < rows.size(); i++) {
            OptionRow r = rows.get(i);
            r.up.setEnabled(i > 0);
            r.down.setEnabled(i < rows.size() - 1);
            optionRows.add(r);
        }
        if (rows.isEmpty()) {
            Span none = new Span("No options yet.");
            none.addClassName("designer-empty-hint");
            optionRows.add(none);
        }
    }

    private void showError(AuthoringException ae) {
        if ("options".equals(ae.field())) {
            optionsError.setText(ae.getMessage());
            optionsError.setVisible(true);
            return;
        }
        HasValidation field = ae.field() == null ? null : fieldsByProperty.get(ae.field());
        if (field != null) {
            field.setInvalid(true);
            field.setErrorMessage(ae.getMessage());
        } else {
            Notification.show(ae.getMessage(), 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}
