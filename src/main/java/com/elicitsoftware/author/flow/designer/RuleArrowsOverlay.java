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

    public RuleArrowsOverlay() {
        getElement().setAttribute("aria-hidden", "true");
    }

    public void setEdges(String json) {
        getElement().setProperty("edges", json == null ? "[]" : json);
    }

    public String edges() {
        return getElement().getProperty("edges", "[]");
    }

    public void setSelectedEdgeId(Integer relationshipId) {
        getElement().setProperty("selectedEdgeId", relationshipId == null ? "" : String.valueOf(relationshipId));
    }

    /** The board's selected node anchor (step-/ss-/sq-id); the overlay focuses the rules touching it. */
    public void setSelectedAnchor(String anchor) {
        getElement().setProperty("selectedAnchor", anchor == null ? "" : anchor);
    }

    /** UC-019 step 2: a rule glyph landed on {@code sourceAnchor}; the client draws the dangling arrow. */
    public void setPending(String sourceAnchor, String action) {
        getElement().setProperty("pendingAction", action == null ? "" : action);
        getElement().setProperty("pendingSource", sourceAnchor == null ? "" : sourceAnchor);
    }

    public String pendingSource() {
        return getElement().getProperty("pendingSource", "");
    }

    public String pendingAction() {
        return getElement().getProperty("pendingAction", "");
    }

    public Registration addConnectRequestedListener(ComponentEventListener<ConnectRequestedEvent> listener) {
        return addListener(ConnectRequestedEvent.class, listener);
    }

    public Registration addEdgeSelectedListener(ComponentEventListener<EdgeSelectedEvent> listener) {
        return addListener(EdgeSelectedEvent.class, listener);
    }

    public Registration addEdgeRepointedListener(ComponentEventListener<EdgeRepointedEvent> listener) {
        return addListener(EdgeRepointedEvent.class, listener);
    }

    @DomEvent("connect-requested")
    public static class ConnectRequestedEvent extends ComponentEvent<RuleArrowsOverlay> {
        private final String source;
        private final String target;
        private final String action;

        public ConnectRequestedEvent(RuleArrowsOverlay source, boolean fromClient,
                                     @EventData("event.detail.source") String sourceAnchor,
                                     @EventData("event.detail.target") String targetAnchor,
                                     @EventData("event.detail.action") String action) {
            super(source, fromClient);
            this.source = sourceAnchor;
            this.target = targetAnchor;
            this.action = action;
        }

        public String sourceAnchor() {
            return source;
        }

        public String targetAnchor() {
            return target;
        }

        public String action() {
            return action;
        }
    }

    @DomEvent("edge-selected")
    public static class EdgeSelectedEvent extends ComponentEvent<RuleArrowsOverlay> {
        private final int edgeId;

        public EdgeSelectedEvent(RuleArrowsOverlay source, boolean fromClient, @EventData("event.detail.edgeId") int edgeId) {
            super(source, fromClient);
            this.edgeId = edgeId;
        }

        public int edgeId() {
            return edgeId;
        }
    }

    @DomEvent("edge-repointed")
    public static class EdgeRepointedEvent extends ComponentEvent<RuleArrowsOverlay> {
        private final int edgeId;
        private final String end;
        private final String target;

        public EdgeRepointedEvent(RuleArrowsOverlay source, boolean fromClient,
                                  @EventData("event.detail.edgeId") int edgeId,
                                  @EventData("event.detail.end") String end,
                                  @EventData("event.detail.target") String target) {
            super(source, fromClient);
            this.edgeId = edgeId;
            this.end = end;
            this.target = target;
        }

        public int edgeId() {
            return edgeId;
        }

        public String end() {
            return end;
        }

        public String targetAnchor() {
            return target;
        }
    }
}
