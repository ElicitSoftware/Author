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

import com.vaadin.flow.component.dnd.DropEffect;
import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.component.html.Div;

/**
 * An insertion bar between two siblings (and before the first / after the last). Highlighted by
 * Vaadin's {@code v-drag-over-target} class while a payload it takes hovers over it; a payload it
 * refuses gets no highlight and a not-allowed pointer (UC-006 BR-007).
 */
public class DropZone extends Div {

    final DropSpot spot;

    public DropZone(SurveyDesignerView view, DropSpot spot) {
        this.spot = spot;
        addClassNames("designer-drop-zone", "designer-drop-zone-" + spot.accepts().name().toLowerCase());
        getElement().setAttribute("aria-hidden", "true");
        DropTarget<DropZone> target = DropTarget.create(this);
        target.setDropEffect(DropEffect.MOVE);
        target.addDropListener(e -> e.getDragData().ifPresent(data -> view.onDrop(data, spot)));
        view.admit(target, spot::takes);
    }

    public DropSpot spot() {
        return spot;
    }
}
