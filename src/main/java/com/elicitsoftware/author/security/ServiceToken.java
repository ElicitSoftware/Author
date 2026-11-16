package com.elicitsoftware.author.security;

/*-
 * ***LICENSE_START***
 * Elicit Author
 * %%
 * Copyright (C) 2025 - 2026 The Regents of the University of Michigan - Rogel Cancer Center
 * %%
 * PolyForm Noncommercial License 1.0.0
 * <https://polyformproject.org/licenses/noncommercial/1.0.0>
 * ***LICENSE_END***
 */

import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Optional;

/**
 * The deployment's service token (NFR-025): the shared secret Elicit's modules authenticate
 * their calls to each other with. Author is a caller only and makes one such call, the request
 * to the preview Survey that drops a reporting schema (UC-047 BR-008).
 * <p>
 * The value is read here and nowhere else, and it leaves this class only as the
 * {@code Authorization} header value of that one request. It is never logged, rendered, put in
 * a URL or included in a message: everything else is told only whether a token is configured
 * (UC-034 BR-001). {@link #toString()} does not carry it.
 * <p>
 * At startup: a token that is set but shorter than {@link #MINIMUM_LENGTH} characters fails
 * the start; no token logs one warning and leaves Author not ready
 * ({@link ServiceTokenHealthCheck}); the development value logs a warning.
 */
@ApplicationScoped
public class ServiceToken {

    /** The environment variable the packaged configuration reads the token from. */
    public static final String VARIABLE = "ELICIT_SERVICE_TOKEN";

    /** The shortest token accepted. */
    public static final int MINIMUM_LENGTH = 32;

    /**
     * The value the platform's compose files and the development profile carry, so that a
     * checkout runs with no setup. It is public knowledge and protects nothing at a real site.
     */
    public static final String DEVELOPMENT_VALUE = "elicit-dev-service-token-not-for-a-real-site";

    /** What a caller reports when it has no token to send. */
    public static final String NOT_CONFIGURED = "service token not configured";

    private final String token;

    @Inject
    public ServiceToken(@ConfigProperty(name = "elicit.service.token") Optional<String> configured) {
        this.token = configured.map(String::trim).filter(v -> !v.isEmpty()).orElse(null);
    }

    void onStart(@Observes StartupEvent event) {
        validate();
    }

    /**
     * Checks the token as the application starts.
     *
     * @throws IllegalStateException when a token is set but too short; the message names the
     *                               variable and the length required, never the value or its length
     */
    void validate() {
        if (token == null) {
            Log.warn(VARIABLE + " is not set. Author reports not ready, and will not ask the preview Survey"
                    + " to drop a reporting schema, until a service token is supplied.");
            return;
        }
        if (token.length() < MINIMUM_LENGTH) {
            throw new IllegalStateException(VARIABLE + " is too short: a service token must be at least "
                    + MINIMUM_LENGTH + " characters. Generate one with: openssl rand -hex 32");
        }
        if (isDevelopmentValue()) {
            Log.warn(VARIABLE + " is the well-known development value. Replace it before this Author"
                    + " and its preview Survey are used at a real site.");
        }
    }

    /** Whether a token is configured. */
    public boolean isConfigured() {
        return token != null;
    }

    /** Whether the configured token is the development value. */
    public boolean isDevelopmentValue() {
        return DEVELOPMENT_VALUE.equals(token);
    }

    /**
     * The {@code Authorization} header value for a call to another module, or empty when no
     * token is configured, in which case the call must not be made. Only for a request to an
     * address the deployment's own configuration names.
     */
    public Optional<String> authorization() {
        return Optional.ofNullable(token).map(t -> "Bearer " + t);
    }

    @Override
    public String toString() {
        return "ServiceToken[" + (token == null ? "not set" : "set") + "]";
    }
}
