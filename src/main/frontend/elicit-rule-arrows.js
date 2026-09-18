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

import { LitElement, html, svg, css } from 'lit';

/**
 * Rule arrows over the designer board (UC-019, UC-020). The element sits inside `.designer-board`
 * and draws one SVG arrow per edge from the upstream question row to the downstream lane
 * header, card header or row, resolving each end by `[data-node-id]` and falling back to the
 * card that holds a row while that card is closed. The action glyph at the midpoint is the
 * rule's handle; both ends can be dragged to re-point the rule; a pending arrow follows the
 * pointer after a rule glyph was dropped on a question (`pendingSource`).
 *
 * Events (all bubble, composed):
 *   connect-requested {source, target, action}  – pending arrow released on a target
 *   edge-selected     {edgeId}                   – action glyph clicked
 *   edge-repointed    {edgeId, end, target}      – an arrow end dropped on another element
 */
const GLYPH = { SHOW: '\u{1F441}', REPEAT: '↻', TEXT: '{T}' };

class ElicitRuleArrows extends LitElement {
  static properties = {
    edges: { type: String },
    selectedEdgeId: { type: String, attribute: 'selected-edge-id' },
    pendingSource: { type: String, attribute: 'pending-source' },
    pendingAction: { type: String, attribute: 'pending-action' },
    _lines: { state: true },
    _pointer: { state: true },
    _size: { state: true }
  };

  static styles = css`
    :host {
      position: absolute;
      left: 0;
      top: 0;
      pointer-events: none;
      z-index: 2;
      overflow: visible;
    }
    svg { overflow: visible; display: block; }
    .edge path.line {
      fill: none;
      stroke: var(--brand-accent, var(--brand-warning, var(--lumo-warning-color, #b7791f)));
      stroke-width: 2;
    }
    .edge.retired path.line { stroke-dasharray: 6 4; opacity: 0.6; }
    .edge.selected path.line { stroke-width: 3.5; }
    .edge .hit { fill: none; stroke: transparent; stroke-width: 14; pointer-events: stroke; cursor: pointer; }
    .glyph { pointer-events: all; cursor: grab; }
    .glyph circle {
      fill: var(--brand-accent, var(--brand-warning, var(--lumo-warning-color, #b7791f)));
      stroke: var(--brand-surface, var(--lumo-base-color, #fff));
      stroke-width: 2;
    }
    .edge.selected .glyph circle { stroke: var(--brand-primary, var(--lumo-primary-color, #1a5fb4)); stroke-width: 3; }
    .glyph text { fill: var(--brand-text-on-accent, #fff); font-size: 11px; font-weight: 700; text-anchor: middle; dominant-baseline: central; font-family: var(--lumo-font-family, sans-serif); pointer-events: none; }
    .label { fill: var(--brand-text-secondary, var(--lumo-secondary-text-color, #555)); font-size: 11px; text-anchor: middle; font-family: var(--lumo-font-family, sans-serif); paint-order: stroke; stroke: var(--brand-surface, var(--lumo-base-color, #fff)); stroke-width: 3px; pointer-events: none; }
    .end { pointer-events: all; cursor: move; fill: var(--brand-accent, var(--brand-warning, var(--lumo-warning-color, #b7791f))); stroke: var(--brand-surface, #fff); stroke-width: 1.5; }
    .pending path { fill: none; stroke: var(--brand-primary, var(--lumo-primary-color, #1a5fb4)); stroke-width: 2; stroke-dasharray: 5 4; }
    marker path { fill: var(--brand-accent, var(--brand-warning, var(--lumo-warning-color, #b7791f))); }
    marker.pending-head path { fill: var(--brand-primary, var(--lumo-primary-color, #1a5fb4)); }
  `;

  constructor() {
    super();
    this.edges = '[]';
    this.selectedEdgeId = '';
    this.pendingSource = '';
    this.pendingAction = '';
    this._lines = [];
    this._pointer = null;
    this._size = { w: 0, h: 0 };
    this._drag = null;
    this._raf = 0;
    this._relayout = () => this._schedule();
    this._onMove = (e) => this._pointerMove(e);
    this._onUp = (e) => this._pointerUp(e);
    this._onKey = (e) => { if (e.key === 'Escape') this._cancel(); };
  }

  connectedCallback() {
    super.connectedCallback();
    const board = this.parentElement;
    if (board) {
      this._resize = new ResizeObserver(this._relayout);
      this._resize.observe(board);
      this._mutations = new MutationObserver(this._relayout);
      this._mutations.observe(board, { subtree: true, childList: true, attributes: true, attributeFilter: ['class', 'hidden', 'style'] });
      board.addEventListener('scroll', this._relayout);
    }
    window.addEventListener('resize', this._relayout);
    this._schedule();
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    this._resize?.disconnect();
    this._mutations?.disconnect();
    this.parentElement?.removeEventListener('scroll', this._relayout);
    window.removeEventListener('resize', this._relayout);
    this._stopTracking();
  }

  updated(changed) {
    if (changed.has('edges')) this._schedule();
    if (changed.has('pendingSource')) {
      if (this.pendingSource) {
        this._drag = { pending: true };
        this._startTracking();
      } else if (this._drag?.pending) {
        this._drag = null;
        this._stopTracking();
      }
      this._schedule();
    }
  }

  _schedule() {
    if (this._raf) return;
    this._raf = requestAnimationFrame(() => { this._raf = 0; this._layout(); });
  }

  _board() { return this.parentElement; }

  _resolve(id, fallbacks) {
    const board = this._board();
    if (!board) return null;
    const visible = (el) => el && el.getClientRects().length > 0;
    let el = board.querySelector(`[data-node-id="${id}"]`);
    if (visible(el)) return el;
    for (const fb of fallbacks || []) {
      el = board.querySelector(`[data-node-id="${fb}"]`);
      if (visible(el)) return el;
    }
    return null;
  }

  _point(el, side) {
    const board = this._board();
    const b = board.getBoundingClientRect();
    const r = (el.querySelector(':scope > .designer-lane-header, :scope > .designer-card-header') || el).getBoundingClientRect();
    const x = (side === 'right' ? r.right : r.left) - b.left + board.scrollLeft;
    const y = r.top + r.height / 2 - b.top + board.scrollTop;
    return { x, y };
  }

  _layout() {
    const board = this._board();
    if (!board) return;
    let edges = [];
    try { edges = JSON.parse(this.edges || '[]'); } catch (e) { edges = []; }
    const lines = [];
    for (const e of edges) {
      const s = this._resolve(e.source, e.sourceFallbacks);
      const t = this._resolve(e.target, e.targetFallbacks);
      if (!s || !t) continue;
      lines.push({ edge: e, from: this._point(s, 'right'), to: this._point(t, 'left') });
    }
    this._lines = lines;
    this._size = { w: board.scrollWidth, h: board.scrollHeight };
  }

  _path(from, to) {
    const dx = Math.max(40, Math.abs(to.x - from.x) / 2);
    return `M${from.x},${from.y} C${from.x + dx},${from.y} ${to.x - dx},${to.y} ${to.x},${to.y}`;
  }

  _startTracking() {
    document.addEventListener('pointermove', this._onMove);
    document.addEventListener('pointerup', this._onUp);
    document.addEventListener('keydown', this._onKey);
  }

  _stopTracking() {
    document.removeEventListener('pointermove', this._onMove);
    document.removeEventListener('pointerup', this._onUp);
    document.removeEventListener('keydown', this._onKey);
  }

  _pointerMove(e) {
    const board = this._board();
    if (!board) return;
    if (this._drag?.armed) {
      if (Math.abs(e.clientX - this._drag.startX) < 5 && Math.abs(e.clientY - this._drag.startY) < 5) return;
      this._drag.armed = false;
    }
    const b = board.getBoundingClientRect();
    this._pointer = { x: e.clientX - b.left + board.scrollLeft, y: e.clientY - b.top + board.scrollTop, cx: e.clientX, cy: e.clientY };
  }

  _pointerUp(e) {
    const drag = this._drag;
    this._drag = null;
    this._stopTracking();
    this._pointer = null;
    const hit = document.elementFromPoint(e.clientX, e.clientY);
    const node = hit ? hit.closest('[data-node-id]') : null;
    const anchor = node ? node.getAttribute('data-node-id') : null;
    if (!drag) return;
    if (drag.armed) {
      // Never moved: a click, not a re-point.
      this._emit('edge-selected', { edgeId: drag.edgeId });
      this._schedule();
      return;
    }
    if (drag.pending) {
      const source = this.pendingSource;
      const action = this.pendingAction;
      this.pendingSource = '';
      if (anchor && anchor !== source) {
        this._emit('connect-requested', { source, target: anchor, action });
      }
    } else if (anchor) {
      this._emit('edge-repointed', { edgeId: drag.edgeId, end: drag.end, target: anchor });
    }
    this._schedule();
  }

  _cancel() {
    this._drag = null;
    this._stopTracking();
    this._pointer = null;
    this.pendingSource = '';
    this._schedule();
  }

  /**
   * Arm a re-point on pointerdown; it only becomes a drag once the pointer has moved a few
   * pixels, so a plain click on the glyph or an end stays a click (edge-selected).
   */
  _grabEnd(e, edgeId, end) {
    e.preventDefault();
    e.stopPropagation();
    this._drag = { edgeId, end, armed: true, startX: e.clientX, startY: e.clientY };
    this._startTracking();
  }

  _emit(name, detail) {
    console.debug('[elicit-rule-arrows]', name, detail);
    this.dispatchEvent(new CustomEvent(name, { detail, bubbles: true, composed: true }));
  }

  render() {
    const { w, h } = this._size;
    const pending = this._drag?.pending && this._pointer ? this._pendingLine() : null;
    const dragged = this._drag && !this._drag.pending ? this._drag : null;
    return html`<svg width="${w}" height="${h}" viewBox="0 0 ${w} ${h}" aria-hidden="true">
      <defs>
        <marker id="head" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="8" markerHeight="8" orient="auto-start-reverse"><path d="M0,0 L10,5 L0,10 z"/></marker>
        <marker id="pending-head" class="pending-head" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="8" markerHeight="8" orient="auto-start-reverse"><path d="M0,0 L10,5 L0,10 z"/></marker>
      </defs>
      ${this._lines.map((l) => this._renderEdge(l, dragged))}
      ${pending ? svg`<g class="pending"><path d="${this._path(pending.from, pending.to)}" marker-end="url(#pending-head)"></path></g>` : ''}
    </svg>`;
  }

  _pendingLine() {
    const src = this._resolve(this.pendingSource, []);
    const from = src ? this._point(src, 'right') : this._pointer;
    return { from, to: { x: this._pointer.x, y: this._pointer.y } };
  }

  _renderEdge(l, dragged) {
    const e = l.edge;
    const id = e.relationshipId ?? e.id;
    let from = l.from;
    let to = l.to;
    if (dragged && String(dragged.edgeId) === String(id) && this._pointer) {
      if (dragged.end === 'SOURCE') from = { x: this._pointer.x, y: this._pointer.y };
      else to = { x: this._pointer.x, y: this._pointer.y };
    }
    const mid = { x: (from.x + to.x) / 2, y: (from.y + to.y) / 2 };
    const selected = String(id) === String(this.selectedEdgeId);
    const cls = `edge ${e.retired ? 'retired' : ''} ${selected ? 'selected' : ''}`;
    const d = this._path(from, to);
    return svg`<g class="${cls}" data-edge-id="${id}">
      <path class="line" d="${d}" marker-end="url(#head)"></path>
      <path class="hit" d="${d}" @click=${() => this._emit('edge-selected', { edgeId: id })}></path>
      <circle class="end" cx="${from.x}" cy="${from.y}" r="5" @pointerdown=${(ev) => this._grabEnd(ev, id, 'SOURCE')}><title>Drag to read from another question</title></circle>
      <circle class="end" cx="${to.x}" cy="${to.y}" r="5" @pointerdown=${(ev) => this._grabEnd(ev, id, 'TARGET')}><title>Drag to another target</title></circle>
      <g class="glyph" transform="translate(${mid.x},${mid.y})"
         @pointerdown=${(ev) => this._grabEnd(ev, id, 'TARGET')}>
        <circle r="11"></circle>
        <text>${GLYPH[e.action] || '?'}</text>
        <title>${e.action} rule: ${e.label}${e.description ? ' — ' + e.description : ''}</title>
      </g>
      <text class="label" x="${mid.x}" y="${mid.y + 24}">${e.label}</text>
    </g>`;
  }
}

customElements.define('elicit-rule-arrows', ElicitRuleArrows);
