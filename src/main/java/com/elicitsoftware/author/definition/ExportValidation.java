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
 * questions without options, tokens no rule fills and Text rules nothing uses (BR-005), empty steps and
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
            if (!reachUses(r, tok, steps, sections, questions, stepsSections, sectionsQuestions, questionsBySection)) {
                issues.add(ValidationIssue.warning("relationships", r.relationshipId,
                        "Rule '" + r.description + "' fills {" + tok + "} but no text it reaches uses it"));
            }
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
