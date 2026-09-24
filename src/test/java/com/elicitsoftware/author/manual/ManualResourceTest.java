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

import com.elicitsoftware.test.PostgresTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC-039 step 2: the manual is served to an author, and refused to anyone else (A4). The test
 * classpath carries a stand-in PDF at {@code src/test/resources/manual/}.
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class ManualResourceTest {

    @Test
    @TestSecurity(user = "manual.author", roles = {"elicit_author"})
    void anAuthorGetsTheManual() {
        Response response = given().when().get("/api/manual").then()
                .statusCode(200)
                .extract().response();

        assertEquals("application/pdf", response.getContentType());
        assertTrue(response.getHeader("Content-Disposition").contains(AuthorManual.FILE_NAME),
                "the browser should name the saved file " + AuthorManual.FILE_NAME);
        // "inline", so the browser's own viewer opens it rather than downloading it silently.
        assertTrue(response.getHeader("Content-Disposition").startsWith("inline"));
        assertTrue(response.asByteArray().length > 0, "the manual has content");
    }

    @Test
    @TestSecurity(user = "manual.admin", roles = {"elicit_admin"})
    void theAdminRoleAloneIsRefused() {
        // UC-002 BR-001: elicit_admin does not imply elicit_author, and the manual follows the
        // authoring views rather than the System pages.
        given().when().get("/api/manual").then().statusCode(403);
    }

    @Test
    void anAnonymousRequestIsRefused() {
        given().redirects().follow(false)
                .when().get("/api/manual")
                .then().statusCode(anyOfAuthChallenge());
    }

    /** OIDC answers an unauthenticated browser request with a redirect to the provider. */
    private static org.hamcrest.Matcher<Integer> anyOfAuthChallenge() {
        return org.hamcrest.Matchers.oneOf(302, 401);
    }
}
