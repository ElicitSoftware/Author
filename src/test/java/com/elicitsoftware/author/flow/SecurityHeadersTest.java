package com.elicitsoftware.author.flow;

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

import com.elicitsoftware.test.PostgresTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * NFR-024: the headers every response carries, read off three kinds of response -- a brand asset
 * (served without a session, UC-030), a path that does not exist, and the challenge an
 * unauthenticated request for a page gets.
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class SecurityHeadersTest {

    private static void assertHeaders(Response response) {
        assertEquals("nosniff", response.getHeader("X-Content-Type-Options"));
        assertEquals("DENY", response.getHeader("X-Frame-Options"));
        assertEquals("same-origin", response.getHeader("Referrer-Policy"));
    }

    @Test
    void aBrandAssetCarriesTheHeaders() {
        assertHeaders(given().when().get("/api/brand/theme.css").then().statusCode(200).extract().response());
    }

    @Test
    void aBrandAssetThatDoesNotExistCarriesTheHeaders() {
        assertHeaders(given().when().get("/api/brand/does-not-exist-anywhere.xyz").then().statusCode(404).extract().response());
    }

    @Test
    void theAnswerToAnUnauthenticatedRequestCarriesTheHeaders() {
        assertHeaders(given().redirects().follow(false).when().get("/").then().extract().response());
    }
}
