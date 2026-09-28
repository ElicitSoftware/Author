package com.elicitsoftware.author.definition;

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
import com.elicitsoftware.model.SectionsQuestion;
import com.elicitsoftware.model.SelectGroup;
import com.elicitsoftware.model.SelectItem;
import com.elicitsoftware.model.Step;
import com.elicitsoftware.model.StepsSections;
import com.elicitsoftware.model.StructuralElement;
import com.elicitsoftware.model.Survey;
import com.elicitsoftware.author.survey.RenameLedger;
import com.elicitsoftware.author.survey.ReportingChangeLedger;
import com.elicitsoftware.author.survey.ReportingService;
import com.elicitsoftware.author.survey.RuleOrdering;
import com.elicitsoftware.author.survey.RulePath;
import com.elicitsoftware.author.survey.TokenSamples;
import com.elicitsoftware.author.survey.Tokens;
import com.elicitsoftware.author.survey.TranslationService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Checks a survey's current elements before export (UC-007): dangling durable references,
 * relationships whose downstream path does not hold together or that point backwards (UC-033 A3), choice
 * questions without options, tokens no rule fills and Text rules nothing uses (BR-005), rules whose token
 * is fed by a question that collects no answer (UC-019 BR-006), rules whose token is not a name a text
 * could write between angle brackets (BR-007), empty steps and
 * sections, duplicate ordering. Errors block export; warnings do not.
 * Read-only (UC-007 BR-004).
 */
@ApplicationScoped
public class ExportValidation {

    @Inject
    RenameLedger renames;

    @Inject
    ReportingService reporting;

    @Inject
    ReportingChangeLedger reportingChanges;

    @Inject
    TranslationService translations;

    public static final Set<String> CHOICE_TYPES = Set.of("RADIO", "COMBOBOX", "MULTI_SELECT", "CHECKBOX_GROUP");

    public List<ValidationIssue> validate(Survey survey) {
        int sid = survey.id;
        List<ValidationIssue> issues = new ArrayList<>();

        Map<Integer, Step> steps = current(Step.list("surveyId", sid), s -> s.stepId);
        Map<Integer, Section> sections = current(Section.list("surveyId", sid), s -> s.sectionId);
        Map<Integer, Question> questions = current(Question.list("surveyId", sid), q -> q.questionId);
        Map<Integer, SelectGroup> groups = current(SelectGroup.list("surveyId", sid), g -> g.selectGroupId);
        List<SelectItem> items = SelectItem.<SelectItem>list("surveyId", sid).stream().filter(StructuralElement::isCurrent).toList();
        Map<Integer, StepsSections> stepsSections = current(StepsSections.list("surveyId", sid), ss -> ss.stepsSectionsId);
        Map<Integer, SectionsQuestion> sectionsQuestions = current(SectionsQuestion.list("surveyId", sid), sq -> sq.sectionsQuestionId);
        List<Relationship> relationships = Relationship.<Relationship>list("surveyId", sid).stream().filter(StructuralElement::isCurrent).toList();

        if (steps.isEmpty()) {
            issues.add(ValidationIssue.warning("surveys", sid, "The survey has no steps"));
        }

        Set<Integer> stepsWithSections = new HashSet<>();
        Set<Integer> sectionsWithQuestions = new HashSet<>();
        Set<Integer> groupsWithItems = new HashSet<>();
        items.forEach(i -> groupsWithItems.add(i.selectGroupId));

        for (StepsSections ss : stepsSections.values()) {
            if (!steps.containsKey(ss.stepId)) {
                issues.add(ValidationIssue.error("steps_sections", ss.stepsSectionsId, "Section assignment refers to a removed or missing step"));
            }
            if (!sections.containsKey(ss.sectionId)) {
                issues.add(ValidationIssue.error("steps_sections", ss.stepsSectionsId, "Section assignment refers to a removed or missing section"));
            }
            stepsWithSections.add(ss.stepId);
        }
        for (SectionsQuestion sq : sectionsQuestions.values()) {
            if (!questions.containsKey(sq.questionId)) {
                issues.add(ValidationIssue.error("sections_questions", sq.sectionsQuestionId, "Question assignment refers to a removed or missing question"));
            }
            if (!sections.containsKey(sq.sectionId)) {
                issues.add(ValidationIssue.error("sections_questions", sq.sectionsQuestionId, "Question assignment refers to a removed or missing section"));
            }
            sectionsWithQuestions.add(sq.sectionId);
        }
        for (Question q : questions.values()) {
            String type = q.questionType == null ? "" : q.questionType.name;
            if (q.selectGroupId != null && !groups.containsKey(q.selectGroupId)) {
                issues.add(ValidationIssue.error("questions", q.questionId, label(q) + " refers to a removed or missing list"));
            } else if (CHOICE_TYPES.contains(type)) {
                if (q.selectGroupId == null) {
                    issues.add(ValidationIssue.error("questions", q.questionId, label(q) + " is a " + type + " question without a list of options"));
                } else if (!groupsWithItems.contains(q.selectGroupId)) {
                    issues.add(ValidationIssue.error("questions", q.questionId, label(q) + " uses a list that has no options"));
                }
            }
            if (q.text == null || q.text.isBlank()) {
                issues.add(ValidationIssue.error("questions", q.questionId, label(q) + " has no text"));
            }
        }
        for (SelectItem i : items) {
            if (!groups.containsKey(i.selectGroupId)) {
                issues.add(ValidationIssue.error("select_items", i.selectItemId, "Option '" + i.displayText + "' belongs to a removed or missing list"));
            }
        }
        Map<Integer, String> sectionNames = new HashMap<>();
        sections.values().forEach(s -> sectionNames.put(s.sectionId, s.name));
        Map<Integer, String> questionNames = new HashMap<>();
        questions.values().forEach(q -> questionNames.put(q.questionId, RuleOrdering.questionLabel(q)));
        RuleOrdering.Board board = new RuleOrdering.Board(steps.values(), stepsSections.values(), sectionsQuestions.values(), sectionNames, questionNames);
        for (Relationship r : relationships) {
            String rule = "Rule '" + r.description + "'";
            // UC-019 BR-001: the downstream path — step, section in it, question in that — must hold together.
            RulePath.problem(r, board).ifPresent(p -> issues.add(ValidationIssue.error("relationships", r.relationshipId, rule + " " + p)));
            if (!sectionsQuestions.containsKey(r.upstreamSqId)) {
                issues.add(ValidationIssue.error("relationships", r.relationshipId, rule + " reads from a removed or missing question assignment"));
            }
            if (r.upstreamStepId != null && !steps.containsKey(r.upstreamStepId)) {
                issues.add(ValidationIssue.error("relationships", r.relationshipId, rule + " reads from a removed or missing step"));
            }
            if (r.downstreamStepId != null && !steps.containsKey(r.downstreamStepId)) {
                issues.add(ValidationIssue.error("relationships", r.relationshipId, rule + " targets a removed or missing step"));
            }
            if (r.downstreamSsId != null && !stepsSections.containsKey(r.downstreamSsId)) {
                issues.add(ValidationIssue.error("relationships", r.relationshipId, rule + " targets a removed or missing section assignment"));
            }
            if (r.downstreamSqId != null && !sectionsQuestions.containsKey(r.downstreamSqId)) {
                issues.add(ValidationIssue.error("relationships", r.relationshipId, rule + " targets a removed or missing question assignment"));
            }
            if (r.operatorType == null || r.actionType == null) {
                issues.add(ValidationIssue.error("relationships", r.relationshipId, rule + " has no operator or action"));
            }
            // UC-033 A3: a rule the respondent can never see fire blocks export (UC-007 BR-001).
            board.problem(r).ifPresent(p -> issues.add(ValidationIssue.error("relationships", r.relationshipId, rule + " points backwards: " + p)));
        }
        checkTokens(issues, steps, sections, questions, stepsSections, sectionsQuestions, relationships, board);
        for (Step s : steps.values()) {
            if (!stepsWithSections.contains(s.stepId)) {
                issues.add(ValidationIssue.warning("steps", s.stepId, "Step '" + s.name + "' has no sections"));
            }
        }
        for (Section s : sections.values()) {
            if (!sectionsWithQuestions.contains(s.sectionId)) {
                issues.add(ValidationIssue.warning("sections", s.sectionId, "Section '" + s.name + "' has no questions"));
            }
        }
        if (!stepsSections.isEmpty()) {
            // UC-016 BR-002: the runtime enters the survey at initial_display_key; it must name a current assignment.
            Set<String> keys = new HashSet<>();
            stepsSections.values().forEach(ss -> keys.add(ss.displayKey));
            if (survey.initialDisplayKey == null || !keys.contains(survey.initialDisplayKey)) {
                issues.add(ValidationIssue.error("surveys", sid, "The initial display key does not name a current section assignment"));
            }
        }
        // UC-007 step 4 / UC-018 BR-001: renames since the last export are retroactive at deployed sites.
        renames.since(sid).forEach(r -> issues.add(ValidationIssue.warning(r.table(), r.durableId(), r.message())));
        // UC-022 BR-001: tag changes since the last export are retroactive at deployed sites too.
        reportingChanges.since(sid).forEach(c -> issues.add(ValidationIssue.warning(c.kind(), c.durableId(), c.message())));
        // UC-024 BR-002: a name the site's ETL would reject blocks the export; the rest are warnings.
        issues.addAll(reporting.impact(sid).problems());
        duplicateOrders(issues, "steps", steps.values().stream().map(s -> Map.entry(s.stepId, "step:" + s.displayOrder)).toList());
        duplicateOrders(issues, "steps_sections", stepsSections.values().stream()
                .map(ss -> Map.entry(ss.stepsSectionsId, ss.stepId + ":" + ss.sectionDisplayOrder)).toList());
        duplicateOrders(issues, "sections_questions", sectionsQuestions.values().stream()
                .map(sq -> Map.entry(sq.sectionsQuestionId, sq.sectionId + ":" + sq.displayOrder)).toList());
        // UC-043 / FR-060: warnings, never errors. An author may legitimately export a survey whose
        // translations are behind -- every one of those strings falls back to the base language at
        // the site -- but they should be reminded once more before the file leaves Author.
        translationGaps(issues, survey);
        return issues;
    }

    private static void duplicateOrders(List<ValidationIssue> issues, String kind, List<Map.Entry<Integer, String>> keyed) {
        Map<String, Integer> seen = new HashMap<>();
        for (Map.Entry<Integer, String> e : keyed) {
            Integer first = seen.putIfAbsent(e.getValue(), e.getKey());
            if (first != null) {
                issues.add(ValidationIssue.warning(kind, e.getKey(), "Two elements share the same position ("
                        + e.getValue().replaceFirst("^[^:]*:", "order ") + ")"));
            }
        }
    }

    private static <E extends StructuralElement> Map<Integer, E> current(List<E> rows, Function<E, Integer> durable) {
        Map<Integer, E> out = new HashMap<>();
        for (E e : rows) {
            if (e.isCurrent()) {
                out.put(durable.apply(e), e);
            }
        }
        return out;
    }

    /**
     * UC-007 BR-005 / FR-005: a token a text uses must be filled by a rule that reaches that text — a
     * rule reaches its target and everything inside it (UC-019 BR-005) — and the token a rule of any
     * action carries must be used by a text it reaches. A placeholder with no default that nothing fills is an error: the
     * runtime would show the respondent the raw placeholder. One with a default is a warning.
     */
    private static void checkTokens(List<ValidationIssue> issues, Map<Integer, Step> steps, Map<Integer, Section> sections,
                                    Map<Integer, Question> questions, Map<Integer, StepsSections> stepsSections,
                                    Map<Integer, SectionsQuestion> sectionsQuestions, List<Relationship> relationships,
                                    RuleOrdering.Board board) {
        TokenSamples fills = TokenSamples.of(relationships, stepsSections.values(), sectionsQuestions.values(), Map.of());
        for (Step s : steps.values()) {
            for (Tokens.Placeholder ph : Tokens.placeholders(s.name)) {
                if (!fills.fills(ph, s.stepId, null, null)) {
                    unfilled(issues, "steps", s.stepId, "Step '" + s.name + "'", ph);
                }
            }
        }
        for (StepsSections ss : stepsSections.values()) {
            Section sec = sections.get(ss.sectionId);
            if (sec == null) {
                continue;
            }
            for (Tokens.Placeholder ph : Tokens.placeholders(sec.name)) {
                if (!fills.fills(ph, ss.stepId, ss.stepsSectionsId, null)) {
                    unfilled(issues, "sections", sec.sectionId, "Section '" + sec.name + "' in step " + board.stepLabel(ss.stepId), ph);
                }
            }
        }
        for (SectionsQuestion sq : sectionsQuestions.values()) {
            Question q = questions.get(sq.questionId);
            if (q == null) {
                continue;
            }
            List<StepsSections> mounts = board.mountsOf(sq.sectionId);
            for (Tokens.Placeholder ph : Tokens.placeholders(q.text)) {
                boolean filled = mounts.isEmpty()
                        ? fills.fills(ph, null, null, sq.sectionsQuestionId)
                        : mounts.stream().allMatch(ss -> fills.fills(ph, ss.stepId, ss.stepsSectionsId, sq.sectionsQuestionId));
                if (!filled) {
                    unfilled(issues, "questions", q.questionId, label(q) + " in section " + board.sectionLabel(sq.sectionId), ph);
                }
            }
        }
        Map<Integer, List<Question>> questionsBySection = new HashMap<>();
        for (SectionsQuestion sq : sectionsQuestions.values()) {
            Question q = questions.get(sq.questionId);
            if (q != null) {
                questionsBySection.computeIfAbsent(sq.sectionId, k -> new ArrayList<>()).add(q);
            }
        }
        for (Relationship r : relationships) {
            if (r.token == null || r.token.isBlank()) {
                continue;
            }
            String tok = r.token.trim();
            if (tokenSpelling(issues, r, tok)) {
                // The token cannot be named at all, so "no text uses it" and "nothing fills it" are the
                // same defect read out again. One finding, at its cause.
                continue;
            }
            if (!reachUses(r, tok, steps, sections, questions, stepsSections, sectionsQuestions, questionsBySection)) {
                issues.add(ValidationIssue.warning("relationships", r.relationshipId,
                        "Rule '" + r.description + "' fills {" + tok + "} but no text it reaches uses it"));
            }
            displayOnlySource(issues, r, tok, questions, sectionsQuestions);
        }
        undelimitedTokens(issues, steps, sections, questions, relationships);
        undefinedReferences(issues, steps, sections, questions, relationships);
    }

    /**
     * UC-007 BR-006: every {@code <NAME>} in a phrase must be a token some rule carries.
     *
     * <p>Filling a phrase is all or nothing — {@code QuestionManager.fillPhrase} returns null the
     * moment one reference has no value — so a single reference nothing defines stops the whole
     * placeholder filling, and the respondent reads its default forever. Nothing about the text
     * looks wrong, which is the same silent failure the undelimited check exists for.</p>
     *
     * <p>This is also how markup written inside a placeholder is caught. {@code <b>} is a
     * well-formed token name, so it cannot be told from a token by its spelling; it can be told by
     * the survey defining no such token. The fix is the same either way — a tag belongs outside the
     * braces, wrapping the placeholder — so the message says so.</p>
     *
     * <p>Only the phrase is checked. The default is used verbatim and is never scanned for
     * references, so markup there renders as markup and fills nothing in.</p>
     *
     * <p>A rule whose token carries its own brackets counts as defining the name here, deliberately:
     * {@code defined} is built from {@link Tokens#bare}, so {@code <S1>} defines {@code S1}. Reading
     * the column strictly instead would report that one broken rule once for every text that names the
     * token, and every report would block export. It is reported once, at its cause, by BR-007.</p>
     */
    private static void undefinedReferences(List<ValidationIssue> issues, Map<Integer, Step> steps,
                                            Map<Integer, Section> sections, Map<Integer, Question> questions,
                                            List<Relationship> relationships) {
        // What a rule carries is what the runtime can fill. Names harvested from the texts
        // themselves would make a stray <b> define itself.
        Set<String> defined = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Relationship r : relationships) {
            if (r.token != null && !r.token.isBlank()) {
                defined.add(Tokens.bare(r.token.trim()));
            }
        }
        for (Step s : steps.values()) {
            undefinedHere(issues, "steps", s.stepId, "Step '" + s.name + "'", s.name, defined);
        }
        for (Section s : sections.values()) {
            undefinedHere(issues, "sections", s.sectionId, "Section '" + s.name + "'", s.name, defined);
        }
        for (Question q : questions.values()) {
            undefinedHere(issues, "questions", q.questionId, label(q), q.text, defined);
        }
        for (Relationship r : relationships) {
            undefinedHere(issues, "relationships", r.relationshipId, "Rule '" + r.description + "'",
                    r.defaultUpstreamValue, defined);
        }
    }

    private static void undefinedHere(List<ValidationIssue> issues, String kind, Integer id, String where,
                                      String text, Set<String> defined) {
        for (Tokens.Placeholder ph : Tokens.placeholders(text)) {
            for (String name : ph.tokens()) {
                if (defined.contains(name)) {
                    continue;
                }
                String ref = Tokens.reference(name);
                issues.add(ValidationIssue.error(kind, id, where + " uses " + ref + " in " + ph.text()
                        + ", but no rule defines the token " + name + ", so the placeholder never fills and"
                        + " respondents always read "
                        + (ph.defaultText() == null ? "it as written" : "\"" + ph.defaultText() + "\"")
                        + ". If " + ref + " is markup, it belongs outside the braces: write " + ref + "{"));
            }
        }
    }

    /**
     * UC-019 BR-006: a token holds the respondent's answer or the rule's own default upstream value.
     * A display-only question ({@code HTML}, {@code MODAL}) collects no answer, so a rule reading one
     * has only its own value to give -- and with neither, the token is never filled at all and the
     * respondent reads the placeholder's default. The Family History Survey drives the possessive on
     * its Mother and Father steps this way, from {@code HTML} questions with a constant value, which
     * is the idiom the warning is there to retire.
     */
    private static void displayOnlySource(List<ValidationIssue> issues, Relationship r, String tok,
                                          Map<Integer, Question> questions, Map<Integer, SectionsQuestion> sectionsQuestions) {
        SectionsQuestion up = sectionsQuestions.get(r.upstreamSqId);
        Question upstream = up == null ? null : questions.get(up.questionId);
        if (upstream == null || Tokens.collectsAnswer(upstream.questionType)) {
            return;
        }
        String type = upstream.questionType == null ? "display-only" : upstream.questionType.name;
        String rule = "Rule '" + r.description + "'";
        if (r.defaultUpstreamValue == null || r.defaultUpstreamValue.isBlank()) {
            issues.add(ValidationIssue.error("relationships", r.relationshipId, rule + " fills {" + tok
                    + "} from " + label(upstream) + ", a display-only " + type
                    + " question that collects no answer, and carries no value of its own, so nothing ever fills {"
                    + tok + "}"));
        } else {
            issues.add(ValidationIssue.warning("relationships", r.relationshipId, rule + " fills {" + tok
                    + "} from " + label(upstream) + ", a display-only " + type
                    + " question that collects no answer; only the rule's own value fills the token."
                    + " Read it from a question the respondent answers"));
        }
    }

    /**
     * UC-007 BR-007: a rule's token is a name, not a reference.
     *
     * <p>The runtime keys a text's token values on {@code relationship.token} exactly as the column
     * holds it, and asks for the name it parsed out of a reference. So a token stored {@code <S1>} is
     * asked for as {@code S1}, never matches, and every placeholder meant to use it falls back to its
     * default forever -- with the rule reading correctly in the designer (Author#21). A token holding
     * a character a reference cannot spell is unnameable the same way.</p>
     *
     * <p>A name that is merely not the canonical spelling still fills, because every match ignores
     * case, so it is a warning rather than an error: a survey that runs today must not be blocked from
     * export over spelling. What it costs is the survey's token list, which then offers two names for
     * one slot.</p>
     *
     * <p>{@link com.elicitsoftware.author.survey.RuleService} canonicalizes on the way in, so what
     * this reports is what an import (UC-005) or an external draft (UC-042) wrote into the column
     * directly.</p>
     *
     * @return true when no text could ever name the token, so the checks that ask which texts use it
     *         would only read the same defect back out once per text
     */
    private static boolean tokenSpelling(List<ValidationIssue> issues, Relationship r, String tok) {
        if (Tokens.TOKEN_NAME.matcher(tok).matches()) {
            return false; // already the name alone, upper case: nothing to say
        }
        String name = Tokens.canonical(tok);
        String rule = "Rule '" + r.description + "'";
        if (!Tokens.bare(tok).equals(tok)) {
            issues.add(ValidationIssue.error("relationships", r.relationshipId, rule + " carries the token "
                    + tok + ", written with the angle brackets a text uses to name it. A phrase naming "
                    + tok + " asks for the token " + Tokens.bare(tok) + ", and " + tok + " is not "
                    + Tokens.bare(tok) + ", so nothing this rule reaches ever fills. Write " + name));
            return true;
        }
        if (!Tokens.nameable(tok)) {
            issues.add(ValidationIssue.error("relationships", r.relationshipId, rule + " carries the token "
                    + tok + ", which no text can name: a token name holds letters, digits and underscores"
                    + " only, so nothing this rule reaches ever fills"));
            return true;
        }
        if (!Tokens.TOKEN_NAME.matcher(name).matches()) {
            issues.add(ValidationIssue.warning("relationships", r.relationshipId, rule + " carries the token "
                    + tok + ", which does not start with a letter. It still fills "
                    + Tokens.reference(tok) + ", but a token name reads as a name: rename it to start with"
                    + " a letter"));
            return false;
        }
        issues.add(ValidationIssue.warning("relationships", r.relationshipId, rule + " carries the token "
                + tok + " rather than " + name + ". It still fills " + Tokens.reference(tok)
                + ", because the runtime matches a token's name without regard to case, but the survey's"
                + " token list then offers " + tok + " and " + name + " as two names for one slot. Write "
                + name));
        return false;
    }

    /**
     * UC-007 BR-005: a placeholder that names its token as a bare word rather than as {@code <TOKEN>}
     * fills nothing, so the respondent always reads its default and nothing about the text looks
     * wrong. This is the shape every placeholder had before the brackets, so it is what an imported
     * definition written against an older Survey carries (ElicitSoftware/FHHS#54).
     */
    private static void undelimitedTokens(List<ValidationIssue> issues, Map<Integer, Step> steps,
                                          Map<Integer, Section> sections, Map<Integer, Question> questions,
                                          List<Relationship> relationships) {
        Set<String> known = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Relationship r : relationships) {
            if (r.token != null && !r.token.isBlank()) {
                known.add(r.token.trim());
            }
        }
        steps.values().forEach(s -> known.addAll(Tokens.names(s.name)));
        sections.values().forEach(s -> known.addAll(Tokens.names(s.name)));
        questions.values().forEach(q -> known.addAll(Tokens.names(q.text)));
        if (known.isEmpty()) {
            return;
        }
        for (Step s : steps.values()) {
            bareTokens(issues, "steps", s.stepId, "Step '" + s.name + "'", s.name, known);
        }
        for (Section s : sections.values()) {
            bareTokens(issues, "sections", s.sectionId, "Section '" + s.name + "'", s.name, known);
        }
        for (Question q : questions.values()) {
            bareTokens(issues, "questions", q.questionId, label(q), q.text, known);
        }
        for (Relationship r : relationships) {
            bareTokens(issues, "relationships", r.relationshipId, "Rule '" + r.description + "'",
                    r.defaultUpstreamValue, known);
        }
    }

    private static void bareTokens(List<ValidationIssue> issues, String kind, Integer id, String where,
                                   String text, Set<String> known) {
        for (Tokens.Placeholder ph : Tokens.placeholders(text)) {
            Set<String> bare = Tokens.bareTokenNames(ph.text(), known);
            if (bare.isEmpty()) {
                continue;
            }
            String suggestion = ph.text();
            for (String name : bare) {
                suggestion = suggestion.replaceAll("(?<![A-Za-z0-9_])" + java.util.regex.Pattern.quote(name)
                        + "(?![A-Za-z0-9_>])", java.util.regex.Matcher.quoteReplacement(Tokens.reference(name)));
            }
            issues.add(ValidationIssue.error(kind, id, where + " uses " + ph.text() + ", which names the token"
                    + (bare.size() == 1 ? " " : "s ") + String.join(", ", bare)
                    + " without angle brackets, so no rule fills it"
                    + (ph.defaultText() == null ? "" : " and respondents always read \"" + ph.defaultText() + "\"")
                    + ". Write " + suggestion));
        }
    }

    private static void unfilled(List<ValidationIssue> issues, String kind, Integer id, String where, Tokens.Placeholder ph) {
        if (ph.defaultText() != null) {
            issues.add(ValidationIssue.warning(kind, id, where + " uses " + ph.text() + " but no rule fills it there, so respondents always read the default"));
        } else {
            issues.add(ValidationIssue.error(kind, id, where + " uses " + ph.text() + " without a default and no rule fills it there"));
        }
    }

    /** Whether the token appears in the text of the rule's target or of anything inside it. */
    private static boolean reachUses(Relationship r, String tok, Map<Integer, Step> steps, Map<Integer, Section> sections,
                                     Map<Integer, Question> questions, Map<Integer, StepsSections> stepsSections,
                                     Map<Integer, SectionsQuestion> sectionsQuestions, Map<Integer, List<Question>> questionsBySection) {
        if (r.downstreamSqId != null) {
            SectionsQuestion sq = sectionsQuestions.get(r.downstreamSqId);
            Question q = sq == null ? null : questions.get(sq.questionId);
            return q != null && Tokens.uses(q.text, tok);
        }
        if (r.downstreamSsId != null) {
            StepsSections ss = stepsSections.get(r.downstreamSsId);
            return ss != null && sectionUses(ss.sectionId, tok, sections, questionsBySection);
        }
        Step step = steps.get(r.downstreamStepId);
        if (step != null && Tokens.uses(step.name, tok)) {
            return true;
        }
        return stepsSections.values().stream()
                .anyMatch(ss -> ss.stepId.equals(r.downstreamStepId) && sectionUses(ss.sectionId, tok, sections, questionsBySection));
    }

    private static boolean sectionUses(Integer sectionId, String tok, Map<Integer, Section> sections, Map<Integer, List<Question>> questionsBySection) {
        Section sec = sections.get(sectionId);
        if (sec != null && Tokens.uses(sec.name, tok)) {
            return true;
        }
        return questionsBySection.getOrDefault(sectionId, List.of()).stream().anyMatch(q -> Tokens.uses(q.text, tok));
    }

    /** One warning per published language that is incomplete or out of date. */
    private void translationGaps(List<ValidationIssue> issues, Survey survey) {
        for (String language : translations.contentLanguages(survey)) {
            TranslationService.Counts counts = translations.counts(survey.id, language);
            if (counts.missing() > 0) {
                issues.add(ValidationIssue.warning("translations", survey.id,
                        language + " is published but " + counts.missing()
                                + " string(s) are not translated; they will show in the base language"));
            }
            if (counts.stale() > 0) {
                issues.add(ValidationIssue.warning("translations", survey.id,
                        counts.stale() + " translation(s) in " + language
                                + " are out of date and will not be shown until they are translated again"));
            }
        }
    }

    private static String label(Question q) {
        return "Question '" + (q.shortText != null && !q.shortText.isBlank() ? q.shortText : "#" + q.questionId) + "'";
    }
}
