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
package com.elicitsoftware.author.flow.reports;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * UC-048 A5 and BR-007: which address the Reports page's Preview in Survey… link opens.
 */
class ReportsViewPreviewLinkTest {

    /** BR-007: the browser-facing address wins over the one Author calls. */
    @Test
    void given_aPublicAddress_when_linked_then_itIsUsed() {
        assertEquals("http://localhost:8085",
                ReportsView.previewLink(Optional.of(" http://localhost:8085 "), Optional.of("http://author-survey:8080")));
    }

    /** BR-007: unset or blank, the link falls back to the address Author calls the preview at. */
    @Test
    void given_noPublicAddress_when_linked_then_theCallAddressIsUsed() {
        assertEquals("http://localhost:8085", ReportsView.previewLink(Optional.empty(), Optional.of("http://localhost:8085")));
        assertEquals("http://localhost:8085", ReportsView.previewLink(Optional.of("  "), Optional.of("http://localhost:8085")));
        assertEquals("http://localhost:8085", ReportsView.previewLink(null, Optional.of("http://localhost:8085")));
    }

    /** BR-007: with neither address set, the page shows no link. */
    @Test
    void given_noAddressAtAll_when_linked_then_thereIsNoLink() {
        assertNull(ReportsView.previewLink(Optional.empty(), Optional.empty()));
        assertNull(ReportsView.previewLink(Optional.of(""), Optional.of(" ")));
        assertNull(ReportsView.previewLink(null, null));
    }
}
