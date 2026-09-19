package com.elicitsoftware.author.flow.designer;

/*-
 * ***LICENSE_START***
 * Elicit Author
 * %%
 * Copyright (C) 2026 The Regents of the University of Michigan - Rogel Cancer Center
 * %%
 * PolyForm Noncommercial License 1.0.0
 * <https://polyformproject.org/licenses/noncommercial/1.0.0>
 * ***LICENSE_END***
 */

import com.vaadin.flow.component.dnd.DropEffect;
import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.component.html.Div;

import java.util.function.BiConsumer;

/**
 * An insertion bar between two siblings (and before the first / after the last). Zero height
 * until a drag is in progress on the board; highlighted by Vaadin's {@code v-drag-over-target}
 * class while a payload hovers over it.
 */
public class DropZone extends Div {

    final DropSpot spot;

    public DropZone(DropSpot spot, BiConsumer<Object, DropSpot> onDrop) {
        this.spot = spot;
        addClassNames("designer-drop-zone", "designer-drop-zone-" + spot.accepts().name().toLowerCase());
        getElement().setAttribute("aria-hidden", "true");
        DropTarget<DropZone> target = DropTarget.create(this);
        target.setDropEffect(DropEffect.MOVE);
        target.addDropListener(e -> e.getDragData().ifPresent(data -> onDrop.accept(data, spot)));
    }

    public DropSpot spot() {
        return spot;
    }
}
