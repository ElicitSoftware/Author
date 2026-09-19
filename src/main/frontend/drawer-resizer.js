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

/*-
 * Adds a mouse-draggable resize handle to the AppLayout drawer's right edge. The handle sets the
 * `--vaadin-app-layout-drawer-width` custom property on the app-layout element (supported by Lumo),
 * clamped to a sensible range, and remembers the chosen width per browser in localStorage.
 *
 * Registered as a global initialiser and invoked from MainLayout with the app-layout element, so
 * there is exactly one handle regardless of navigation.
 */
window.__initDrawerResizer = function (appLayout) {
  if (!appLayout || appLayout.__resizerInstalled) return;
  appLayout.__resizerInstalled = true;

  const KEY = 'elicit-author-drawer-width';
  const MIN = 220;
  const MAX = 680;

  const saved = parseInt(localStorage.getItem(KEY), 10);
  if (saved >= MIN && saved <= MAX) {
    appLayout.style.setProperty('--vaadin-app-layout-drawer-width', saved + 'px');
  }

  const handle = document.createElement('div');
  handle.className = 'drawer-resizer';
  handle.setAttribute('title', 'Drag to resize the drawer');
  handle.setAttribute('role', 'separator');
  handle.setAttribute('aria-label', 'Resize drawer');
  document.body.appendChild(handle);

  const drawer = () => appLayout.shadowRoot && appLayout.shadowRoot.querySelector('[part="drawer"]');

  function place() {
    const d = drawer();
    const r = d && d.getBoundingClientRect();
    if (!r || r.width === 0) { handle.style.display = 'none'; return; }
    handle.style.display = 'block';
    handle.style.top = r.top + 'px';
    handle.style.height = r.height + 'px';
    handle.style.left = (r.right - 3) + 'px';
  }

  let dragging = false;
  let drawerLeft = 0;

  handle.addEventListener('pointerdown', (e) => {
    const d = drawer();
    if (!d) return;
    dragging = true;
    drawerLeft = d.getBoundingClientRect().left;
    handle.setPointerCapture(e.pointerId);
    document.body.style.userSelect = 'none';
    e.preventDefault();
  });
  handle.addEventListener('pointermove', (e) => {
    if (!dragging) return;
    let w = Math.round(e.clientX - drawerLeft);
    w = Math.max(MIN, Math.min(MAX, w));
    appLayout.style.setProperty('--vaadin-app-layout-drawer-width', w + 'px');
    place();
  });
  const end = (e) => {
    if (!dragging) return;
    dragging = false;
    document.body.style.userSelect = '';
    try { handle.releasePointerCapture(e.pointerId); } catch (_) { /* ignore */ }
    const d = drawer();
    if (d) localStorage.setItem(KEY, String(Math.round(d.getBoundingClientRect().width)));
  };
  handle.addEventListener('pointerup', end);
  handle.addEventListener('pointercancel', end);

  const d0 = drawer();
  if (d0 && window.ResizeObserver) new ResizeObserver(place).observe(d0);
  window.addEventListener('resize', place);
  new MutationObserver(place).observe(appLayout, { attributes: true, attributeFilter: ['style', 'drawer-opened', 'overlay'] });
  requestAnimationFrame(place);
  setTimeout(place, 300);
  setTimeout(place, 1200);
};
