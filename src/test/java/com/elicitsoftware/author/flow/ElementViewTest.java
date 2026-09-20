package com.elicitsoftware.author.flow;

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
import com.elicitsoftware.model.Survey;
import com.elicitsoftware.test.PostgresTestResource;
import com.vaadin.browserless.quarkus.QuarkusBrowserlessTest;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** UC-006: element detail and the drawer tree highlight follow the address; UC-009/UC-010 from the view. */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class ElementViewTest extends QuarkusBrowserlessTest {

    @Test
    @TestSecurity(user = "element.author", roles = {"elicit_author"})
    void showsQuestionDetailAndHighlightsItInTheDrawerTree() {
        Survey s = Survey.find("name", "AuthorFixture").firstResult();
        Question q = Question.find("shortText", "FixtureQ3").firstResult();

        ElementView view = navigate(ElementView.class, Map.of(
                SurveyEditorView.SURVEY_ID, String.valueOf(s.id), ElementView.KIND, "question", ElementView.DURABLE_ID, String.valueOf(q.questionId)));

        assertEquals("FixtureQ3", view.heading.getText());
        assertTrue(view.remove.isVisible());
        MainLayout layout = (MainLayout) UI.getCurrent().getChildren().filter(MainLayout.class::isInstance).findFirst().orElseThrow();
        StructureNavigationTree tree = layout.structureTree();
        assertNotNull(tree, "inside a survey the drawer holds the structure tree");
        assertEquals(s.id, tree.surveyId());
        assertTrue(tree.grid.getTreeData().getRootItems().stream().anyMatch(n -> "Fixture Step One".equals(n.label())));
        assertTrue(tree.grid.getTreeData().getRootItems().stream()
                .flatMap(step -> tree.grid.getTreeData().getChildren(step).stream())
                .flatMap(sec -> tree.grid.getTreeData().getChildren(sec).stream())
                .anyMatch(tree::isCurrent), "the addressed question is the highlighted row (UC-006 BR-003)");
        assertFalse(tree.grid.getColumns().get(0).isSortable(),
                "UC-006 step 2: the tree keeps display order, the header must not re-sort it");
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "element.author", roles = {"elicit_author"})
    void removeOffersUndoAndRestoreAsksForConfirmation() {
        Survey s = Survey.find("name", "AuthorFixture").firstResult();
        Question q = Question.find("shortText", "FixtureQ5").firstResult();
        Map<String, String> address = Map.of(SurveyEditorView.SURVEY_ID, String.valueOf(s.id),
                ElementView.KIND, "question", ElementView.DURABLE_ID, String.valueOf(q.questionId));

        ElementView view = navigate(ElementView.class, address);
        test(view.remove).click();
        ConfirmDialog removeDialog = $(ConfirmDialog.class).single();
        assertTrue(removeDialog.isOpened());
        ComponentUtil.fireEvent(removeDialog, new ConfirmDialog.ConfirmEvent(removeDialog, true));
        removeDialog.close(); // a real click closes it client-side; the fired event alone does not

        assertTrue(Question.<Question>find("shortText", "FixtureQ5").firstResult().isRetired());
        assertNotNull(view.undo, "UC-009 step 6: the removal notice carries an Undo");
        assertTrue(view.removalNotice.isOpened());
        // A Notification attaches itself to the UI only before the next client response.
        UI.getCurrent().getInternals().getStateTree().runExecutionsBeforeClientResponse();
        test(view.undo).click();
        assertTrue(Question.<Question>find("shortText", "FixtureQ5").firstResult().isCurrent(), "Undo restores (UC-010 step 1)");

        // Remove again, then go through the Restore button: it must confirm first (UC-010 step 3).
        view = navigate(ElementView.class, address);
        test(view.remove).click();
        ConfirmDialog again = $(ConfirmDialog.class).single();
        ComponentUtil.fireEvent(again, new ConfirmDialog.ConfirmEvent(again, true));
        again.close(); // a real click closes it client-side; the fired event alone does not
        view.removalNotice.close();
        view = navigate(ElementView.class, address);
        assertTrue(view.restore.isVisible());
        test(view.restore).click();
        ConfirmDialog restoreDialog = $(ConfirmDialog.class).single();
        assertTrue(restoreDialog.isOpened());
        assertTrue(restoreDialog.getElement().getProperty("header").startsWith("Restore Question 'FixtureQ5'"));
        assertTrue(Question.<Question>find("shortText", "FixtureQ5").firstResult().isRetired(), "nothing restored before confirming");
        ComponentUtil.fireEvent(restoreDialog, new ConfirmDialog.ConfirmEvent(restoreDialog, true));
        restoreDialog.close(); // a real click closes it client-side; the fired event alone does not
        assertTrue(Question.<Question>find("shortText", "FixtureQ5").firstResult().isCurrent());
    }

    @Test
    @TestSecurity(user = "element.author", roles = {"elicit_author"})
    void leavingTheSurveyRestoresApplicationNavigation() {
        Survey s = Survey.find("name", "AuthorFixture").firstResult();
        navigate(SurveyEditorView.class, Map.of(SurveyEditorView.SURVEY_ID, String.valueOf(s.id)));
        MainLayout layout = (MainLayout) UI.getCurrent().getChildren().filter(MainLayout.class::isInstance).findFirst().orElseThrow();
        assertNotNull(layout.structureTree());
        navigate(SurveysView.class);
        assertTrue(layout.structureTree() == null, "UC-006 step 7: drawer back to the application navigation");
    }
}
