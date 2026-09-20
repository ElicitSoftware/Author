package com.elicitsoftware;

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

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Guards the test-only mirror of Survey's migrations ({@code src/test/resources/db/schema-mirror})
 * against drifting from the sibling checkout. Skips when {@code ../Survey} is not present (CI),
 * fails with the fix-up command when it is present and differs.
 *
 * <p>Traceability: infrastructure for every use case that maps entities onto the shared schema
 * (UC-003 .. UC-010).</p>
 */
class SchemaMirrorFreshnessTest {

    private static final Path SIBLING = Path.of("..", "Survey", "src", "main", "resources", "db", "migration");
    private static final Path MIRROR = Path.of("src", "test", "resources", "db", "schema-mirror");

    @Test
    void mirrorMatchesSiblingSurveyMigrations() throws IOException, NoSuchAlgorithmException {
        Assumptions.assumeTrue(Files.isDirectory(SIBLING), "../Survey not checked out; skipping mirror check");
        Map<String, String> expected = digests(SIBLING);
        Map<String, String> actual = digests(MIRROR);
        assertEquals(expected, actual,
                "src/test/resources/db/schema-mirror differs from ../Survey/src/main/resources/db/migration; "
                        + "run scripts/sync-survey-schema.sh");
    }

    private static Map<String, String> digests(Path dir) throws IOException, NoSuchAlgorithmException {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        Map<String, String> out = new TreeMap<>();
        try (Stream<Path> files = Files.list(dir)) {
            for (Path f : files.filter(p -> p.getFileName().toString().endsWith(".sql")).toList()) {
                out.put(f.getFileName().toString(), HexFormat.of().formatHex(sha.digest(Files.readAllBytes(f))));
            }
        }
        return out;
    }
}
