package com.elicitsoftware.author.security;

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

import io.quarkus.logging.Log;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Stamps every authenticated identity with where its roles came from. Author has no database
 * fallback (unlike Admin): roles are exactly what the identity provider assigned for the
 * {@code elicit-author} client, and nothing is implied from other roles (UC-001 BR-001, UC-002).
 */
@ApplicationScoped
public class RoleSecurityIdentityAugmentor implements SecurityIdentityAugmentor {

    public static final String ROLE_SOURCE_ATTRIBUTE = "roleSource";
    public static final String ROLE_SOURCE_OIDC = "oidc";

    @Override
    public Uni<SecurityIdentity> augment(SecurityIdentity identity, AuthenticationRequestContext context) {
        if (identity.isAnonymous()) {
            return Uni.createFrom().item(identity);
        }
        boolean hasAuthorRole = identity.getRoles().stream().anyMatch(AuthorRoles.ALL::contains);
        Log.debugf("Principal %s roles %s; authoring access %s", identity.getPrincipal().getName(),
                identity.getRoles(), hasAuthorRole ? "granted" : "not granted");
        return Uni.createFrom().item(QuarkusSecurityIdentity.builder(identity)
                .addAttribute(ROLE_SOURCE_ATTRIBUTE, ROLE_SOURCE_OIDC)
                .build());
    }
}
