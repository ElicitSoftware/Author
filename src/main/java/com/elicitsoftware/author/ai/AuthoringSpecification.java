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

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * The AI authoring specification as it ships inside the application (UC-042 BR-006).
 * <p>
 * The document is authored at {@code docs/ai/AUTHORING_FROM_AI.md} and copied onto the classpath
 * at {@value #DEFAULT_RESOURCE} by a {@code <resource>} block in {@code pom.xml}. Like the
 * manual it is deliberately <em>not</em> under {@code META-INF/resources}, which Quarkus would
 * serve without a role check; it reaches the browser only through
 * {@link AuthoringSpecificationResource}, which requires the authoring role.
 * <p>
 * A build that did not package it simply does not offer the path: {@link #isAvailable()} is
 * false and the navigation leaves it out (UC-042 A5).
 */
@ApplicationScoped
public class AuthoringSpecification {

    /** Where the pom's resource block puts the document, relative to the classpath root. */
    static final String DEFAULT_RESOURCE = "ai/AUTHORING_FROM_AI.md";

    /** What the browser calls the file when the author saves it. */
    public static final String FILE_NAME = "AUTHORING_FROM_AI.md";

    /** Overridable so a test can exercise the build that carries no specification (UC-042 A5). */
    @ConfigProperty(name = "author.ai.specification.resource", defaultValue = DEFAULT_RESOURCE)
    String resource;

    /** Creates the bean; CDI injects the resource path. */
    public AuthoringSpecification() {
        // CDI managed bean
    }

    /**
     * Whether this build carries the specification.
     *
     * @return {@code true} when the resource is on the classpath
     */
    public boolean isAvailable() {
        return loader().getResource(resource) != null;
    }

    /**
     * Opens the packaged specification.
     *
     * @return the document's bytes, or {@code null} when this build carries none
     */
    public InputStream open() {
        return loader().getResourceAsStream(resource);
    }

    /**
     * Reads the packaged specification as text, for the copy-to-clipboard payload.
     *
     * @return the document, or {@code null} when this build carries none
     */
    public String text() {
        try (InputStream in = open()) {
            return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read " + resource, e);
        }
    }

    private static ClassLoader loader() {
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        return contextLoader != null ? contextLoader : AuthoringSpecification.class.getClassLoader();
    }
}
