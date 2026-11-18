package com.elicitsoftware.author.diagnostics;

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

import com.elicitsoftware.author.diagnostics.BrandDiagnostics.AssetReport;
import com.elicitsoftware.author.diagnostics.BrandDiagnostics.BrandReport;
import com.elicitsoftware.author.diagnostics.BrandDiagnostics.Source;
import com.elicitsoftware.author.util.BrandUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC-036 BR-003: the logos are reported under the names the mounted {@code brand-config.json}
 * gives them, and what the brand gets wrong about them is a warning (UC-036 A3). The mount is a
 * temporary directory of the test's making; the embedded tier is the default brand packaged
 * under {@code META-INF/brand}, and the logos are resolved by the real {@link BrandUtil}.
 */
class BrandDiagnosticsTest {

    @TempDir
    Path temp;

    private Path external;
    private BrandDiagnostics diagnostics;

    @BeforeEach
    void setUp() {
        external = temp.resolve("mounted");
        diagnostics = new BrandDiagnostics();
        diagnostics.brandFileSystemPath = external.toString();
        diagnostics.brandLocalPath = temp.resolve("local").toString();
        diagnostics.brandUtil = new BrandUtil();
        setBrandPath(diagnostics.brandUtil, external);
    }

    private static void setBrandPath(BrandUtil util, Path path) {
        try {
            Field field = BrandUtil.class.getDeclaredField("brandFileSystemPath");
            field.setAccessible(true);
            field.set(util, path.toString());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }

    /** UC-036 step 4, BR-001: with nothing mounted the default brand's logos are reported, embedded. */
    @Test
    void report_noMount_reportsTheDefaultLogosWithoutWarnings() {
        BrandReport report = diagnostics.report();

        assertEquals(List.of("colors/brand-colors.css", "typography/brand-typography.css", "theme.css",
                        "images/HorizontalLogo.png", "images/icon-white.png", "images/favicon.ico"),
                report.assets().stream().map(AssetReport::path).toList());
        report.assets().forEach(asset -> assertEquals(Source.EMBEDDED, asset.source(), asset.path()));
        assertEquals("/api/brand/images/HorizontalLogo.png", report.logoUrl());
        assertEquals(List.of(), report.warnings());
    }

    /** UC-036 BR-003: the logos are looked for under their configured names. */
    @Test
    void report_mountNamingItsLogos_reportsThemUnderThoseNames() throws IOException {
        write(external.resolve("brand-config.json"), "{\"name\": \"Acme\", \"logos\": {\"horizontal\": \"Wide.svg\","
                + " \"icon\": \"mark.svg\"}, \"favicon\": \"tab.svg\"}");
        write(external.resolve("images/Wide.svg"), "<svg/>");
        write(external.resolve("images/mark.svg"), "<svg/>");
        write(external.resolve("images/tab.svg"), "<svg/>");

        BrandReport report = diagnostics.report();

        List<AssetReport> logos = report.assets().subList(3, 6);
        assertEquals(List.of("images/Wide.svg", "images/mark.svg", "images/tab.svg"),
                logos.stream().map(AssetReport::path).toList());
        logos.forEach(logo -> assertEquals(Source.EXTERNAL, logo.source(), logo.path()));
        assertEquals("/api/brand/images/Wide.svg", report.logoUrl());
        assertEquals(List.of(), report.warnings());
    }

    /** UC-036 A3, BR-003 (umbrella UC-014 BR-009): a favicon of its own with no icon of its own is a warning. */
    @Test
    void report_mountedFaviconWithoutIcon_reportsTheDefaultIconAndAWarning() throws IOException {
        write(external.resolve("brand-config.json"), "{\"name\": \"Acme\", \"logos\": {\"horizontal\": \"Wide.png\"},"
                + " \"favicon\": \"favicon.svg\"}");
        write(external.resolve("images/Wide.png"), "png");
        write(external.resolve("images/favicon.svg"), "<svg/>");

        BrandReport report = diagnostics.report();

        assertEquals(new AssetReport("images/icon-white.png", "header icon", Source.EMBEDDED,
                "META-INF/brand/images/icon-white.png"), report.assets().get(4));
        assertEquals(1, report.warnings().size());
        assertTrue(report.warnings().get(0).contains("logos.icon"), report.warnings().get(0));
    }

    /** UC-036 A3, BR-003: a logo the configuration names but the mount lacks is a warning, and falls back. */
    @Test
    void report_namedLogoTheMountLacks_reportsTheDefaultAndAWarning() throws IOException {
        write(external.resolve("brand-config.json"), "{\"name\": \"Acme\", \"logos\": {\"icon\": \"mark.svg\"}}");

        BrandReport report = diagnostics.report();

        assertEquals("images/icon-white.png", report.assets().get(4).path());
        assertEquals(List.of("brand-config.json names images/mark.svg, which the mounted brand does not hold."
                + " The file of the default brand is shown instead."), report.warnings());
    }

    /** UC-036 BR-003: a mount that names no logos and supplies none warns about nothing. */
    @Test
    void report_mountWithoutLogos_warnsAboutNothing() throws IOException {
        write(external.resolve("colors/brand-colors.css"), ":root{}");

        assertEquals(List.of(), diagnostics.report().warnings());
    }
}
