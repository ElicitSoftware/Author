/*-
 * ***LICENSE_START***
 * Elicit Author
 * %%
 * Copyright (C) 2025 - 2026 The Regents of the University of Michigan - Rogel Cancer Center
 * %%
 * PolyForm Noncommercial License 1.0.0
 * <https://polyformproject.org/licenses/noncommercial/1.0.0>
 * ***LICENSE_END***
 */
package com.elicitsoftware.author.flow.actions;

import com.elicitsoftware.author.flow.SurveyEditorView;
import com.elicitsoftware.model.PostSurveyAction;
import com.elicitsoftware.model.Survey;
import com.elicitsoftware.test.PostgresTestResource;
import com.vaadin.browserless.quarkus.QuarkusBrowserlessTest;
import com.vaadin.flow.component.UI;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** UC-052 on the Post-survey actions page. */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class PostSurveyActionsViewTest extends QuarkusBrowserlessTest {

    private PostSurveyActionsView open() {
        Survey s = Survey.find("name", "AuthorFixture").firstResult();
        return navigate(PostSurveyActionsView.class, Map.of(SurveyEditorView.SURVEY_ID, String.valueOf(s.id)));
    }

    private static List<PostSurveyAction> rows(PostSurveyActionsView view) {
        return view.grid.getGenericDataView().getItems().toList();
    }

    @Test
    @TestTransaction
    @TestSecurity(user = "actions.author", roles = {"elicit_author"})
    void addDialogAddsActionsThatCanBeReorderedEditedAndRemoved() {
        PostSurveyActionsView view = open();
        int before = rows(view).size();
        assertEquals(before == 0, view.empty.isVisible(), "UC-052 step 2: an empty list says what that means");

        test(view.addAction).click();
        PostSurveyActionDialog dialog = $(PostSurveyActionDialog.class).single();
        assertTrue(dialog.isOpened());
        test(dialog.name).setValue("File the history");
        test(dialog.description).setValue("Uploads the PDF.");
        test(dialog.url).setValue("/familyhistory/generate");
        test(dialog.save).click();
        assertTrue(dialog.url.isInvalid(), "UC-052 A2 / BR-004: a path is not enough for an action");
        test(dialog.url).setValue("http://fhhs:8080/familyhistory/generate");
        test(dialog.save).click();
        assertFalse(dialog.isOpened());

        view.actions.add(view.survey.id, "Notify", "Tells someone.", "POST", "https://example.org/notify", false);
        view.load();
        List<PostSurveyAction> rows = rows(view);
        assertEquals(before + 2, rows.size(), "UC-052 step 4");
        assertTrue(view.grid.isVisible());
        assertFalse(view.empty.isVisible());
        PostSurveyAction filed = rows.get(rows.size() - 2);
        PostSurveyAction notify = rows.get(rows.size() - 1);
        assertEquals("File the history", filed.name);

        view.move(notify, filed, true);
        rows = rows(view);
        assertEquals("Notify", rows.get(rows.size() - 2).name, "UC-052 step 5: dragged above");

        PostSurveyActionDialog edit = new PostSurveyActionDialog(view.actions, view.survey, notify, saved -> view.load());
        UI.getCurrent().add(edit);
        test(edit.description).setValue("Tells the coordinator.");
        test(edit.save).click();
        assertEquals("Tells the coordinator.", view.actions.find(view.survey.id, notify.id).orElseThrow().description,
                "UC-052 step 6");

        view.actions.remove(view.survey.id, notify.id);
        view.load();
        assertEquals(before + 1, rows(view).size(), "UC-052 step 7");
    }
}
