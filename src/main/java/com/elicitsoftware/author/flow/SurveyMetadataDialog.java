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
import com.elicitsoftware.author.survey.LanguageTags;
import com.elicitsoftware.author.survey.SurveyService;
import com.elicitsoftware.model.Survey;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.ModalityMode;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;

import java.util.Map;
import java.util.Objects;
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
    final Button save = new Button(getTranslation("common.save"));
    final Button cancel = new Button(getTranslation("common.cancel"));

    private final Binder<Survey> binder = new Binder<>(Survey.class);
    private final Map<String, com.vaadin.flow.component.HasValidation> fieldsByProperty;
    /** Open while the author decides on a changed base language (UC-004 A4); for tests. */
    ConfirmDialog baseLanguageWarning;

    /**
     * A dialog that creates a survey (UC-003) or edits an existing survey's metadata (UC-004).
     *
     * @param service  the service that creates or updates the survey
     * @param existing the survey to edit, or {@code null} to create one
     * @param onSaved receives the persisted survey
     */
    public SurveyMetadataDialog(SurveyService service, Survey existing, Consumer<Survey> onSaved) {
        boolean creating = existing == null;
        setHeaderTitle(getTranslation(creating ? "surveyMetadataDialog.newSurvey" : "surveyMetadataDialog.editSurvey"));
        setModality(ModalityMode.STRICT);

        name.setRequiredIndicatorVisible(true);
        name.setMaxLength(255);
        title.setRequiredIndicatorVisible(true);
        title.setMaxLength(255);
        description.setMaxLength(2000);
        initialDisplayKey.setMaxLength(255);
        postSurveyUrl.setMaxLength(255);
        surveyKey.setReadOnly(true);
        surveyKey.setVisible(!creating);
        // UC-044. The base language stays a plain field: it is what the questions are written in,
        // and a survey may be authored in a language Elicit has no application text for -- its base
        // text is served whatever the chrome does. It is not asked for on create, where a survey
        // is written in the entity's "en" (UC-003 BR-006). The published set is on neither form;
        // the Translations page manages it (UC-044 BR-007).
        baseLanguage.setMaxLength(35);
        baseLanguage.setPlaceholder("en");
        baseLanguage.setHelperText(getTranslation("surveyMetadataDialog.baseLanguage.helper"));
        baseLanguage.setVisible(!creating);

        binder.forField(name).asRequired(getTranslation("surveyMetadataDialog.nameRequired")).bind(s -> s.name, (s, v) -> s.name = v);
        binder.forField(title).asRequired(getTranslation("surveyMetadataDialog.titleRequired")).bind(s -> s.title, (s, v) -> s.title = v);
        binder.forField(description).bind(s -> s.description, (s, v) -> s.description = v);
        binder.forField(initialDisplayKey).bind(s -> s.initialDisplayKey, (s, v) -> s.initialDisplayKey = v);
        binder.forField(postSurveyUrl).bind(s -> s.postSurveyUrl, (s, v) -> s.postSurveyUrl = v);
        binder.forField(surveyKey).bind(s -> s.surveyKey == null ? "" : s.surveyKey.toString(), (s, v) -> { });
        if (!creating) {
            binder.forField(baseLanguage).bind(s -> s.baseLanguage, (s, v) -> s.baseLanguage = v);
        }
        fieldsByProperty = Map.of("name", name, "title", title, "description", description,
                "initialDisplayKey", initialDisplayKey, "postSurveyUrl", postSurveyUrl,
                "baseLanguage", baseLanguage);

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
        }
        binder.readBean(bean);

        FormLayout form = new FormLayout(name, title, description, initialDisplayKey, postSurveyUrl,
                baseLanguage, surveyKey);
        form.setColspan(description, 2);
        add(form);

        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.addClickListener(e -> {
            try {
                binder.writeBean(bean);
            } catch (ValidationException ve) {
                return;
            }
            if (!creating && baseLanguageChanged(existing.baseLanguage, bean.baseLanguage)) {
                warnBaseLanguageChange(existing.baseLanguage, bean.baseLanguage,
                        () -> persist(service, bean, false, onSaved));
            } else {
                persist(service, bean, creating, onSaved);
            }
        });
        cancel.addClickListener(e -> close());
        getFooter().add(cancel, save);
    }

    private void persist(SurveyService service, Survey bean, boolean creating, Consumer<Survey> onSaved) {
        try {
            Survey saved = creating ? service.create(bean) : service.updateMetadata(bean.id, bean);
            close();
            Notification.show(getTranslation(creating ? "surveyMetadataDialog.created" : "surveyMetadataDialog.updated"), 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            onSaved.accept(saved);
        } catch (AuthoringException ae) {
            showError(ae);
        }
    }

    /**
     * Whether the base language was really changed. A blank field is stored as "en" and a tag is
     * compared in canonical form, so retyping {@code EN} for {@code en} is not a change.
     */
    static boolean baseLanguageChanged(String stored, String entered) {
        return !Objects.equals(comparable(stored), comparable(entered));
    }

    private static String comparable(String tag) {
        String value = tag == null || tag.isBlank() ? "en" : tag.strip();
        String canonical = LanguageTags.canonical(value);
        return canonical != null ? canonical : value;
    }

    /**
     * UC-044 A3 / UC-004 A4: changing the base language translates nothing, and every existing
     * translation was made from text in the old one. Nothing is stored until the author confirms;
     * declining leaves this dialog open with their entries.
     */
    private void warnBaseLanguageChange(String from, String to, Runnable onConfirm) {
        String fromTag = from == null || from.isBlank() ? "en" : from.strip();
        String toTag = to == null || to.isBlank() ? "en" : to.strip();
        baseLanguageWarning = new ConfirmDialog();
        baseLanguageWarning.setHeader(getTranslation("surveyMetadataDialog.baseLanguageChange.header"));
        baseLanguageWarning.setText(getTranslation("surveyMetadataDialog.baseLanguageChange.text", fromTag, toTag));
        baseLanguageWarning.setCancelable(true);
        baseLanguageWarning.setCancelText(getTranslation("common.cancel"));
        baseLanguageWarning.setConfirmText(getTranslation("surveyMetadataDialog.baseLanguageChange.confirm"));
        baseLanguageWarning.addConfirmListener(e -> onConfirm.run());
        baseLanguageWarning.open();
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
