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
import com.elicitsoftware.model.SelectItem;
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
import java.util.HashMap;
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

    final ComboBox<QuestionType> questionType = new ComboBox<>();
    final TextArea text = new TextArea();
    final TextField shortText = new TextField();
    final TextField toolTip = new TextField();
    final Checkbox required = new Checkbox();
    final IntegerField minValue = new IntegerField();
    final IntegerField maxValue = new IntegerField();
    final TextField validationText = new TextField();
    final ComboBox<SelectGroup> selectGroup = new ComboBox<>();
    final Button newList = new Button();
    final TextField mask = new TextField();
    final TextField placeholder = new TextField();
    final TextField defaultValue = new TextField();
    final TextField sample = new TextField();
    final MultiSelectComboBox<String> variant = new MultiSelectComboBox<>();
    final Button save = new Button();
    final Button cancel = new Button();

    /** Vaadin theme-variant names the Survey app honors on a question's input field. */
    static final List<String> VARIANTS = List.of(
            TextFieldVariant.LUMO_SMALL.getVariantName(),
            ComboBoxVariant.LUMO_ALIGN_LEFT.getVariantName(),
            TextFieldVariant.LUMO_ALIGN_CENTER.getVariantName(),
            TextFieldVariant.LUMO_ALIGN_RIGHT.getVariantName(),
            TextFieldVariant.LUMO_HELPER_ABOVE_FIELD.getVariantName(),
            RadioGroupVariant.LUMO_VERTICAL.getVariantName());

    private final Binder<Question> binder = new Binder<>(Question.class);
    private final Div preview = new Div();
    private final int surveyId;
    /** Option texts per select group, read the first time the preview needs them. */
    private final Map<Integer, List<String>> optionTexts = new HashMap<>();
    private final TokenSamples.Values tokenValues;
    private final Map<String, HasValidation> fieldsByProperty;
    private final com.elicitsoftware.author.survey.ListService lists;

    public QuestionDialog(int surveyId, Question existing, QuestionType presetType, Function<Question, Question> persist, Consumer<Question> onSaved) {
        this(surveyId, existing, presetType, persist, onSaved, null);
    }

    /** @param lists when given, a "New list…" shortcut next to the list picker creates one in place (UC-014). */
    public QuestionDialog(int surveyId, Question existing, QuestionType presetType, Function<Question, Question> persist, Consumer<Question> onSaved,
                          com.elicitsoftware.author.survey.ListService lists) {
        this.lists = lists;
        this.surveyId = surveyId;
        boolean creating = existing == null;
        setHeaderTitle(creating ? getTranslation("questionDialog.title.new") : getTranslation("questionDialog.title.edit"));
        setModal(true);
        setWidth("48rem");
        questionType.setLabel(getTranslation("questionDialog.type"));
        text.setLabel(getTranslation("questionDialog.text"));
        shortText.setLabel(getTranslation("questionDialog.shortText"));
        toolTip.setLabel(getTranslation("questionDialog.toolTip"));
        required.setLabel(getTranslation("questionDialog.required"));
        minValue.setLabel(getTranslation("questionDialog.minimum"));
        maxValue.setLabel(getTranslation("questionDialog.maximum"));
        validationText.setLabel(getTranslation("questionDialog.validationText"));
        selectGroup.setLabel(getTranslation("questionDialog.selectGroup"));
        newList.setText(getTranslation("questionDialog.newList"));
        mask.setLabel(getTranslation("questionDialog.mask"));
        placeholder.setLabel(getTranslation("questionDialog.placeholder"));
        defaultValue.setLabel(getTranslation("questionDialog.defaultValue"));
        sample.setLabel(getTranslation("questionDialog.sample"));
        variant.setLabel(getTranslation("questionDialog.variant"));
        save.setText(getTranslation("common.save"));
        cancel.setText(getTranslation("common.cancel"));

        // What the rules that reach this question — on its step, its section or itself — write into
        // its text, so the preview substitutes {<TOKEN>} placeholders the way the runtime will (FR-013).
        tokenValues = existing == null ? TokenSamples.Values.NONE : TokenSamples.load(surveyId).forQuestion(existing.questionId);

        List<QuestionType> types = QuestionType.listAll(Sort.by("name"));
        questionType.setItems(types);
        questionType.setItemLabelGenerator(t -> t.name);
        questionType.setRequiredIndicatorVisible(true);
        text.setRequiredIndicatorVisible(true);
        text.setMaxLength(8000);
        shortText.setMaxLength(255);
        shortText.setHelperText(getTranslation("questionDialog.shortText.helper"));
        toolTip.setMaxLength(255);
        validationText.setMaxLength(255);
        mask.setMaxLength(255);
        placeholder.setMaxLength(255);
        defaultValue.setMaxLength(255);
        sample.setMaxLength(255);
        sample.setHelperText(getTranslation("questionDialog.sample.helper"));
        variant.setItems(VARIANTS);
        variant.setHelperText(getTranslation("questionDialog.variant.helper"));

        FieldHelp.help(questionType, getTranslation("questionDialog.help.type"));
        FieldHelp.help(shortText, getTranslation("questionDialog.help.shortText"));
        FieldHelp.help(text, getTranslation("questionDialog.help.text"));
        FieldHelp.help(toolTip, getTranslation("questionDialog.help.toolTip"));
        FieldHelp.help(required, getTranslation("questionDialog.help.required"));
        FieldHelp.help(minValue, getTranslation("questionDialog.help.minimum"));
        FieldHelp.help(maxValue, getTranslation("questionDialog.help.maximum"));
        FieldHelp.help(selectGroup, getTranslation("questionDialog.help.selectGroup"));
        FieldHelp.help(mask, getTranslation("questionDialog.help.mask"));
        FieldHelp.help(placeholder, getTranslation("questionDialog.help.placeholder"));
        FieldHelp.help(defaultValue, getTranslation("questionDialog.help.defaultValue"));
        FieldHelp.help(sample, getTranslation("questionDialog.help.sample"));
        FieldHelp.help(variant, getTranslation("questionDialog.help.variant"));

        List<SelectGroup> groups = new java.util.ArrayList<>(
                SelectGroup.<SelectGroup>list("surveyId = ?1 and effectiveTo = ?2 order by name", surveyId, StructuralElement.SENTINEL)); // i18n:ignore
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

        binder.forField(questionType).asRequired(getTranslation("questionDialog.error.typeRequired")).bind(q -> q.questionType, (q, v) -> q.questionType = v);
        binder.forField(text).asRequired(getTranslation("questionDialog.error.textRequired")).bind(q -> q.text, (q, v) -> q.text = v);
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
        Div previewBox = new Div(new Span(getTranslation("questionDialog.preview")), preview);
        previewBox.addClassName("question-preview-box");
        add(form, previewBox);
        questionType.addValueChangeListener(e -> rebuildPreview());
        variant.addValueChangeListener(e -> rebuildPreview());
        selectGroup.addValueChangeListener(e -> rebuildPreview());
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
                Notification.show(creating ? getTranslation("questionDialog.saved.created") : getTranslation("questionDialog.saved.updated"), 3000, Notification.Position.BOTTOM_START)
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
        preview.getElement().setAttribute("data-i18n-content", ""); // authored question text, options and samples
        QuestionType t = questionType.getValue();
        if (t == null) {
            preview.add(new Span(getTranslation("questionDialog.preview.chooseType")));
            return;
        }
        if (PreviewFields.isDisplayOnly(t)) {
            String content = text.getValue() == null || text.getValue().isBlank() ? null : substituteTokens(text.getValue());
            preview.add(PreviewFields.html(content));
            return;
        }
        String label = shortText.getValue() != null && !shortText.getValue().isBlank()
                ? shortText.getValue() : (text.getValue() == null ? "" : text.getValue());
        preview.add(PreviewFields.field(t.name, substituteTokens(label), placeholder.getValue(), toolTip.getValue(),
                Boolean.TRUE.equals(required.getValue()), variant.getValue(), previewOptions()));
    }

    /**
     * The chosen list's option texts, so a choice type previews the answers the respondent will see
     * rather than the sample options (UC-011 BR-004). Empty until a list is chosen, and for a list
     * created here it is read once the creation has committed.
     */
    private List<String> previewOptions() {
        SelectGroup group = selectGroup.getValue();
        if (group == null || group.selectGroupId == null) {
            return List.of();
        }
        return optionTexts.computeIfAbsent(group.selectGroupId, id ->
                SelectItem.<SelectItem>list("surveyId = ?1 and selectGroupId = ?2 and effectiveTo = ?3 order by displayOrder", // i18n:ignore
                                surveyId, id, StructuralElement.SENTINEL).stream()
                        .map(i -> i.displayText == null ? "" : i.displayText)
                        .toList());
    }

    /** Replace {<TOKEN>} / {<TOKEN>|default} with what the rules reaching this question write, else the default. */
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
        boolean html = PreviewFields.isDisplayOnly(t);
        boolean choice = t != null && ExportValidation.CHOICE_TYPES.contains(t.name);
        boolean numeric = "INTEGER".equals(name) || "DOUBLE".equals(name);
        boolean checkboxSingle = "CHECKBOX".equals(name);
        boolean textLike = "TEXT".equals(name) || "TEXTAREA".equals(name) || "EMAIL".equals(name) || "PASSWORD".equals(name);
        boolean datey = "DATE_PICKER".equals(name) || "DATE_TIME_PICKER".equals(name) || "TIME_PICKER".equals(name);
        boolean lengths = lengthBounded(name);

        selectGroup.setVisible(choice);
        newList.setVisible(choice && lists != null);
        selectGroup.setRequiredIndicatorVisible(choice);
        minValue.setVisible(numeric || lengths);
        maxValue.setVisible(numeric || lengths);
        // The same two columns mean characters for the text types and values for the numbers, so say which.
        minValue.setLabel(lengths ? getTranslation("questionDialog.minimumLength") : getTranslation("questionDialog.minimum"));
        maxValue.setLabel(lengths ? getTranslation("questionDialog.maximumLength") : getTranslation("questionDialog.maximum"));
        FieldHelp.help(minValue, lengths ? getTranslation("questionDialog.help.minimumLength") : getTranslation("questionDialog.help.minimum"));
        FieldHelp.help(maxValue, lengths ? getTranslation("questionDialog.help.maximumLength") : getTranslation("questionDialog.help.maximum"));
        mask.setVisible(textLike);
        placeholder.setVisible(textLike || datey || "COMBOBOX".equals(name) || "MULTI_SELECT".equals(name));
        validationText.setVisible(!html && !checkboxSingle);
        defaultValue.setVisible(textLike || numeric || datey);
        toolTip.setVisible(!html);
        required.setVisible(!html);
        variant.setVisible(!html);
        sample.setVisible(!html);
    }

    /**
     * Types whose minimum and maximum are character counts rather than numeric bounds (BR-004): Survey's
     * TEXT, TEXTAREA and PASSWORD fields validate the answer's length against them. EMAIL is not one —
     * its field reads the pair only as a switch for the address-pattern check and ignores the values —
     * and the date and time types encode theirs as epoch days, HHMM and nanoseconds, which is no
     * representation to put in front of an author.
     */
    static boolean lengthBounded(String typeName) {
        String name = typeName == null ? "" : typeName.toUpperCase();
        return "TEXT".equals(name) || "TEXTAREA".equals(name) || "PASSWORD".equals(name);
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
