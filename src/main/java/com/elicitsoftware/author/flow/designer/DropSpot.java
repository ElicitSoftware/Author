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

import com.elicitsoftware.author.survey.StructureNode;

/**
 * Where a drop landed: the container (null for the board itself, whose children are steps),
 * the insertion index among that container's current children (0..n), and the kind of
 * element the spot accepts. Position comes from which zone was hit, never from coordinates.
 */
public record DropSpot(StructureNode container, int index, StructureNode.Kind accepts) {

    /**
     * UC-006 BR-007: whether a drop of {@code payload} lands here — an element of the kind the spot
     * accepts, into a container that has not been removed. A rule or a list lands on no spot.
     */
    public boolean takes(DragPayload payload) {
        return payload == DragPayload.of(accepts) && (container == null || !container.retired());
    }
}
