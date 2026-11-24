package com.elicitsoftware.author.manual;

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
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC-039: the manual ships with the application (BR-004). The test classpath carries a stand-in
 * PDF at {@code src/test/resources/manual/}, so these tests exercise the build that has one;
 * {@link MissingManualTest} covers the build that does not (A1).
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class AuthorManualTest {

    @Inject
    AuthorManual manual;

    @Test
    void aBuildThatCarriesTheManualOffersIt() {
        assertTrue(manual.isAvailable(), "the packaged manual should be found on the classpath");
    }

    @Test
    void theManualOpensAsAPdf() throws Exception {
        try (InputStream pdf = manual.open()) {
            assertNotNull(pdf, "isAvailable() and open() must agree");
            byte[] header = pdf.readNBytes(5);
            assertEquals("%PDF-", new String(header), "the packaged manual is a PDF");
        }
    }

    @Test
    void theFileNameIsWhatTheReaderSaves() {
        assertEquals("elicit-author-manual.pdf", AuthorManual.FILE_NAME);
    }
}
