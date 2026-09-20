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

import com.elicitsoftware.author.survey.Tokens;
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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds the respondent's field for a question the way the Survey app's {@code Elicit*} input
 * wrappers do: the question text as the label, tooltip, placeholder, required indicator and theme
 * variants, choice types fed with their list's options, HTML rendered as HTML, tokens substituted
 * the way the runtime does. Shared by the question dialog's live preview (UC-011 BR-004) and the
 * designer's section preview.
 */
final class PreviewFields {

    /** The options a choice field shows when the question has no list yet. */
    static final List<String> SAMPLE_OPTIONS = List.of("Option A", "Option B", "Option C");

    /** A value may itself hold placeholders ("{G1's|your} mother"); the runtime resolves those too. */
    private static final int MAX_NESTING = 5;

    private PreviewFields() { }

    /**
     * Substitute placeholders the way the runtime does (QuestionManager.replaceTokens): a placeholder
     * {phrase|default} whose phrase contains a token with a value becomes the phrase with the token
     * replaced, else the default, else stays as written — then the runtime's own tidying: "Dennis's"
     * reads "Dennis'", and "her's"/"his's"/"Your's" lose the stray possessive.
     */
    static String substituteTokens(String text, Map<String, String> values) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String result = text;
        for (int pass = 0; pass < MAX_NESTING && result.indexOf('{') >= 0; pass++) {
            String next = substituteOnce(result, values);
            if (next.equals(result)) {
                break;
            }
            result = next;
        }
        return result.replace(" her's ", " her ").replace(" his's ", " his ").replace(" Your's ", " Your ")
                .replaceAll("s's", "s'");
    }

    private static String substituteOnce(String text, Map<String, String> values) {
        Matcher m = Tokens.PLACEHOLDER.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String phrase = m.group(1);
            String def = m.group(2);
            String replacement = null;
            // Keys in sorted order, as the runtime's TreeMap walks them; every key the phrase holds is replaced.
            for (String key : new TreeMap<>(values).keySet()) {
                if (phrase.contains(key)) {
                    phrase = phrase.replaceFirst(Pattern.quote(key), Matcher.quoteReplacement(values.get(key)));
                    replacement = phrase;
                }
            }
            if (replacement == null) {
                replacement = def != null ? def : m.group();
            }
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
