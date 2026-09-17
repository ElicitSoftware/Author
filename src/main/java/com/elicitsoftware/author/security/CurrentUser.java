package com.elicitsoftware.author.security;

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

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

/**
 * The signed-in author's principal name, recorded as {@code published_by} on surveys and exports
 * (UC-003 BR-004, UC-008 BR-003). Vaadin UI requests run inside an HTTP request, so the
 * request-scoped identity is available from view code.
 */
@RequestScoped
public class CurrentUser {

    @Inject
    SecurityIdentity identity;

    /** @return the principal name, or {@code null} when no one is signed in. */
    public String name() {
        return identity == null || identity.isAnonymous() ? null : identity.getPrincipal().getName();
    }

    public boolean isAuthor() {
        return identity != null && identity.hasRole(AuthorRoles.AUTHOR);
    }
}
