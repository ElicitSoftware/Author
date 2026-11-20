package com.elicitsoftware.test;

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

import io.vertx.core.http.HttpServerRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;

/**
 * Test-only resource that answers with the address Author believes it is served at and the client
 * address it would log, as {@code <scheme>://<host> <client>}. NFR-029's tests read it to see
 * whether a forwarding header was honored.
 */
@Path("/test-only/forwarded") // served under /api, Author's JAX-RS root
public class ForwardedEchoResource {

    @Context
    UriInfo uriInfo;

    @Context
    HttpServerRequest request;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String echo() {
        var base = uriInfo.getBaseUri();
        return base.getScheme() + "://" + base.getHost() + " " + request.remoteAddress().hostAddress();
    }
}
