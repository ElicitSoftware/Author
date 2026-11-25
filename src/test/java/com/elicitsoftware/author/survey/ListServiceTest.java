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

import com.elicitsoftware.author.AuthoringException;
import com.elicitsoftware.author.survey.ListService.OptionDraft;
import com.elicitsoftware.model.Question;
import com.elicitsoftware.model.SelectGroup;
import com.elicitsoftware.model.SelectItem;
import com.elicitsoftware.model.Survey;
import com.elicitsoftware.test.PostgresTestResource;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** UC-014 Manage Select Groups and Items. */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class ListServiceTest {

    @Inject
    ListService service;

    @Inject
    DesignerService designer;

    private Survey fixture() {
        return Survey.find("name", "AuthorFixture").firstResult();
    }

    private SelectGroup yesNo(Survey s) {
        return service.lists(s.id).stream().filter(g -> g.name.equals("FixtureYesNo")).findFirst().orElseThrow();
    }

    private static SelectGroup draft(String name) {
        SelectGroup g = new SelectGroup();
        g.name = name;
        g.dataType = "Text";
        return g;
    }

    @Test
    void fixtureListIsListedWithItsOptions() {
        Survey s = fixture();
        SelectGroup g = yesNo(s);
        assertEquals(List.of("Yes", "No"), service.options(s.id, g.selectGroupId).stream().map(i -> i.displayText).toList());
        assertEquals(2L, service.optionCounts(s.id).get(g.selectGroupId));
        assertEquals(1L, service.usedBy(s.id, g.selectGroupId), "FixtureQ3 uses it");
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "list.author", roles = {"elicit_author"})
    void create_mintsKeysAndOrdersOptions() {
        Survey s = fixture();
        SelectGroup g = service.create(s.id, draft("Colors"), List.of(
                new OptionDraft(null, "Red", "R"), new OptionDraft(null, " Green ", ""), new OptionDraft(null, "Blue", "B")));
        assertNotNull(g.selectGroupKey, "C-017: key minted");
        assertEquals("Text", g.dataType);
        List<SelectItem> items = service.options(s.id, g.selectGroupId);
        assertEquals(List.of(1, 2, 3), items.stream().map(i -> i.displayOrder).toList(), "UC-014 BR-004");
        assertEquals("Green", items.get(1).displayText, "trimmed");
        assertEquals("Green", items.get(1).codedValue, "UC-014 BR-002: blank coded value is the text");
        assertNotNull(items.get(0).selectItemKey);
    }

    @Test
    @TestTransaction
    void create_refusesDuplicateNameEmptyOptionAndRepeatedOption() {
        Survey s = fixture();
        assertEquals("name", assertThrows(AuthoringException.class,
                () -> service.create(s.id, draft("FixtureYesNo"), List.of())).field(), "UC-014 A4 / BR-003");
        assertEquals("options", assertThrows(AuthoringException.class,
                () -> service.create(s.id, draft("Empty"), List.of(new OptionDraft(null, " ", null)))).field());
        assertEquals("options", assertThrows(AuthoringException.class,
                () -> service.create(s.id, draft("Twice"), List.of(new OptionDraft(null, "A", null), new OptionDraft(null, "a", null)))).field());
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "list.author", roles = {"elicit_author"})
    void update_editsInPlaceRetiresRemovedAndAddsNew() {
        Survey s = fixture();
        SelectGroup g = yesNo(s);
        List<SelectItem> before = service.options(s.id, g.selectGroupId);
        SelectItem yes = before.get(0);
        SelectItem no = before.get(1);
        SelectGroup edited = new SelectGroup();
        edited.selectGroupId = g.selectGroupId;
        edited.name = "FixtureYesNo";
        edited.description = "Yes, maybe";
        edited.dataType = "Text";
        service.update(s.id, edited, List.of(
                new OptionDraft(null, "Maybe", "M"),
                new OptionDraft(yes.selectItemId, "Yes!", "Y")));
        List<SelectItem> after = service.options(s.id, g.selectGroupId);
        assertEquals(List.of("Maybe", "Yes!"), after.stream().map(i -> i.displayText).toList(), "UC-014 A1: order as edited");
        assertEquals(List.of(1, 2), after.stream().map(i -> i.displayOrder).toList());
        assertEquals(yes.selectItemId, after.get(1).selectItemId, "existing option updated in place, same durable id");
        assertTrue(SelectItem.<SelectItem>findById(no.id).isRetired(), "removed option retired, not deleted");
        assertEquals("Yes, maybe", service.list(s.id, g.selectGroupId).description);
        Question q3 = Question.find("shortText", "FixtureQ3").firstResult();
        assertEquals(g.selectGroupId, q3.selectGroupId, "questions keep the list");
    }

    @Test
    @TestTransaction
    void remove_isRefusedWhileUsedAndRetiresOtherwise() {
        Survey s = fixture();
        SelectGroup g = yesNo(s);
        assertThrows(AuthoringException.class, () -> service.remove(s.id, g.selectGroupId), "UC-014 A2");
        SelectGroup spare = service.create(s.id, draft("Spare"), List.of(new OptionDraft(null, "X", null)));
        service.remove(s.id, spare.selectGroupId);
        assertTrue(service.lists(s.id).stream().noneMatch(l -> l.name.equals("Spare")));
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "list.author", roles = {"elicit_author"})
    void assignList_choiceTypeOnly() {
        Survey s = fixture();
        SelectGroup g = yesNo(s);
        Question q2 = Question.find("shortText", "FixtureQ2").firstResult(); // TEXT
        assertThrows(AuthoringException.class, () -> designer.assignList(s.id, q2.questionId, g.selectGroupId), "UC-014 A3 / BR-001");
        Question q3 = Question.find("shortText", "FixtureQ3").firstResult(); // RADIO
        SelectGroup other = service.create(s.id, draft("Other"), List.of(new OptionDraft(null, "Z", null)));
        Question updated = designer.assignList(s.id, q3.questionId, other.selectGroupId);
        assertEquals(other.selectGroupId, updated.selectGroupId, "UC-014 step 7");
        assertNull(Question.<Question>find("shortText", "FixtureQ2").firstResult().selectGroupId);
    }
}
