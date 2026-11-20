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
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

/**
 * NFR-029: Forwarding Headers Only From a Trusted Proxy — a client that is not in the trusted list
 * cannot choose the address Author builds its redirect URIs from, nor the client address it logs,
 * by sending forwarding headers of its own.
 */
@QuarkusTest
@TestProfile(UntrustedProxyTest.UntrustedProxyProfile.class)
@QuarkusTestResource(PostgresTestResource.class)
class UntrustedProxyTest {

    @Test
    @TestSecurity(user = "proxy.author", roles = {"elicit_author"})
    void aClientOutsideTheTrustedListCannotForwardHeaders() {
        given()
                .header("X-Forwarded-Proto", "https")
                .header("X-Forwarded-Host", "attacker.example")
                .header("X-Forwarded-For", "198.51.100.20")
                .header("Forwarded", "for=198.51.100.21;host=attacker.example;proto=https")
                .when().get("/api/test-only/forwarded")
                .then().statusCode(200)
                .body(not(containsString("attacker.example")))
                .body(not(containsString("198.51.100.2")))
                .body(not(containsString("https://")));
    }

    /**
     * Trusts only a documentation address (RFC 5737) as the reverse proxy, so the test client,
     * which connects from loopback, is a client the forwarding headers must not be taken from.
     */
    public static class UntrustedProxyProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.http.proxy.trusted-proxies", "203.0.113.7");
        }
    }
}
