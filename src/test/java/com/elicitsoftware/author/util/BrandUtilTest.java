package com.elicitsoftware.author.util;

/*-
 * ***LICENSE_START***
 * Elicit Author
 * %%
 * Copyright (C) 2025 Elicit Software LLC
 * %%
 * PolyForm Noncommercial License 1.0.0
 * <https://polyformproject.org/licenses/noncommercial/1.0.0>
 * ***LICENSE_END***
 */

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BrandUtil.brandFileSystemPath is package-private and settable directly from a test in the
 * same package, so the filesystem-mounted-brand path can be exercised with a real @TempDir
 * instead of the actual /brand mount point used in production.
 *
 * Traceability: UC-030 (Brand the Authoring Interface) — BR-001 fallback order, BR-002 default brand,
 * BR-006 configured logo names.
 */
class BrandUtilTest {

    private static BrandUtil util(Path brandDir) {
        BrandUtil util = new BrandUtil();
        util.brandFileSystemPath = brandDir.toString();
        return util;
    }

    private static void writeConfig(Path dir, String json) throws IOException {
        Files.writeString(dir.resolve("brand-config.json"), json);
    }

    // No filesystem mount: the brand embedded under META-INF/brand/ (a copy of elicit-brand) is used.
    @Test
    void detectCurrentBrand_noMount_fallsBackToEmbeddedBrand(@TempDir Path emptyDir) {
        BrandUtil.BrandInfo info = util(emptyDir).detectCurrentBrand();

        assertEquals("elicit-default-brand", info.getBrandKey());
        assertEquals("Elicit Software", info.getDisplayName());
        assertEquals("brand/images/HorizontalLogo.png", info.getLogoPath());
    }

    @Test
    void detectCurrentBrand_malformedJson_fallsBackToDefaultBrand(@TempDir Path brandDir) throws IOException {
        writeConfig(brandDir, "{not valid json");

        BrandUtil.BrandInfo info = util(brandDir).detectCurrentBrand();

        assertEquals("elicit-default-brand", info.getBrandKey(), "malformed external config falls back to the embedded brand");
    }

    @Test
    void detectCurrentBrand_topLevelName_usedAsBrandNameAndKey(@TempDir Path brandDir) throws IOException {
        writeConfig(brandDir, "{\"name\": \"My Cool  Brand!!\"}");

        BrandUtil.BrandInfo info = util(brandDir).detectCurrentBrand();

        assertEquals("my-cool-brand", info.getBrandKey());
        assertEquals("My Cool  Brand!!", info.getDisplayName());
    }

    @Test
    void detectCurrentBrand_nestedBrandName_usedWhenNoTopLevelName(@TempDir Path brandDir) throws IOException {
        writeConfig(brandDir, "{\"brand\": {\"name\": \"Nested Name\"}}");

        BrandUtil.BrandInfo info = util(brandDir).detectCurrentBrand();

        assertEquals("nested-name", info.getBrandKey());
    }

    @Test
    void detectCurrentBrand_noNameAnywhere_fallsBackToExternalBrand(@TempDir Path brandDir) throws IOException {
        writeConfig(brandDir, "{}");

        BrandUtil.BrandInfo info = util(brandDir).detectCurrentBrand();

        assertEquals("external-brand", info.getBrandKey());
    }

    @Test
    void detectCurrentBrand_organizationPresent_usedAsDisplayNameInsteadOfBrandName(@TempDir Path brandDir) throws IOException {
        writeConfig(brandDir, "{\"name\": \"Internal Name\", \"organization\": \"Public Org\"}");

        BrandUtil.BrandInfo info = util(brandDir).detectCurrentBrand();

        assertEquals("Public Org", info.getDisplayName());
    }

    @Test
    void detectCurrentBrand_horizontalLogoPreferred_overPrimary(@TempDir Path brandDir) throws IOException {
        writeConfig(brandDir, "{\"name\": \"Acme\", \"logos\": {\"horizontal\": \"h.png\", \"primary\": \"p.png\"}}");

        BrandUtil.BrandInfo info = util(brandDir).detectCurrentBrand();

        assertEquals("brand/images/h.png", info.getLogoPath());
    }

    @Test
    void detectCurrentBrand_primaryLogoUsed_whenNoHorizontal(@TempDir Path brandDir) throws IOException {
        writeConfig(brandDir, "{\"name\": \"Acme\", \"logos\": {\"primary\": \"p.png\"}}");

        BrandUtil.BrandInfo info = util(brandDir).detectCurrentBrand();

        assertEquals("brand/images/p.png", info.getLogoPath());
    }

    @Test
    void detectCurrentBrand_firstAvailableLogoUsed_whenNoHorizontalOrPrimary(@TempDir Path brandDir) throws IOException {
        writeConfig(brandDir, "{\"name\": \"Acme\", \"logos\": {\"square\": \"s.png\"}}");

        BrandUtil.BrandInfo info = util(brandDir).detectCurrentBrand();

        assertEquals("brand/images/s.png", info.getLogoPath());
    }

    @Test
    void detectCurrentBrand_noLogosKey_usesDefaultLogoPath(@TempDir Path brandDir) throws IOException {
        writeConfig(brandDir, "{\"name\": \"Acme\"}");

        BrandUtil.BrandInfo info = util(brandDir).detectCurrentBrand();

        assertEquals("brand/images/HorizontalLogo.png", info.getLogoPath());
    }

    @Test
    void detectCurrentBrand_secondCall_returnsCachedInstance(@TempDir Path brandDir) throws IOException {
        writeConfig(brandDir, "{\"name\": \"Acme\"}");
        BrandUtil util = util(brandDir);

        BrandUtil.BrandInfo first = util.detectCurrentBrand();
        BrandUtil.BrandInfo second = util.detectCurrentBrand();

        assertSame(first, second);
    }

    @Test
    void clearCache_forcesRedetectionOnNextCall(@TempDir Path brandDir) throws IOException {
        writeConfig(brandDir, "{\"name\": \"Acme\"}");
        BrandUtil util = util(brandDir);
        BrandUtil.BrandInfo first = util.detectCurrentBrand();

        util.clearCache();
        BrandUtil.BrandInfo second = util.detectCurrentBrand();

        assertNotSame(first, second);
        assertEquals(first.getBrandKey(), second.getBrandKey());
    }

    private static void image(Path dir, String file) throws IOException {
        Files.createDirectories(dir.resolve("images"));
        Files.writeString(dir.resolve("images").resolve(file), "image");
    }

    // UC-030 BR-006: the header icon is the file logos.icon names, served by the brand handler.
    @Test
    void getIconResourcePath_mountNamingItsIcon_servesThatFile(@TempDir Path brandDir) throws IOException {
        writeConfig(brandDir, "{\"name\": \"Acme\", \"logos\": {\"icon\": \"mark.svg\"}, \"favicon\": \"tab.svg\"}");
        image(brandDir, "mark.svg");
        image(brandDir, "tab.svg");
        BrandUtil util = util(brandDir);

        assertEquals("/api/brand/images/mark.svg", util.getIconResourcePath(util.detectCurrentBrand()));
        assertEquals(new BrandUtil.LogoFile("mark.svg", true), util.getLogoFile(BrandUtil.Logo.ICON));
        assertEquals("/api/brand/images/tab.svg", util.getLogoUrl(BrandUtil.Logo.FAVICON));
        assertFalse(util.isIconMissingBesideFavicon());
        assertEquals(List.of(), util.getMissingLogoFiles());
    }

    // UC-030 BR-006: the lockup is the file logos.horizontal names.
    @Test
    void getLogoResourcePath_mountNamingItsLockup_servesThatFile(@TempDir Path brandDir) throws IOException {
        writeConfig(brandDir, "{\"name\": \"Acme\", \"logos\": {\"horizontal\": \"Wide.png\"}}");
        image(brandDir, "Wide.png");
        BrandUtil util = util(brandDir);

        assertEquals("/api/brand/images/Wide.png", util.getLogoResourcePath(util.detectCurrentBrand()));
    }

    // UC-030 BR-006: with nothing mounted every logo is the default brand's file.
    @Test
    void logos_noMount_areTheDefaultBrandsFiles(@TempDir Path emptyDir) {
        BrandUtil util = util(emptyDir);

        assertEquals("/api/brand/images/HorizontalLogo.png", util.getLogoUrl(BrandUtil.Logo.HORIZONTAL));
        assertEquals(new BrandUtil.LogoFile("icon-white.png", false), util.getLogoFile(BrandUtil.Logo.ICON));
        assertEquals("/api/brand/images/favicon.ico", util.getLogoUrl(BrandUtil.Logo.FAVICON));
        assertFalse(util.isIconMissingBesideFavicon());
    }

    // UC-030 BR-006 (umbrella UC-014 BR-009): a mounted favicon without a mounted icon leaves the default mark, and says so.
    @Test
    void isIconMissingBesideFavicon_mountedFaviconWithoutIcon_isTrue(@TempDir Path brandDir) throws IOException {
        writeConfig(brandDir, "{\"name\": \"Acme\", \"favicon\": \"favicon.svg\"}");
        image(brandDir, "favicon.svg");
        BrandUtil util = util(brandDir);

        assertEquals("/api/brand/images/icon-white.png", util.getIconResourcePath(util.detectCurrentBrand()));
        assertTrue(util.isIconMissingBesideFavicon());
    }

    // UC-030 A3: a file the mount holds under the default name counts as mounted.
    @Test
    void getLogoFile_mountHoldingTheDefaultIconName_isMounted(@TempDir Path brandDir) throws IOException {
        writeConfig(brandDir, "{\"name\": \"Acme\", \"favicon\": \"favicon.svg\"}");
        image(brandDir, "favicon.svg");
        image(brandDir, "icon-white.png");
        BrandUtil util = util(brandDir);

        assertEquals(new BrandUtil.LogoFile("icon-white.png", true), util.getLogoFile(BrandUtil.Logo.ICON));
        assertFalse(util.isIconMissingBesideFavicon());
    }

    // UC-030 BR-006: a named file the mount lacks falls back to the default and is reported.
    @Test
    void getLogoFile_namedIconTheMountLacks_fallsBackAndIsReported(@TempDir Path brandDir) throws IOException {
        writeConfig(brandDir, "{\"name\": \"Acme\", \"logos\": {\"icon\": \"mark.svg\"}}");
        BrandUtil util = util(brandDir);

        assertEquals(new BrandUtil.LogoFile("icon-white.png", false), util.getLogoFile(BrandUtil.Logo.ICON));
        assertEquals(List.of("images/mark.svg"), util.getMissingLogoFiles());
    }

    // UC-030 BR-003, BR-006: a configured name that climbs out of images/ is not a file of the brand.
    @Test
    void getIconResourcePath_nameOutsideImages_isIgnored(@TempDir Path brandDir) throws IOException {
        writeConfig(brandDir, "{\"name\": \"Acme\", \"logos\": {\"icon\": \"../brand-config.json\"}}");
        BrandUtil util = util(brandDir);

        assertEquals("/api/brand/images/icon-white.png", util.getIconResourcePath(util.detectCurrentBrand()));
    }

    // UC-036 BR-002: the logos are cached until the brand is cleared.
    @Test
    void logos_changedMount_cachedUntilCleared(@TempDir Path brandDir) throws IOException {
        BrandUtil util = util(brandDir);
        assertEquals("/api/brand/images/icon-white.png", util.getLogoUrl(BrandUtil.Logo.ICON));
        writeConfig(brandDir, "{\"name\": \"Acme\", \"logos\": {\"icon\": \"mark.svg\"}}");
        image(brandDir, "mark.svg");

        assertEquals("/api/brand/images/icon-white.png", util.getLogoUrl(BrandUtil.Logo.ICON));
        util.clearCache();
        assertEquals("/api/brand/images/mark.svg", util.getLogoUrl(BrandUtil.Logo.ICON));
    }

    @Test
    void getApplicationTitle_defaultBrand_prefixedWithElicit(@TempDir Path brandDir) {
        BrandUtil.BrandInfo defaultBrand = new BrandUtil.BrandInfo("default-brand", "Elicit", "logo.png", "default-brand");
        assertEquals("Elicit Author", util(brandDir).getApplicationTitle(defaultBrand, "Author"));
    }

    @Test
    void getApplicationTitle_externalBrand_usesDisplayName(@TempDir Path brandDir) {
        BrandUtil.BrandInfo external = new BrandUtil.BrandInfo("acme", "Acme Corp", "logo.png", "acme");
        assertEquals("Acme Corp Author", util(brandDir).getApplicationTitle(external, "Author"));
    }

    // UC-040 BR-006: the brand's "localized" block supplies per-language display names.
    @Test
    void detectCurrentBrand_localizedBlock_resolvesTagThenLanguageThenBase(@TempDir Path brandDir) throws IOException {
        writeConfig(brandDir, "{\"name\": \"Health Test\", \"organization\": \"Health Test Organization\", "
                + "\"localized\": {\"es-419\": {\"organization\": \"Organizaci\u00f3n de prueba\"}, "
                + "\"ar\": {\"name\": \"\u0645\u0646\u0638\u0645\u0629\"}}}");

        BrandUtil.BrandInfo info = util(brandDir).detectCurrentBrand();

        assertEquals("Health Test Organization", info.getDisplayName());
        assertEquals("Organizaci\u00f3n de prueba", info.getDisplayName(Locale.forLanguageTag("es-419")));
        assertEquals("Organizaci\u00f3n de prueba", info.getDisplayName(Locale.forLanguageTag("es-GT")), "same-language variant");
        assertEquals("\u0645\u0646\u0638\u0645\u0629", info.getDisplayName(Locale.forLanguageTag("ar")), "name is used when no organization variant");
        assertEquals("Health Test Organization", info.getDisplayName(Locale.FRENCH), "base name when the language has no variant");
        assertEquals("health-test", info.getBrandKey(), "the technical key always derives from the base name");
    }

    @Test
    void detectCurrentBrand_withoutLocalizedBlock_localeLookupReturnsBase(@TempDir Path brandDir) throws IOException {
        writeConfig(brandDir, "{\"name\": \"Plain Brand\"}");

        BrandUtil.BrandInfo info = util(brandDir).detectCurrentBrand();

        assertEquals("Plain Brand", info.getDisplayName(Locale.forLanguageTag("ar")));
        assertEquals("Plain Brand", info.getDisplayName(null));
        assertFalse(info.isDefaultBrand());
    }
}
