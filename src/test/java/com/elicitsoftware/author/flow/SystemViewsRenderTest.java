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

import com.elicitsoftware.author.diagnostics.BrandDiagnostics;
import com.elicitsoftware.author.diagnostics.RequiredConfigCheck;
import com.elicitsoftware.author.util.BrandUtil;
import com.elicitsoftware.author.security.ServiceToken;
import com.elicitsoftware.test.PostgresTestResource;
import com.vaadin.browserless.quarkus.QuarkusBrowserlessTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Pre;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The System screens render for an {@code elicit_author_admin} holder, show their sections, and never
 * render a secret.
 *
 * <p>Traceability: UC-034 (View System Overview), UC-035 (Diagnose Database), UC-036 (Diagnose
 * Branding), UC-037 (Check Connections), UC-038 (View OIDC Diagnostics); NFR-014.</p>
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
@TestSecurity(user = "system.admin", roles = {"elicit_author_admin"})
class SystemViewsRenderTest extends QuarkusBrowserlessTest {

    @Inject
    RequiredConfigCheck requiredConfig;

    /** The secret values the test profile configures; none may appear on a screen (NFR-014). */
    private static List<String> secrets() {
        var config = ConfigProvider.getConfig();
        return List.of(config.getValue("quarkus.datasource.password", String.class),
                config.getValue("quarkus.oidc.credentials.secret", String.class),
                config.getValue("elicit.service.token", String.class));
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

        assertEquals(view.getTranslation("systemOverviewView.title"), find(H3.class, view).single().getText());
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
        assertTrue(settings.stream().anyMatch(s -> s.property().equals("elicit.service.token")
                && s.envVar().equals("ELICIT_SERVICE_TOKEN") && s.present()), "NFR-025: the service token is a required setting");
        for (var setting : settings) {
            for (String secret : secrets()) {
                assertFalse(setting.toString().contains(secret), "a required-setting report carries no value");
            }
        }
    }

    /** UC-034 A4: the development-value warning appears only for the development value; the tests' token is not it. */
    @Test
    void overviewShowsNoDevelopmentWarningForADeploymentsOwnToken() {
        SystemOverviewView view = attach(SystemOverviewView.class);

        assertFalse(containsId(view, SystemOverviewView.SERVICE_TOKEN_WARNING_ID));
    }

    /** UC-034 A4: the development value is warned about, by the variable's name and never by value. */
    @Test
    void overviewWarnsAboutTheDevelopmentValue() {
        QuarkusMock.installMockForType(new ServiceToken(Optional.of(ServiceToken.DEVELOPMENT_VALUE)), ServiceToken.class);
        SystemOverviewView view = attach(SystemOverviewView.class);

        assertTrue(containsId(view, SystemOverviewView.SERVICE_TOKEN_WARNING_ID));
        String text = view.getElement().getTextRecursively();
        assertTrue(text.contains("ELICIT_SERVICE_TOKEN"), text);
        assertFalse(text.contains(ServiceToken.DEVELOPMENT_VALUE), "the development value itself is not rendered");
    }

    /** UC-035: the database screen shows the connection and the schema state. */
    @Test
    void databaseViewRendersConnectionAndSchema() {
        SystemDatabaseView view = attach(SystemDatabaseView.class);

        assertEquals(view.getTranslation("systemDatabaseView.title"), find(H3.class, view).single().getText());
        assertTrue(containsId(view, SystemDatabaseView.CONNECTION_GRID_ID));
        assertTrue(containsId(view, SystemDatabaseView.SCHEMA_GRID_ID));
        assertNoSecretRendered(view);
    }

    /** UC-036: the branding screen shows resolution and assets, and reload re-renders. */
    @Test
    void brandingViewRendersAssetsAndReloads() {
        SystemBrandingView view = attach(SystemBrandingView.class);

        assertEquals(view.getTranslation("systemBrandingView.title"), find(H3.class, view).single().getText());
        assertTrue(containsId(view, SystemBrandingView.SUMMARY_GRID_ID));
        assertTrue(containsId(view, SystemBrandingView.ASSETS_GRID_ID));

        view.render();

        assertEquals(2, find(Grid.class, view).all().size(), "reload must replace the grids, not duplicate them");
    }

    /** UC-036 BR-003: a brand without logo problems shows no warnings, and the preview is the default lockup. */
    @Test
    void brandingViewShowsNoWarningsForADefaultBrand() {
        SystemBrandingView view = attach(SystemBrandingView.class);

        assertFalse(containsId(view, SystemBrandingView.WARNINGS_ID));
        assertEquals("/api/brand/images/HorizontalLogo.png", find(Image.class, view).single().getSrc());
    }

    /** UC-036 A3, BR-003: each logo warning is listed and the configured lockup is previewed. */
    @Test
    void brandingViewListsLogoWarningsAndPreviewsTheConfiguredLockup() {
        QuarkusMock.installMockForType(new BrandDiagnostics() {
            @Override
            public BrandReport report() {
                return new BrandReport("/brand", "brand", true, false, "brand-config.json", "Acme", null, null,
                        new BrandUtil.BrandInfo("acme", "Acme", "logo.png", "acme"), List.of(),
                        "/api/brand/images/Wide.svg", List.of("first warning", "second warning"));
            }
        }, BrandDiagnostics.class);
        SystemBrandingView view = attach(SystemBrandingView.class);

        assertEquals(List.of("first warning", "second warning"),
                find(ListItem.class, view).all().stream().map(ListItem::getText).toList());
        assertTrue(containsId(view, SystemBrandingView.WARNINGS_ID));
        assertEquals("/api/brand/images/Wide.svg", find(Image.class, view).single().getSrc());
    }

    /** UC-037: the connections screen lists its targets or says there are none. */
    @Test
    void connectionsViewRendersItsTargets() {
        SystemConnectionsView view = attach(SystemConnectionsView.class);

        assertEquals(view.getTranslation("systemConnectionsView.title"), find(H3.class, view).single().getText());
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

        assertEquals(view.getTranslation("oidcDiagnosticsView.title"), find(H3.class, view).single().getText());
        String info = find(Pre.class, view).single().getText();
        assertTrue(info.contains("User: system.admin"), info);
        assertTrue(info.contains("Has elicit_author_admin: true"), info);
        assertTrue(info.contains("Has elicit_author: false"), info);
        assertEquals("**cdef", OidcDiagnosticsView.maskToken("abcdef", false));
        assertEquals("********wxyz", OidcDiagnosticsView.maskToken("abcdefghwxyz", false));
        assertEquals("abcdef", OidcDiagnosticsView.maskToken("abcdef", true));
    }
}
