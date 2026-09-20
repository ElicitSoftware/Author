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
import com.elicitsoftware.author.ConstraintViolationTranslator;
import com.elicitsoftware.model.Question;
import com.elicitsoftware.model.SelectGroup;
import com.elicitsoftware.model.SelectItem;
import com.elicitsoftware.model.StructuralElement;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Lists of options (UC-014): a survey's select groups and their items, edited as working copies.
 * A list is saved whole — group fields plus the ordered options — so one dialog Save creates,
 * updates in place, retires or renumbers whatever changed (A1).
 */
@ApplicationScoped
public class ListService {

    /** One option as the dialog edits it; {@code selectItemId} is null for a new option. */
    public record OptionDraft(Integer selectItemId, String displayText, String codedValue) { }

    @Inject
    EntityManager em;

    @Inject
    ElementService elements;

    public List<SelectGroup> lists(int surveyId) {
        return SelectGroup.list("surveyId = ?1 and effectiveTo = ?2 order by name", surveyId, StructuralElement.SENTINEL);
    }

    public SelectGroup list(int surveyId, int selectGroupId) {
        return elements.selectGroup(surveyId, selectGroupId).filter(StructuralElement::isCurrent)
                .orElseThrow(() -> new AuthoringException("The list no longer exists"));
    }

    public List<SelectItem> options(int surveyId, int selectGroupId) {
        return SelectItem.list("surveyId = ?1 and selectGroupId = ?2 and effectiveTo = ?3 order by displayOrder",
                surveyId, selectGroupId, StructuralElement.SENTINEL);
    }

    /** Current questions using the list. */
    public long usedBy(int surveyId, int selectGroupId) {
        return Question.count("surveyId = ?1 and selectGroupId = ?2 and effectiveTo = ?3", surveyId, selectGroupId, StructuralElement.SENTINEL);
    }

    /** Option display texts per list, for the panel's search. */
    public Map<Integer, List<String>> optionTexts(int surveyId) {
        return SelectItem.<SelectItem>list("surveyId = ?1 and effectiveTo = ?2 order by displayOrder", surveyId, StructuralElement.SENTINEL).stream()
                .collect(Collectors.groupingBy(i -> i.selectGroupId, Collectors.mapping(i -> i.displayText == null ? "" : i.displayText, Collectors.toList())));
    }

    /** Option counts per list, for the panel. */
    public Map<Integer, Long> optionCounts(int surveyId) {
        return SelectItem.<SelectItem>list("surveyId = ?1 and effectiveTo = ?2", surveyId, StructuralElement.SENTINEL).stream()
                .collect(Collectors.groupingBy(i -> i.selectGroupId, Collectors.counting()));
    }

    /** UC-014 step 5: a new list with its options in the given order. */
    @Transactional
    public SelectGroup create(int surveyId, SelectGroup draft, List<OptionDraft> options) {
        validate(draft);
        List<OptionDraft> clean = validateOptions(options);
        SelectGroup group = new SelectGroup();
        group.surveyId = surveyId;
        copy(draft, group);
        elements.create(group);
        int order = 1;
        for (OptionDraft o : clean) {
            createOption(surveyId, group.selectGroupId, o, order++);
        }
        return group;
    }

    /** UC-014 A1: fields and existing options in place, removed options retired, new ones created, order renumbered. */
    @Transactional
    public SelectGroup update(int surveyId, SelectGroup edited, List<OptionDraft> options) {
        validate(edited);
        List<OptionDraft> clean = validateOptions(options);
        SelectGroup group = list(surveyId, edited.selectGroupId);
        copy(edited, group);
        try {
            em.flush();
        } catch (RuntimeException e) {
            throw ConstraintViolationTranslator.translate(e);
        }
        Map<Integer, SelectItem> existing = options(surveyId, group.selectGroupId).stream()
                .collect(Collectors.toMap(i -> i.selectItemId, Function.identity()));
        Set<Integer> kept = new HashSet<>();
        int order = 1;
        for (OptionDraft o : clean) {
            SelectItem item = o.selectItemId() == null ? null : existing.get(o.selectItemId());
            if (item == null) {
                createOption(surveyId, group.selectGroupId, o, order++);
            } else {
                item.displayText = o.displayText();
                item.codedValue = o.codedValue();
                item.displayOrder = order++;
                kept.add(item.selectItemId);
            }
        }
        OffsetDateTime now = OffsetDateTime.now();
        existing.values().stream().filter(i -> !kept.contains(i.selectItemId)).forEach(i -> i.effectiveTo = now);
        try {
            em.flush();
        } catch (RuntimeException e) {
            throw ConstraintViolationTranslator.translate(e);
        }
        return group;
    }

    /** UC-014 A2: refused while a current question uses the list (ElementService rule). */
    @Transactional
    public void remove(int surveyId, int selectGroupId) {
        elements.removeSelectGroup(surveyId, selectGroupId);
    }

    private void createOption(int surveyId, int selectGroupId, OptionDraft o, int order) {
        SelectItem item = new SelectItem();
        item.surveyId = surveyId;
        item.selectGroupId = selectGroupId;
        item.displayText = o.displayText();
        item.codedValue = o.codedValue();
        item.displayOrder = order;
        elements.create(item);
    }

    private static void copy(SelectGroup from, SelectGroup to) {
        to.name = from.name.trim();
        to.description = from.description == null || from.description.isBlank() ? null : from.description.trim();
        to.dataType = from.dataType == null || from.dataType.isBlank() ? "Text" : from.dataType.trim();
    }

    static void validate(SelectGroup g) {
        if (g.name == null || g.name.isBlank()) {
            throw new AuthoringException("Name is required", "name");
        }
        if (g.name.length() > 255) {
            throw new AuthoringException("Name must be 255 characters or fewer", "name");
        }
        if (g.description != null && g.description.length() > 255) {
            throw new AuthoringException("Description must be 255 characters or fewer", "description");
        }
        if (g.dataType != null && g.dataType.length() > 50) {
            throw new AuthoringException("Data type must be 50 characters or fewer", "dataType");
        }
    }

    /** UC-014 A4 / BR-002: display text required and unique within the list; blank coded value = display text. */
    static List<OptionDraft> validateOptions(List<OptionDraft> options) {
        List<OptionDraft> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        if (options == null) {
            return out;
        }
        for (OptionDraft o : options) {
            String text = o.displayText() == null ? "" : o.displayText().trim();
            if (text.isEmpty()) {
                throw new AuthoringException("Every option needs its display text", "options");
            }
            if (text.length() > 255) {
                throw new AuthoringException("Option text must be 255 characters or fewer", "options");
            }
            if (!seen.add(text.toLowerCase(Locale.ROOT))) {
                throw new AuthoringException("Option '" + text + "' appears twice", "options");
            }
            String coded = o.codedValue() == null || o.codedValue().isBlank() ? text : o.codedValue().trim();
            if (coded.length() > 255) {
                throw new AuthoringException("Coded value must be 255 characters or fewer", "options");
            }
            out.add(new OptionDraft(o.selectItemId(), text, coded));
        }
        return out;
    }

    static boolean same(OptionDraft a, SelectItem b) {
        return Objects.equals(a.displayText(), b.displayText) && Objects.equals(a.codedValue(), b.codedValue);
    }
}
