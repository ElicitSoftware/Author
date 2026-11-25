package com.elicitsoftware.author.i18n;

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

import com.elicitsoftware.author.survey.Tokens;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC-045 BR-003: the interface must teach the token syntax the exporter accepts.
 *
 * <p>A token is written {@code <NAME>} inside a placeholder's phrase, and
 * {@code ExportValidation} reports a placeholder that names a known token as a bare word as an
 * <em>error</em>. The helper text under the token field, the guide and the field help are where an
 * author learns the syntax, so a bundle left on the undelimited form does not merely read oddly --
 * it walks the author into a survey that cannot be exported. Judged by {@link Tokens}, the same
 * helpers the exporter uses, so the strings answer to the rule the product enforces.</p>
 *
 * <p>Every bundle is checked, not only the English one: the curated translations in the umbrella's
 * {@code elicit-i18n/author/} carry the same fragments verbatim (the translator context says to keep them literal), and a
 * language fixed in one place and missed in another is exactly what this catches.</p>
 */
class TokenSyntaxInBundlesTest {

    @Test
    void everyBundleNamesItsTokensInAngleBrackets() throws IOException {
        Path root = DisplayedStringsCoverageTest.moduleRoot();
        List<String> problems = new ArrayList<>();

        Map<Path, Map<String, String>> bundles = new LinkedHashMap<>();
        for (Path file : bundleFiles(root)) {
            bundles.put(file, TranslationBundleConsistencyTest.load(file, problems));
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));

        // A name is a token because some string brackets it: the bundles declare their own
        // vocabulary, exactly as a survey's texts declare a survey's.
        Set<String> known = new LinkedHashSet<>();
        bundles.values().forEach(b -> b.values().forEach(v -> known.addAll(Tokens.names(v))));

        for (var bundle : bundles.entrySet()) {
            for (var entry : bundle.getValue().entrySet()) {
                Set<String> bare = Tokens.bareTokenNames(entry.getValue(), known);
                if (!bare.isEmpty()) {
                    problems.add(root.relativize(bundle.getKey()) + " / " + entry.getKey()
                            + " names " + String.join(", ", bare) + " without angle brackets: "
                            + entry.getValue());
                }
            }
        }

        assertTrue(problems.isEmpty(), "UC-045 BR-003: an author following these strings writes a placeholder"
                + " ExportValidation refuses. Write the token as <NAME> inside the phrase --"
                + " {<TOKEN>|default}, {Has <TOKEN> been|Were you} -- in every bundle:\n"
                + String.join("\n", problems));
    }

    /**
     * The English bundle and its translator-context sidecar, plus every curated translation in
     * {@code ../elicit-i18n/author/} that is present. Absent copies are skipped rather than failed --
     * {@code TranslationBundleConsistencyTest} is what insists they exist.
     */
    private static List<Path> bundleFiles(Path root) throws IOException {
        List<Path> files = new ArrayList<>();
        Path source = root.resolve(TranslationBundleConsistencyTest.BUNDLE_DIR);
        files.add(source.resolve("translations.properties"));
        files.add(source.resolve("translations.context.properties"));
        Path dir = root.resolve(TranslationBundleConsistencyTest.TRANSLATIONS_DIR).normalize();
        if (Files.isDirectory(dir)) {
            try (Stream<Path> listed = Files.list(dir)) {
                listed.filter(f -> f.getFileName().toString().startsWith("translations")
                                && f.getFileName().toString().endsWith(".properties"))
                        .sorted().forEach(files::add);
            }
        }
        return files;
    }
}
