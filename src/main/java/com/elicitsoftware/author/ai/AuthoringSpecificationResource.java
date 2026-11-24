package com.elicitsoftware.author.ai;

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

import com.elicitsoftware.author.security.AuthorRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

import java.io.InputStream;

/**
 * Serves the packaged AI authoring specification at {@code /api/authoring-spec} (UC-042 step 2).
 * <p>
 * The specification is authoring documentation, so it is gated on the authoring role exactly as
 * the manual and the authoring views are: a signed-in user without {@code elicit_author} is
 * refused. A build that carries no specification answers 404, and the navigation does not offer
 * it in the first place (UC-042 A5).
 */
@Path("/authoring-spec")
public class AuthoringSpecificationResource {

    @Inject
    AuthoringSpecification specification;

    /** Creates the resource; CDI injects the specification. */
    public AuthoringSpecificationResource() {
        // CDI managed bean
    }

    /**
     * Returns the specification as a Markdown attachment named {@value AuthoringSpecification#FILE_NAME}.
     *
     * @return 200 with the document, or 404 when this build carries none
     */
    @GET
    @Produces("text/markdown; charset=UTF-8")
    @RolesAllowed(AuthorRoles.AUTHOR)
    public Response specification() {
        InputStream document = specification.open();
        if (document == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        // "attachment": the author saves the file to hand to an agent that takes attachments.
        // A browser that rendered Markdown inline would give them nothing to attach.
        return Response.ok(document)
                .header("Content-Disposition", "attachment; filename=\"" + AuthoringSpecification.FILE_NAME + "\"")
                .build();
    }
}
