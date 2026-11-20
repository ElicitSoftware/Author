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

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.InputStream;

/**
 * The author's manual as it ships inside the application (UC-039 BR-004).
 * <p>
 * The PDF is produced by {@code docs/manual/build-manual.sh} during the image build and lands on
 * the classpath at {@value #DEFAULT_RESOURCE}. It is deliberately <em>not</em> under
 * {@code META-INF/resources}, which Quarkus would serve without a role check; it reaches the
 * browser only through {@link ManualResource}, which requires the authoring role.
 * <p>
 * A build that did not produce it simply has no manual: {@link #isAvailable()} is false and the
 * header leaves the link out (UC-039 A1).
 */
@ApplicationScoped
public class AuthorManual {

    /** Where {@code build-manual.sh} puts the PDF, relative to the classpath root. */
    static final String DEFAULT_RESOURCE = "manual/elicit-author-manual.pdf";

    /** What the browser calls the file when the reader saves it. */
    public static final String FILE_NAME = "elicit-author-manual.pdf";

    /** Overridable so a test can exercise the build that carries no manual (UC-039 A1). */
    @ConfigProperty(name = "author.manual.resource", defaultValue = DEFAULT_RESOURCE)
    String resource;

    /** Creates the bean; CDI injects the resource path. */
    public AuthorManual() {
        // CDI managed bean
    }

    /**
     * Whether this build carries the manual.
     *
     * @return whether the manual's resource is on the classpath
     */
    public boolean isAvailable() {
        return loader().getResource(resource) != null;
    }

    /**
     * Opens the packaged manual.
     *
     * @return the PDF's bytes, or {@code null} when this build carries no manual
     */
    public InputStream open() {
        return loader().getResourceAsStream(resource);
    }

    private static ClassLoader loader() {
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        return contextLoader != null ? contextLoader : AuthorManual.class.getClassLoader();
    }
}
