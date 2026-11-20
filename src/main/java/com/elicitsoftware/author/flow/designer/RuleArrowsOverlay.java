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

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.EventData;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.shared.Registration;

/**
 * Java side of the arrow overlay (UC-020): the board hands it the edges as JSON and listens for
 * the three gestures the client resolves — connect a pending rule, select an edge, re-point an
 * end. The drawing itself is client-side; nothing here is stored (UC-020 BR-001).
 */
@Tag("elicit-rule-arrows")
@JsModule("./elicit-rule-arrows.js")
public class RuleArrowsOverlay extends Component {

    /** Creates the overlay, hidden from assistive technology because it only draws. */
    public RuleArrowsOverlay() {
        getElement().setAttribute("aria-hidden", "true");
    }

    /** The overlay's own words, in the author's language (NFR-018); the script keeps English defaults. */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        getElement().setProperty("textBackwards", getTranslation("ruleArrows.backwards"));
        getElement().setProperty("textDragSource", getTranslation("ruleArrows.dragSource"));
        getElement().setProperty("textDragTarget", getTranslation("ruleArrows.dragTarget"));
        getElement().setProperty("textWhen", getTranslation("ruleArrows.when"));
    }

    /**
     * Hands the client the edges to draw.
     *
     * @param json the edges as a JSON array; {@code null} draws none
     */
    public void setEdges(String json) {
        getElement().setProperty("edges", json == null ? "[]" : json);
    }

    /**
     * Returns the edges last handed to the client.
     *
     * @return the edges as a JSON array, {@code "[]"} when none were set
     */
    public String edges() {
        return getElement().getProperty("edges", "[]");
    }

    /**
     * Marks one edge as selected.
     *
     * @param relationshipId the selected rule's relationship id, or {@code null} to select none
     */
    public void setSelectedEdgeId(Integer relationshipId) {
        getElement().setProperty("selectedEdgeId", relationshipId == null ? "" : String.valueOf(relationshipId));
    }

    /**
     * The board's selected node anchor (step-/ss-/sq-id); the overlay focuses the rules touching it.
     *
     * @param anchor the selected node's anchor, or {@code null} for none
     */
    public void setSelectedAnchor(String anchor) {
        getElement().setProperty("selectedAnchor", anchor == null ? "" : anchor);
    }

    /**
     * UC-019 step 2: a rule glyph landed on {@code sourceAnchor}; the client draws the dangling arrow.
     * The values are pushed to the client even when unchanged.
     *
     * @param sourceAnchor the anchor of the question the rule starts at, or {@code null} to clear it
     * @param action the pending rule's action, or {@code null} to clear it
     */
    public void setPending(String sourceAnchor, String action) {
        String a = action == null ? "" : action;
        String s = sourceAnchor == null ? "" : sourceAnchor;
        getElement().setProperty("pendingAction", a);
        getElement().setProperty("pendingSource", s);
        // The client clears pendingSource on its own after a miss or Escape, which the server
        // never hears about; a plain setProperty with the same value would then send nothing
        // and a second attempt on the same question would show no arrow. Push it explicitly.
        getElement().executeJs("this.pendingAction = $0; this.pendingSource = $1;", a, s);
    }

    /**
     * Returns the pending rule's source anchor as last synchronized from the client.
     *
     * @return the source anchor, or an empty string when no rule is pending
     */
    public String pendingSource() {
        return getElement().getProperty("pendingSource", "");
    }

    /**
     * Returns the pending rule's action as last synchronized from the client.
     *
     * @return the action, or an empty string when no rule is pending
     */
    public String pendingAction() {
        return getElement().getProperty("pendingAction", "");
    }

    /**
     * Listens for a pending arrow released on a target.
     *
     * @param listener the listener to add
     * @return the registration that removes the listener
     */
    public Registration addConnectRequestedListener(ComponentEventListener<ConnectRequestedEvent> listener) {
        return addListener(ConnectRequestedEvent.class, listener);
    }

    /**
     * Listens for a click on an edge's glyph, line or end.
     *
     * @param listener the listener to add
     * @return the registration that removes the listener
     */
    public Registration addEdgeSelectedListener(ComponentEventListener<EdgeSelectedEvent> listener) {
        return addListener(EdgeSelectedEvent.class, listener);
    }

    /**
     * Listens for an edge's end dropped on another element.
     *
     * @param listener the listener to add
     * @return the registration that removes the listener
     */
    public Registration addEdgeRepointedListener(ComponentEventListener<EdgeRepointedEvent> listener) {
        return addListener(EdgeRepointedEvent.class, listener);
    }

    /** The client's {@code connect-requested} event: the pending arrow was released on a target (UC-019). */
    @DomEvent("connect-requested")
    public static class ConnectRequestedEvent extends ComponentEvent<RuleArrowsOverlay> {
        private final String source;
        private final String target;
        private final String mount;
        private final String action;

        /**
         * Creates the event from the client's detail.
         *
         * @param source the overlay that fired the event
         * @param fromClient whether the event came from the client
         * @param sourceAnchor the anchor the pending arrow starts at
         * @param targetAnchor the anchor it was released on
         * @param mountAnchor the section card ({@code ss-N}) a question-row target sits in, or null
         * @param action the pending rule's action
         */
        public ConnectRequestedEvent(RuleArrowsOverlay source, boolean fromClient,
                                     @EventData("event.detail.source") String sourceAnchor,
                                     @EventData("event.detail.target") String targetAnchor,
                                     @EventData("event.detail.mount") String mountAnchor,
                                     @EventData("event.detail.action") String action) {
            super(source, fromClient);
            this.source = sourceAnchor;
            this.target = targetAnchor;
            this.mount = mountAnchor;
            this.action = action;
        }

        /**
         * The section card ({@code ss-N}) a question-row target sits in, or null.
         *
         * @return the mount anchor, or {@code null}
         */
        public String mountAnchor() {
            return mount;
        }

        /**
         * The anchor the pending arrow starts at.
         *
         * @return the source anchor
         */
        public String sourceAnchor() {
            return source;
        }

        /**
         * The anchor the pending arrow was released on.
         *
         * @return the target anchor
         */
        public String targetAnchor() {
            return target;
        }

        /**
         * The pending rule's action.
         *
         * @return the action
         */
        public String action() {
            return action;
        }
    }

    /** The client's {@code edge-selected} event: an edge was clicked. */
    @DomEvent("edge-selected")
    public static class EdgeSelectedEvent extends ComponentEvent<RuleArrowsOverlay> {
        private final int edgeId;

        /**
         * Creates the event from the client's detail.
         *
         * @param source the overlay that fired the event
         * @param fromClient whether the event came from the client
         * @param edgeId the clicked edge's relationship id
         */
        public EdgeSelectedEvent(RuleArrowsOverlay source, boolean fromClient, @EventData("event.detail.edgeId") int edgeId) {
            super(source, fromClient);
            this.edgeId = edgeId;
        }

        /**
         * The clicked edge's relationship id.
         *
         * @return the relationship id
         */
        public int edgeId() {
            return edgeId;
        }
    }

    /** The client's {@code edge-repointed} event: one end of an edge was dropped on another element. */
    @DomEvent("edge-repointed")
    public static class EdgeRepointedEvent extends ComponentEvent<RuleArrowsOverlay> {
        private final int edgeId;
        private final String end;
        private final String target;
        private final String mount;

        /**
         * Creates the event from the client's detail.
         *
         * @param source the overlay that fired the event
         * @param fromClient whether the event came from the client
         * @param edgeId the moved edge's relationship id
         * @param end which end moved, {@code SOURCE} or {@code TARGET}
         * @param target the anchor the end was dropped on
         * @param mount the section card ({@code ss-N}) a question-row target sits in, or null
         */
        public EdgeRepointedEvent(RuleArrowsOverlay source, boolean fromClient,
                                  @EventData("event.detail.edgeId") int edgeId,
                                  @EventData("event.detail.end") String end,
                                  @EventData("event.detail.target") String target,
                                  @EventData("event.detail.mount") String mount) {
            super(source, fromClient);
            this.edgeId = edgeId;
            this.end = end;
            this.target = target;
            this.mount = mount;
        }

        /**
         * The section card ({@code ss-N}) a question-row target sits in, or null.
         *
         * @return the mount anchor, or {@code null}
         */
        public String mountAnchor() {
            return mount;
        }

        /**
         * The moved edge's relationship id.
         *
         * @return the relationship id
         */
        public int edgeId() {
            return edgeId;
        }

        /**
         * Which end of the edge moved.
         *
         * @return {@code SOURCE} or {@code TARGET}
         */
        public String end() {
            return end;
        }

        /**
         * The anchor the end was dropped on.
         *
         * @return the target anchor
         */
        public String targetAnchor() {
            return target;
        }
    }
}
