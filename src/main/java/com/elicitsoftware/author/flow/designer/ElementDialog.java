package com.elicitsoftware.author.flow.designer;

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
import com.elicitsoftware.model.Section;
import com.elicitsoftware.model.Step;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.ModalityMode;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;

import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Name / dimension name / description editor shared by steps and sections. Creating takes the
 * persist function the caller decides (position comes from the drop, not the dialog); editing
 * implements UC-018: a changed name shows the retroactivity warning and Save waits for an
 * explicit acknowledgement (A2: no warning when only the description changes).
 *
 * @param <T> {@link Step} or {@link Section}
 */
public class ElementDialog<T> extends Dialog {

    /** Translation keys of the element kind words ("step" / "section") used inside sentences. */
    static final String KIND_STEP = "elementDialog.kind.step";
    static final String KIND_SECTION = "elementDialog.kind.section";

    final TextField name = new TextField();
    final TextField dimensionName = new TextField();
    final TextArea description = new TextArea();
    final Div renameWarning = new Div();
    final Checkbox acknowledge = new Checkbox();
    final Button save = new Button();
    final Button cancel = new Button();

    private final Binder<T> binder = new Binder<>();
    private final Map<String, HasValidation> fieldsByProperty = Map.of("name", name, "dimensionName", dimensionName, "description", description);
    private final String originalName;

    private ElementDialog(String kindKey, T bean, String originalName,
                          Function<T, String> getName, BiConsumer<T, String> setName,
                          Function<T, String> getDimension, BiConsumer<T, String> setDimension,
                          Function<T, String> getDescription, BiConsumer<T, String> setDescription,
                          Function<T, T> persist, Consumer<T> onSaved) {
        boolean creating = originalName == null;
        this.originalName = originalName;
        String what = getTranslation(kindKey);
        setHeaderTitle(creating ? getTranslation("elementDialog.title.new", what) : getTranslation("elementDialog.title.edit", what));
        setModality(ModalityMode.STRICT);
        name.setLabel(getTranslation("elementDialog.name"));
        dimensionName.setLabel(getTranslation("elementDialog.dimensionName"));
        description.setLabel(getTranslation("elementDialog.description"));
        acknowledge.setLabel(getTranslation("elementDialog.acknowledge"));
        save.setText(getTranslation("common.save"));
        cancel.setText(getTranslation("common.cancel"));

        name.setRequiredIndicatorVisible(true);
        name.setMaxLength(255);
        dimensionName.setMaxLength(255);
        dimensionName.setHelperText(getTranslation("elementDialog.dimensionName.helper"));
        description.setMaxLength(2000);

        FieldHelp.help(name, getTranslation("elementDialog.help.name", what));
        FieldHelp.help(dimensionName, getTranslation("elementDialog.help.dimensionName", what));
        FieldHelp.help(description, getTranslation("elementDialog.help.description"));

        binder.forField(name).asRequired(getTranslation("elementDialog.error.nameRequired")).bind(getName::apply, setName::accept);
        binder.forField(dimensionName).bind(getDimension::apply, setDimension::accept);
        binder.forField(description).bind(getDescription::apply, setDescription::accept);
        binder.readBean(bean);

        renameWarning.setText(getTranslation("elementDialog.renameWarning"));
        renameWarning.addClassName("rename-warning");
        renameWarning.getElement().setAttribute("role", "alert");
        renameWarning.setVisible(false);
        acknowledge.setVisible(false);
        name.addValueChangeListener(e -> updateRenameGuard());
        acknowledge.addValueChangeListener(e -> updateRenameGuard());

        FormLayout form = new FormLayout(name, dimensionName, description);
        form.setColspan(description, 2);
        add(form, renameWarning, acknowledge);
        if (creating) {
            add(new Paragraph(getTranslation("elementDialog.placed", what)));
        }

        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.addClickListener(e -> {
            try {
                binder.writeBean(bean);
            } catch (ValidationException ve) {
                return;
            }
            try {
                T saved = persist.apply(bean);
                close();
                Notification.show(creating ? getTranslation("elementDialog.saved.created", what, getName.apply(saved))
                                : getTranslation("elementDialog.saved.updated", what, getName.apply(saved)),
                        3000, Notification.Position.BOTTOM_START).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                onSaved.accept(saved);
            } catch (AuthoringException ae) {
                showError(ae);
            }
        });
        cancel.addClickListener(e -> close());
        getFooter().add(cancel, save);
    }

    /** UC-018 steps 4-5: warning + acknowledgement whenever the name differs from the stored one. */
    void updateRenameGuard() {
        boolean renamed = originalName != null && !Objects.equals(originalName, name.getValue() == null ? "" : name.getValue().trim());
        renameWarning.setVisible(renamed);
        acknowledge.setVisible(renamed);
        if (!renamed) {
            acknowledge.setValue(false);
        }
        save.setEnabled(!renamed || Boolean.TRUE.equals(acknowledge.getValue()));
    }

    boolean renameWarningShown() {
        return renameWarning.isVisible();
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

    /** A step dialog; {@code existing} null creates. */
    public static ElementDialog<Step> step(Step existing, Function<Step, Step> persist, Consumer<Step> onSaved) {
        Step bean = new Step();
        if (existing != null) {
            bean.stepId = existing.stepId;
            bean.surveyId = existing.surveyId;
            bean.name = existing.name;
            bean.dimensionName = existing.dimensionName;
            bean.description = existing.description;
        }
        return new ElementDialog<>(KIND_STEP, bean, existing == null ? null : existing.name,
                s -> s.name, (s, v) -> s.name = v,
                s -> s.dimensionName, (s, v) -> s.dimensionName = v,
                s -> s.description, (s, v) -> s.description = v,
                persist, onSaved);
    }

    /** A section dialog; {@code existing} null creates. */
    public static ElementDialog<Section> section(Section existing, Function<Section, Section> persist, Consumer<Section> onSaved) {
        Section bean = new Section();
        if (existing != null) {
            bean.sectionId = existing.sectionId;
            bean.surveyId = existing.surveyId;
            bean.name = existing.name;
            bean.dimensionName = existing.dimensionName;
            bean.description = existing.description;
        }
        return new ElementDialog<>(KIND_SECTION, bean, existing == null ? null : existing.name,
                s -> s.name, (s, v) -> s.name = v,
                s -> s.dimensionName, (s, v) -> s.dimensionName = v,
                s -> s.description, (s, v) -> s.description = v,
                persist, onSaved);
    }
}
