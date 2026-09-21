package com.elicitsoftware.author.flow;

/*-
 * ***LICENSE_START***
 * Elicit Survey
 * %%
 * Copyright (C) 2025 - 2026 Elicit Software LLC
 * %%
 * PolyForm Noncommercial License 1.0.0
 * <https://polyformproject.org/licenses/noncommercial/1.0.0>
 * ***LICENSE_END***
 */

import com.elicitsoftware.test.PostgresTestResource;
import com.vaadin.browserless.quarkus.QuarkusBrowserlessTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.sidenav.SideNavItem;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.enterprise.inject.spi.CDI;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC-001 / UC-002: the shell shows authoring navigation only to holders of {@code elicit_author}
 * and carries the branded header.
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class MainLayoutTest extends QuarkusBrowserlessTest {

    private MainLayout attachLayout() {
        MainLayout layout = CDI.current().select(MainLayout.class).get();
        UI.getCurrent().add(layout);
        return layout;
    }

    private boolean hasNavItem(MainLayout layout, String text) {
        return find(SideNavItem.class, layout).all().stream()
                .anyMatch(item -> text.equals(item.getLabel()));
    }

    @Test
    @TestSecurity(user = "mainlayout.author", roles = {"elicit_author"})
    void authorSeesAuthoringNavigation() {
        MainLayout layout = attachLayout();
        assertTrue(hasNavItem(layout, "Surveys"));
        assertTrue(hasNavItem(layout, "Import definition"));
        assertTrue(hasNavItem(layout, "Logout"));
    }

    @Test
    @TestSecurity(user = "mainlayout.admin", roles = {"elicit_admin"})
    void adminRoleAloneDoesNotGrantAuthoring() {
        MainLayout layout = attachLayout();
        assertFalse(hasNavItem(layout, "Surveys"), "elicit_admin must not imply elicit_author (UC-002 BR-001)");
        assertTrue(hasNavItem(layout, "Logout"));
        // UC-034..UC-038: elicit_admin sees the System section and its five entries.
        assertTrue(hasNavItem(layout, "System"), "elicit_admin should see the System section");
        for (String entry : List.of("Overview", "Database", "Branding", "Connections", "OIDC")) {
            assertTrue(hasNavItem(layout, entry), "elicit_admin should see System > " + entry);
        }
    }

    @Test
    @TestSecurity(user = "mainlayout.author.only", roles = {"elicit_author"})
    void authorRoleAloneDoesNotSeeSystem() {
        MainLayout layout = attachLayout();
        assertTrue(hasNavItem(layout, "Surveys"));
        assertFalse(hasNavItem(layout, "System"), "elicit_author alone must not see the System section");
        assertFalse(hasNavItem(layout, "OIDC"));
    }

    @Test
    @TestSecurity(user = "mainlayout.none", roles = {})
    void noRoleSeesOnlyLogout() {
        MainLayout layout = attachLayout();
        assertFalse(hasNavItem(layout, "Surveys"));
        assertFalse(hasNavItem(layout, "Import definition"));
        assertTrue(hasNavItem(layout, "Logout"));
    }

    @Test
    @TestSecurity(user = "mainlayout.author", roles = {"elicit_author"})
    void headerCarriesBrandedTitle() {
        MainLayout layout = attachLayout();
        Anchor title = find(Anchor.class, layout).withClassName("brand-title").single();
        assertTrue(title.getText().endsWith("Author"), "title ends with the application name: " + title.getText());
    }

    @Test
    @TestSecurity(user = "mainlayout.author", roles = {"elicit_author"})
    void afterNavigationDoesNotThrow() {
        MainLayout layout = attachLayout();
        assertDoesNotThrow(() -> UI.getCurrent().navigate(SurveysView.class));
        assertDoesNotThrow(() -> layout.afterNavigation(null));
    }
}
