package com.elicitsoftware.author.flow;

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
import com.elicitsoftware.author.survey.SurveyService;
import com.elicitsoftware.model.Survey;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;

import java.util.Map;
import java.util.function.Consumer;

/** Create (UC-003) or edit (UC-004) a survey's metadata. */
public class SurveyMetadataDialog extends Dialog {

    final TextField name = new TextField(getTranslation("surveyMetadataDialog.name"));
    final TextField title = new TextField(getTranslation("surveyMetadataDialog.title"));
    final TextArea description = new TextArea(getTranslation("surveyMetadataDialog.description"));
    final TextField initialDisplayKey = new TextField(getTranslation("surveyMetadataDialog.initialDisplayKey"));
    final TextField postSurveyUrl = new TextField(getTranslation("surveyMetadataDialog.postSurveyUrl"));
    final TextField surveyKey = new TextField(getTranslation("surveyMetadataDialog.surveyKey"));
    final TextField baseLanguage = new TextField(getTranslation("surveyMetadataDialog.baseLanguage"));
    final TextField contentLanguages = new TextField(getTranslation("surveyMetadataDialog.contentLanguages"));
    final Button save = new Button(getTranslation("common.save"));
    final Button cancel = new Button(getTranslation("common.cancel"));

    private final Binder<Survey> binder = new Binder<>(Survey.class);
    private final Map<String, com.vaadin.flow.component.HasValidation> fieldsByProperty;

    /**
     * @param existing the survey to edit, or {@code null} to create one
     * @param onSaved receives the persisted survey
     */
    public SurveyMetadataDialog(SurveyService service, Survey existing, Consumer<Survey> onSaved) {
        boolean creating = existing == null;
        setHeaderTitle(getTranslation(creating ? "surveyMetadataDialog.newSurvey" : "surveyMetadataDialog.editSurvey"));
        setModal(true);

        name.setRequiredIndicatorVisible(true);
        name.setMaxLength(255);
        title.setRequiredIndicatorVisible(true);
        title.setMaxLength(255);
        description.setMaxLength(2000);
        initialDisplayKey.setMaxLength(255);
        postSurveyUrl.setMaxLength(255);
        surveyKey.setReadOnly(true);
        surveyKey.setVisible(!creating);
        // UC-044. Two plain fields rather than a picker: a content language is any BCP-47 tag, and
        // which of them a site actually serves is decided there, by what its chrome mount carries.
        baseLanguage.setMaxLength(35);
        baseLanguage.setPlaceholder("en");
        baseLanguage.setHelperText(getTranslation("surveyMetadataDialog.baseLanguage.helper"));
        contentLanguages.setMaxLength(255);
        contentLanguages.setPlaceholder("es-419,ar");
        contentLanguages.setHelperText(getTranslation("surveyMetadataDialog.contentLanguages.helper"));

        binder.forField(name).asRequired(getTranslation("surveyMetadataDialog.nameRequired")).bind(s -> s.name, (s, v) -> s.name = v);
        binder.forField(title).asRequired(getTranslation("surveyMetadataDialog.titleRequired")).bind(s -> s.title, (s, v) -> s.title = v);
        binder.forField(description).bind(s -> s.description, (s, v) -> s.description = v);
        binder.forField(initialDisplayKey).bind(s -> s.initialDisplayKey, (s, v) -> s.initialDisplayKey = v);
        binder.forField(postSurveyUrl).bind(s -> s.postSurveyUrl, (s, v) -> s.postSurveyUrl = v);
        binder.forField(surveyKey).bind(s -> s.surveyKey == null ? "" : s.surveyKey.toString(), (s, v) -> { });
        binder.forField(baseLanguage).bind(s -> s.baseLanguage, (s, v) -> s.baseLanguage = v);
        binder.forField(contentLanguages).bind(s -> s.contentLanguages, (s, v) -> s.contentLanguages = v);
        // Both language fields are here so that a refused tag lands on the field it belongs to
        // rather than in a notification across the middle of the screen (UC-044).
        fieldsByProperty = Map.of("name", name, "title", title, "description", description,
                "initialDisplayKey", initialDisplayKey, "postSurveyUrl", postSurveyUrl,
                "baseLanguage", baseLanguage, "contentLanguages", contentLanguages);

        Survey bean = new Survey();
        if (!creating) {
            bean.id = existing.id;
            bean.surveyKey = existing.surveyKey;
            bean.name = existing.name;
            bean.title = existing.title;
            bean.description = existing.description;
            bean.initialDisplayKey = existing.initialDisplayKey;
            bean.postSurveyUrl = existing.postSurveyUrl;
            bean.baseLanguage = existing.baseLanguage;
            bean.contentLanguages = existing.contentLanguages;
        }
        binder.readBean(bean);

        FormLayout form = new FormLayout(name, title, description, initialDisplayKey, postSurveyUrl,
                baseLanguage, contentLanguages, surveyKey);
        form.setColspan(description, 2);
        add(form);

        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.addClickListener(e -> {
            try {
                binder.writeBean(bean);
            } catch (ValidationException ve) {
                return;
            }
            try {
                Survey saved = creating ? service.create(bean) : service.updateMetadata(bean.id, bean);
                close();
                Notification.show(getTranslation(creating ? "surveyMetadataDialog.created" : "surveyMetadataDialog.updated"), 3000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                onSaved.accept(saved);
            } catch (AuthoringException ae) {
                showError(ae);
            }
        });
        cancel.addClickListener(e -> close());
        getFooter().add(cancel, save);
    }

    private void showError(AuthoringException ae) {
        com.vaadin.flow.component.HasValidation field = ae.field() == null ? null : fieldsByProperty.get(ae.field());
        if (field != null) {
            field.setInvalid(true);
            field.setErrorMessage(ae.getMessage());
        } else {
            Notification.show(ae.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}
