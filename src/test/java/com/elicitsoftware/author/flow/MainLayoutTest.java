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

import com.elicitsoftware.test.PostgresTestResource;
import com.vaadin.browserless.quarkus.QuarkusBrowserlessTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.sidenav.SideNavItem;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.enterprise.inject.spi.CDI;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
        assertTrue(hasNavItem(layout, layout.getTranslation("mainLayout.nav.surveys")));
        assertTrue(hasNavItem(layout, layout.getTranslation("mainLayout.nav.importDefinition")));
        assertTrue(hasNavItem(layout, layout.getTranslation("mainLayout.nav.logout")));
    }

    @Test
    @TestSecurity(user = "mainlayout.admin", roles = {"elicit_author_admin"})
    void adminRoleAloneDoesNotGrantAuthoring() {
        MainLayout layout = attachLayout();
        assertFalse(hasNavItem(layout, layout.getTranslation("mainLayout.nav.surveys")), "elicit_author_admin must not imply elicit_author (UC-002 BR-001)");
        assertTrue(hasNavItem(layout, layout.getTranslation("mainLayout.nav.logout")));
        // UC-034..UC-038: elicit_author_admin sees the System section and its five entries.
        assertTrue(hasNavItem(layout, layout.getTranslation("mainLayout.nav.system")), "elicit_author_admin should see the System section");
        for (String entry : List.of("systemOverview", "systemDatabase", "systemBranding", "systemConnections", "systemOidc")) {
            assertTrue(hasNavItem(layout, layout.getTranslation("mainLayout.nav." + entry)), "elicit_author_admin should see System > " + entry);
        }
    }

    @Test
    @TestSecurity(user = "mainlayout.author.only", roles = {"elicit_author"})
    void authorRoleAloneDoesNotSeeSystem() {
        MainLayout layout = attachLayout();
        assertTrue(hasNavItem(layout, layout.getTranslation("mainLayout.nav.surveys")));
        assertFalse(hasNavItem(layout, layout.getTranslation("mainLayout.nav.system")), "elicit_author alone must not see the System section");
        assertFalse(hasNavItem(layout, layout.getTranslation("mainLayout.nav.systemOidc")));
    }

    @Test
    @TestSecurity(user = "mainlayout.none", roles = {})
    void noRoleSeesOnlyLogout() {
        MainLayout layout = attachLayout();
        assertFalse(hasNavItem(layout, layout.getTranslation("mainLayout.nav.surveys")));
        assertFalse(hasNavItem(layout, layout.getTranslation("mainLayout.nav.importDefinition")));
        assertTrue(hasNavItem(layout, layout.getTranslation("mainLayout.nav.logout")));
    }

    @Test
    @TestSecurity(user = "mainlayout.author", roles = {"elicit_author"})
    void headerCarriesBrandedTitle() {
        MainLayout layout = attachLayout();
        Anchor title = find(Anchor.class, layout).withClassName("brand-title").single();
        assertTrue(title.getText().endsWith(layout.getTranslation("common.appType.author")), "title ends with the application name: " + title.getText());
    }

    /**
     * UC-039 step 1: an author reaches the manual from the header and from the navigation. The
     * test classpath carries a stand-in PDF, so the link is expected here; the build that has no
     * manual is covered by {@code MissingManualTest}.
     */
    @Test
    @TestSecurity(user = "mainlayout.author", roles = {"elicit_author"})
    void authorReachesTheManual() {
        MainLayout layout = attachLayout();

        Anchor manual = find(Anchor.class, layout).withClassName("header-manual-link").single();
        assertEquals("/api/manual", manual.getHref());
        assertTrue(manual.isRouterIgnore(), "/api/manual is served outside the Vaadin router");
        assertEquals(AnchorTarget.BLANK.getValue(), manual.getTarget().orElse(null),
                "the manual opens in a new tab so the author keeps their place (UC-039 step 2)");

        assertTrue(hasNavItem(layout, layout.getTranslation("mainLayout.nav.manual")));
    }

    /** UC-039 A4: the manual is authoring documentation, so it is not offered without the role. */
    @Test
    @TestSecurity(user = "mainlayout.admin", roles = {"elicit_author_admin"})
    void adminRoleAloneIsNotOfferedTheManual() {
        MainLayout layout = attachLayout();
        assertEquals(0, find(Anchor.class, layout).withClassName("header-manual-link").all().size());
        assertFalse(hasNavItem(layout, layout.getTranslation("mainLayout.nav.manual")));
    }

    @Test
    @TestSecurity(user = "mainlayout.author", roles = {"elicit_author"})
    void afterNavigationDoesNotThrow() {
        MainLayout layout = attachLayout();
        assertDoesNotThrow(() -> UI.getCurrent().navigate(SurveysView.class));
        assertDoesNotThrow(() -> layout.afterNavigation(null));
    }
}
