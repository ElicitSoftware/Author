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

import java.util.Set;

/**
 * Roles Author understands. They are Keycloak <em>client</em> roles of the {@code elicit-author}
 * client; roles held for other Elicit clients (including Admin's {@code elicit_admin})
 * grant nothing here (UC-002 BR-001).
 */
public final class AuthorRoles {

    /** The only role that grants authoring access. */
    public static final String AUTHOR = "elicit_author";

    /**
     * Author administrator: sees the System setup and diagnostics section (UC-034 to UC-038)
     * but gains no authoring access from this role alone (UC-002 BR-001). Named apart from
     * Admin's {@code elicit_admin} so the two are not confused.
     */
    public static final String AUTHOR_ADMIN = "elicit_author_admin";

    public static final Set<String> ALL = Set.of(AUTHOR);

    private AuthorRoles() {
    }
}
