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

import com.elicitsoftware.author.flow.SurveyEditorView;
import com.elicitsoftware.author.survey.RelationshipQuery;
import com.elicitsoftware.author.survey.Tokens;
import com.elicitsoftware.model.OperatorType;
import com.elicitsoftware.model.Relationship;
import com.elicitsoftware.model.StructuralElement;
import com.elicitsoftware.model.Survey;
import com.elicitsoftware.test.PostgresTestResource;
import com.vaadin.browserless.quarkus.QuarkusBrowserlessTest;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.combobox.ComboBoxBase;
import com.vaadin.flow.component.dnd.DragStartEvent;
import com.vaadin.flow.component.dnd.DropEvent;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class SurveyDesignerViewRulesTest extends QuarkusBrowserlessTest {

    @Inject
    RelationshipQuery relationshipQuery;

    @Inject
    EntityManager em;

    private Survey fixture() {
        return Survey.find("name", "AuthorFixture").firstResult();
    }

    private SurveyDesignerView open(Survey s) {
        return navigate(SurveyDesignerView.class, Map.of(SurveyEditorView.SURVEY_ID, String.valueOf(s.id)));
    }

    @Test
    @TestSecurity(user = "rules.author", roles = {"elicit_author"})
    void handsTheOverlayOneEdgePerRuleWithCardFallbacks() {
        SurveyDesignerView view = open(fixture());
        assertTrue(view.showRules.getValue(), "UC-020 step 1: rules are shown by default");
        String edges = view.overlay.edges();
        assertTrue(edges.contains("\"action\":\"SHOW\"") && edges.contains("\"condition\":\"= Y\""), edges);
        assertTrue(edges.contains("\"label\":\"Show "), "the label reads the action and target: " + edges);
        assertTrue(edges.contains("\"sourceFallbacks\":[\"ss-"), "UC-020 step 3: closed cards receive the arrow: " + edges);

        view.showRules.setValue(false);
        assertEquals("[]", view.overlay.edges(), "hidden rules draw nothing");
    }

    @Test
    @TestSecurity(user = "rules.author", roles = {"elicit_author"})
    void droppingARuleGlyphOnAQuestionStartsAPendingArrow() {
        SurveyDesignerView view = open(fixture());
        SectionCard card = view.lanes.get(0).cards.get(1);
        view.toggleAndSelect(card.node());
        QuestionRow row = view.lanes.get(0).cards.get(1).rows.get(0);

        ComponentUtil.fireEvent(view.palette.repeatRuleChip, new DragStartEvent<>(view.palette.repeatRuleChip, true, 0, 0, 0, 0));
        ComponentUtil.fireEvent(row, new DropEvent<>(row, true, "copy", 0, 0, 0, 0));

        assertEquals("sq-" + row.node().assignmentId(), view.overlay.pendingSource(), "UC-019 step 2: the arrow hangs from the question");
        assertEquals("REPEAT", view.overlay.pendingAction());
        assertNotNull(row.menu.find(row.getTranslation("questionRow.menu.addRule")), "UC-019 A1: keyboard path on the row menu");
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "rules.author", roles = {"elicit_author"})
    void releasingTheArrowOnASectionOpensThePrefilledDialogAndSavesTheRule() {
        Survey s = fixture();
        SurveyDesignerView view = open(s);
        SectionCard sectionB = view.lanes.get(0).cards.get(1);
        view.toggleAndSelect(sectionB.node());
        QuestionRow count = view.lanes.get(0).cards.get(1).rows.get(1);
        SectionCard sectionC = view.lanes.get(1).cards.get(0);
        int before = relationshipQuery.edges(s.id, false).size();

        ComponentUtil.fireEvent(view.overlay, new RuleArrowsOverlay.ConnectRequestedEvent(view.overlay, true,
                "sq-" + count.node().assignmentId(), "ss-" + sectionC.node().assignmentId(), null, "REPEAT"));

        RelationshipDialog dialog = find(RelationshipDialog.class).single();
        assertTrue(dialog.isOpened(), "UC-019 step 4");
        assertEquals(count.node().assignmentId(), dialog.upstream.getValue().assignmentId(), "upstream pre-filled");
        assertEquals("REPEAT", dialog.action.getValue().name, "action pre-filled from the glyph");
        assertEquals(RelationshipDialog.KIND_SECTION, dialog.targetKind.getValue());
        assertEquals(sectionC.node().assignmentId(), dialog.target.getValue().assignmentId(), "target pre-filled");
        assertTrue(dialog.token.isVisible(), "UC-019 BR-005: any action may carry a token");
        assertFalse(dialog.token.isRequiredIndicatorVisible(), "only a Text rule requires one (UC-019 A2)");
        assertFalse(dialog.remove.isVisible(), "no Remove while creating");

        test(dialog.operator).selectItem("GREATER THAN");
        test(dialog.referenceValue).setValue("0");
        test(dialog.save).click();

        assertFalse(dialog.isOpened());
        assertEquals(before + 1, relationshipQuery.edges(s.id, false).size(), "UC-019 step 6: the rule exists");
        assertTrue(view.overlay.edges().contains("\"action\":\"REPEAT\""), "and is drawn");
    }

    /**
     * UC-019 A6: a Repeat arrow drawn from a multi-select opens the dialog saying the target repeats
     * once per selected item, already on the operator that holds for a selection; one drawn from a
     * count says nothing and leaves the operator to the author.
     */
    @Test
    @TestTransaction
    @TestSecurity(user = "rules.author", roles = {"elicit_author"})
    void aRepeatArrowFromAMultiSelectStartsOnFieldExistsAndSaysItRepeatsPerItem() {
        Survey s = fixture();
        em.createNativeQuery("UPDATE survey.questions SET type_id = (SELECT id FROM survey.question_types WHERE name = 'MULTI_SELECT') "
                + "WHERE survey_id = ?1 AND short_text = 'FixtureQ3'").setParameter(1, s.id).executeUpdate();
        em.flush();
        em.clear();
        SurveyDesignerView view = open(s);
        SectionCard sectionB = view.lanes.get(0).cards.get(1);
        view.toggleAndSelect(sectionB.node());
        QuestionRow children = view.lanes.get(0).cards.get(1).rows.get(0);
        QuestionRow count = view.lanes.get(0).cards.get(1).rows.get(1);
        assertEquals("MULTI_SELECT", children.node().detail(), "the fixture's choice question, made a multi-select");
        SectionCard sectionC = view.lanes.get(1).cards.get(0);
        int before = relationshipQuery.edges(s.id, false).size();

        ComponentUtil.fireEvent(view.overlay, new RuleArrowsOverlay.ConnectRequestedEvent(view.overlay, true,
                "sq-" + children.node().assignmentId(), "ss-" + sectionC.node().assignmentId(), null, "REPEAT"));

        RelationshipDialog dialog = find(RelationshipDialog.class).single();
        assertEquals("FIELD_EXIST", dialog.operator.getValue().name, "A6 step 1: a new rule starts on Field exists");
        assertEquals(dialog.getTranslation("relationshipDialog.operator.helper.perItem"), dialog.operator.getHelperText(),
                "A6 step 1: the dialog says what the rule will do");

        // Reading a count instead, the hint goes; the operator the author already has is kept.
        dialog.upstream.setValue(dialog.upstream.getListDataView().getItems()
                .filter(n -> n.assignmentId().equals(count.node().assignmentId())).findFirst().orElseThrow());
        assertNull(dialog.operator.getHelperText(), "a count repeats 1 to N; nothing to say");
        dialog.upstream.setValue(dialog.upstream.getListDataView().getItems()
                .filter(n -> n.assignmentId().equals(children.node().assignmentId())).findFirst().orElseThrow());

        test(dialog.save).click();

        assertFalse(dialog.isOpened());
        assertEquals(before + 1, relationshipQuery.edges(s.id, false).size(), "the per-item Repeat is saved");
    }

    @Test
    @TestSecurity(user = "rules.author", roles = {"elicit_author"})
    void selectingAnArrowOpensTheRuleForEditingWithRemove() {
        Survey s = fixture();
        SurveyDesignerView view = open(s);
        int ruleId = relationshipQuery.edges(s.id, false).get(0).relationshipId();

        ComponentUtil.fireEvent(view.overlay, new RuleArrowsOverlay.EdgeSelectedEvent(view.overlay, true, ruleId));

        RelationshipDialog dialog = find(RelationshipDialog.class).single();
        assertTrue(dialog.isOpened(), "UC-020 step 5");
        assertTrue(dialog.remove.isVisible());
        assertEquals("EQUAL", dialog.operator.getValue().name);
        assertEquals("Y", dialog.referenceValue.getValue());
        assertTrue(dialog.sentence.getElement().getTextRecursively().contains("EQUAL Y → SHOW"), "UC-019 BR-003: " + dialog.sentence.getElement().getTextRecursively());
        test(dialog.cancel).click();
    }

    private Relationship ruleOnto(Survey s, int downstreamSqId) {
        return Relationship.find("surveyId = ?1 and downstreamSqId = ?2 and effectiveTo = ?3 order by id desc",
                s.id, downstreamSqId, StructuralElement.SENTINEL).firstResult();
    }

    /** UC-019 BR-001: a rule between two questions of one section is stored question-only and fires from every mount. */
    @Test
    @TestTransaction
    @TestSecurity(user = "rules.author", roles = {"elicit_author"})
    void savingARuleWithinOneSectionStoresItQuestionOnly() {
        Survey s = fixture();
        SurveyDesignerView view = open(s);
        view.toggleAndSelect(view.lanes.get(0).cards.get(0).node());
        SectionCard sectionA = view.lanes.get(0).cards.get(0);
        QuestionRow welcome = sectionA.rows.get(0);
        QuestionRow name = sectionA.rows.get(1);

        ComponentUtil.fireEvent(view.overlay, new RuleArrowsOverlay.ConnectRequestedEvent(view.overlay, true,
                "sq-" + welcome.node().assignmentId(), "sq-" + name.node().assignmentId(), "ss-" + sectionA.node().assignmentId(), "SHOW"));
        RelationshipDialog dialog = find(RelationshipDialog.class).single();
        assertEquals(name.node().assignmentId(), dialog.target.getValue().assignmentId(), "target pre-filled in the card it was dropped in");
        test(dialog.operator).selectItem("FIELD_EXIST");
        test(dialog.save).click();
        assertFalse(dialog.isOpened());

        Relationship stored = ruleOnto(s, name.node().assignmentId());
        assertEquals(welcome.node().assignmentId(), stored.upstreamSqId);
        assertNull(stored.downstreamSsId, "same section: no downstream section");
        assertNull(stored.downstreamStepId, "same section: no downstream step");
        assertNull(stored.upstreamStepId, "same section: fires from every mount");
        assertTrue(view.overlay.edges().contains("\"target\":\"sq-" + name.node().assignmentId() + "\""), "and the arrow lands on the row: " + view.overlay.edges());
    }

    /** UC-019 BR-001: a rule onto a question in another section names the step and section it is shown in. */
    @Test
    @TestTransaction
    @TestSecurity(user = "rules.author", roles = {"elicit_author"})
    void savingARuleAcrossSectionsStoresTheFullPath() {
        Survey s = fixture();
        SurveyDesignerView view = open(s);
        view.toggleAndSelect(view.lanes.get(0).cards.get(1).node());
        view.toggleAndSelect(view.lanes.get(1).cards.get(0).node());
        QuestionRow count = view.lanes.get(0).cards.get(1).rows.get(1);
        SectionCard sectionC = view.lanes.get(1).cards.get(0);
        QuestionRow anythingElse = sectionC.rows.get(0);

        ComponentUtil.fireEvent(view.overlay, new RuleArrowsOverlay.ConnectRequestedEvent(view.overlay, true,
                "sq-" + count.node().assignmentId(), "sq-" + anythingElse.node().assignmentId(), "ss-" + sectionC.node().assignmentId(), "SHOW"));
        RelationshipDialog dialog = find(RelationshipDialog.class).single();
        test(dialog.operator).selectItem("GREATER THAN");
        test(dialog.referenceValue).setValue("0");
        test(dialog.save).click();
        assertFalse(dialog.isOpened());

        Relationship stored = ruleOnto(s, anythingElse.node().assignmentId());
        assertEquals(sectionC.node().assignmentId(), stored.downstreamSsId, "another section: its mount is named");
        assertEquals(view.lanes.get(1).node().durableId(), stored.downstreamStepId, "and its step");
        assertEquals(view.lanes.get(0).node().durableId(), stored.upstreamStepId, "and the upstream step the rule fires from");
    }

    /** UC-019 BR-001: editing a question-only rule keeps it question-only. */
    @Test
    @TestTransaction
    @TestSecurity(user = "rules.author", roles = {"elicit_author"})
    void editingAQuestionOnlyRuleKeepsItQuestionOnly() {
        Survey s = fixture();
        SurveyDesignerView view = open(s);
        RelationshipQuery.RuleEdge edge = relationshipQuery.edges(s.id, false).get(0);
        Relationship before = Relationship.find("surveyId = ?1 and relationshipId = ?2", s.id, edge.relationshipId()).firstResult();
        assertNull(before.downstreamSsId, "the fixture rule is question-only");

        ComponentUtil.fireEvent(view.overlay, new RuleArrowsOverlay.EdgeSelectedEvent(view.overlay, true, edge.relationshipId()));
        RelationshipDialog dialog = find(RelationshipDialog.class).single();
        test(dialog.description).setValue("Edited");
        test(dialog.save).click();
        assertFalse(dialog.isOpened());

        Relationship after = Relationship.find("surveyId = ?1 and relationshipId = ?2", s.id, edge.relationshipId()).firstResult();
        assertEquals("Edited", after.description);
        assertEquals(before.downstreamSqId, after.downstreamSqId);
        assertNull(after.downstreamSsId, "still question-only");
        assertNull(after.downstreamStepId);
        assertNull(after.upstreamStepId, "still fires from every mount");
    }

    // ---- UC-019 A2 / FR-012: the token field shows the name, the sentence shows the reference ----

    /**
     * Author#21: a token typed as the reference a text writes settles on its name, because the runtime
     * keys a text's values on the name. The sentence reads the placeholder back the way a text writes
     * it, so the author sees both halves of the convention at once.
     */
    @Test
    @TestTransaction
    @TestSecurity(user = "rules.author", roles = {"elicit_author"})
    void aTokenTypedAsAReferenceSettlesOnItsName() {
        Survey s = fixture();
        SurveyDesignerView view = open(s);
        SectionCard sectionA = view.lanes.get(0).cards.get(0);
        view.toggleAndSelect(sectionA.node());
        QuestionRow welcome = sectionA.rows.get(0);
        QuestionRow name = sectionA.rows.get(1);

        ComponentUtil.fireEvent(view.overlay, new RuleArrowsOverlay.ConnectRequestedEvent(view.overlay, true,
                "sq-" + welcome.node().assignmentId(), "sq-" + name.node().assignmentId(),
                "ss-" + sectionA.node().assignmentId(), "SHOW"));
        RelationshipDialog dialog = find(RelationshipDialog.class).single();

        ComponentUtil.fireEvent(dialog.token, new ComboBoxBase.CustomValueSetEvent<>(dialog.token, true, " <s1> "));

        assertEquals("S1", dialog.token.getValue(), "the brackets belong to the text that names the token");
        assertTrue(dialog.sentence.getElement().getTextRecursively().contains("{<S1>}"),
                "and the sentence reads it as a text writes it: " + dialog.sentence.getElement().getTextRecursively());
        test(dialog.cancel).click();
    }

    /** An import can write the column directly (UC-005); the dialog shows the name the platform means. */
    @Test
    @TestTransaction
    @TestSecurity(user = "rules.author", roles = {"elicit_author"})
    void openingARuleSavedWithItsBracketsShowsTheNameAlone() {
        Survey s = fixture();
        em.createNativeQuery("UPDATE survey.relationships SET token = '<S1>' WHERE survey_id = ?1")
                .setParameter(1, s.id).executeUpdate();
        em.flush();
        em.clear();
        SurveyDesignerView view = open(s);
        int ruleId = relationshipQuery.edges(s.id, false).get(0).relationshipId();

        ComponentUtil.fireEvent(view.overlay, new RuleArrowsOverlay.EdgeSelectedEvent(view.overlay, true, ruleId));

        RelationshipDialog dialog = find(RelationshipDialog.class).single();
        assertEquals(Tokens.canonical("<S1>"), dialog.token.getValue(), "saving the rule corrects the row");
        test(dialog.cancel).click();
    }
}
