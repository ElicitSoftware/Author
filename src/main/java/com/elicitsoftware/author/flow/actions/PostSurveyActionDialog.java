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
package com.elicitsoftware.author.flow.actions;

import com.elicitsoftware.author.AuthoringException;
import com.elicitsoftware.author.flow.designer.FieldHelp;
import com.elicitsoftware.author.survey.PostSurveyActionService;
import com.elicitsoftware.model.PostSurveyAction;
import com.elicitsoftware.model.Survey;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import java.util.Map;
import java.util.function.Consumer;

/**
 * UC-052 steps 3-4 and 6: add or edit a post-survey action: its name, description, method,
 * address and whether it sends the subject's External ID (A5, BR-009..BR-011).
 */
public class PostSurveyActionDialog extends Dialog {

    final TextField name = new TextField();
    final TextField description = new TextField();
    final Select<String> method = new Select<>();
    final TextField url = new TextField();
    final Checkbox sendsExternalId = new Checkbox();
    final Paragraph externalIdWarning = new Paragraph();
    final Button save = new Button();
    final Button cancel = new Button();

    private final Map<String, HasValidation> fieldsByProperty = Map.of("name", name, "description", description, "method", method, "url", url);

    /**
     * @param existing the action to edit, or {@code null} to add one
     * @param onSaved  receives the persisted action
     */
    public PostSurveyActionDialog(PostSurveyActionService service, Survey survey, PostSurveyAction existing, Consumer<PostSurveyAction> onSaved) {
        boolean adding = existing == null;
        name.setLabel(getTranslation("postSurveyActionDialog.name"));
        description.setLabel(getTranslation("postSurveyActionDialog.description"));
        url.setLabel(getTranslation("postSurveyActionDialog.url"));
        method.setLabel(getTranslation("postSurveyActionDialog.method"));
        method.setItems(PostSurveyActionService.METHODS);
        method.setValue("POST");
        method.setHelperText(getTranslation("postSurveyActionDialog.method.helper"));
        sendsExternalId.setLabel(getTranslation("postSurveyActionDialog.sendsExternalId"));
        externalIdWarning.setText(getTranslation("postSurveyActionDialog.sendsExternalId.warning"));
        externalIdWarning.getStyle().set("color", "var(--vaadin-text-color-secondary, #4a5a6a)");
        externalIdWarning.setVisible(false);
        sendsExternalId.addValueChangeListener(e -> {
            externalIdWarning.setVisible(e.getValue());
            url.setInvalid(false);
        });
        save.setText(getTranslation("common.save"));
        cancel.setText(getTranslation("common.cancel"));
        setHeaderTitle(adding ? getTranslation("postSurveyActionDialog.title.add") : getTranslation("postSurveyActionDialog.title.edit"));
        setModal(true);
        setWidth("40rem");

        name.setRequiredIndicatorVisible(true);
        name.setMaxLength(255);
        name.setWidthFull();
        description.setRequiredIndicatorVisible(true);
        description.setMaxLength(255);
        description.setWidthFull();
        url.setRequiredIndicatorVisible(true);
        url.setMaxLength(255);
        url.setWidthFull();
        url.setHelperText(getTranslation("postSurveyActionDialog.url.helper"));
        FieldHelp.help(name, getTranslation("postSurveyActionDialog.help.name"));
        FieldHelp.help(url, getTranslation("postSurveyActionDialog.help.url"));
        if (!adding) {
            name.setValue(existing.name == null ? "" : existing.name);
            description.setValue(existing.description == null ? "" : existing.description);
            url.setValue(existing.url == null ? "" : existing.url);
            method.setValue(existing.method == null ? "POST" : existing.method);
            sendsExternalId.setValue(existing.sendsExternalId);
        }
        name.addValueChangeListener(e -> name.setInvalid(false));
        description.addValueChangeListener(e -> description.setInvalid(false));
        url.addValueChangeListener(e -> url.setInvalid(false));

        FormLayout form = new FormLayout(name, description, method, url, sendsExternalId, externalIdWarning);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        add(form);

        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.addClickListener(e -> {
            try {
                PostSurveyAction saved = adding
                        ? service.add(survey.id, name.getValue(), description.getValue(), method.getValue(), url.getValue(), sendsExternalId.getValue())
                        : service.update(survey.id, existing.id, name.getValue(), description.getValue(), method.getValue(), url.getValue(),
                                sendsExternalId.getValue());
                close();
                Notification.show(adding ? getTranslation("postSurveyActionDialog.added", saved.name) : getTranslation("postSurveyActionDialog.updated", saved.name),
                                3000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                onSaved.accept(saved);
            } catch (AuthoringException ae) {
                HasValidation field = ae.field() == null ? null : fieldsByProperty.get(ae.field());
                if (field != null) {
                    field.setInvalid(true);
                    field.setErrorMessage(ae.getMessage());
                } else {
                    Notification.show(ae.getMessage(), 5000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            }
        });
        cancel.addClickListener(e -> close());
        getFooter().add(cancel, save);
    }
}
