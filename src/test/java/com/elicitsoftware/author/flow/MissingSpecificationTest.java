package com.elicitsoftware.author.flow;

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

import com.elicitsoftware.author.ai.AuthoringSpecification;
import com.elicitsoftware.test.PostgresTestResource;
import com.vaadin.browserless.quarkus.QuarkusBrowserlessTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.sidenav.SideNavItem;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC-042 A5: a build that packaged no specification does not offer the path at all, rather than
 * offering a button that copies nothing. The other two ways to start a survey are untouched.
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
@TestProfile(MissingSpecificationTest.NoSpecificationProfile.class)
class MissingSpecificationTest extends QuarkusBrowserlessTest {

    /** Points the specification at a classpath resource no build produces. */
    public static class NoSpecificationProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("author.ai.specification.resource", "ai/NO_SUCH_SPECIFICATION.md");
        }
    }

    @Inject
    AuthoringSpecification specification;

    @Test
    void theBeanReportsItMissingConsistently() {
        assertFalse(specification.isAvailable());
        assertNull(specification.open(), "isAvailable() and open() must agree");
        assertNull(specification.text(), "and text() with them");
    }

    @Test
    @TestSecurity(user = "spec.author", roles = {"elicit_author"})
    void theEndpointAnswersNotFound() {
        given().when().get("/api/authoring-spec").then().statusCode(404);
    }

    @Test
    @TestSecurity(user = "spec.author", roles = {"elicit_author"})
    void nothingOffersThePath() {
        SurveysView surveys = navigate(SurveysView.class);
        assertFalse(surveys.aiDraft.isVisible(), "no specification, no button on the surveys list");
        assertTrue(surveys.newSurvey.isVisible(), "UC-003 is unaffected");

        MainLayout layout = (MainLayout) UI.getCurrent().getChildren()
                .filter(MainLayout.class::isInstance).findFirst().orElseThrow();
        assertFalse(find(SideNavItem.class, layout).all().stream()
                        .anyMatch(i -> layout.getTranslation("mainLayout.nav.aiDraft").equals(i.getLabel())),
                "no specification, no navigation entry");
        assertTrue(find(SideNavItem.class, layout).all().stream()
                        .anyMatch(i -> layout.getTranslation("mainLayout.nav.importDefinition").equals(i.getLabel())),
                "UC-005 is unaffected");

        ImportDefinitionView importView = navigate(ImportDefinitionView.class);
        assertFalse(importView.draftOrigin.isVisible(), "and nothing points at a page that cannot help");
    }
}
