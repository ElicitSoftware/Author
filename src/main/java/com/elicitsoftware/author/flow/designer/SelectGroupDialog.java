package com.elicitsoftware.author.flow.designer;

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
import com.elicitsoftware.author.survey.ListService;
import com.elicitsoftware.author.survey.ListService.OptionDraft;
import com.elicitsoftware.model.SelectGroup;
import com.elicitsoftware.model.SelectItem;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.ModalityMode;
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

    final TextField name = new TextField();
    final TextField description = new TextField();
    final TextField dataType = new TextField();
    final Div optionRows = new Div();
    final Span optionsError = new Span();
    final Button addOption = new Button(VaadinIcon.PLUS.create());
    final Button save = new Button();
    final Button cancel = new Button();
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
            displayText.setPlaceholder(getTranslation("selectGroupDialog.option.displayText"));
            displayText.setMaxLength(255);
            displayText.setValue(text == null ? "" : text);
            displayText.setAriaLabel(getTranslation("selectGroupDialog.option.displayText.aria"));
            codedValue.setPlaceholder(getTranslation("selectGroupDialog.option.codedValue"));
            codedValue.setMaxLength(255);
            codedValue.setValue(coded == null ? "" : coded);
            codedValue.setAriaLabel(getTranslation("selectGroupDialog.option.codedValue.aria"));
            for (Button b : List.of(up, down, remove)) {
                b.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY_INLINE);
            }
            up.setAriaLabel(getTranslation("selectGroupDialog.option.moveUp"));
            down.setAriaLabel(getTranslation("selectGroupDialog.option.moveDown"));
            remove.setAriaLabel(getTranslation("selectGroupDialog.option.remove"));
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
     * Builds the dialog, filled from {@code existing} and its options when editing. Nothing is
     * saved until Save, which creates or updates the list with its options as one whole.
     *
     * @param service  saves the list and reads its current options
     * @param surveyId the survey the list belongs to
     * @param existing the list to edit, or {@code null} to create one
     * @param onSaved  receives the persisted list
     */
    public SelectGroupDialog(ListService service, int surveyId, SelectGroup existing, Consumer<SelectGroup> onSaved) {
        boolean creating = existing == null;
        setHeaderTitle(creating ? getTranslation("selectGroupDialog.title.new") : getTranslation("selectGroupDialog.title.edit"));
        setModality(ModalityMode.STRICT);
        setWidth("44rem");
        name.setLabel(getTranslation("selectGroupDialog.name"));
        description.setLabel(getTranslation("selectGroupDialog.description"));
        dataType.setLabel(getTranslation("selectGroupDialog.dataType"));
        addOption.setText(getTranslation("selectGroupDialog.addOption"));
        save.setText(getTranslation("common.save"));
        cancel.setText(getTranslation("common.cancel"));

        name.setRequiredIndicatorVisible(true);
        name.setMaxLength(255);
        description.setMaxLength(255);
        dataType.setMaxLength(50);
        dataType.setHelperText(getTranslation("selectGroupDialog.dataType.helper"));
        FieldHelp.help(name, getTranslation("selectGroupDialog.help.name"));
        FieldHelp.help(description, getTranslation("selectGroupDialog.help.description"));
        FieldHelp.help(dataType, getTranslation("selectGroupDialog.help.dataType"));

        binder.forField(name).asRequired(getTranslation("selectGroupDialog.error.nameRequired")).bind(g -> g.name, (g, v) -> g.name = v);
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
            bean.dataType = "Text"; // i18n:ignore stored default of the list's data type, not display text
        }
        binder.readBean(bean);

        optionRows.addClassName("designer-option-rows");
        optionRows.getElement().setAttribute("data-i18n-content", ""); // authored option texts and coded values
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
        Span optionsLabel = new Span(getTranslation("selectGroupDialog.optionsLabel"));
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
                Notification.show(creating ? getTranslation("selectGroupDialog.saved.created", saved.name)
                                : getTranslation("selectGroupDialog.saved.updated", saved.name), 3000, Notification.Position.BOTTOM_START)
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
            Span none = new Span(getTranslation("selectGroupDialog.noOptions"));
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
