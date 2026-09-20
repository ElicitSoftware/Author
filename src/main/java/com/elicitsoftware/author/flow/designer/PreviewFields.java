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

import com.elicitsoftware.model.Relationship;
import com.elicitsoftware.model.StructuralElement;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasLabel;
import com.vaadin.flow.component.HasPlaceholder;
import com.vaadin.flow.component.HasValueAndElement;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.shared.HasTooltip;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds the respondent's field for a question the way the Survey app's {@code Elicit*} input
 * wrappers do: the question text as the label, tooltip, placeholder, required indicator and theme
 * variants, choice types fed with their list's options, HTML rendered as HTML. Shared by the
 * question dialog's live preview (UC-011 BR-004) and the designer's section preview.
 */
final class PreviewFields {

    private static final Pattern TOKEN = Pattern.compile("\\{([^{}|]+)(?:\\|([^{}]*))?}");

    /** The options a choice field shows when the question has no list yet. */
    static final List<String> SAMPLE_OPTIONS = List.of("Option A", "Option B", "Option C");

    private PreviewFields() { }

    /** Token -> sample answer, from the survey's TEXT rules (FR-013), so a preview can show substitution. */
    static Map<String, String> tokenSamples(int surveyId) {
        Map<String, String> samples = new HashMap<>();
        for (Relationship r : Relationship.<Relationship>list("surveyId = ?1 and effectiveTo = ?2", surveyId, StructuralElement.SENTINEL)) {
            if (r.actionType != null && "TEXT".equals(r.actionType.name)
                    && r.token != null && !r.token.isBlank() && r.sample != null && !r.sample.isBlank()) {
                samples.put(r.token.trim(), r.sample);
            }
        }
        return samples;
    }

    /** Replace {TOKEN} / {TOKEN|default} with a rule's sample answer, then its default, else leave it. */
    static String substituteTokens(String text, Map<String, String> samples) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        Matcher m = TOKEN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String tok = m.group(1).trim();
            String def = m.group(2);
            String replacement = samples.getOrDefault(tok, def != null ? def : m.group());
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /** An HTML question has no input; the respondent sees its text rendered as HTML. */
    static Component html(String content) {
        String body = content == null || content.isBlank() ? "<em>HTML content preview</em>" : content;
        try {
            return new Html("<div class=\"question-preview-html\">" + body + "</div>");
        } catch (RuntimeException invalid) {
            return new Span("Enter valid HTML to preview it.");
        }
    }

    /**
     * The respondent's input for a non-HTML question.
     *
     * @param typeName    the question type name (TEXT, COMBOBOX, …)
     * @param label       the label as the respondent reads it (tokens already substituted)
     * @param options     the choice texts for choice types; empty falls back to sample options
     * @param variants    Vaadin theme variant names to apply, space- or comma-separated, may be null
     */
    static Component field(String typeName, String label, String placeholder, String toolTip, boolean required,
                           Collection<String> variants, List<String> options) {
        Component field = input(typeName, options == null || options.isEmpty() ? SAMPLE_OPTIONS : options);
        if (field instanceof HasLabel hl) {
            hl.setLabel(label);
        }
        if (field instanceof HasPlaceholder hp && placeholder != null && !placeholder.isBlank()) {
            hp.setPlaceholder(placeholder);
        }
        if (field instanceof HasValueAndElement<?, ?> hv) {
            hv.setRequiredIndicatorVisible(required);
        }
        if (field instanceof HasTooltip ht && toolTip != null && !toolTip.isBlank()) {
            ht.setTooltipText(toolTip);
        }
        if (variants != null) {
            for (String v : variants) {
                if (v != null && !v.isBlank()) {
                    field.getElement().getThemeList().add(v);
                }
            }
        }
        return field;
    }

    private static Component input(String typeName, List<String> options) {
        String name = typeName == null ? "" : typeName.toUpperCase();
        return switch (name) {
            case "TEXTAREA" -> new TextArea();
            case "INTEGER" -> new IntegerField();
            case "DOUBLE" -> new NumberField();
            case "EMAIL" -> new EmailField();
            case "PASSWORD" -> new PasswordField();
            case "DATE_PICKER" -> new DatePicker();
            case "DATETIME" -> new DateTimePicker();
            case "CHECKBOX" -> new Checkbox();
            case "RADIO" -> {
                RadioButtonGroup<String> group = new RadioButtonGroup<>();
                group.setItems(options);
                yield group;
            }
            case "CHECKBOX_GROUP" -> {
                CheckboxGroup<String> group = new CheckboxGroup<>();
                group.setItems(options);
                yield group;
            }
            case "COMBOBOX" -> {
                ComboBox<String> combo = new ComboBox<>();
                combo.setItems(options);
                yield combo;
            }
            case "MULTI_SELECT" -> {
                MultiSelectComboBox<String> combo = new MultiSelectComboBox<>();
                combo.setItems(options);
                yield combo;
            }
            default -> new TextField();
        };
    }
}
