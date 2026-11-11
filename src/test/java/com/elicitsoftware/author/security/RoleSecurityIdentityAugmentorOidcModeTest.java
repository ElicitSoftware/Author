package com.elicitsoftware.author.security;

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
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** UC-001 BR-001 / UC-002 BR-001: roles are taken verbatim from the identity provider; nothing is implied. */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class RoleSecurityIdentityAugmentorOidcModeTest {

    private static final AuthenticationRequestContext SYNC_CONTEXT =
            supplier -> Uni.createFrom().item(supplier.get());

    @Inject
    RoleSecurityIdentityAugmentor augmentor;

    @Test
    void authorRolePassesThroughAndIsStampedAsOidc() {
        SecurityIdentity identity = QuarkusSecurityIdentity.builder()
                .setPrincipal(new QuarkusPrincipal("author"))
                .addRole(AuthorRoles.AUTHOR)
                .build();
        SecurityIdentity augmented = augmentor.augment(identity, SYNC_CONTEXT).await().indefinitely();
        assertTrue(augmented.hasRole(AuthorRoles.AUTHOR));
        assertEquals(RoleSecurityIdentityAugmentor.ROLE_SOURCE_OIDC,
                augmented.getAttribute(RoleSecurityIdentityAugmentor.ROLE_SOURCE_ATTRIBUTE));
    }

    @Test
    void adminRoleDoesNotImplyAuthor() {
        SecurityIdentity identity = QuarkusSecurityIdentity.builder()
                .setPrincipal(new QuarkusPrincipal("admin"))
                .addRole(AuthorRoles.AUTHOR_ADMIN)
                .build();
        SecurityIdentity augmented = augmentor.augment(identity, SYNC_CONTEXT).await().indefinitely();
        assertFalse(augmented.hasRole(AuthorRoles.AUTHOR));
    }

    @Test
    void anonymousIdentityIsLeftAlone() {
        SecurityIdentity anonymous = QuarkusSecurityIdentity.builder().setAnonymous(true).build();
        SecurityIdentity augmented = augmentor.augment(anonymous, SYNC_CONTEXT).await().indefinitely();
        assertTrue(augmented.isAnonymous());
        assertTrue(augmented.getRoles().isEmpty());
    }
}
