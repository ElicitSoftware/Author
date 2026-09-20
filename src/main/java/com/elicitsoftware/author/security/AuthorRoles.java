package com.elicitsoftware.author.security;

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

import java.util.Set;

/**
 * Roles Author understands. They are Keycloak <em>client</em> roles of the {@code elicit-author}
 * client; roles held for other Elicit clients (including {@code elicit_admin} on the Admin client)
 * grant nothing here (UC-002 BR-001).
 */
public final class AuthorRoles {

    /** The only role that grants authoring access. */
    public static final String AUTHOR = "elicit_author";

    public static final Set<String> ALL = Set.of(AUTHOR);

    private AuthorRoles() {
    }
}
