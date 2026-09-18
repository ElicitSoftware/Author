package com.elicitsoftware.author.flow;

/*-
 * ***LICENSE_START***
 * Elicit Author
 * %%
 * Copyright (C) 2026 The Regents of the University of Michigan - Rogel Cancer Center
 * %%
 * PolyForm Noncommercial License 1.0.0
 * <https://polyformproject.org/licenses/noncommercial/1.0.0>
 * ***LICENSE_END***
 */

import com.elicitsoftware.model.Question;
import com.elicitsoftware.model.Survey;
import com.elicitsoftware.test.PostgresTestResource;
import com.vaadin.browserless.quarkus.QuarkusBrowserlessTest;
import com.vaadin.flow.component.UI;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** UC-006: element detail and the drawer tree highlight follow the address. */
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
