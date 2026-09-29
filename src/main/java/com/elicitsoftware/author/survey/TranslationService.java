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

import com.elicitsoftware.author.AuthoringException;
import com.elicitsoftware.model.Question;
import com.elicitsoftware.model.Section;
import com.elicitsoftware.model.SelectGroup;
import com.elicitsoftware.model.SelectItem;
import com.elicitsoftware.model.Step;
import com.elicitsoftware.model.StructuralElement;
import com.elicitsoftware.model.Survey;
import com.elicitsoftware.model.Translation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The translatable strings of a survey and their translations (UC-043).
 * <p>
 * Everything the Translations view and the hand-off file need: the rows to show, the counts to
 * report, and the two writes -- upsert and remove. Author holds one version-0 row per translation;
 * versions are created at each site when the console applies a changed file (Admin UC-017).
 * <p>
 * The one rule worth stating twice: a write always recomputes {@code source_hash} and
 * {@code source_text} from the element's <em>current</em> base text. A translation is a claim about
 * a particular wording, and that is what makes it verifiable later.
 */
@ApplicationScoped
public class TranslationService {

    @Inject
    EntityManager em;

    /**
     * A translation is created through the same path as every other element, so it gets its durable
     * id, its minted key and the version-0 lifecycle without restating any of it. (The two services
     * reference each other -- retirement cascades the other way -- which CDI resolves through the
     * normal-scoped proxies.)
     */
    @Inject
    ElementService elements;

    /** Status of one translatable string in one language. */
    public enum Status {
        /** A current translation whose hash matches the base text. */
        TRANSLATED,
        /** No current translation at all. */
        MISSING,
        /** A current translation made from base text that has since been edited. */
        STALE
    }

    /**
     * One translatable string of a survey, in one language.
     *
     * @param elementType  the table the element lives in, as the file names it
     * @param targetKey    the element's key
     * @param field        the base column being translated
     * @param context      where it sits, for a human: step, section, question, or list and option
     * @param baseText     the current base text
     * @param translation  the current translation, or {@code null}
     * @param previousBase the base text that translation was made from, when it is stale
     * @param status       translated, missing or stale
     */
    public record Row(String elementType, UUID targetKey, String field, String context,
                      String baseText, String translation, String previousBase, Status status) {

        /** A stable id for the grid and for the hand-off file. */
        public String id() {
            return targetKey + ":" + field;
        }
    }

    /** How one language stands. */
    public record Counts(int translated, int missing, int stale) {
        public int total() {
            return translated + missing + stale;
        }
    }

    /**
     * Every translatable string of the survey's current structure, in one language.
     *
     * @param surveyId the survey
     * @param language the BCP-47 tag being worked on
     * @return one row per (element, field), ordered as the survey reads
     */
    public List<Row> rows(int surveyId, String language) {
        Map<String, Translation> current = new LinkedHashMap<>();
        for (Translation t : Translation.<Translation>list(
                "surveyId = ?1 and language = ?2 and effectiveTo = ?3",
                surveyId, language, StructuralElement.SENTINEL)) {
            current.put(t.targetKey + ":" + t.field, t);
        }

        List<Row> rows = new ArrayList<>();
        Survey survey = Survey.findById(surveyId);
        if (survey != null) {
            add(rows, current, "surveys", survey.surveyKey, "title", survey.name, survey.title);
            add(rows, current, "surveys", survey.surveyKey, "description", survey.name, survey.description);
        }
        for (Step step : Step.<Step>list("surveyId = ?1 and effectiveTo = ?2 order by displayOrder",
                surveyId, StructuralElement.SENTINEL)) {
            String where = "Step " + step.name;
            add(rows, current, "steps", step.stepKey, "name", where, step.name);
            add(rows, current, "steps", step.stepKey, "description", where, step.description);
        }
        for (Section section : Section.<Section>list("surveyId = ?1 and effectiveTo = ?2 order by displayOrder",
                surveyId, StructuralElement.SENTINEL)) {
            String where = "Section " + section.name;
            add(rows, current, "sections", section.sectionKey, "name", where, section.name);
            add(rows, current, "sections", section.sectionKey, "description", where, section.description);
        }
        for (Question question : Question.<Question>list("surveyId = ?1 and effectiveTo = ?2 order by questionId",
                surveyId, StructuralElement.SENTINEL)) {
            String where = "Question " + (question.shortText == null ? "" : question.shortText);
            add(rows, current, "questions", question.questionKey, "text", where, question.text);
            add(rows, current, "questions", question.questionKey, "short_text", where, question.shortText);
            add(rows, current, "questions", question.questionKey, "tool_tip", where, question.toolTip);
            add(rows, current, "questions", question.questionKey, "placeholder", where, question.placeholder);
            add(rows, current, "questions", question.questionKey, "validation_text", where, question.validationText);
        }
        for (SelectItem item : SelectItem.<SelectItem>list("surveyId = ?1 and effectiveTo = ?2 order by selectGroupId, displayOrder",
                surveyId, StructuralElement.SENTINEL)) {
            add(rows, current, "select_items", item.selectItemKey, "display_text",
                    "List " + groupName(surveyId, item.selectGroupId), item.displayText);
        }
        // reports are in the whitelist and travel in the file, but Author models no report entity
        // yet (docs/entity_model.md, "Excluded from This Model"), so there is nothing to list here.
        return rows;
    }

    /** How the language stands across the whole survey. */
    public Counts counts(int surveyId, String language) {
        int translated = 0;
        int missing = 0;
        int stale = 0;
        for (Row row : rows(surveyId, language)) {
            switch (row.status()) {
                case TRANSLATED -> translated++;
                case MISSING -> missing++;
                case STALE -> stale++;
            }
        }
        return new Counts(translated, missing, stale);
    }

    /**
     * Writes one translation, creating it or overwriting the working copy in place.
     * <p>
     * The hash and the snapshot are taken from the element's base text as it stands now, never from
     * whatever the caller believes it to be: that is what makes an imported translation land
     * current when it was made from unchanged text and stale when it was not.
     *
     * @param surveyId    the survey
     * @param elementType the table the element lives in
     * @param targetKey   the element's key
     * @param field       the base column being translated
     * @param language    the BCP-47 tag
     * @param value       the translated text; blank removes the translation
     * @return the stored translation, or {@code null} when a blank value removed it
     */
    @Transactional
    public Translation upsert(int surveyId, String elementType, UUID targetKey, String field,
                              String language, String value) {
        if (!TranslatableFields.isTranslatable(elementType, field)) {
            throw new AuthoringException(elementType + "." + field + " carries no respondent-facing text");
        }
        String baseText = baseText(surveyId, elementType, targetKey, field);
        if (baseText == null) {
            throw new AuthoringException("The element this translation belongs to no longer has that text");
        }
        Translation existing = current(surveyId, targetKey, field, language);
        if (value == null || value.isBlank()) {
            if (existing != null) {
                remove(existing);
            }
            return null;
        }
        if (existing == null) {
            Translation created = new Translation();
            created.surveyId = surveyId;
            created.elementType = elementType;
            created.targetKey = targetKey;
            created.field = field;
            created.language = language;
            // Every column is set before the insert: value and source_hash are NOT NULL.
            created.value = value;
            created.sourceHash = ContentHash.of(baseText);
            created.sourceText = baseText;
            return elements.create(created);
        }
        existing.value = value;
        existing.sourceHash = ContentHash.of(baseText);
        existing.sourceText = baseText;
        em.flush();
        return existing;
    }

    /**
     * Retires one translation, so the string falls back to the base language (UC-043 A2). Closed,
     * never emptied: a site must be able to tell "no translation" from "translated to nothing".
     */
    @Transactional
    public void remove(int surveyId, UUID targetKey, String field, String language) {
        Translation existing = current(surveyId, targetKey, field, language);
        if (existing != null) {
            remove(existing);
        }
    }

    private void remove(Translation translation) {
        translation.effectiveTo = OffsetDateTime.now();
        em.flush();
    }

    /**
     * Retires every current translation of an element, at the instant it was retired (UC-043 A3).
     * Called from {@code ElementService}: nothing else links a translation to its element, so a
     * retirement that forgot this would leave a live translation of an unreachable string.
     *
     * @param surveyId the survey
     * @param key      the retired element's key
     * @param at       the instant the element was closed at, so restore can find them again
     * @return how many translations were closed
     */
    @Transactional
    public int retireTranslationsOf(int surveyId, UUID key, OffsetDateTime at) {
        List<Translation> live = Translation.list("surveyId = ?1 and targetKey = ?2 and effectiveTo = ?3",
                surveyId, key, StructuralElement.SENTINEL);
        live.forEach(t -> t.effectiveTo = at);
        em.flush();
        return live.size();
    }

    /** Reopens the translations closed with an element at {@code at} (UC-010). */
    @Transactional
    public int restoreTranslationsOf(int surveyId, UUID key, OffsetDateTime at) {
        List<Translation> closed = Translation.list("surveyId = ?1 and targetKey = ?2 and effectiveTo = ?3",
                surveyId, key, at);
        closed.forEach(t -> t.effectiveTo = StructuralElement.SENTINEL);
        em.flush();
        return closed.size();
    }

    /**
     * The languages a wording change would invalidate: the current translations of one field whose
     * hash matches the text as it stands <em>before</em> the change (UC-043 A1, FR-060).
     *
     * @param surveyId  the survey
     * @param targetKey the element's key
     * @param field     the base column about to change
     * @return the tags whose translations become stale, in order
     */
    public List<String> languagesAboutToGoStale(int surveyId, UUID targetKey, String field) {
        return Translation.<Translation>list("surveyId = ?1 and targetKey = ?2 and field = ?3 and effectiveTo = ?4",
                        surveyId, targetKey, field, StructuralElement.SENTINEL)
                .stream().map(t -> t.language).sorted().toList();
    }

    /** The survey's published set, as a list. */
    public List<String> contentLanguages(Survey survey) {
        if (survey == null || survey.contentLanguages == null || survey.contentLanguages.isBlank()) {
            return List.of();
        }
        return Arrays.stream(survey.contentLanguages.split(","))
                .map(String::trim).filter(t -> !t.isEmpty()).sorted(Comparator.naturalOrder()).toList();
    }

    /**
     * Every language this survey can be worked on (UC-043 step 2): the ones it publishes, then the
     * common ones it does not, never the base language.
     * <p>
     * Offering a language the survey does not publish yet is what lets the Translations page start
     * one. Nothing here needs the published set -- {@link #rows}, {@link #counts} and
     * {@link #upsert} all work for any tag -- but choosing one publishes it, so that the work
     * reaches every site at the next export (UC-044).
     *
     * @param survey the survey
     * @return canonical tags in offer order
     */
    public List<String> offerableLanguages(Survey survey) {
        return survey == null
                ? List.of()
                : LanguageTags.offerable(survey.baseLanguage, contentLanguages(survey));
    }

    private Translation current(int surveyId, UUID targetKey, String field, String language) {
        return Translation.<Translation>find(
                        "surveyId = ?1 and targetKey = ?2 and field = ?3 and language = ?4 and effectiveTo = ?5",
                        surveyId, targetKey, field, language, StructuralElement.SENTINEL)
                .firstResult();
    }

    private void add(List<Row> rows, Map<String, Translation> current, String elementType, UUID key,
                     String field, String context, String baseText) {
        if (key == null || baseText == null || baseText.isBlank()) {
            return;
        }
        Translation t = current.get(key + ":" + field);
        Status status;
        if (t == null) {
            status = Status.MISSING;
        } else if (ContentHash.matches(t.sourceHash, baseText)) {
            status = Status.TRANSLATED;
        } else {
            status = Status.STALE;
        }
        rows.add(new Row(elementType, key, field, context, baseText,
                t == null ? null : t.value, t == null ? null : t.sourceText, status));
    }

    /** The element's base text as it stands now, or {@code null} when it has none. */
    String baseText(int surveyId, String elementType, UUID key, String field) {
        return switch (elementType) {
            case "surveys" -> {
                Survey s = Survey.findById(surveyId);
                yield s == null ? null : switch (field) {
                    case "title" -> s.title;
                    case "description" -> s.description;
                    default -> null;
                };
            }
            case "steps" -> Step.<Step>find("surveyId = ?1 and stepKey = ?2 and effectiveTo = ?3",
                            surveyId, key, StructuralElement.SENTINEL).firstResultOptional()
                    .map(s -> "name".equals(field) ? s.name : s.description).orElse(null);
            case "sections" -> Section.<Section>find("surveyId = ?1 and sectionKey = ?2 and effectiveTo = ?3",
                            surveyId, key, StructuralElement.SENTINEL).firstResultOptional()
                    .map(s -> "name".equals(field) ? s.name : s.description).orElse(null);
            case "questions" -> Question.<Question>find("surveyId = ?1 and questionKey = ?2 and effectiveTo = ?3",
                            surveyId, key, StructuralElement.SENTINEL).firstResultOptional()
                    .map(q -> switch (field) {
                        case "text" -> q.text;
                        case "short_text" -> q.shortText;
                        case "tool_tip" -> q.toolTip;
                        case "placeholder" -> q.placeholder;
                        case "validation_text" -> q.validationText;
                        default -> null;
                    }).orElse(null);
            case "select_items" -> SelectItem.<SelectItem>find("surveyId = ?1 and selectItemKey = ?2 and effectiveTo = ?3",
                            surveyId, key, StructuralElement.SENTINEL).firstResultOptional()
                    .map(i -> i.displayText).orElse(null);
            // "reports" is deliberately absent: Author has no report entity to read a base text
            // from, so a report translation cannot be written here (only carried through the file).
            default -> null;
        };
    }

    private String groupName(int surveyId, Integer selectGroupId) {
        return SelectGroup.<SelectGroup>find("surveyId = ?1 and selectGroupId = ?2 and effectiveTo = ?3",
                        surveyId, selectGroupId, StructuralElement.SENTINEL)
                .firstResultOptional().map(g -> g.name).orElse("");
    }
}
