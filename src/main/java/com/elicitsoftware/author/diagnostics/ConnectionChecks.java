package com.elicitsoftware.author.diagnostics;

/*-
 * ***LICENSE_START***
 * Elicit Survey
 * %%
 * Copyright (C) 2025 - 2026 The Regents of the University of Michigan - Rogel Cancer Center
 * %%
 * PolyForm Noncommercial License 1.0.0
 * <https://polyformproject.org/licenses/noncommercial/1.0.0>
 * ***LICENSE_END***
 */

import com.elicitsoftware.author.i18n.Translations;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Bounded, harmless probes of Author's outbound dependencies (UC-037): the identity provider
 * and the telemetry collector. Author calls nothing else; its only other dependency is the
 * database (UC-035). Every probe gives up after {@link #TIMEOUT} (NFR-015).
 */
@ApplicationScoped
public class ConnectionChecks {

    /** The bound on every probe. */
    public static final Duration TIMEOUT = Duration.ofSeconds(5);

    static final String DISCOVERY_SUFFIX = ".well-known/openid-configuration";

    /** How a target is probed. */
    public enum Kind {
        /** GET the OIDC discovery document and expect an issuer. */
        OIDC_DISCOVERY,
        /** GET the address; any HTTP answer counts as reachable. */
        HTTP,
        /** Open a TCP connection to host:port. */
        TCP
    }

    /**
     * One outbound dependency.
     *
     * @param group   what kind of dependency
     * @param name    the setting it came from
     * @param address the address as configured
     * @param kind    how it is probed
     */
    public record Target(String group, String name, String address, Kind kind) {
    }

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    public ConnectionChecks() {
        // CDI managed bean
    }

    /** Every target this deployment knows, in display order. */
    public List<Target> targets() {
        Config config = ConfigProvider.getConfig();
        List<Target> targets = new ArrayList<>();
        value(config, "quarkus.oidc.auth-server-url").ifPresent(url ->
                targets.add(new Target(Translations.get("systemConnectionsView.group.identityProvider"), "quarkus.oidc.auth-server-url", url, Kind.OIDC_DISCOVERY)));
        Optional<String> otlp = value(config, "quarkus.otel.exporter.otlp.endpoint")
                .or(() -> value(config, "quarkus.otel.exporter.otlp.traces.endpoint"));
        otlp.ifPresent(endpoint -> targets.add(new Target(Translations.get("systemConnectionsView.group.telemetryCollector"),
                "quarkus.otel.exporter.otlp.endpoint", endpoint, Kind.TCP)));
        return targets;
    }

    /** Probes one target within {@link #TIMEOUT}. */
    public CheckResult check(Target target) {
        long start = System.nanoTime();
        try {
            return switch (target.kind()) {
                case OIDC_DISCOVERY -> discovery(target, start);
                case HTTP -> http(target, start);
                case TCP -> tcp(target, start);
            };
        } catch (HttpTimeoutException e) {
            return CheckResult.down(target.name(), Translations.get("systemConnectionsView.result.timedOut",
                    Long.toString(TIMEOUT.toSeconds())), elapsed(start));
        } catch (IOException | RuntimeException e) {
            return CheckResult.down(target.name(), reason(e), elapsed(start));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CheckResult.down(target.name(), Translations.get("systemConnectionsView.result.interrupted"), elapsed(start));
        }
    }

    private CheckResult discovery(Target target, long start) throws IOException, InterruptedException {
        String base = target.address().endsWith("/") ? target.address() : target.address() + "/";
        URI uri = URI.create(base + DISCOVERY_SUFFIX);
        HttpResponse<String> response = client.send(get(uri), HttpResponse.BodyHandlers.ofString());
        long ms = elapsed(start);
        if (response.statusCode() == HttpURLConnection.HTTP_OK && response.body().contains("\"issuer\"")) {
            return CheckResult.up(target.name(), Translations.get("systemConnectionsView.result.discoveryServed", uri.toString()), ms);
        }
        return CheckResult.down(target.name(), Translations.get("systemConnectionsView.result.notDiscovery",
                Integer.toString(response.statusCode()), uri.toString()), ms);
    }

    private CheckResult http(Target target, long start) throws IOException, InterruptedException {
        HttpResponse<Void> response = client.send(get(URI.create(target.address())), HttpResponse.BodyHandlers.discarding());
        return CheckResult.up(target.name(), Translations.get("systemConnectionsView.result.reachable",
                Integer.toString(response.statusCode())), elapsed(start));
    }

    private static CheckResult tcp(Target target, long start) throws IOException {
        URI uri = target.address().contains("://") ? URI.create(target.address()) : URI.create("tcp://" + target.address());
        String host = uri.getHost();
        int port = uri.getPort();
        if (host == null || port < 0) {
            return CheckResult.unknown(target.name(), Translations.get("systemConnectionsView.result.noHostPort", target.address()));
        }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), (int) TIMEOUT.toMillis());
        }
        return CheckResult.up(target.name(), Translations.get("systemConnectionsView.result.connected", host, Integer.toString(port)),
                elapsed(start));
    }

    private static HttpRequest get(URI uri) {
        return HttpRequest.newBuilder(uri).GET().timeout(TIMEOUT).build();
    }

    private static long elapsed(long start) {
        return (System.nanoTime() - start) / 1_000_000;
    }

    private static String reason(Exception e) {
        if (e instanceof java.net.SocketTimeoutException) {
            return Translations.get("systemConnectionsView.result.timedOut", Long.toString(TIMEOUT.toSeconds()));
        }
        String message = e.getMessage();
        return message == null || message.isBlank() ? e.getClass().getSimpleName() : message;
    }

    private static Optional<String> value(Config config, String property) {
        try {
            return config.getOptionalValue(property, String.class).filter(v -> !v.isBlank());
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }
}
