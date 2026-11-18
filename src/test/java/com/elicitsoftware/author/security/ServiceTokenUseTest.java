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

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Where the service token may be read and sent, held by reading the sources: a new use of it
 * changes this test, so a reviewer sees it.
 *
 * <p>Traceability: NFR-025; UC-047 BR-008; UC-037 BR-001; UC-034 BR-001.</p>
 */
class ServiceTokenUseTest {

    private static final Path ROOT = moduleRoot();
    private static final Path MAIN = ROOT.resolve("src/main/java");

    private static Path moduleRoot() {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null && !Files.exists(dir.resolve("pom.xml"))) {
            dir = dir.getParent();
        }
        return dir;
    }

    private static Set<String> sourcesContaining(String text) throws IOException {
        Set<String> names = new TreeSet<>();
        try (Stream<Path> files = Files.walk(MAIN)) {
            for (Path file : (Iterable<Path>) files.filter(f -> f.toString().endsWith(".java"))::iterator) {
                if (Files.readString(file, StandardCharsets.UTF_8).contains(text)) {
                    names.add(file.getFileName().toString());
                }
            }
        }
        return names;
    }

    private static List<String> settings(String file) throws IOException {
        return Files.readAllLines(ROOT.resolve(file), StandardCharsets.UTF_8).stream()
                .map(String::trim).filter(l -> !l.isEmpty() && !l.startsWith("#")).toList();
    }

    /** NFR-025: the property is read by ServiceToken; the required-settings check asks only whether it is present. */
    @Test
    void thePropertyIsReadInOnePlace() throws IOException {
        assertEquals(Set.of("RequiredConfigCheck.java", "ServiceToken.java"), sourcesContaining("elicit.service.token"));
    }

    /** UC-047 BR-008: one class asks for the Authorization value and sets the header, the preview Survey client. */
    @Test
    void theHeaderIsSetByThePreviewSurveyClientOnly() throws IOException {
        assertEquals(Set.of("PreviewSurveyClient.java"), sourcesContaining(".authorization()"));
        assertEquals(Set.of("PreviewSurveyClient.java"), sourcesContaining("\"Authorization\""));
    }

    /** UC-037 BR-001: the connection probes know nothing of the token. */
    @Test
    void theProbesDoNotTouchTheToken() throws IOException {
        String probes = Files.readString(MAIN.resolve("com/elicitsoftware/author/diagnostics/ConnectionChecks.java"), StandardCharsets.UTF_8);
        assertFalse(probes.contains("ServiceToken"));
        assertFalse(probes.contains(".header("));
    }

    /** NFR-025: the image carries no token; only the dev profile names the development value. */
    @Test
    void thePackagedConfigurationCarriesNoToken() throws IOException {
        List<String> lines = settings("src/main/resources/application.properties").stream()
                .filter(l -> l.contains("elicit.service.token")).toList();
        assertEquals(List.of("elicit.service.token=${ELICIT_SERVICE_TOKEN:}",
                "%dev.elicit.service.token=" + ServiceToken.DEVELOPMENT_VALUE), lines);
    }

    /** NFR-025: nothing configured records request headers in a trace, an access log or a client log. */
    @Test
    void noSettingRecordsRequestHeaders() throws IOException {
        for (String line : settings("src/main/resources/application.properties")) {
            String lower = line.toLowerCase();
            assertFalse(lower.contains("capture-headers") || lower.contains("capture-request-headers"), line);
            assertFalse(lower.contains("rest-client.logging"), line);
            assertFalse(lower.contains("%{i,authorization}"), line);
        }
        assertTrue(settings("src/main/resources/application.properties").stream()
                .anyMatch(l -> l.startsWith("quarkus.http.access-log.pattern=")), "the access log pattern is stated");
    }
}
