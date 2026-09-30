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

import com.elicitsoftware.test.PostgresTestResource;
import com.vaadin.browserless.quarkus.QuarkusBrowserlessTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.RouterLink;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC-042: the AI drafting path is offered by Author itself, not only by the repository. The page
 * hands over the specification and states the round trip; the drafting happens elsewhere.
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class AiDraftViewTest extends QuarkusBrowserlessTest {

    @Test
    @TestSecurity(user = "aidraft.author", roles = {"elicit_author"})
    void handsOverTheSpecificationBothWays() {
        AiDraftView view = navigate(AiDraftView.class);

        assertTrue(view.copy.isVisible() && view.copy.isEnabled(), "UC-042 step 1: copy the prompt");
        assertTrue(view.download.isVisible(), "UC-042 step 1: or take the file to an agent that reads attachments");
        assertEquals(AiDraftView.DOWNLOAD_HREF, view.download.getHref());
        assertFalse(view.unavailable.isVisible(), "this build carries the specification");
    }

    /** The clipboard payload is the author's instruction plus the specification, verbatim. */
    @Test
    @TestSecurity(user = "aidraft.author", roles = {"elicit_author"})
    void theCopiedPromptCarriesTheWholeSpecification() {
        AiDraftView view = navigate(AiDraftView.class);

        String payload = view.promptPayload();
        assertNotNull(payload);
        assertTrue(payload.startsWith(view.getTranslation("aiDraftView.promptPreamble")),
                "the agent is told what it is being asked for before it is given the specification");
        // Landmarks from each end of the document, so a truncated payload cannot pass.
        assertTrue(payload.contains("# Writing an Elicit survey definition"), "the specification's own heading");
        assertTrue(payload.contains("ELICIT_SURVEY_DRAFT_V1"), "the marker the agent has to write (BR-003)");
        assertTrue(payload.contains("## 8. How to work"), "through to the document's last section");
    }

    /** Step 4 of the round trip: the page does not leave the author to find the import page. */
    @Test
    @TestSecurity(user = "aidraft.author", roles = {"elicit_author"})
    void pointsAtTheImportPage() {
        AiDraftView view = navigate(AiDraftView.class);

        assertTrue(find(RouterLink.class, view).all().stream()
                        .anyMatch(link -> "import".equals(link.getHref())),
                "UC-042 step 6: the draft comes back through Import definition");
    }

    @Test
    @TestSecurity(user = "aidraft.author", roles = {"elicit_author"})
    void isReachableFromTheNavigationAndFromTheSurveysList() {
        navigate(AiDraftView.class);
        MainLayout layout = (MainLayout) UI.getCurrent().getChildren()
                .filter(MainLayout.class::isInstance).findFirst().orElseThrow();
        assertTrue(find(SideNavItem.class, layout).all().stream()
                        .anyMatch(i -> layout.getTranslation("mainLayout.nav.aiDraft").equals(i.getLabel())),
                "UC-042 step 1: the path is in the application navigation");

        SurveysView surveys = navigate(SurveysView.class);
        assertTrue(surveys.aiDraft.isVisible(),
                "UC-042 step 1: and beside New survey, where an author starts");
        assertTrue(find(Button.class, surveys).all().contains(surveys.newSurvey),
                "the blank form (UC-003) is still offered alongside it");
    }

    /** The other end of the round trip advertises where a draft comes from (UC-042 step 6). */
    @Test
    @TestSecurity(user = "aidraft.author", roles = {"elicit_author"})
    void theImportPageLinksBack() {
        ImportDefinitionView importView = navigate(ImportDefinitionView.class);

        assertTrue(importView.draftOrigin.isVisible());
        assertTrue(find(RouterLink.class, importView).all().stream()
                        .anyMatch(link -> "ai-draft".equals(link.getHref())),
                "an author with no draft yet is told how to get one");
    }
}
