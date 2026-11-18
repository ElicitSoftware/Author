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

import com.elicitsoftware.test.CapturedLog;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What Author does with its service token as it starts, and what it tells anyone about it.
 *
 * <p>Traceability: NFR-025; UC-034 BR-001, A4; UC-050 A7, BR-008.</p>
 */
class ServiceTokenTest {

    private static final String TOKEN = "a-token-only-this-test-knows-0123456789";
    private static final String LOGGER = ServiceToken.class.getName();

    private static ServiceToken token(String value) {
        return new ServiceToken(Optional.ofNullable(value));
    }

    /** NFR-025: a token of the required length starts quietly and is sent as a bearer credential. */
    @Test
    void aTokenOfTheRequiredLengthIsAcceptedAndBecomesABearerCredential() {
        ServiceToken serviceToken = token(TOKEN);
        try (CapturedLog log = CapturedLog.of(LOGGER)) {
            assertDoesNotThrow(serviceToken::validate);
            assertEquals("", log.text(), "nothing to report");
        }
        assertTrue(serviceToken.isConfigured());
        assertFalse(serviceToken.isDevelopmentValue());
        assertEquals(Optional.of("Bearer " + TOKEN), serviceToken.authorization());
    }

    /** NFR-025: exactly the minimum length is accepted. */
    @Test
    void theMinimumLengthIsThirtyTwoCharacters() {
        assertEquals(32, ServiceToken.MINIMUM_LENGTH);
        assertDoesNotThrow(() -> token("x".repeat(32)).validate());
        assertThrows(IllegalStateException.class, () -> token("x".repeat(31)).validate());
    }

    /** UC-050 A7: a short token fails the start, naming the variable and the length required, not the value or its length. */
    @Test
    void aShortTokenFailsTheStartWithoutDisclosingItself() {
        String shortToken = "short-value-7";
        try (CapturedLog log = CapturedLog.of(LOGGER)) {
            IllegalStateException failure = assertThrows(IllegalStateException.class, () -> token(shortToken).validate());

            String message = failure.getMessage();
            assertTrue(message.contains("ELICIT_SERVICE_TOKEN"), message);
            assertTrue(message.contains("32"), message);
            assertFalse(message.contains(shortToken), "the value is not in the message");
            assertFalse(message.contains(Integer.toString(shortToken.length())), "nor is its length");
            assertFalse(log.text().contains(shortToken), "nor in the log");
        }
    }

    /** UC-050 A7: no token is not a failed start; one warning names the variable. */
    @Test
    void noTokenStartsWithOneWarning() {
        for (String unset : new String[]{null, "", "   "}) {
            ServiceToken serviceToken = token(unset);
            try (CapturedLog log = CapturedLog.of(LOGGER)) {
                assertDoesNotThrow(serviceToken::validate);
                assertEquals(1, log.lines().size(), log.text());
                assertEquals(1, log.warnings(), log.text());
                assertTrue(log.text().contains("ELICIT_SERVICE_TOKEN is not set"), log.text());
            }
            assertFalse(serviceToken.isConfigured());
            assertEquals(Optional.empty(), serviceToken.authorization());
        }
    }

    /** UC-050 BR-008, UC-034 A4: the development value starts, with a warning that does not print it. */
    @Test
    void theDevelopmentValueIsWarnedAbout() {
        assertEquals("elicit-dev-service-token-not-for-a-real-site", ServiceToken.DEVELOPMENT_VALUE);
        ServiceToken serviceToken = token(ServiceToken.DEVELOPMENT_VALUE);
        try (CapturedLog log = CapturedLog.of(LOGGER)) {
            assertDoesNotThrow(serviceToken::validate);
            assertEquals(1, log.warnings(), log.text());
            assertTrue(log.text().contains("development value"), log.text());
            assertFalse(log.text().contains(ServiceToken.DEVELOPMENT_VALUE), log.text());
        }
        assertTrue(serviceToken.isDevelopmentValue());
        assertTrue(serviceToken.isConfigured());
    }

    /** UC-034 BR-001: the token's text form says only whether it is set. */
    @Test
    void theTextFormCarriesNoValue() {
        assertEquals("ServiceToken[set]", token(TOKEN).toString());
        assertEquals("ServiceToken[not set]", token(null).toString());
    }

    /** UC-050 A7: without a token the readiness check service-token is down, with the reason. */
    @Test
    void readinessIsDownWithTheReasonWhileNoTokenIsSet() {
        ServiceTokenHealthCheck check = new ServiceTokenHealthCheck();
        check.serviceToken = token(null);

        HealthCheckResponse response = check.call();

        assertEquals("service-token", response.getName());
        assertEquals(HealthCheckResponse.Status.DOWN, response.getStatus());
        assertEquals("ELICIT_SERVICE_TOKEN is not set", response.getData().orElseThrow().get("reason"));
    }

    /** NFR-025: with a token the readiness check is up and says nothing about the value. */
    @Test
    void readinessIsUpWithAToken() {
        ServiceTokenHealthCheck check = new ServiceTokenHealthCheck();
        check.serviceToken = token(TOKEN);

        HealthCheckResponse response = check.call();

        assertEquals("service-token", response.getName());
        assertEquals(HealthCheckResponse.Status.UP, response.getStatus());
        assertFalse(response.getData().isPresent());
    }
}
