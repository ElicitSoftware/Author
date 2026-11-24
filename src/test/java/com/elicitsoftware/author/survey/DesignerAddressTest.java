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

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** UC-006 BR-003: the designer's {@code select} parameter, {@code kind:id}; anything else selects nothing (A2). */
class DesignerAddressTest {

    @Test
    void parse_readsKindAndId() {
        assertEquals(Optional.of(new DesignerAddress("question", 12)), DesignerAddress.parse("question:12"));
        assertEquals(Optional.of(new DesignerAddress("step", 3)), DesignerAddress.parse("STEP:3"), "the kind is case-insensitive");
        assertEquals(Optional.of(new DesignerAddress("rule", 5)), DesignerAddress.parse("rule:5:extra"), "trailing parts are ignored");
    }

    @Test
    void parse_malformed_namesNothing() {
        assertEquals(Optional.empty(), DesignerAddress.parse(null));
        assertEquals(Optional.empty(), DesignerAddress.parse(""));
        assertEquals(Optional.empty(), DesignerAddress.parse("step"));
        assertEquals(Optional.empty(), DesignerAddress.parse("step:"));
        assertEquals(Optional.empty(), DesignerAddress.parse("step:abc"));
    }

    @Test
    void factories_nameTheirKind() {
        assertEquals(new DesignerAddress(DesignerAddress.MOUNT, 7), DesignerAddress.mount(7));
        assertEquals(new DesignerAddress(DesignerAddress.ASSIGNMENT, 8), DesignerAddress.assignment(8));
        assertEquals(new DesignerAddress(DesignerAddress.RULE, 9), DesignerAddress.rule(9));
    }

    @Test
    void of_aStructureNode_usesItsRouteKindAndDurableId() {
        StructureNode node = new StructureNode(StructureNode.Kind.SECTION, 42, 420, "Family", null,
                null, UUID.randomUUID(), false, null);
        assertEquals(new DesignerAddress(DesignerAddress.SECTION, 42), DesignerAddress.of(node));
    }

    @Test
    void toString_roundTripsThroughParse() {
        DesignerAddress address = DesignerAddress.assignment(15);
        assertEquals("sq:15", address.toString());
        assertEquals(Optional.of(address), DesignerAddress.parse(address.toString()));
    }
}
