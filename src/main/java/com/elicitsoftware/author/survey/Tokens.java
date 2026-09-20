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
import com.elicitsoftware.model.Relationship;
import com.elicitsoftware.model.Section;
import com.elicitsoftware.model.Step;
import com.elicitsoftware.model.StructuralElement;

import java.util.ArrayList;
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

    /** A phrase that is one bare word is the token itself: {@code {NAME}} or {@code {NAME|default}}. */
    private static final Pattern BARE_NAME = Pattern.compile("[A-Za-z0-9_]+");

    /** Runtime counters the Survey app fills itself. */
    public static final Set<String> BUILT_IN = Set.of("Q#", "S#");

    public static final int MAX_LENGTH = 10;

    private Tokens() { }

    /** One placeholder as written: its phrase and its default, null when it has none. */
    public record Placeholder(String phrase, String defaultText) {

        /** Whether a rule carrying this token fills the placeholder — the runtime tests containment. */
        public boolean uses(String token) {
            return token != null && !token.isBlank() && phrase.contains(token);
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

    /** Token names a text declares outright — bare-word phrases such as {@code {NAME|friend}}. */
    public static Set<String> names(String text) {
        Set<String> names = new LinkedHashSet<>();
        for (Placeholder ph : placeholders(text)) {
            if (BARE_NAME.matcher(ph.phrase()).matches()) {
                names.add(ph.phrase());
            }
        }
        return names;
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
