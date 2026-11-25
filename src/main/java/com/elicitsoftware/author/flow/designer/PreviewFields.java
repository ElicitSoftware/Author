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

import com.elicitsoftware.author.i18n.Translations;
import com.elicitsoftware.author.survey.TokenSamples;
import com.elicitsoftware.author.survey.Tokens;
import com.elicitsoftware.model.QuestionType;
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
import com.vaadin.flow.component.timepicker.TimePicker;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * Builds the respondent's field for a question the way the Survey app's {@code Elicit*} input
 * wrappers do: the question text as the label, tooltip, placeholder, required indicator and theme
 * variants, choice types fed with their list's options, HTML rendered as HTML, tokens substituted
 * the way the runtime does. Shared by the question dialog's live preview (UC-011 BR-004) and the
 * designer's section preview.
 */
final class PreviewFields {

    /** The options a choice field shows when the question has no list yet. */
    static List<String> sampleOptions() {
        return List.of(Translations.get("previewFields.sampleOption", "A"),
                Translations.get("previewFields.sampleOption", "B"), Translations.get("previewFields.sampleOption", "C"));
    }

    /** A value may itself hold placeholders ("{&lt;G1&gt;'s|your} mother"); the runtime resolves those too. */
    private static final int MAX_NESTING = 5;

    private PreviewFields() { }

    /**
     * Substitute placeholders the way the runtime does (QuestionManager.replaceTokens).
     *
     * <p>A placeholder is {@code {phrase|default}}, and the phrase names its tokens as
     * {@code <NAME>} (UC-045 BR-003). A phrase is filled all-or-nothing: every reference in it must
     * have a value, or the placeholder falls back rather than filling partially. The fallback is
     * the default where the placeholder has one -- {@code {<S1>|}} is an empty default, which is a
     * default, and renders nothing -- and otherwise the phrase as written, brackets and all, which
     * is what a respondent reads. {@code {Q#}} and {@code {S#}} are the runtime's own counters and
     * are left exactly as written.</p>
     *
     * <p>A value may itself be a placeholder ({@code "{<G1>'s|your} mother"}), so the text is
     * resolved repeatedly up to {@link #MAX_NESTING} passes, stopping at a fixed point.</p>
     *
     * <p>Then the runtime's own tidying: "Dennis's" reads "Dennis'", and "her's"/"his's"/"Your's"
     * lose the stray possessive. That tidying is English-only and is applied with no locale check,
     * exactly as the runtime applies it. It is reproduced here rather than corrected because the
     * preview's job is to show the author what a respondent will actually read, warts included.</p>
     *
     * <p>The four replacements are an independent copy of the ones in
     * {@code QuestionManager.replaceTokens} -- two string literals with no shared constant, so a
     * change to either side silently desynchronizes this preview from the runtime. Change one,
     * change both. Two of them are load-bearing for the Family History Survey rather than
     * cosmetic; see FHHS C-009 for what depends on them.</p>
     *
     * <p>A sample answer stands in for the respondent's own words, so it is treated as the runtime
     * treats them: HTML-escaped on the way in where the text is rendered as HTML, spliced in as
     * typed where it is plain text, and never re-read for placeholders of its own, so an answer
     * that happens to contain braces is not mistaken for one. A value the author wrote on
     * a rule is neither escaped nor withheld from the re-reading -- it is prose the respondent is
     * meant to read, and it is often a placeholder in its own right. {@code TokenSamples.Values}
     * carries which is which (ElicitSoftware/Author#22).</p>
     *
     * <p>Each of the two places a preview lands mirrors the runtime component it stands for. A
     * display-only question's text goes to {@link #html(String)} and so to {@code innerHTML}, as it
     * does in Survey's {@code ElicitHtml} (sanitized first, UC-011 BR-004), so a sample is escaped there and an {@code &lt;b&gt;}
     * renders as the literal {@code <b>} a respondent reads. A field's label and a section's title
     * are plain text, as they are in {@code new TextField(answer.label())}, so a sample goes in as
     * typed and {@code O'Brien} is not shown as {@code O&#39;Brien} (ElicitSoftware/Survey#149).</p>
     */
    static String substituteTokens(String text, TokenSamples.Values values) {
        return substituteTokens(text, values, false);
    }

    /**
     * {@link #substituteTokens(String, TokenSamples.Values)} for a text rendered as plain text
     * ({@code html} false) or as HTML ({@code html} true, a display-only question): only in the
     * second is a sample answer HTML-escaped, as Survey's {@code QuestionManager.replaceTokens} does.
     */
    static String substituteTokens(String text, TokenSamples.Values values, boolean html) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        // One pass over the text, over values whose own placeholders were resolved first -- the
        // runtime's shape (QuestionManager.replaceTokens). Looping over the text instead would
        // re-read a respondent's words on the second pass.
        String result = substituteOnce(text, resolveAuthoredValues(values, html), values.authored(), html);
        return result.replace(" her's ", " her ").replace(" his's ", " his ").replace(" Your's ", " Your ")
                .replaceAll("s's", "s'");
    }

    /**
     * The values with every authored one's own placeholders resolved, so a rule's value reaches a
     * sentence as prose and not as braces. The Family History Survey fills {@code S1} with
     * {@code {<G1>'s|your} mother} and {@code G1} with the patient's name, so "Please indicate if
     * {@code {<S1>|you}} is still living" needs two rounds. Only authored values are re-read.
     */
    private static Map<String, String> resolveAuthoredValues(TokenSamples.Values values, boolean html) {
        Set<String> authored = values.authored();
        if (authored.isEmpty()) {
            return values.values();
        }
        Map<String, String> ready = new HashMap<>(values.values());
        for (int pass = 0; pass < MAX_NESTING; pass++) {
            boolean changed = false;
            for (String token : authored) {
                String value = ready.get(token);
                if (value == null || value.indexOf('{') < 0) {
                    continue;
                }
                String next = substituteOnce(value, ready, authored, html);
                if (!next.equals(value)) {
                    ready.put(token, next);
                    changed = true;
                }
            }
            if (!changed) {
                break;
            }
        }
        return ready;
    }

    private static String substituteOnce(String text, Map<String, String> values, Set<String> authored, boolean html) {
        Matcher placeholder = Tokens.PLACEHOLDER.matcher(text);
        StringBuilder out = new StringBuilder();
        while (placeholder.find()) {
            String phrase = placeholder.group(1);
            String defaultText = placeholder.group(2);
            String replacement;
            if (Tokens.BUILT_IN.contains(phrase.trim())) {
                // {Q#} and {S#} are runtime counters, filled by the Survey app against the answer's own key.
                replacement = placeholder.group();
            } else {
                String filled = fillPhrase(phrase, values, authored, html);
                replacement = filled != null ? filled : (defaultText == null ? phrase : defaultText);
            }
            placeholder.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        placeholder.appendTail(out);
        return out.toString();
    }

    /**
     * The phrase with every {@code <NAME>} replaced by its value, or null when it cannot be filled:
     * when any reference has no value, and when the phrase names no token at all. Either way the
     * caller falls back, so a phrase is never half filled.
     */
    private static String fillPhrase(String phrase, Map<String, String> values, Set<String> authored, boolean html) {
        Matcher ref = Tokens.TOKEN_REF.matcher(phrase);
        StringBuilder filled = new StringBuilder();
        boolean any = false;
        while (ref.find()) {
            any = true;
            String value = valueOf(ref.group(1), values);
            if (value == null) {
                return null;
            }
            // A sample answer stands in for the respondent's words, which are escaped in a text
            // rendered as HTML and go in as typed elsewhere. A value the author wrote is never
            // escaped: it has already had its own placeholders resolved, and escaping it here would
            // double-escape the sample spliced into it -- and would destroy the <TOKEN> brackets it
            // carries before they could be read at all.
            boolean fromAuthor = authored.stream().anyMatch(t -> t.equalsIgnoreCase(ref.group(1)));
            ref.appendReplacement(filled, Matcher.quoteReplacement(fromAuthor || !html ? value : escapeHtml(value)));
        }
        ref.appendTail(filled);
        return any ? filled.toString() : null;
    }

    /**
     * A verbatim copy of {@code QuestionManager.escapeHtml}, down to the order: {@code &} first, so
     * the ampersands the later replacements introduce are not escaped again. Change one, change
     * both -- a preview that escapes differently from the runtime is the bug this exists to close.
     */
    private static String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * A token's value, matched by name without regard to case, mirroring QuestionManager.valueOf.
     *
     * <p>The name is matched as the map holds it. A rule whose token was typed {@code <S1>} rather
     * than {@code S1} is keyed that way here, and does not fill {@code <S1>} -- which is exactly
     * what happens at runtime, where the values map is keyed on {@code relationship.token} verbatim
     * too. The preview shows such a rule as unfilled because the deployed survey leaves it
     * unfilled.</p>
     */
    private static String valueOf(String token, Map<String, String> values) {
        String exact = values.get(token);
        if (exact != null) {
            return exact;
        }
        for (Map.Entry<String, String> e : values.entrySet()) {
            if (e.getKey() != null && e.getKey().equalsIgnoreCase(token)) {
                return e.getValue();
            }
        }
        return null;
    }

    /**
     * True for the display-only types, which collect no value and whose text is rendered as HTML:
     * HTML inline on the page, MODAL in a dialog. Both preview the same way.
     */
    static boolean isDisplayOnly(QuestionType type) {
        return Tokens.isDisplayOnly(type);
    }

    /**
     * The markup a display-only question's preview may render (UC-011 BR-004): the formatting
     * set, with {@code class} and {@code style} on every element. Scripts, event handlers,
     * {@code javascript:} addresses, frames and forms are not in it.
     */
    private static final Safelist PREVIEW_HTML = Safelist.relaxed().addAttributes(":all", "class", "style");

    /**
     * UC-011 BR-004: an HTML question's text with everything that could run in the viewer's
     * session removed. The text is another author's, or came from an imported file or an AI
     * draft, and the preview renders it in this author's session.
     *
     * @param html the question text, tokens already substituted
     * @return the text reduced to the formatting set
     */
    static String sanitize(String html) {
        // Not pretty-printed: added whitespace around inline elements would change what renders.
        return Jsoup.clean(html, "", PREVIEW_HTML, new Document.OutputSettings().prettyPrint(false));
    }

    /** An HTML question has no input; the respondent sees its text rendered as HTML. */
    static Component html(String content) {
        String body = content == null || content.isBlank() ? Translations.get("previewFields.htmlPlaceholder") : content;
        try {
            return new Html("<div class=\"question-preview-html\">" + sanitize(body) + "</div>"); // i18n:ignore markup wrapper
        } catch (RuntimeException invalid) {
            return new Span(Translations.get("previewFields.invalidHtml"));
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
        Component field = input(typeName, options == null || options.isEmpty() ? sampleOptions() : options);
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
            case "DATE_TIME_PICKER" -> new DateTimePicker();
            case "TIME_PICKER" -> new TimePicker();
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
