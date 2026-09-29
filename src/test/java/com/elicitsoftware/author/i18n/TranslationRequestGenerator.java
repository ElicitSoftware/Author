package com.elicitsoftware.author.i18n;

/*-
 * ***LICENSE_START***
 * Elicit Author
 * %%
 * Copyright (C) 2025 The Regents of the University of Michigan - Rogel Cancer Center
 * %%
 * PolyForm Noncommercial License 1.0.0
 * <https://polyformproject.org/licenses/noncommercial/1.0.0>
 * ***LICENSE_END***
 */

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds {@code i18n/TRANSLATION_REQUEST.md} (UC-040 / FR-053): the document handed to a
 * translator or an AI agent. It is assembled from a hand-written header
 * ({@code src/test/resources/i18n/translation-request-header.md}: purpose, audience, glossary,
 * rules), the default bundle in file order, and the context sidecar, so the handoff can never
 * drift from the strings actually shipped.
 */
final class TranslationRequestGenerator {

    static final Path OUTPUT = Path.of("i18n/TRANSLATION_REQUEST.md");
    static final Path HEADER = Path.of("src/test/resources/i18n/translation-request-header.md");

    private TranslationRequestGenerator() {
    }

    static String generate(Path root) throws IOException {
        Path dir = root.resolve(TranslationBundleConsistencyTest.BUNDLE_DIR);
        Map<String, String> defaults = readOrdered(dir.resolve("translations.properties"));
        Map<String, String> context = readOrdered(dir.resolve("translations.context.properties"));
        StringBuilder md = new StringBuilder();
        md.append(Files.readString(root.resolve(HEADER), StandardCharsets.UTF_8).strip()).append("\n\n");
        md.append("## Strings to translate\n\n");
        md.append("Every row is one key in `translations.properties`. Return a file `translations_<tag>.properties` ")
          .append("with exactly these keys in this order, one `key=translation` per line, UTF-8, ")
          .append("no additions and no omissions.\n\n");
        md.append("| Key | English | Where it appears | Max length | Notes |\n");
        md.append("|-----|---------|------------------|------------|-------|\n");
        for (Map.Entry<String, String> e : defaults.entrySet()) {
            String[] ctx = contextFields(context.getOrDefault(e.getKey(), " |  |  | "));
            String where = ctx.length > 1 ? (ctx[0].strip() + " · " + ctx[1].strip()) : ctx[0].strip();
            String max = ctx.length > 2 ? ctx[2].strip() : "";
            String notes = ctx.length > 3 ? ctx[3].strip() : "";
            md.append("| `").append(e.getKey()).append("` | ")
              .append(cell(e.getValue())).append(" | ")
              .append(cell(where)).append(" | ")
              .append(cell(max)).append(" | ")
              .append(cell(notes)).append(" |\n");
        }
        md.append("\n## English source file\n\n```properties\n");
        for (Map.Entry<String, String> e : defaults.entrySet()) {
            md.append(e.getKey()).append('=').append(e.getValue()).append('\n');
        }
        md.append("```\n");
        return md.toString();
    }

    /**
     * The four fields of a context entry: {@code where | detail | max | notes}.
     *
     * <p>Taken from the ends inwards rather than by a plain split, because a detail may itself
     * contain a pipe: two of them quote the placeholder syntax ({@code {<TOKEN>|default}}), and a
     * plain split shifted every column right of the example -- the budget column showed a fragment
     * of the note and the budget itself was dropped past the fourth field, so a translator was
     * told nothing about length for exactly the two longest strings.</p>
     */
    static String[] contextFields(String value) {
        String[] all = value.split("\\|", -1);
        if (all.length <= 4) {
            return all;
        }
        String detail = String.join("|", List.of(all).subList(1, all.length - 2));
        return new String[]{all[0], detail, all[all.length - 2], all[all.length - 1]};
    }

    private static String cell(String value) {
        return value.replace("|", "\\|").replace("\n", " ");
    }

    /** Minimal ordered .properties reader (key=value, {@code #} comments, {@code \} continuation). */
    static Map<String, String> readOrdered(Path file) throws IOException {
        Map<String, String> map = new LinkedHashMap<>();
        if (!Files.exists(file)) {
            return map;
        }
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        List<String> logical = new ArrayList<>();
        StringBuilder current = null;
        for (String line : lines) {
            if (current != null) {
                current.append(line.stripLeading());
            } else {
                current = new StringBuilder(line);
            }
            if (current.toString().endsWith("\\") && !current.toString().endsWith("\\\\")) {
                current.setLength(current.length() - 1);
            } else {
                logical.add(current.toString());
                current = null;
            }
        }
        if (current != null) {
            logical.add(current.toString());
        }
        for (String line : logical) {
            String s = line.strip();
            if (s.isEmpty() || s.startsWith("#") || s.startsWith("!")) {
                continue;
            }
            int eq = s.indexOf('=');
            if (eq < 0) {
                continue;
            }
            map.put(s.substring(0, eq).strip(), unescape(s.substring(eq + 1).stripLeading()));
        }
        return map;
    }

    private static String unescape(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' && i + 1 < value.length()) {
                char n = value.charAt(++i);
                switch (n) {
                    case 'n' -> out.append('\n');
                    case 't' -> out.append('\t');
                    case 'u' -> {
                        out.append((char) Integer.parseInt(value.substring(i + 1, i + 5), 16));
                        i += 4;
                    }
                    default -> out.append(n);
                }
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
