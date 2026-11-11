package com.elicitsoftware.author.ai;

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

import com.elicitsoftware.test.PostgresTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC-042 step 2: Author hands out the specification itself, so an author with no checkout of
 * this repository can still take it to an AI session. It is authoring documentation, so it is
 * gated exactly as the manual is (UC-002 BR-001).
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class AuthoringSpecificationResourceTest {

    @Test
    @TestSecurity(user = "spec.author", roles = {"elicit_author"})
    void anAuthorGetsTheSpecification() {
        Response response = given().when().get("/api/authoring-spec").then()
                .statusCode(200)
                .extract().response();

        assertTrue(response.getContentType().startsWith("text/markdown"), response.getContentType());
        assertTrue(response.getHeader("Content-Disposition").contains(AuthoringSpecification.FILE_NAME),
                "the browser should name the saved file " + AuthoringSpecification.FILE_NAME);
        // "attachment": the author needs a file to hand to an agent, not a rendered page.
        assertTrue(response.getHeader("Content-Disposition").startsWith("attachment"));
        assertTrue(response.asString().contains("ELICIT_SURVEY_DRAFT_V1"),
                "what is served is the authoring specification, not some other document");
    }

    /**
     * UC-042 BR-006: what the image serves is the reviewed document, byte for byte. A packaged
     * copy that had drifted from {@code docs/ai/} would be gated by nothing — the drift test
     * (NFR-017) reads the working tree.
     */
    @Test
    @TestSecurity(user = "spec.author", roles = {"elicit_author"})
    void whatIsServedIsTheDocumentUnderDocs() throws Exception {
        String served = given().when().get("/api/authoring-spec").then()
                .statusCode(200).extract().asString();
        String authored = Files.readString(
                Path.of("docs", "ai", AuthoringSpecification.FILE_NAME), StandardCharsets.UTF_8);
        assertEquals(authored, served);
    }

    @Test
    @TestSecurity(user = "spec.admin", roles = {"elicit_author_admin"})
    void theAdminRoleAloneIsRefused() {
        given().when().get("/api/authoring-spec").then().statusCode(403);
    }

    @Test
    void anAnonymousRequestIsRefused() {
        given().redirects().follow(false)
                .when().get("/api/authoring-spec")
                .then().statusCode(org.hamcrest.Matchers.oneOf(302, 401));
    }
}
