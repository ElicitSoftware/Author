package com.elicitsoftware.author.flow.designer;

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

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** FR-013: the preview substitutes tokens the way QuestionManager.replaceTokens does. */
class PreviewFieldsTest {

    @Test
    void substitutesAValueElseTheDefaultElseLeavesThePlaceholder() {
        assertEquals("Hello Bob", PreviewFields.substituteTokens("Hello {PName}", Map.of("PName", "Bob")));
        assertEquals("Hello friend", PreviewFields.substituteTokens("Hello {PName|friend}", Map.of()));
        assertEquals("Hello {PName}", PreviewFields.substituteTokens("Hello {PName}", Map.of()));
    }

    @Test
    void valuesMayThemselvesHoldPlaceholders() {
        Map<String, String> values = Map.of("S1", "{G1's|your} mother", "G1", "Bob");
        assertEquals("Is Bob's mother living?", PreviewFields.substituteTokens("Is {S1} living?", values));
        assertEquals("Is your mother living?", PreviewFields.substituteTokens("Is {S1} living?", Map.of("S1", "{G1's|your} mother")));
    }

    @Test
    void thePhraseAroundTheTokenSurvives() {
        assertEquals("Has Dennis been diagnosed", PreviewFields.substituteTokens("{Has CS2 been|Were you} diagnosed", Map.of("CS2", "Dennis")));
        assertEquals("Were you diagnosed", PreviewFields.substituteTokens("{Has CS2 been|Were you} diagnosed", Map.of()));
    }

    @Test
    void tidiesPossessivesLikeTheRuntime() {
        assertEquals("Dennis' Cancers", PreviewFields.substituteTokens("{CS2's|Your} Cancers", Map.of("CS2", "Dennis")));
        assertEquals("Your Cancers", PreviewFields.substituteTokens("{CS2's|Your} Cancers", Map.of()));
        assertEquals("What is her name", PreviewFields.substituteTokens("What is {S1's|your} name", Map.of("S1", "her")));
    }
}
