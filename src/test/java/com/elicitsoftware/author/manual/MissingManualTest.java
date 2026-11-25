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

import com.elicitsoftware.author.flow.MainLayout;
import com.elicitsoftware.test.PostgresTestResource;
import com.vaadin.browserless.quarkus.QuarkusBrowserlessTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.sidenav.SideNavItem;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.TestSecurity;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * UC-039 A1: an image built without the manual simply does not offer it. Nothing else changes —
 * the author keeps the in-app guide (UC-029) and every other navigation entry.
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
@TestProfile(MissingManualTest.NoManualProfile.class)
class MissingManualTest extends QuarkusBrowserlessTest {

    /** Points the manual at a classpath resource no build produces. */
    public static class NoManualProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("author.manual.resource", "manual/no-such-manual.pdf");
        }
    }

    @Inject
    AuthorManual manual;

    @Test
    void aBuildWithoutTheManualReportsItMissing() {
        assertFalse(manual.isAvailable());
        assertNull(manual.open(), "isAvailable() and open() must agree");
    }

    @Test
    @TestSecurity(user = "manual.author", roles = {"elicit_author"})
    void theHeaderLeavesTheLinkOut() {
        MainLayout layout = CDI.current().select(MainLayout.class).get();
        UI.getCurrent().add(layout);

        assertEquals(0, find(Anchor.class, layout).withClassName("header-manual-link").all().size(),
                "no manual in the image means no link in the header (UC-039 A1)");
        assertFalse(find(SideNavItem.class, layout).all().stream()
                        .anyMatch(item -> layout.getTranslation("mainLayout.nav.manual").equals(item.getLabel())),
                "no manual in the image means no navigation entry (UC-039 A1)");
    }

    @Test
    @TestSecurity(user = "manual.author", roles = {"elicit_author"})
    void theInAppGuideRemains() {
        MainLayout layout = CDI.current().select(MainLayout.class).get();
        UI.getCurrent().add(layout);

        assertFalse(find(SideNavItem.class, layout).all().stream()
                        .noneMatch(item -> "How surveys work".equals(item.getLabel())),
                "the guide (UC-029) is what the author falls back to");
    }
}
