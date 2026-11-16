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
import com.elicitsoftware.author.i18n.Translations;
import com.elicitsoftware.author.survey.RepeatSource;
import com.elicitsoftware.author.survey.RenameLedger;
import com.elicitsoftware.author.survey.ReportingChangeLedger;
import com.elicitsoftware.author.survey.ReportingService;
import com.elicitsoftware.author.survey.RuleOrdering;
import com.elicitsoftware.author.survey.RulePath;
import com.elicitsoftware.author.survey.StructureNode;
import com.elicitsoftware.author.survey.SurveyService;
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
 * could write between angle brackets (BR-007), empty steps and sections, sections in no step and
 * questions in no section (BR-010), questions outside the first step that no Show or Repeat rule builds
 * (BR-012), duplicate ordering. Errors block export; warnings do not.
 * Read-only (UC-007 BR-004).
 *
 * <p>Every finding is worded for the author and resolved through the language files (UC-007 BR-013 /
 * FR-066): the sentence is a {@code validation.*} key, and only what the author wrote — a name, a
 * placeholder, a token — is passed into it.</p>
 */
@ApplicationScoped
public class ExportValidation {

    @Inject
    RenameLedger renames;

    @Inject
    ReportingService reporting;

    @Inject
    com.elicitsoftware.author.survey.ReportMarkService reportMarks;

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
            issues.add(ValidationIssue.warning("surveys", sid, Translations.get("validation.survey.noSteps")));
        }
        // BR-015: the details form refuses these on save; an imported or drafted survey never passed it.
        if (!SurveyService.singleLine(survey.name)) {
            issues.add(ValidationIssue.error("surveys", sid, Translations.get("validation.survey.nameSingleLine",
                    Translations.get("surveyMetadataDialog.editSurvey"))));
        }
        if (!SurveyService.singleLine(survey.title)) {
            issues.add(ValidationIssue.error("surveys", sid, Translations.get("validation.survey.titleSingleLine",
                    Translations.get("surveyMetadataDialog.editSurvey"))));
        }

        Set<Integer> stepsWithSections = new HashSet<>();
        Set<Integer> sectionsWithQuestions = new HashSet<>();
        Set<Integer> mountedSections = new HashSet<>();
        Set<Integer> assignedQuestions = new HashSet<>();
        Set<Integer> groupsWithItems = new HashSet<>();
        items.forEach(i -> groupsWithItems.add(i.selectGroupId));

        for (StepsSections ss : stepsSections.values()) {
            if (!steps.containsKey(ss.stepId)) {
                issues.add(ValidationIssue.error("steps_sections", ss.stepsSectionsId, Translations.get("validation.mount.stepRemoved")));
            }
            if (!sections.containsKey(ss.sectionId)) {
                issues.add(ValidationIssue.error("steps_sections", ss.stepsSectionsId, Translations.get("validation.mount.sectionRemoved")));
            }
            stepsWithSections.add(ss.stepId);
            mountedSections.add(ss.sectionId);
        }
        for (SectionsQuestion sq : sectionsQuestions.values()) {
            if (!questions.containsKey(sq.questionId)) {
                issues.add(ValidationIssue.error("sections_questions", sq.sectionsQuestionId, Translations.get("validation.assignment.questionRemoved")));
            }
            if (!sections.containsKey(sq.sectionId)) {
                issues.add(ValidationIssue.error("sections_questions", sq.sectionsQuestionId, Translations.get("validation.assignment.sectionRemoved")));
            }
            sectionsWithQuestions.add(sq.sectionId);
            assignedQuestions.add(sq.questionId);
        }
        for (Question q : questions.values()) {
            String type = q.questionType == null ? "" : q.questionType.name;
            if (q.selectGroupId != null && !groups.containsKey(q.selectGroupId)) {
                issues.add(ValidationIssue.error("questions", q.questionId, Translations.get("validation.question.listRemoved", name(q))));
            } else if (CHOICE_TYPES.contains(type)) {
                if (q.selectGroupId == null) {
                    issues.add(ValidationIssue.error("questions", q.questionId, Translations.get("validation.question.noList", name(q), type)));
                } else if (!groupsWithItems.contains(q.selectGroupId)) {
                    issues.add(ValidationIssue.error("questions", q.questionId, Translations.get("validation.question.emptyList", name(q))));
                }
            }
            if (q.text == null || q.text.isBlank()) {
                issues.add(ValidationIssue.error("questions", q.questionId, Translations.get("validation.question.noText", name(q))));
            }
        }
        for (SelectItem i : items) {
            if (!groups.containsKey(i.selectGroupId)) {
                issues.add(ValidationIssue.error("select_items", i.selectItemId, Translations.get("validation.option.listRemoved", i.displayText)));
            }
        }
        Map<Integer, String> sectionNames = new HashMap<>();
        sections.values().forEach(s -> sectionNames.put(s.sectionId, s.name));
        Map<Integer, String> questionNames = new HashMap<>();
        questions.values().forEach(q -> questionNames.put(q.questionId, RuleOrdering.questionLabel(q)));
        RuleOrdering.Board board = new RuleOrdering.Board(steps.values(), stepsSections.values(), sectionsQuestions.values(), sectionNames, questionNames);
        for (Relationship r : relationships) {
            String rule = rule(r);
            // UC-019 BR-001: the downstream path — step, section in it, question in that — must hold together.
            RulePath.problem(r, board, rule).ifPresent(p -> issues.add(ValidationIssue.error("relationships", r.relationshipId, p)));
            if (!sectionsQuestions.containsKey(r.upstreamSqId)) {
                issues.add(ValidationIssue.error("relationships", r.relationshipId, Translations.get("validation.rule.readsRemovedQuestion", rule)));
            }
            if (r.upstreamStepId != null && !steps.containsKey(r.upstreamStepId)) {
                issues.add(ValidationIssue.error("relationships", r.relationshipId, Translations.get("validation.rule.readsRemovedStep", rule)));
            }
            if (r.downstreamStepId != null && !steps.containsKey(r.downstreamStepId)) {
                issues.add(ValidationIssue.error("relationships", r.relationshipId, Translations.get("validation.rule.targetsRemovedStep", rule)));
            }
            if (r.downstreamSsId != null && !stepsSections.containsKey(r.downstreamSsId)) {
                issues.add(ValidationIssue.error("relationships", r.relationshipId, Translations.get("validation.rule.targetsRemovedSection", rule)));
            }
            if (r.downstreamSqId != null && !sectionsQuestions.containsKey(r.downstreamSqId)) {
                issues.add(ValidationIssue.error("relationships", r.relationshipId, Translations.get("validation.rule.targetsRemovedQuestion", rule)));
            }
            if (r.operatorType == null || r.actionType == null) {
                issues.add(ValidationIssue.error("relationships", r.relationshipId, Translations.get("validation.rule.incomplete", rule)));
            }
            // UC-007 BR-014: a Repeat reading a multi-select with an operator that never holds for a
            // selection repeats nothing, for anyone. The rule dialog refuses it (UC-019 A6); an imported
            // or drafted definition never passed through the dialog.
            if (r.actionType != null && "REPEAT".equals(r.actionType.name) && RepeatSource.neverHoldsForASelection(r.operatorType)) {
                SectionsQuestion upstream = sectionsQuestions.get(r.upstreamSqId);
                Question read = upstream == null ? null : questions.get(upstream.questionId);
                if (read != null && RepeatSource.perItem(read.questionType)) {
                    issues.add(ValidationIssue.error("relationships", r.relationshipId,
                            Translations.get("validation.rule.repeatSelectionOperator", rule, r.operatorType.name)));
                }
            }
            // UC-033 A3: a rule the respondent can never see fire blocks export (UC-007 BR-001).
            board.problem(r).ifPresent(p -> issues.add(ValidationIssue.error("relationships", r.relationshipId, Translations.get("validation.rule.backwards", rule, p))));
        }
        checkTokens(issues, steps, sections, questions, stepsSections, sectionsQuestions, relationships, board);
        justInTime(issues, steps, sections, questions, stepsSections, sectionsQuestions, relationships, board);
        for (Step s : steps.values()) {
            if (!stepsWithSections.contains(s.stepId)) {
                issues.add(ValidationIssue.warning("steps", s.stepId,
                        Translations.get("validation.step.empty", StructureNode.nameOr(s.name, s.stepId))));
            }
        }
        for (Section s : sections.values()) {
            String name = StructureNode.nameOr(s.name, s.sectionId);
            // UC-007 BR-010: a section out of the structure is reported as that, and not also as
            // empty -- the emptiness is downstream of the real defect, and mounting it is the fix.
            if (!mountedSections.contains(s.sectionId)) {
                issues.add(ValidationIssue.warning("sections", s.sectionId, Translations.get("validation.section.unplaced", name)));
            } else if (!sectionsWithQuestions.contains(s.sectionId)) {
                issues.add(ValidationIssue.warning("sections", s.sectionId, Translations.get("validation.section.empty", name)));
            }
        }
        for (Question q : questions.values()) {
            if (!assignedQuestions.contains(q.questionId)) {
                issues.add(ValidationIssue.warning("questions", q.questionId, Translations.get("validation.question.unplaced", name(q))));
            }
        }
        if (!stepsSections.isEmpty()) {
            // UC-016 BR-002: the runtime enters the survey at initial_display_key; it must name a current assignment.
            Set<String> keys = new HashSet<>();
            stepsSections.values().forEach(ss -> keys.add(ss.displayKey));
            if (survey.initialDisplayKey == null || !keys.contains(survey.initialDisplayKey)) {
                // The field and the dialog are named as the form labels them, in the reader's language (BR-013).
                issues.add(ValidationIssue.error("surveys", sid, Translations.get("validation.survey.startingPoint",
                        Translations.get("surveyMetadataDialog.initialDisplayKey"), Translations.get("surveyMetadataDialog.editSurvey"))));
            }
        }
        // UC-007 step 4 / UC-018 BR-001: renames since the last export are retroactive at deployed sites.
        renames.since(sid).forEach(r -> issues.add(ValidationIssue.warning(r.table(), r.durableId(), r.message())));
        // UC-022 BR-001: tag changes since the last export are retroactive at deployed sites too.
        reportingChanges.since(sid).forEach(c -> issues.add(ValidationIssue.warning(c.kind(), c.durableId(), c.message())));
        // UC-024 BR-002: a name the site's ETL would reject blocks the export; the rest are warnings.
        issues.addAll(reporting.impact(sid).problems());
        // UC-049 A3: a mark on a removed element is a warning.
        issues.addAll(reportMarks.problems(sid));
        duplicateOrders(issues, "steps", "validation.order.steps", steps.values().stream().map(s -> Map.entry(s.stepId, "step:" + s.displayOrder)).toList());
        duplicateOrders(issues, "steps_sections", "validation.order.sections", stepsSections.values().stream()
                .map(ss -> Map.entry(ss.stepsSectionsId, ss.stepId + ":" + ss.sectionDisplayOrder)).toList());
        duplicateOrders(issues, "sections_questions", "validation.order.questions", sectionsQuestions.values().stream()
                .map(sq -> Map.entry(sq.sectionsQuestionId, sq.sectionId + ":" + sq.displayOrder)).toList());
        // UC-043 / FR-060: warnings, never errors. An author may legitimately export a survey whose
        // translations are behind -- every one of those strings falls back to the base language at
        // the site -- but they should be reminded once more before the file leaves Author.
        translationGaps(issues, survey);
        return issues;
    }

    /**
     * UC-007 BR-012 / FR-064: only the first step may stand without a rule.
     *
     * <p>A respondent's first sign-in builds every element no rule controls -- {@code QuestionManager.init}
     * walks every mount of the survey and saves an answer row for each question that is not a rule's
     * downstream target -- so an uncontrolled question in a later step is created for every respondent
     * before the first page, whether they ever reach it or not. Everything after the first step has to
     * be built just in time, by a Show or Repeat rule. Text rules build nothing, so they do not count.</p>
     *
     * <p>A question is reached the way a token is (BR-005): by a rule on its step, on its section as
     * mounted there, or on the question itself through that mount or, in the upstream question's own
     * section, with no mount named. One missing rule is one finding: the step when nothing in it is
     * reached, else the mount when nothing in that is, else the question.</p>
     */
    private static void justInTime(List<ValidationIssue> issues, Map<Integer, Step> steps, Map<Integer, Section> sections,
                                   Map<Integer, Question> questions, Map<Integer, StepsSections> stepsSections,
                                   Map<Integer, SectionsQuestion> sectionsQuestions, List<Relationship> relationships,
                                   RuleOrdering.Board board) {
        Step first = steps.values().stream()
                .filter(s -> s.displayOrder != null)
                .min(java.util.Comparator.comparing((Step s) -> s.displayOrder).thenComparing(s -> s.stepId))
                .orElse(null);
        if (first == null) {
            return;
        }
        List<Relationship> builders = relationships.stream()
                .filter(r -> r.actionType != null && BUILDING_ACTIONS.contains(r.actionType.name))
                .toList();
        Map<Integer, List<SectionsQuestion>> bySection = new HashMap<>();
        for (SectionsQuestion sq : sectionsQuestions.values()) {
            if (questions.containsKey(sq.questionId)) {
                bySection.computeIfAbsent(sq.sectionId, k -> new ArrayList<>()).add(sq);
            }
        }
        for (Step step : steps.values()) {
            if (step.stepId.equals(first.stepId)) {
                continue;
            }
            Map<StepsSections, List<SectionsQuestion>> unreached = new java.util.LinkedHashMap<>();
            int total = 0;
            int missing = 0;
            for (StepsSections ss : stepsSections.values().stream()
                    .filter(m -> m.stepId.equals(step.stepId) && sections.containsKey(m.sectionId))
                    .sorted(java.util.Comparator.comparing(m -> m.sectionDisplayOrder == null ? java.math.BigDecimal.ZERO : m.sectionDisplayOrder))
                    .toList()) {
                List<SectionsQuestion> held = bySection.getOrDefault(ss.sectionId, List.of());
                List<SectionsQuestion> out = held.stream().filter(sq -> !reaches(builders, ss, sq)).toList();
                total += held.size();
                missing += out.size();
                unreached.put(ss, out);
            }
            if (missing == 0) {
                continue;
            }
            if (missing == total) {
                issues.add(ValidationIssue.error("steps", step.stepId, Translations.get("validation.step.noBuildingRule",
                        StructureNode.nameOr(step.name, step.stepId))));
                continue;
            }
            for (Map.Entry<StepsSections, List<SectionsQuestion>> e : unreached.entrySet()) {
                StepsSections ss = e.getKey();
                List<SectionsQuestion> out = e.getValue();
                if (out.isEmpty()) {
                    continue;
                }
                String stepName = board.stepLabel(step.stepId);
                if (out.size() == bySection.getOrDefault(ss.sectionId, List.of()).size()) {
                    issues.add(ValidationIssue.error("steps_sections", ss.stepsSectionsId, Translations.get(
                            "validation.section.noBuildingRule", board.sectionLabel(ss.sectionId), stepName)));
                    continue;
                }
                for (SectionsQuestion sq : out) {
                    issues.add(ValidationIssue.error("sections_questions", sq.sectionsQuestionId, Translations.get(
                            "validation.question.noBuildingRule", name(questions.get(sq.questionId)),
                            board.sectionLabel(ss.sectionId), stepName)));
                }
            }
        }
    }

    private static final Set<String> BUILDING_ACTIONS = Set.of("SHOW", "REPEAT");

    /** Whether a Show or Repeat rule reaches the question as it is shown in this mount (UC-019 BR-005). */
    private static boolean reaches(List<Relationship> builders, StepsSections ss, SectionsQuestion sq) {
        for (Relationship r : builders) {
            if (r.downstreamSqId != null) {
                if (r.downstreamSqId.equals(sq.sectionsQuestionId)
                        && (r.downstreamSsId == null || r.downstreamSsId.equals(ss.stepsSectionsId))) {
                    return true;
                }
            } else if (r.downstreamSsId != null) {
                if (r.downstreamSsId.equals(ss.stepsSectionsId)) {
                    return true;
                }
            } else if (ss.stepId.equals(r.downstreamStepId)) {
                return true;
            }
        }
        return false;
    }

    private static void duplicateOrders(List<ValidationIssue> issues, String kind, String messageKey,
                                        List<Map.Entry<Integer, String>> keyed) {
        Map<String, Integer> seen = new HashMap<>();
        for (Map.Entry<Integer, String> e : keyed) {
            Integer first = seen.putIfAbsent(e.getValue(), e.getKey());
            if (first != null) {
                issues.add(ValidationIssue.warning(kind, e.getKey(),
                        Translations.get(messageKey, e.getValue().replaceFirst("^[^:]*:", ""))));
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
     * UC-007 BR-005 / FR-005: every token a text uses must be filled by a rule that reaches that text — a
     * rule reaches its target and everything inside it (UC-019 BR-005) — and the token a rule of any
     * action carries must be used by a text it reaches. Every token, because filling a phrase is all or
     * nothing (BR-006): a phrase one of whose references nothing reaches fills no more than one where
     * nothing reaches any. A placeholder with no default that nothing fills is an error: the
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
                    unfilled(issues, "steps", s.stepId, Translations.get("validation.where.step", s.name), ph);
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
                    unfilled(issues, "sections", sec.sectionId,
                            Translations.get("validation.where.sectionInStep", sec.name, board.stepLabel(ss.stepId)), ph);
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
                    unfilled(issues, "questions", q.questionId,
                            Translations.get("validation.where.questionInSection", name(q), board.sectionLabel(sq.sectionId)), ph);
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
                issues.add(ValidationIssue.warning("relationships", r.relationshipId, Translations.get(
                        "validation.rule.tokenUnused", rule(r), tok, "{" + Tokens.reference(tok) + "}")));
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
            undefinedHere(issues, "steps", s.stepId, Translations.get("validation.where.step", s.name), s.name, defined);
        }
        for (Section s : sections.values()) {
            undefinedHere(issues, "sections", s.sectionId, Translations.get("validation.where.section", s.name), s.name, defined);
        }
        for (Question q : questions.values()) {
            undefinedHere(issues, "questions", q.questionId, Translations.get("validation.where.question", name(q)), q.text, defined);
        }
        for (Relationship r : relationships) {
            undefinedHere(issues, "relationships", r.relationshipId, rule(r),
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
                String outside = ref + "{…}";
                issues.add(ValidationIssue.error(kind, id, ph.defaultText() == null
                        ? Translations.get("validation.token.undefinedNoDefault", where, ref, ph.text(), name, outside)
                        : Translations.get("validation.token.undefined", where, ref, ph.text(), name, outside, ph.defaultText())));
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
        String type = upstream.questionType == null ? "?" : upstream.questionType.name;
        String slot = "{" + Tokens.reference(tok) + "}";
        // The field is named as the rule dialog labels it, in the reader's language (BR-013).
        String field = Translations.get("relationshipDialog.defaultUpstreamValue");
        if (r.defaultUpstreamValue == null || r.defaultUpstreamValue.isBlank()) {
            issues.add(ValidationIssue.error("relationships", r.relationshipId, Translations.get(
                    "validation.rule.displayOnlyNoValue", rule(r), slot, name(upstream), type, field)));
        } else {
            issues.add(ValidationIssue.warning("relationships", r.relationshipId, Translations.get(
                    "validation.rule.displayOnlyConstant", rule(r), slot, name(upstream), type, field)));
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
        String rule = rule(r);
        if (!Tokens.bare(tok).equals(tok)) {
            issues.add(ValidationIssue.error("relationships", r.relationshipId, Translations.get(
                    "validation.rule.tokenBracketed", rule, tok, name, "{" + Tokens.reference(name) + "}")));
            return true;
        }
        if (!Tokens.nameable(tok)) {
            issues.add(ValidationIssue.error("relationships", r.relationshipId,
                    Translations.get("validation.rule.tokenInvalid", rule, tok)));
            return true;
        }
        if (!Tokens.TOKEN_NAME.matcher(name).matches()) {
            issues.add(ValidationIssue.warning("relationships", r.relationshipId,
                    Translations.get("validation.rule.tokenLeadingDigit", rule, tok)));
            return false;
        }
        issues.add(ValidationIssue.warning("relationships", r.relationshipId,
                Translations.get("validation.rule.tokenCase", rule, tok, name)));
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
            bareTokens(issues, "steps", s.stepId, Translations.get("validation.where.step", s.name), s.name, known);
        }
        for (Section s : sections.values()) {
            bareTokens(issues, "sections", s.sectionId, Translations.get("validation.where.section", s.name), s.name, known);
        }
        for (Question q : questions.values()) {
            bareTokens(issues, "questions", q.questionId, Translations.get("validation.where.question", name(q)), q.text, known);
        }
        for (Relationship r : relationships) {
            bareTokens(issues, "relationships", r.relationshipId, rule(r),
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
            issues.add(ValidationIssue.error(kind, id, ph.defaultText() == null
                    ? Translations.get("validation.token.undelimited", where, ph.text(), suggestion)
                    : Translations.get("validation.token.undelimitedWithDefault", where, ph.text(), suggestion, ph.defaultText())));
        }
    }

    private static void unfilled(List<ValidationIssue> issues, String kind, Integer id, String where, Tokens.Placeholder ph) {
        if (ph.defaultText() != null) {
            issues.add(ValidationIssue.warning(kind, id,
                    Translations.get("validation.token.unfilledWithDefault", where, ph.text(), ph.defaultText())));
        } else {
            issues.add(ValidationIssue.error(kind, id, Translations.get("validation.token.unfilledNoDefault",
                    where, ph.text(), "{" + ph.phrase() + "|…}")));
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
                issues.add(ValidationIssue.warning("translations", survey.id, Translations.get(
                        counts.missing() == 1 ? "validation.translation.missing.one" : "validation.translation.missing.many",
                        language, String.valueOf(counts.missing()))));
            }
            if (counts.stale() > 0) {
                issues.add(ValidationIssue.warning("translations", survey.id, Translations.get(
                        counts.stale() == 1 ? "validation.translation.stale.one" : "validation.translation.stale.many",
                        language, String.valueOf(counts.stale()))));
            }
        }
    }

    /** What a finding calls a question: its short text, or its durable id when it has none (BR-011). */
    private static String name(Question q) {
        return q.shortText != null && !q.shortText.isBlank() ? q.shortText : "#" + q.questionId;
    }

    /** A rule as the subject of a finding. */
    private static String rule(Relationship r) {
        return Translations.get("validation.where.rule", r.description);
    }
}
