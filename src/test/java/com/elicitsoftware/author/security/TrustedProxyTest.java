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

import com.elicitsoftware.test.PostgresTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * NFR-029: Forwarding Headers Only From a Trusted Proxy — with the shipped list, a proxy on loopback
 * or a private network decides the address Author builds its redirect URIs and links from, and the
 * client address it logs.
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class TrustedProxyTest {

    @Test
    @TestSecurity(user = "proxy.author", roles = {"elicit_author"})
    void aTrustedProxyDecidesTheSiteAddressAndTheClient() {
        given()
                .header("X-Forwarded-Proto", "https")
                .header("X-Forwarded-Host", "author.example.org")
                .header("X-Forwarded-For", "198.51.100.20")
                .when().get("/api/test-only/forwarded")
                .then().statusCode(200)
                .body(equalTo("https://author.example.org 198.51.100.20"));
    }
}
