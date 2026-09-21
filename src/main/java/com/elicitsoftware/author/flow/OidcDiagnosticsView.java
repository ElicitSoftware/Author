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

import com.elicitsoftware.author.security.AuthorRoles;
import com.elicitsoftware.author.security.RoleSecurityIdentityAugmentor;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import io.quarkus.oidc.AccessTokenCredential;
import io.quarkus.oidc.IdToken;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * OIDC diagnostics (UC-038): the current sign-in's principal, roles, where the roles came
 * from, and the tokens masked to their last four characters unless
 * {@code elicit.debug.reveal-tokens=true}. Mirrors the Admin console's page.
 */
@Route(value = "oidc", layout = MainLayout.class)
@RolesAllowed(AuthorRoles.ADMIN)
public class OidcDiagnosticsView extends VerticalLayout implements HasDynamicTitle {

    static final String INFO_ID = "oidc-info";

    @Inject
    SecurityIdentity identity;

    @Inject
    @IdToken
    Instance<JsonWebToken> idTokenInstance;

    @Inject
    Instance<AccessTokenCredential> accessTokenInstance;

    @ConfigProperty(name = "elicit.debug.reveal-tokens", defaultValue = "false")
    boolean revealTokens;

    public OidcDiagnosticsView() {
        setWidthFull();
        setPadding(true);
    }

    @PostConstruct
    void init() {
        add(new H3(getTranslation("oidcDiagnosticsView.title")));
        add(new Paragraph(getTranslation("oidcDiagnosticsView.intro")));

        StringBuilder sb = new StringBuilder();
        sb.append(getTranslation("oidcDiagnosticsView.user", identity.getPrincipal().getName())).append('\n');
        sb.append(getTranslation("oidcDiagnosticsView.isAnonymous", String.valueOf(identity.isAnonymous()))).append('\n');
        sb.append(getTranslation("oidcDiagnosticsView.roles", String.valueOf(identity.getRoles()))).append('\n');
        String roleSource = identity.getAttribute(RoleSecurityIdentityAugmentor.ROLE_SOURCE_ATTRIBUTE);
        sb.append(getTranslation("oidcDiagnosticsView.roleSource", String.valueOf(roleSource))).append('\n');
        sb.append(getTranslation("oidcDiagnosticsView.hasRole", AuthorRoles.AUTHOR,
                String.valueOf(identity.hasRole(AuthorRoles.AUTHOR)))).append('\n');
        sb.append(getTranslation("oidcDiagnosticsView.hasRole", AuthorRoles.ADMIN,
                String.valueOf(identity.hasRole(AuthorRoles.ADMIN)))).append("\n\n");

        try {
            if (idTokenInstance.isResolvable()) {
                JsonWebToken idToken = idTokenInstance.get();
                sb.append(getTranslation("oidcDiagnosticsView.issuer", String.valueOf(idToken.getIssuer()))).append('\n');
                sb.append(getTranslation("oidcDiagnosticsView.email", String.valueOf((Object) idToken.getClaim("email")))).append('\n');
                sb.append(getTranslation("oidcDiagnosticsView.idToken", maskToken(idToken.getRawToken(), revealTokens))).append("\n\n");
            } else {
                sb.append(getTranslation("oidcDiagnosticsView.idTokenUnavailable")).append("\n\n");
            }
        } catch (Exception e) {
            sb.append(getTranslation("oidcDiagnosticsView.idTokenError", String.valueOf(e.getMessage()))).append("\n\n");
        }
        try {
            if (accessTokenInstance.isResolvable()) {
                sb.append(getTranslation("oidcDiagnosticsView.accessToken",
                        maskToken(accessTokenInstance.get().getToken(), revealTokens))).append('\n');
            } else {
                sb.append(getTranslation("oidcDiagnosticsView.accessTokenUnavailable")).append('\n');
            }
        } catch (Exception e) {
            sb.append(getTranslation("oidcDiagnosticsView.accessTokenError", String.valueOf(e.getMessage()))).append('\n');
        }

        Pre info = new Pre(sb.toString());
        info.setId(INFO_ID);
        info.addClassName("debug-info");
        add(info);
    }

    /** Masks a token to its last four characters unless revealing is switched on (NFR-014). */
    static String maskToken(String rawToken, boolean reveal) {
        if (reveal || rawToken == null) {
            return rawToken;
        }
        int visible = Math.min(4, rawToken.length());
        return "*".repeat(Math.max(0, rawToken.length() - visible)) + rawToken.substring(rawToken.length() - visible);
    }

    @Override
    public String getPageTitle() {
        return getTranslation("oidcDiagnosticsView.pageTitle");
    }
}
