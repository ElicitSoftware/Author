package com.elicitsoftware.author.diagnostics;

/*-
 * ***LICENSE_START***
 * Elicit Author
 * %%
 * Copyright (C) 2025 - 2026 The Regents of the University of Michigan - Rogel Cancer Center
 * Copyright (C) 2026 Elicit Software LLC
 * %%
 * PolyForm Noncommercial License 1.0.0
 * <https://polyformproject.org/licenses/noncommercial/1.0.0>
 * ***LICENSE_END***
 */

import com.elicitsoftware.test.PostgresTestResource;
import com.sun.net.httpserver.HttpServer;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Probes are harmless, bounded, and read the right answer from each kind of target.
 *
 * <p>Traceability: UC-037 (Check Connections), A1, A2, A3, NFR-015.</p>
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class ConnectionChecksTest {

    private static final List<String> AUTHORIZATION_SEEN = new java.util.concurrent.CopyOnWriteArrayList<>();
    private static final java.util.concurrent.atomic.AtomicInteger PROBED = new java.util.concurrent.atomic.AtomicInteger();

    private static HttpServer server;
    private static String base;

    @Inject
    ConnectionChecks checks;

    @BeforeAll
    static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ok", exchange -> respond(exchange, 200, "ok"));
        server.createContext("/protected/401", exchange -> {
            exchange.getResponseHeaders().add("WWW-Authenticate", "Bearer");
            respond(exchange, 401, "");
        });
        server.createContext("/protected/503", exchange -> respond(exchange, 503, "{\"error\":\"service token not configured\"}"));
        server.createContext("/forbidden", exchange -> respond(exchange, 403, "no license"));
        server.createContext("/realm/.well-known/openid-configuration",
                exchange -> respond(exchange, 200, "{\"issuer\":\"" + base + "/realm\"}"));
        server.createContext("/plain/.well-known/openid-configuration",
                exchange -> respond(exchange, 200, "<html>not a discovery document</html>"));
        server.createContext("/slow", exchange -> {
            try {
                Thread.sleep(ConnectionChecks.TIMEOUT.toMillis() + 2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            respond(exchange, 200, "late");
        });
        server.start();
        base = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterAll
    static void stopServer() {
        server.stop(0);
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, int status, String body) throws IOException {
        PROBED.incrementAndGet();
        List<String> authorization = exchange.getRequestHeaders().get("Authorization");
        if (authorization != null) {
            AUTHORIZATION_SEEN.addAll(authorization);
        }
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    /** UC-037 step 4: any HTTP answer counts as reachable. */
    @Test
    void httpTargetThatAnswersIsUp() {
        CheckResult result = checks.check(new ConnectionChecks.Target("Report service", "t", base + "/ok",
                ConnectionChecks.Kind.HTTP));

        assertTrue(result.isUp(), result.detail());
        assertTrue(result.detail().contains("HTTP 200"));
    }

    /** UC-037 BR-001: an endpoint that requires the service token answers 401, or 503 without one of its own; both count as reachable. */
    @Test
    void anEndpointThatRequiresTheServiceTokenIsReachable() {
        for (int status : new int[]{401, 503}) {
            CheckResult result = checks.check(new ConnectionChecks.Target("Preview Survey", "t", base + "/protected/" + status,
                    ConnectionChecks.Kind.HTTP));

            assertTrue(result.isUp(), result.detail());
            assertTrue(result.detail().contains("HTTP " + status), result.detail());
        }
    }

    /** UC-037 BR-001: no probe of any kind carries a credential. */
    @Test
    void probesCarryNoCredential() {
        AUTHORIZATION_SEEN.clear();
        PROBED.set(0);
        checks.check(new ConnectionChecks.Target("Preview Survey", "t", base + "/protected/401", ConnectionChecks.Kind.HTTP));
        checks.check(new ConnectionChecks.Target("Report service", "t", base + "/ok", ConnectionChecks.Kind.HTTP));
        checks.check(new ConnectionChecks.Target("Identity provider", "oidc", base + "/realm/", ConnectionChecks.Kind.OIDC_DISCOVERY));

        assertTrue(PROBED.get() >= 3, "every probe reached the server");
        assertTrue(AUTHORIZATION_SEEN.isEmpty(), "no Authorization header on a probe");
    }

    /** UC-037 step 4: the discovery document is recognized by its issuer. */
    @Test
    void discoveryDocumentIsUp() {
        CheckResult result = checks.check(new ConnectionChecks.Target("Identity provider", "oidc", base + "/realm/",
                ConnectionChecks.Kind.OIDC_DISCOVERY));

        assertTrue(result.isUp(), result.detail());
    }

    /** UC-037 A3: an answer without an issuer is misaddressed, not up. */
    @Test
    void nonDiscoveryAnswerIsDown() {
        CheckResult result = checks.check(new ConnectionChecks.Target("Identity provider", "oidc", base + "/plain",
                ConnectionChecks.Kind.OIDC_DISCOVERY));

        assertFalse(result.isUp());
        assertTrue(result.detail().contains("not an OIDC discovery document"), result.detail());
    }

    /** UC-037 A1 / NFR-015: a target that does not answer in time is reported as timed out. */
    @Test
    void slowTargetTimesOut() {
        CheckResult result = checks.check(new ConnectionChecks.Target("Report service", "t", base + "/slow",
                ConnectionChecks.Kind.HTTP));

        assertFalse(result.isUp());
        assertTrue(result.detail().contains("timed out"), result.detail());
        assertTrue(result.durationMs() < ConnectionChecks.TIMEOUT.toMillis() + 1500, "the probe must give up at the bound");
    }

    /** UC-037 step 4: a socket target is connected to and closed. */
    @Test
    void tcpTargetIsUpWhenListeningAndDownWhenClosed() throws IOException {
        int listening = server.getAddress().getPort();
        int closed;
        try (ServerSocket socket = new ServerSocket(0)) {
            closed = socket.getLocalPort();
        }

        CheckResult up = checks.check(new ConnectionChecks.Target("Mail relay", "smtp", "127.0.0.1:" + listening,
                ConnectionChecks.Kind.TCP));
        CheckResult down = checks.check(new ConnectionChecks.Target("Telemetry collector", "otlp",
                "http://127.0.0.1:" + closed, ConnectionChecks.Kind.TCP));

        assertTrue(up.isUp(), up.detail());
        assertFalse(down.isUp());
    }

    /** UC-037 step 2: targets come from configuration; a mocked mailer and a blank provider add none. */
    @Test
    void targetsFollowConfiguration() {
        List<ConnectionChecks.Target> targets = checks.targets();

        assertFalse(targets.stream().anyMatch(t -> t.group().equals("Identity provider")),
                "a blank auth-server-url adds no target");
        assertEquals(0, targets.stream().filter(t -> t.address() == null || t.address().isBlank()).count());
    }
}
