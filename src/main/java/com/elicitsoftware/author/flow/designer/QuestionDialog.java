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
import com.elicitsoftware.author.definition.ExportValidation;
import com.elicitsoftware.model.Question;
import com.elicitsoftware.model.QuestionType;
import com.elicitsoftware.model.SelectGroup;
import com.elicitsoftware.model.StructuralElement;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import io.quarkus.panache.common.Sort;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * UC-011: a question's type, text and validation. The select group is required for choice types
 * (BR-002) and hidden otherwise. Creating takes the persist function the caller decides.
 */
public class QuestionDialog extends Dialog {

    final ComboBox<QuestionType> questionType = new ComboBox<>("Type");
    final TextArea text = new TextArea("Question text");
    final TextField shortText = new TextField("Short text");
    final TextField toolTip = new TextField("Tooltip");
    final Checkbox required = new Checkbox("Required");
    final IntegerField minValue = new IntegerField("Minimum");
    final IntegerField maxValue = new IntegerField("Maximum");
    final TextField validationText = new TextField("Validation message");
    final ComboBox<SelectGroup> selectGroup = new ComboBox<>("List of options");
    final TextField mask = new TextField("Input mask");
    final TextField placeholder = new TextField("Placeholder");
    final TextField defaultValue = new TextField("Default value");
    final TextField variant = new TextField("Variant");
    final Button save = new Button("Save");
    final Button cancel = new Button("Cancel");

    private final Binder<Question> binder = new Binder<>(Question.class);
    private final Map<String, HasValidation> fieldsByProperty;

    public QuestionDialog(int surveyId, Question existing, QuestionType presetType, Function<Question, Question> persist, Consumer<Question> onSaved) {
        boolean creating = existing == null;
        setHeaderTitle(creating ? "New question" : "Edit question");
        setModal(true);
        setWidth("48rem");

        List<QuestionType> types = QuestionType.listAll(Sort.by("name"));
        questionType.setItems(types);
        questionType.setItemLabelGenerator(t -> t.name);
        questionType.setRequiredIndicatorVisible(true);
        text.setRequiredIndicatorVisible(true);
        text.setMaxLength(8000);
        shortText.setMaxLength(255);
        shortText.setHelperText("Shown in the tree and on the board");
        toolTip.setMaxLength(255);
        validationText.setMaxLength(255);
        mask.setMaxLength(255);
        placeholder.setMaxLength(255);
        defaultValue.setMaxLength(255);
        variant.setMaxLength(255);

        List<SelectGroup> groups = SelectGroup.list("surveyId = ?1 and effectiveTo = ?2 order by name", surveyId, StructuralElement.SENTINEL);
        selectGroup.setItems(groups);
        selectGroup.setItemLabelGenerator(g -> g.name);
        selectGroup.setClearButtonVisible(true);

        binder.forField(questionType).asRequired("Choose a question type").bind(q -> q.questionType, (q, v) -> q.questionType = v);
        binder.forField(text).asRequired("A question needs its text").bind(q -> q.text, (q, v) -> q.text = v);
        binder.forField(shortText).bind(q -> q.shortText, (q, v) -> q.shortText = v);
        binder.forField(toolTip).bind(q -> q.toolTip, (q, v) -> q.toolTip = v);
        binder.forField(required).bind(q -> q.required, (q, v) -> q.required = Boolean.TRUE.equals(v));
        binder.forField(minValue).bind(q -> q.minValue, (q, v) -> q.minValue = v);
        binder.forField(maxValue).bind(q -> q.maxValue, (q, v) -> q.maxValue = v);
        binder.forField(validationText).bind(q -> q.validationText, (q, v) -> q.validationText = v);
        binder.forField(selectGroup).bind(
                q -> q.selectGroupId == null ? null : groups.stream().filter(g -> g.selectGroupId.equals(q.selectGroupId)).findFirst().orElse(null),
                (q, v) -> q.selectGroupId = v == null ? null : v.selectGroupId);
        binder.forField(mask).bind(q -> q.mask, (q, v) -> q.mask = v);
        binder.forField(placeholder).bind(q -> q.placeholder, (q, v) -> q.placeholder = v);
        binder.forField(defaultValue).bind(q -> q.defaultValue, (q, v) -> q.defaultValue = v);
        binder.forField(variant).bind(q -> q.variant, (q, v) -> q.variant = v);
        fieldsByProperty = Map.of("questionType", questionType, "text", text, "shortText", shortText, "minValue", minValue,
                "maxValue", maxValue, "selectGroupId", selectGroup);

        Question bean = new Question();
        if (!creating) {
            bean.questionId = existing.questionId;
            bean.surveyId = existing.surveyId;
            bean.questionType = existing.questionType;
            bean.text = existing.text;
            bean.shortText = existing.shortText;
            bean.toolTip = existing.toolTip;
            bean.required = existing.required;
            bean.minValue = existing.minValue;
            bean.maxValue = existing.maxValue;
            bean.validationText = existing.validationText;
            bean.selectGroupId = existing.selectGroupId;
            bean.mask = existing.mask;
            bean.placeholder = existing.placeholder;
            bean.defaultValue = existing.defaultValue;
            bean.variant = existing.variant;
        } else if (presetType != null) {
            bean.questionType = presetType;
        }
        binder.readBean(bean);
        questionType.addValueChangeListener(e -> updateChoiceFields());
        updateChoiceFields();

        FormLayout form = new FormLayout(questionType, shortText, text, toolTip, required, selectGroup,
                minValue, maxValue, validationText, mask, placeholder, defaultValue, variant);
        form.setColspan(text, 2);
        add(form);

        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.addClickListener(e -> {
            try {
                binder.writeBean(bean);
            } catch (ValidationException ve) {
                return;
            }
            try {
                Question saved = persist.apply(bean);
                close();
                Notification.show(creating ? "Question created" : "Question updated", 3000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                onSaved.accept(saved);
            } catch (AuthoringException ae) {
                showError(ae);
            }
        });
        cancel.addClickListener(e -> close());
        getFooter().add(cancel, save);
    }

    /** UC-011 BR-002: choice types need a list; other types never show one. */
    void updateChoiceFields() {
        QuestionType t = questionType.getValue();
        boolean choice = t != null && ExportValidation.CHOICE_TYPES.contains(t.name);
        selectGroup.setVisible(choice);
        selectGroup.setRequiredIndicatorVisible(choice);
        boolean numeric = t != null && ("INTEGER".equals(t.name) || "DOUBLE".equals(t.name));
        minValue.setVisible(numeric);
        maxValue.setVisible(numeric);
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
