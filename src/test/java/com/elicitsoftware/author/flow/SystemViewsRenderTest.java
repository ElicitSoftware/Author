package com.elicitsoftware.author.flow;

/*-
 * ***LICENSE_START***
 * Elicit Survey
 * %%
 * Copyright (C) 2025 - 2026 The Regents of the University of Michigan - Rogel Cancer Center
 * %%
 * PolyForm Noncommercial License 1.0.0
 * <https://polyformproject.org/licenses/noncommercial/1.0.0>
 * ***LICENSE_END***
 */

import com.elicitsoftware.author.diagnostics.RequiredConfigCheck;
import com.elicitsoftware.test.PostgresTestResource;
import com.vaadin.browserless.quarkus.QuarkusBrowserlessTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Pre;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The System screens render for an {@code elicit_admin} holder, show their sections, and never
 * render a secret.
 *
 * <p>Traceability: UC-034 (View System Overview), UC-035 (Diagnose Database), UC-036 (Diagnose
 * Branding), UC-037 (Check Connections), UC-038 (View OIDC Diagnostics); NFR-014.</p>
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
@TestSecurity(user = "system.admin", roles = {"elicit_admin"})
class SystemViewsRenderTest extends QuarkusBrowserlessTest {

    @Inject
    RequiredConfigCheck requiredConfig;

    /** The secret values the test profile configures; none may appear on a screen (NFR-014). */
    private static List<String> secrets() {
        var config = ConfigProvider.getConfig();
        return List.of(config.getValue("quarkus.datasource.password", String.class),
                config.getValue("quarkus.oidc.credentials.secret", String.class));
    }

    private <T extends Component> T attach(Class<T> type) {
        T view = CDI.current().select(type).get();
        UI.getCurrent().add(view);
        return view;
    }

    private static boolean containsId(Component root, String id) {
        if (root.getId().filter(id::equals).isPresent()) {
            return true;
        }
        return root.getChildren().anyMatch(child -> containsId(child, id));
    }

    private static void assertNoSecretRendered(Component view) {
        String text = view.getElement().getTextRecursively();
        for (String secret : secrets()) {
            assertFalse(text.contains(secret), "a secret value must never be rendered");
        }
    }

    /** UC-034: the overview shows build, health and required settings. */
    @Test
    void overviewRendersEverySection() {
        SystemOverviewView view = attach(SystemOverviewView.class);

        assertEquals("System Overview", find(H3.class, view).single().getText());
        assertTrue(containsId(view, SystemOverviewView.BUILD_GRID_ID));
        assertTrue(containsId(view, SystemOverviewView.HEALTH_GRID_ID));
        assertTrue(containsId(view, SystemOverviewView.CONFIG_GRID_ID));
        assertNoSecretRendered(view);
    }

    /** UC-034 step 4 / BR-001: required settings are reported as present, never by value. */
    @Test
    void requiredSettingsAreReportedByPresenceOnly() {
        var settings = requiredConfig.requiredSettings();

        assertTrue(settings.stream().anyMatch(s -> s.property().equals("quarkus.datasource.password") && s.present()));
        assertTrue(settings.stream().anyMatch(s -> s.property().equals("quarkus.oidc.credentials.secret") && s.present()));
        for (var setting : settings) {
            for (String secret : secrets()) {
                assertFalse(setting.toString().contains(secret), "a required-setting report carries no value");
            }
        }
    }

    /** UC-035: the database screen shows the connection and the schema state. */
    @Test
    void databaseViewRendersConnectionAndSchema() {
        SystemDatabaseView view = attach(SystemDatabaseView.class);

        assertEquals("Database", find(H3.class, view).single().getText());
        assertTrue(containsId(view, SystemDatabaseView.CONNECTION_GRID_ID));
        assertTrue(containsId(view, SystemDatabaseView.SCHEMA_GRID_ID));
        assertNoSecretRendered(view);
    }

    /** UC-036: the branding screen shows resolution and assets, and reload re-renders. */
    @Test
    void brandingViewRendersAssetsAndReloads() {
        SystemBrandingView view = attach(SystemBrandingView.class);

        assertEquals("Branding", find(H3.class, view).single().getText());
        assertTrue(containsId(view, SystemBrandingView.SUMMARY_GRID_ID));
        assertTrue(containsId(view, SystemBrandingView.ASSETS_GRID_ID));

        view.render();

        assertEquals(2, find(Grid.class, view).all().size(), "reload must replace the grids, not duplicate them");
    }

    /** UC-037: the connections screen lists its targets or says there are none. */
    @Test
    void connectionsViewRendersItsTargets() {
        SystemConnectionsView view = attach(SystemConnectionsView.class);

        assertEquals("Connections", find(H3.class, view).single().getText());
        if (view.rows().isEmpty()) {
            assertTrue(containsId(view, SystemConnectionsView.EMPTY_ID));
        } else {
            assertTrue(containsId(view, SystemConnectionsView.GRID_ID));
        }
    }

    /** UC-038: the OIDC screen shows the identity and masks tokens. */
    @Test
    void oidcViewShowsIdentityAndMasksTokens() {
        OidcDiagnosticsView view = attach(OidcDiagnosticsView.class);

        assertEquals("OIDC", find(H3.class, view).single().getText());
        String info = find(Pre.class, view).single().getText();
        assertTrue(info.contains("User: system.admin"), info);
        assertTrue(info.contains("Has elicit_admin: true"), info);
        assertTrue(info.contains("Has elicit_author: false"), info);
        assertEquals("**cdef", OidcDiagnosticsView.maskToken("abcdef", false));
        assertEquals("********wxyz", OidcDiagnosticsView.maskToken("abcdefghwxyz", false));
        assertEquals("abcdef", OidcDiagnosticsView.maskToken("abcdef", true));
    }
}
