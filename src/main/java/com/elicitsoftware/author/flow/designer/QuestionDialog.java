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
import com.elicitsoftware.author.definition.ExportValidation;
import com.elicitsoftware.author.survey.TokenSamples;
import com.elicitsoftware.model.Question;
import com.elicitsoftware.model.QuestionType;
import com.elicitsoftware.model.SelectGroup;
import com.elicitsoftware.model.StructuralElement;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.ComboBoxVariant;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import io.quarkus.panache.common.Sort;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    final Button newList = new Button("New list…");
    final TextField mask = new TextField("Input mask");
    final TextField placeholder = new TextField("Placeholder");
    final TextField defaultValue = new TextField("Default value");
    final TextField sample = new TextField("Sample answer");
    final MultiSelectComboBox<String> variant = new MultiSelectComboBox<>("Variant");
    final Button save = new Button("Save");
    final Button cancel = new Button("Cancel");

    /** Vaadin theme-variant names the Survey app honours on a question's input field. */
    static final List<String> VARIANTS = List.of(
            TextFieldVariant.LUMO_SMALL.getVariantName(),
            ComboBoxVariant.LUMO_ALIGN_LEFT.getVariantName(),
            TextFieldVariant.LUMO_ALIGN_CENTER.getVariantName(),
            TextFieldVariant.LUMO_ALIGN_RIGHT.getVariantName(),
            TextFieldVariant.LUMO_HELPER_ABOVE_FIELD.getVariantName(),
            RadioGroupVariant.LUMO_VERTICAL.getVariantName());

    private final Binder<Question> binder = new Binder<>(Question.class);
    private final Div preview = new Div();
    private final Map<String, String> tokenValues;
    private final Map<String, HasValidation> fieldsByProperty;
    private final com.elicitsoftware.author.survey.ListService lists;

    public QuestionDialog(int surveyId, Question existing, QuestionType presetType, Function<Question, Question> persist, Consumer<Question> onSaved) {
        this(surveyId, existing, presetType, persist, onSaved, null);
    }

    /** @param lists when given, a "New list…" shortcut next to the list picker creates one in place (UC-014). */
    public QuestionDialog(int surveyId, Question existing, QuestionType presetType, Function<Question, Question> persist, Consumer<Question> onSaved,
                          com.elicitsoftware.author.survey.ListService lists) {
        this.lists = lists;
        boolean creating = existing == null;
        setHeaderTitle(creating ? "New question" : "Edit question");
        setModal(true);
        setWidth("48rem");

        // What the rules that reach this question — on its step, its section or itself — write into
        // its text, so the preview substitutes {TOKEN} placeholders the way the runtime will (FR-013).
        tokenValues = existing == null ? Map.of() : TokenSamples.load(surveyId).forQuestion(existing.questionId);

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
        sample.setMaxLength(255);
        sample.setHelperText("Previews the texts that Text rules fill with this answer");
        variant.setItems(VARIANTS);
        variant.setHelperText("Vaadin theme variants applied to the field");

        FieldHelp.help(questionType, "The kind of input the respondent uses to answer.");
        FieldHelp.help(shortText, "A short machine-friendly name, shown in the tree and on the board.");
        FieldHelp.help(text, "The question as the respondent sees it. May contain {TOKEN} placeholders that TEXT rules fill in.");
        FieldHelp.help(toolTip, "Help text shown to the respondent when they hover the field.");
        FieldHelp.help(required, "The respondent must answer this before continuing.");
        FieldHelp.help(minValue, "The smallest number the respondent may enter.");
        FieldHelp.help(maxValue, "The largest number the respondent may enter.");
        FieldHelp.help(selectGroup, "The list of choices the respondent picks from.");
        FieldHelp.help(mask, "A pattern that constrains what the respondent can type.");
        FieldHelp.help(placeholder, "Faint example text shown in the empty field.");
        FieldHelp.help(defaultValue, "The value the field starts with.");
        FieldHelp.help(sample, "An example answer, used only in previews. Wherever a Text rule writes this question's answer into another text, "
                + "the preview shows this instead: sample \"Bob\" previews \"Hello {PName}\" as \"Hello Bob\". Respondents never see it.");
        FieldHelp.help(variant, "Visual style variants applied to the field: alignment, size, or layout.");

        List<SelectGroup> groups = new java.util.ArrayList<>(
                SelectGroup.<SelectGroup>list("surveyId = ?1 and effectiveTo = ?2 order by name", surveyId, StructuralElement.SENTINEL));
        selectGroup.setItems(groups);
        selectGroup.setItemLabelGenerator(g -> g.name);
        selectGroup.setClearButtonVisible(true);
        newList.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        newList.addClickListener(e -> new SelectGroupDialog(lists, surveyId, null, created -> {
            groups.add(created);
            groups.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
            selectGroup.setItems(groups);
            selectGroup.setValue(created);
        }).open());

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
        binder.forField(sample).bind(q -> q.sample, (q, v) -> q.sample = v);
        binder.forField(variant).bind(q -> parseVariants(q.variant),
                (q, v) -> q.variant = v == null || v.isEmpty() ? null : String.join(" ", v));
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
            bean.sample = existing.sample;
        } else if (presetType != null) {
            bean.questionType = presetType;
        }
        binder.readBean(bean);
        questionType.addValueChangeListener(e -> updateChoiceFields());
        updateChoiceFields();

        com.vaadin.flow.component.orderedlayout.HorizontalLayout listRow = new com.vaadin.flow.component.orderedlayout.HorizontalLayout(selectGroup, newList);
        listRow.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.END);
        listRow.setWidthFull();
        listRow.expand(selectGroup);
        FormLayout form = new FormLayout(questionType, shortText, text, toolTip, required, listRow,
                minValue, maxValue, validationText, mask, placeholder, defaultValue, variant, sample);
        form.setColspan(text, 2);

        // Live preview of the respondent's field, so the author sees what the type and variants do.
        Div previewBox = new Div(new Span("Preview"), preview);
        previewBox.addClassName("question-preview-box");
        add(form, previewBox);
        questionType.addValueChangeListener(e -> rebuildPreview());
        variant.addValueChangeListener(e -> rebuildPreview());
        shortText.addValueChangeListener(e -> rebuildPreview());
        text.addValueChangeListener(e -> rebuildPreview());
        toolTip.addValueChangeListener(e -> rebuildPreview());
        placeholder.addValueChangeListener(e -> rebuildPreview());
        required.addValueChangeListener(e -> rebuildPreview());
        rebuildPreview();

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

    /** Rebuild the live field preview from the current type, label, placeholder and theme variants. */
    void rebuildPreview() {
        preview.removeAll();
        preview.addClassName("question-preview");
        QuestionType t = questionType.getValue();
        if (t == null) {
            preview.add(new Span("Choose a type to preview the field."));
            return;
        }
        if ("HTML".equalsIgnoreCase(t.name)) {
            String content = text.getValue() == null || text.getValue().isBlank() ? null : substituteTokens(text.getValue());
            preview.add(PreviewFields.html(content));
            return;
        }
        String label = shortText.getValue() != null && !shortText.getValue().isBlank()
                ? shortText.getValue() : (text.getValue() == null ? "" : text.getValue());
        preview.add(PreviewFields.field(t.name, substituteTokens(label), placeholder.getValue(), toolTip.getValue(),
                Boolean.TRUE.equals(required.getValue()), variant.getValue(), List.of()));
    }

    /** Replace {TOKEN} / {TOKEN|default} with what the rules reaching this question write, else the default. */
    private String substituteTokens(String text) {
        return PreviewFields.substituteTokens(text, tokenValues);
    }

    /** The known variant names present in a stored value (space- or comma-separated), for the dropdown. */
    static Set<String> parseVariants(String stored) {
        if (stored == null || stored.isBlank()) {
            return Set.of();
        }
        Set<String> tokens = Arrays.stream(stored.trim().split("[\\s,]+")).collect(Collectors.toSet());
        return VARIANTS.stream().filter(tokens::contains).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** Show only the settings the selected type actually uses (UC-011 BR-002; e.g. HTML has no mask). */
    void updateChoiceFields() {
        QuestionType t = questionType.getValue();
        String name = t == null ? "" : t.name.toUpperCase();
        boolean html = "HTML".equals(name);
        boolean choice = t != null && ExportValidation.CHOICE_TYPES.contains(t.name);
        boolean numeric = "INTEGER".equals(name) || "DOUBLE".equals(name);
        boolean checkboxSingle = "CHECKBOX".equals(name);
        boolean textLike = "TEXT".equals(name) || "TEXTAREA".equals(name) || "EMAIL".equals(name) || "PASSWORD".equals(name);
        boolean datey = "DATE_PICKER".equals(name) || "DATETIME".equals(name);

        selectGroup.setVisible(choice);
        newList.setVisible(choice && lists != null);
        selectGroup.setRequiredIndicatorVisible(choice);
        minValue.setVisible(numeric);
        maxValue.setVisible(numeric);
        mask.setVisible(textLike);
        placeholder.setVisible(textLike || datey || "COMBOBOX".equals(name) || "MULTI_SELECT".equals(name));
        validationText.setVisible(!html && !checkboxSingle);
        defaultValue.setVisible(textLike || numeric || datey);
        toolTip.setVisible(!html);
        required.setVisible(!html);
        variant.setVisible(!html);
        sample.setVisible(!html);
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
