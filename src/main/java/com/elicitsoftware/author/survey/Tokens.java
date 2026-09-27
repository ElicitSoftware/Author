package com.elicitsoftware.author.survey;

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

import com.elicitsoftware.model.Question;
import com.elicitsoftware.model.QuestionType;
import com.elicitsoftware.model.Relationship;
import com.elicitsoftware.model.Section;
import com.elicitsoftware.model.Step;
import com.elicitsoftware.model.StructuralElement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Token placeholders in respondent-facing text. The Survey runtime (QuestionManager.replaceTokens)
 * reads {@code {phrase|default}} or {@code {phrase}} in a question's text, a section's name or a
 * step's name: when a rule reaching the text carries a token the phrase <em>contains</em>, the
 * placeholder becomes the phrase with the token replaced by the rule's value — {@code {CS2's|Your}}
 * reads "Dennis'" or "Your", {@code {Has CS2 been|Were you}} reads "Has Dennis been" or "Were you";
 * otherwise it becomes the default. A token is a slot name, declared by being used: any rule that
 * carries it fills it, and one token may be filled from many questions (UC-019 BR-005).
 * {@code {Q#}} and {@code {S#}} are the runtime's instance counters, not tokens.
 */
public final class Tokens {

    /** Group 1 is the phrase, group 2 the default text (null when the placeholder has none). */
    public static final Pattern PLACEHOLDER = Pattern.compile("\\{([^{}|]+)(?:\\|([^{}]*))?}");

    /**
     * A token where it is used: {@code <NAME>}, inside a placeholder's phrase.
     *
     * <p>The brackets are the whole point. A token used to be recognized by
     * {@code phrase.contains(token)}, which cannot tell a token from an ordinary word:
     * {@code surname} contains {@code name} and {@code S10} contains {@code S1}, so a phrase
     * mentioning a word as prose was indistinguishable from one naming the token. Delimited, a
     * reference matches exactly or not at all.</p>
     *
     * <p>Read case-insensitively, so a definition written before the convention still parses; a
     * name is required to be upper case where it is authored ({@link #TOKEN_NAME}).</p>
     */
    public static final Pattern TOKEN_REF = Pattern.compile("<([A-Za-z0-9_]+)>");

    /** A well-formed token name: upper case, starting with a letter (FR-012). */
    public static final Pattern TOKEN_NAME = Pattern.compile("[A-Z][A-Z0-9_]*");

    /** Runtime counters the Survey app fills itself. */
    public static final Set<String> BUILT_IN = Set.of("Q#", "S#");

    /** Anything written between angle brackets, token reference or HTML tag alike. */
    private static final Pattern ANGLED = Pattern.compile("<[^>]+>");

    public static final int MAX_LENGTH = 10;

    private Tokens() { }

    /**
     * Whether a question collects nothing, so no rule reading it can fill a token from an answer:
     * {@code HTML} renders text inline and {@code MODAL} in a dialog, and neither has an input.
     * <p>
     * Read off {@code question_types.data_type}, which the platform leaves empty for exactly those
     * two and sets for all fourteen types that collect a value. The flag rather than the two names,
     * because that is the same question the Survey runtime asks of the same row when it decides what
     * a token holds (UC-002 BR-010), and a display-only type added later is covered in both modules
     * without editing either. A null type is not display-only -- it is a question whose type the
     * author has not chosen yet.
     */
    public static boolean isDisplayOnly(QuestionType type) {
        return type != null && (type.dataType == null || type.dataType.isBlank());
    }

    /**
     * Whether a rule reading this question can fill a token with the respondent's answer (UC-019
     * BR-006). A display-only question cannot: the only thing a rule on one can put in a slot is the
     * constant the author wrote on the rule itself.
     */
    public static boolean collectsAnswer(QuestionType type) {
        return type != null && !isDisplayOnly(type);
    }

    /** One placeholder as written: its phrase and its default, null when it has none. */
    public record Placeholder(String phrase, String defaultText) {

        /** Whether a rule carrying this token fills the placeholder: the phrase refers to it. */
        public boolean uses(String token) {
            if (token == null || token.isBlank()) {
                return false;
            }
            String wanted = bare(token);
            return tokens().stream().anyMatch(t -> t.equalsIgnoreCase(wanted));
        }

        /** Every token this phrase refers to, in the order written. */
        public Set<String> tokens() {
            Set<String> used = new LinkedHashSet<>();
            Matcher m = TOKEN_REF.matcher(phrase);
            while (m.find()) {
                used.add(m.group(1));
            }
            return used;
        }

        /** The placeholder as written, for messages. */
        public String text() {
            return "{" + phrase + (defaultText == null ? "" : "|" + defaultText) + "}";
        }
    }

    /** The placeholders a text carries, in order, without the built-in counters. */
    public static List<Placeholder> placeholders(String text) {
        List<Placeholder> found = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return found;
        }
        Matcher m = PLACEHOLDER.matcher(text);
        while (m.find()) {
            String phrase = m.group(1).trim();
            if (!phrase.isEmpty() && !BUILT_IN.contains(phrase)) {
                found.add(new Placeholder(phrase, m.group(2)));
            }
        }
        return found;
    }

    /** Whether any placeholder in the text is filled by a rule carrying this token. */
    public static boolean uses(String text, String token) {
        return placeholders(text).stream().anyMatch(ph -> ph.uses(token));
    }

    /** True when every placeholder the token fills carries a default. */
    public static boolean alwaysDefaulted(String text, String token) {
        return placeholders(text).stream().filter(ph -> ph.uses(token)).allMatch(ph -> ph.defaultText() != null);
    }

    /**
     * Every token a text refers to, in the order written.
     *
     * <p>Every use declares its token now, which the old bare-word rule could not do: it saw
     * {@code {NAME|friend}} but not {@code {NAME's|your}}, so a token only ever written inside a
     * phrase was invisible to the designer and to the hand-off document.</p>
     */
    public static Set<String> names(String text) {
        Set<String> names = new LinkedHashSet<>();
        for (Placeholder ph : placeholders(text)) {
            names.addAll(ph.tokens());
        }
        return names;
    }

    /**
     * Whether the text carries markup, counting only what lies outside its placeholders.
     *
     * <p>Markup belongs around a placeholder, never inside it: {@code <b>{<NAME>|name}</b>}, not
     * {@code {<b><NAME></b>|<b>name</b>}}. Inside the braces every {@code <...>} is a token, so a
     * plain {@code contains("<")} answers "has a token" rather than "has markup" and flags every
     * personalized string in the survey.</p>
     */
    public static boolean hasMarkup(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return ANGLED.matcher(outsidePlaceholders(text)).find();
    }

    /** The text with every placeholder removed, leaving what surrounds them. */
    private static String outsidePlaceholders(String text) {
        return PLACEHOLDER.matcher(text).replaceAll(" ");
    }

    /** {@code <NAME>} or {@code NAME}, either way: the name itself. */
    public static String bare(String token) {
        String trimmed = token == null ? "" : token.trim();
        Matcher m = TOKEN_REF.matcher(trimmed);
        return m.matches() ? m.group(1) : trimmed;
    }

    /** A token as it is written in a phrase. */
    public static String reference(String token) {
        return "<" + bare(token) + ">";
    }

    /**
     * The known token names a text names as bare words inside a placeholder rather than as
     * {@code <NAME>}, in the order written.
     *
     * <p>This is the shape every placeholder had before the brackets: {@code {Does S1|Do you}}. The
     * runtime fills only {@code <S1>}, so such a placeholder fills nothing and the respondent reads
     * the default -- silently, because nothing about the text is malformed. Matched as a whole word
     * and case-sensitively against the names the survey actually uses, so prose is not mistaken for a
     * token: only a name some rule carries, or some other text brackets, can be reported.</p>
     *
     * @param text  the string to inspect
     * @param known the token names the survey uses -- see {@link #known(int)}
     */
    public static Set<String> bareTokenNames(String text, Collection<String> known) {
        Set<String> bare = new LinkedHashSet<>();
        if (text == null || known == null || known.isEmpty()) {
            return bare;
        }
        for (Placeholder ph : placeholders(text)) {
            String written = ph.text();
            Set<String> bracketed = ph.tokens();
            for (String token : known) {
                String name = bare(token);
                if (name.isEmpty() || bracketed.stream().anyMatch(t -> t.equalsIgnoreCase(name))) {
                    continue;
                }
                if (Pattern.compile("(?<![A-Za-z0-9_])" + Pattern.quote(name) + "(?![A-Za-z0-9_>])")
                        .matcher(written).find()) {
                    bare.add(name);
                }
            }
        }
        return bare;
    }

    /** Token references whose name is not upper case, for validation (FR-012). */
    public static Set<String> malformed(String text) {
        Set<String> bad = new LinkedHashSet<>();
        for (Placeholder ph : placeholders(text)) {
            for (String token : ph.tokens()) {
                if (!TOKEN_NAME.matcher(token).matches()) {
                    bad.add(token);
                }
            }
        }
        return bad;
    }

    /**
     * Every token the survey already knows, sorted: the ones its current rules carry and the bare
     * names its current texts use. The rule dialog offers these so an author reuses a name instead
     * of mistyping it (FR-012).
     */
    public static Set<String> known(int surveyId) {
        Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Relationship r : Relationship.<Relationship>list("surveyId = ?1 and effectiveTo = ?2", surveyId, StructuralElement.SENTINEL)) {
            if (r.token != null && !r.token.isBlank()) {
                names.add(r.token.trim());
            }
        }
        for (Question q : Question.<Question>list("surveyId = ?1 and effectiveTo = ?2", surveyId, StructuralElement.SENTINEL)) {
            names.addAll(names(q.text));
        }
        for (Section s : Section.<Section>list("surveyId = ?1 and effectiveTo = ?2", surveyId, StructuralElement.SENTINEL)) {
            names.addAll(names(s.name));
        }
        for (Step s : Step.<Step>list("surveyId = ?1 and effectiveTo = ?2", surveyId, StructuralElement.SENTINEL)) {
            names.addAll(names(s.name));
        }
        return names;
    }
}
