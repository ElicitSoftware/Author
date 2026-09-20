package com.elicitsoftware.author.flow.designer;

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

import com.elicitsoftware.author.flow.SurveyEditorView;
import com.elicitsoftware.author.survey.RelationshipQuery;
import com.elicitsoftware.model.OperatorType;
import com.elicitsoftware.model.Survey;
import com.elicitsoftware.test.PostgresTestResource;
import com.vaadin.browserless.quarkus.QuarkusBrowserlessTest;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.dnd.DragStartEvent;
import com.vaadin.flow.component.dnd.DropEvent;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class SurveyDesignerViewRulesTest extends QuarkusBrowserlessTest {

    @Inject
    RelationshipQuery relationshipQuery;

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

        ComponentUtil.fireEvent(view.palette.repeatRuleChip, new DragStartEvent<>(view.palette.repeatRuleChip, true));
        ComponentUtil.fireEvent(row, new DropEvent<>(row, true, "copy"));

        assertEquals("sq-" + row.node().assignmentId(), view.overlay.pendingSource(), "UC-019 step 2: the arrow hangs from the question");
        assertEquals("REPEAT", view.overlay.pendingAction());
        assertNotNull(row.menu.find("Add rule…"), "UC-019 A1: keyboard path on the row menu");
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
                "sq-" + count.node().assignmentId(), "ss-" + sectionC.node().assignmentId(), "REPEAT"));

        RelationshipDialog dialog = $(RelationshipDialog.class).single();
        assertTrue(dialog.isOpened(), "UC-019 step 4");
        assertEquals(count.node().assignmentId(), dialog.upstream.getValue().assignmentId(), "upstream pre-filled");
        assertEquals("REPEAT", dialog.action.getValue().name, "action pre-filled from the glyph");
        assertEquals("Section", dialog.targetKind.getValue());
        assertEquals(sectionC.node().assignmentId(), dialog.target.getValue().assignmentId(), "target pre-filled");
        assertFalse(dialog.token.isVisible(), "token only for Text rules");
        assertFalse(dialog.remove.isVisible(), "no Remove while creating");

        test(dialog.operator).selectItem("GREATER THAN");
        test(dialog.referenceValue).setValue("0");
        test(dialog.save).click();

        assertFalse(dialog.isOpened());
        assertEquals(before + 1, relationshipQuery.edges(s.id, false).size(), "UC-019 step 6: the rule exists");
        assertTrue(view.overlay.edges().contains("\"action\":\"REPEAT\""), "and is drawn");
    }

    @Test
    @TestSecurity(user = "rules.author", roles = {"elicit_author"})
    void selectingAnArrowOpensTheRuleForEditingWithRemove() {
        Survey s = fixture();
        SurveyDesignerView view = open(s);
        int ruleId = relationshipQuery.edges(s.id, false).get(0).relationshipId();

        ComponentUtil.fireEvent(view.overlay, new RuleArrowsOverlay.EdgeSelectedEvent(view.overlay, true, ruleId));

        RelationshipDialog dialog = $(RelationshipDialog.class).single();
        assertTrue(dialog.isOpened(), "UC-020 step 5");
        assertTrue(dialog.remove.isVisible());
        assertEquals("EQUAL", dialog.operator.getValue().name);
        assertEquals("Y", dialog.referenceValue.getValue());
        assertTrue(dialog.sentence.getElement().getTextRecursively().contains("EQUAL Y → SHOW"), "UC-019 BR-003: " + dialog.sentence.getElement().getTextRecursively());
        test(dialog.cancel).click();
    }
}
