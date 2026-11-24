package com.elicitsoftware.author.survey;

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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UC-043 / UC-046: the base-text hash a content translation carries. Lower-case hex SHA-256 of the
 * UTF-8 bytes, no trimming, the same as Survey's copy and PostgreSQL's
 * {@code encode(sha256(convert_to(text, 'UTF8')), 'hex')}.
 */
class ContentHashTest {

    @Test
    void of_isLowerCaseHexSha256OfUtf8() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", ContentHash.of("abc"));
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", ContentHash.of(""));
        assertEquals("4a99557e4033c3539de2eb65472017cad5f9557f7a0625a09f1c3f6e2ba69c4c", ContentHash.of("é"),
                "UTF-8, not the platform charset");
        assertNull(ContentHash.of(null));
    }

    @Test
    void matches_onlyTheExactBaseText() {
        String hash = ContentHash.of("How old are you?");
        assertTrue(ContentHash.matches(hash, "How old are you?"));
        assertFalse(ContentHash.matches(hash, "How old are you? "), "no trimming");
        assertFalse(ContentHash.matches(hash, "how old are you?"));
        assertFalse(ContentHash.matches(null, "How old are you?"), "an unhashed translation never matches");
        assertFalse(ContentHash.matches(hash, null));
    }
}
