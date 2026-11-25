package com.elicitsoftware.author.survey;

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
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Token placeholders in respondent-facing text. The Survey runtime (QuestionManager.replaceTokens)
 * reads {@code {phrase|default}} or {@code {phrase}} in a question's text, a section's name or a
 * step's name. The phrase names its tokens as {@code <NAME>} (UC-045 BR-003): when a rule reaching
 * the text carries a token the phrase refers to, the placeholder becomes the phrase with the
 * reference replaced by the rule's value — {@code {<CS2>'s|Your}} reads "Dennis'" or "Your",
 * {@code {Has <CS2> been|Were you}} reads "Has Dennis been" or "Were you"; otherwise it becomes the
 * default. A phrase is filled all-or-nothing, so one it names with no value leaves the whole
 * placeholder to its default. A token is a slot name, declared by being used: any rule that carries
 * it fills it, and one token may be filled from many questions (UC-019 BR-005).
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

    /**
     * The longest a token name may be, which is the width of the column that carries it:
     * {@code survey.relationships.token} is {@code character varying(10)}. The write path refuses a
     * longer name rather than letting the insert fail on the constraint.
     */
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
     *
     * @param type the question's type, or null when none is chosen
     * @return true when the type has no {@code data_type}; false for a null type
     */
    public static boolean isDisplayOnly(QuestionType type) {
        return type != null && (type.dataType == null || type.dataType.isBlank());
    }

    /**
     * Whether a rule reading this question can fill a token with the respondent's answer (UC-019
     * BR-006). A display-only question cannot: the only thing a rule on one can put in a slot is the
     * constant the author wrote on the rule itself.
     *
     * @param type the question's type, or null when none is chosen
     * @return true when the type is set and not display-only
     */
    public static boolean collectsAnswer(QuestionType type) {
        return type != null && !isDisplayOnly(type);
    }

    /** Stored ISO form; the three types the respondent picks a moment with. Mirrors TokenSource. */
    private static final Set<String> DATE_TYPES = Set.of("DATE_PICKER", "DATE_TIME_PICKER", "TIME_PICKER");

    /**
     * Whether the answer to this question outranks the constant an author wrote on a rule reading
     * it, which is how the Survey runtime chooses between the two ({@code TokenSource.prefersAnswer}).
     *
     * <p>True when the answer is the respondent's own words or a moment they chose, false when it is
     * an option's coded value or a bare figure, which would read as nonsense in a sentence and so
     * loses to the author's prose. A question with a select group stores the chosen option's coded
     * value whatever its {@code data_type} says, which is why the group is asked about separately.</p>
     *
     * <p>The designer's previews ask this of a question's sample answer, which stands in for a
     * respondent's (UC-011 BR-004). Author cannot know whether a respondent will have answered, so
     * the preview shows the case where they have -- a blank sample is the case where they have
     * not, and falls back to the constant on its own.</p>
     *
     * @param type          the question's type, or null when none is chosen
     * @param selectGroupId the question's select group, null when it has none
     * @return true for a date or time type, or a {@code Text} type with no select group; false
     *         for a type that collects no answer
     */
    public static boolean answerBeatsConstant(QuestionType type, Integer selectGroupId) {
        if (!collectsAnswer(type)) {
            return false;
        }
        if (DATE_TYPES.contains(type.name)) {
            return true;
        }
        return "Text".equalsIgnoreCase(type.dataType) && selectGroupId == null;
    }

    /**
     * One placeholder as written: its phrase and its default, null when it has none.
     *
     * @param phrase      the text before the {@code |}, trimmed
     * @param defaultText the text after the {@code |}, or null when the placeholder has none
     */
    public record Placeholder(String phrase, String defaultText) {

        /**
         * Whether a rule carrying this token fills the placeholder: the phrase refers to it.
         *
         * @param token the token, with or without its angle brackets
         * @return true when the phrase names the token, ignoring case; false for a null or blank token
         */
        public boolean uses(String token) {
            if (token == null || token.isBlank()) {
                return false;
            }
            String wanted = bare(token);
            return tokens().stream().anyMatch(t -> t.equalsIgnoreCase(wanted));
        }

        /**
         * Every token this phrase refers to, in the order written.
         *
         * @return the names between angle brackets, without the brackets
         */
        public Set<String> tokens() {
            Set<String> used = new LinkedHashSet<>();
            Matcher m = TOKEN_REF.matcher(phrase);
            while (m.find()) {
                used.add(m.group(1));
            }
            return used;
        }

        /**
         * The placeholder as written, for messages.
         *
         * @return the phrase and default rebuilt between braces
         */
        public String text() {
            return "{" + phrase + (defaultText == null ? "" : "|" + defaultText) + "}";
        }
    }

    /**
     * The placeholders a text carries, in order, without the built-in counters.
     *
     * @param text the text to read, may be null
     * @return the placeholders found; empty for a null or empty text
     */
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

    /**
     * Whether any placeholder in the text is filled by a rule carrying this token.
     *
     * @param text  the text to read, may be null
     * @param token the token, with or without its angle brackets
     * @return true when some placeholder's phrase names the token
     */
    public static boolean uses(String text, String token) {
        return placeholders(text).stream().anyMatch(ph -> ph.uses(token));
    }

    /**
     * True when every placeholder the token fills carries a default.
     *
     * @param text  the text to read, may be null
     * @param token the token, with or without its angle brackets
     * @return true when no placeholder naming the token lacks a default, including when none names it
     */
    public static boolean alwaysDefaulted(String text, String token) {
        return placeholders(text).stream().filter(ph -> ph.uses(token)).allMatch(ph -> ph.defaultText() != null);
    }

    /**
     * Every token a text refers to, in the order written.
     *
     * <p>Every use declares its token now, which the old bare-word rule could not do: it saw
     * {@code {NAME|friend}} but not {@code {NAME's|your}}, so a token only ever written inside a
     * phrase was invisible to the designer and to the hand-off document.</p>
     *
     * @param text the text to read, may be null
     * @return the token names, without brackets; empty when the text has none
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
     *
     * @param text the text to read, may be null
     * @return true when an angle-bracketed tag lies outside every placeholder; false for a null or
     *         empty text
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

    /**
     * {@code <NAME>} or {@code NAME}, either way: the name itself.
     *
     * @param token the token, with or without its angle brackets, may be null
     * @return the trimmed name without brackets; empty for null
     */
    public static String bare(String token) {
        String trimmed = token == null ? "" : token.trim();
        Matcher m = TOKEN_REF.matcher(trimmed);
        return m.matches() ? m.group(1) : trimmed;
    }

    /**
     * A token as it is written in a phrase.
     *
     * @param token the token, with or without its angle brackets
     * @return the bare name between angle brackets
     */
    public static String reference(String token) {
        return "<" + bare(token) + ">";
    }

    /**
     * A token as Author stores it: the name alone, upper case (FR-012).
     *
     * <p>The brackets belong to the text that names the token, never to the token itself. The Survey
     * runtime keys a text's values on {@code relationship.token} exactly as the column holds it and
     * asks for the name it parsed out of a reference, so a token stored {@code <S1>} is asked for
     * under a name it does not have and fills nothing. Case is free -- every match ignores it -- but
     * one spelling per slot keeps the survey's token list one entry per slot.</p>
     *
     * @param token the token, with or without its angle brackets, may be null
     * @return the bare name in upper case; empty for null
     */
    public static String canonical(String token) {
        return bare(token).toUpperCase(Locale.ROOT);
    }

    /**
     * Whether a phrase could name this token at all.
     *
     * <p>A reference is {@code <NAME>} and the name is what lies between the brackets, so only a
     * token spelled exactly as that name can ever be named. {@link #TOKEN_REF} is the authority for
     * which characters that is, so the question is asked by writing the brackets on and seeing
     * whether the result is a reference: {@code S1} yes, {@code S 1} no, and {@code <S1>} no, because
     * a phrase naming it would have to read {@code <<S1>>}.</p>
     *
     * @param token the token as stored, may be null
     * @return true when the trimmed token in brackets is a {@link #TOKEN_REF}; false for null
     */
    public static boolean nameable(String token) {
        return token != null && TOKEN_REF.matcher("<" + token.trim() + ">").matches();
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
     * @return the bare names found; empty when the text or {@code known} is null or empty
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

    /**
     * Token references whose name is not upper case, for validation (FR-012).
     *
     * @param text the text to read, may be null
     * @return the names that do not match {@link #TOKEN_NAME}; empty when there are none
     */
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
     * Every token the survey already knows, sorted: the ones its current rules carry and the names its
     * current texts use. The rule dialog offers these so an author reuses a name instead of mistyping
     * it (FR-012).
     *
     * <p>A rule's token is offered as its {@link #canonical} name, so a rule an import saved with the
     * brackets on does not add a second entry for a slot the survey already has.</p>
     *
     * @param surveyId the survey
     * @return the token names, sorted and de-duplicated ignoring case
     */
    public static Set<String> known(int surveyId) {
        Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Relationship r : Relationship.<Relationship>list("surveyId = ?1 and effectiveTo = ?2", surveyId, StructuralElement.SENTINEL)) {
            if (r.token != null && !r.token.isBlank()) {
                names.add(canonical(r.token));
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
