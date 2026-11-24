package com.elicitsoftware.author.survey;

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

import com.elicitsoftware.author.AuthoringException;
import com.elicitsoftware.author.security.ServiceToken;
import com.elicitsoftware.test.CapturedLog;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Author's call to the preview Survey carries the service token, goes only where the
 * deployment's configuration points, and tells no log line or message what the token is.
 *
 * <p>Traceability: UC-047 BR-008, A7, A8; NFR-025.</p>
 */
class PreviewSurveyClientTest {

    private static final String TOKEN = "preview-client-test-token-0123456789abcdef";
    private static final UUID KEY = UUID.fromString("5b0c1d6e-3f1a-4e0b-9c55-0a1b2c3d4e5f");

    /** One request as a server saw it. */
    private record Seen(String method, String path, String query, List<String> authorization) {
    }

    private HttpServer preview;
    private HttpServer elsewhere;
    private final List<Seen> atPreview = new CopyOnWriteArrayList<>();
    private final List<Seen> atElsewhere = new CopyOnWriteArrayList<>();
    private volatile int status = 200;
    private volatile String location;

    @BeforeEach
    void startServers() throws IOException {
        elsewhere = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        elsewhere.createContext("/", exchange -> {
            atElsewhere.add(seen(exchange));
            respond(exchange, 200, "{}");
        });
        elsewhere.start();
        preview = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        preview.createContext("/", exchange -> {
            atPreview.add(seen(exchange));
            if (location != null) {
                exchange.getResponseHeaders().add("Location", location);
            }
            respond(exchange, status, status == 200 ? "{\"dropped\":true}" : "");
        });
        preview.start();
    }

    @AfterEach
    void stopServers() {
        preview.stop(0);
        elsewhere.stop(0);
    }

    private static Seen seen(HttpExchange exchange) {
        List<String> authorization = exchange.getRequestHeaders().get("Authorization");
        return new Seen(exchange.getRequestMethod(), exchange.getRequestURI().getPath(), exchange.getRequestURI().getQuery(),
                authorization == null ? List.of() : List.copyOf(authorization));
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        }
        exchange.close();
    }

    private static String address(HttpServer server) {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private PreviewSurveyClient client(String token) {
        return new PreviewSurveyClient(address(preview), new ServiceToken(Optional.ofNullable(token)));
    }

    /** UC-047 BR-008: the drop is a DELETE by survey key carrying the token as a bearer credential, and nowhere in the address. */
    @Test
    void theDropCarriesTheServiceTokenAsABearerCredential() {
        assertDoesNotThrow(() -> client(TOKEN).dropReportingSchema(KEY));

        assertEquals(1, atPreview.size());
        Seen request = atPreview.get(0);
        assertEquals("DELETE", request.method());
        assertEquals("/api/etl/schema/" + KEY, request.path());
        assertEquals(List.of("Bearer " + TOKEN), request.authorization(), "exactly one Authorization header");
        assertEquals(null, request.query(), "the token is never a query parameter");
    }

    /** UC-047 A8: without a token no request is sent at all, and the refusal says why. */
    @Test
    void withoutATokenNoCallIsMade() {
        for (String unset : new String[]{null, "", "  "}) {
            AuthoringException refusal = assertThrows(AuthoringException.class, () -> client(unset).dropReportingSchema(KEY));

            assertTrue(refusal.getMessage().contains("service token not configured"), refusal.getMessage());
            assertTrue(refusal.getMessage().contains("ELICIT_SERVICE_TOKEN"), refusal.getMessage());
        }
        assertTrue(atPreview.isEmpty(), "an unauthenticated request is never sent");
    }

    /** UC-047 BR-008: a redirect is not followed, so the token reaches no second address. */
    @Test
    void aRedirectIsNotFollowed() {
        status = 302;
        location = address(elsewhere) + "/api/etl/schema/" + KEY;

        assertThrows(AuthoringException.class, () -> client(TOKEN).dropReportingSchema(KEY));

        assertEquals(1, atPreview.size());
        assertTrue(atElsewhere.isEmpty(), "the redirect target saw no request");
    }

    /** UC-047 A7: a 401 is reported as a token mismatch, naming the variable. */
    @Test
    void anUnauthorizedAnswerNamesTheSetting() {
        status = 401;

        AuthoringException refusal = assertThrows(AuthoringException.class, () -> client(TOKEN).dropReportingSchema(KEY));

        assertTrue(refusal.getMessage().contains("ELICIT_SERVICE_TOKEN"), refusal.getMessage());
        assertTrue(refusal.getMessage().contains("same service token"), refusal.getMessage());
    }

    /** UC-047 A7: any other answer than 200 still refuses the delete, with the answer named. */
    @Test
    void aNotConfiguredAnswerFromThePreviewRefusesTheDelete() {
        status = 503;

        AuthoringException refusal = assertThrows(AuthoringException.class, () -> client(TOKEN).dropReportingSchema(KEY));

        assertTrue(refusal.getMessage().contains("503"), refusal.getMessage());
    }

    /** NFR-025: no outcome of the call writes the token to the log or into a message. */
    @Test
    void theTokenIsInNoLogLineAndNoMessage() throws IOException {
        try (CapturedLog log = CapturedLog.of("com.elicitsoftware.author")) {
            StringBuilder messages = new StringBuilder();
            client(TOKEN).dropReportingSchema(KEY);
            for (int answer : new int[]{401, 403, 500, 503, 302}) {
                status = answer;
                location = answer == 302 ? address(elsewhere) + "/" : null;
                messages.append(assertThrows(AuthoringException.class, () -> client(TOKEN).dropReportingSchema(KEY)).getMessage());
            }
            // Unreachable: the address of a server that has stopped.
            PreviewSurveyClient unreachable = client(TOKEN);
            preview.stop(0);
            messages.append(assertThrows(AuthoringException.class, () -> unreachable.dropReportingSchema(KEY)).getMessage());
            messages.append(assertThrows(AuthoringException.class, () -> client(null).dropReportingSchema(KEY)).getMessage());

            assertFalse(log.lines().isEmpty(), "the outcomes were logged, so the capture is live");
            assertFalse(log.text().contains(TOKEN), log.text());
            assertFalse(log.text().contains("Bearer"), log.text());
            assertFalse(messages.toString().contains(TOKEN), messages.toString());
            assertFalse(messages.toString().contains("Bearer"), messages.toString());
        }
    }
}
