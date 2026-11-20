package com.elicitsoftware.author.manual;

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

import com.elicitsoftware.author.security.AuthorRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

import java.io.InputStream;

/**
 * Serves the packaged author's manual at {@code /api/manual} (UC-039 step 2).
 * <p>
 * The manual is authoring documentation, so it is gated on the authoring role exactly as the
 * authoring views are: a signed-in user without {@code elicit_author} is refused (UC-039 A4).
 * A build that carries no manual answers 404, and the header does not offer the link in the
 * first place (UC-039 A1).
 */
@Path("/manual")
public class ManualResource {

    @Inject
    AuthorManual manual;

    /** Creates the resource; the container injects the manual. */
    public ManualResource() {
        // CDI managed bean
    }

    /**
     * The manual as a PDF, to be shown inline.
     *
     * @return 200 with the PDF, or 404 when the build carries no manual
     */
    @GET
    @Produces("application/pdf")
    @RolesAllowed(AuthorRoles.AUTHOR)
    public Response manual() {
        InputStream pdf = manual.open();
        if (pdf == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        // "inline" so the browser's own viewer opens it in the new tab; the reader can still
        // save or print it from there (UC-039 A2).
        return Response.ok(pdf)
                .header("Content-Disposition", "inline; filename=\"" + AuthorManual.FILE_NAME + "\"")
                .build();
    }
}
