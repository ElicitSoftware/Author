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

/*-
 * Adds a mouse-draggable resize handle to the AppLayout drawer's right edge. The handle sets the
 * `--vaadin-app-layout-drawer-width` custom property on the app-layout element (supported by Lumo),
 * clamped to a sensible range, and remembers the chosen width per browser in localStorage.
 *
 * Registered as a global initializer and invoked from MainLayout with the app-layout element, so
 * there is exactly one handle regardless of navigation.
 */
window.__initDrawerResizer = function (appLayout, texts) {
  if (!appLayout || appLayout.__resizerInstalled) return;
  appLayout.__resizerInstalled = true;
  texts = texts || {};
  const rtl = () => getComputedStyle(document.documentElement).direction === 'rtl';

  // Elicit Author is a desktop/tablet authoring tool; below tablet width show a full-screen notice
  // (styled and toggled by the .small-screen-notice media query) instead of a cramped, unusable UI.
  if (!document.querySelector('.small-screen-notice')) {
    const notice = document.createElement('div');
    notice.className = 'small-screen-notice';
    const card = document.createElement('div');
    card.className = 'small-screen-notice-card';
    const h = document.createElement('h2');
    h.textContent = texts.smallScreenHeading || 'A bigger screen is needed';
    const p = document.createElement('p');
    p.textContent = texts.smallScreenText || 'Elicit Author is built for designing surveys on a tablet or larger screen. Please switch to a wider device.';
    card.appendChild(h);
    card.appendChild(p);
    notice.appendChild(card);
    document.body.appendChild(notice);
  }

  const KEY = 'elicit-author-drawer-width';
  const MIN = 220;
  const MAX = 680;

  const saved = parseInt(localStorage.getItem(KEY), 10);
  if (saved >= MIN && saved <= MAX) {
    appLayout.style.setProperty('--vaadin-app-layout-drawer-width', saved + 'px');
  }

  const handle = document.createElement('div');
  handle.className = 'drawer-resizer';
  handle.setAttribute('title', texts.title || 'Drag to resize the drawer');
  handle.setAttribute('role', 'separator');
  handle.setAttribute('aria-label', texts.ariaLabel || 'Resize drawer');
  document.body.appendChild(handle);

  const drawer = () => appLayout.shadowRoot && appLayout.shadowRoot.querySelector('[part="drawer"]');

  function place() {
    const d = drawer();
    const r = d && d.getBoundingClientRect();
    if (!r || r.width === 0) { handle.style.display = 'none'; return; }
    handle.style.display = 'block';
    handle.style.top = r.top + 'px';
    handle.style.height = r.height + 'px';
    // The drawer sits at the inline start, so its resize edge is on the left in RTL layouts.
    handle.style.left = (rtl() ? r.left - 3 : r.right - 3) + 'px';
  }

  let dragging = false;
  let drawerLeft = 0;
  let drawerRight = 0;

  handle.addEventListener('pointerdown', (e) => {
    const d = drawer();
    if (!d) return;
    dragging = true;
    drawerLeft = d.getBoundingClientRect().left;
    drawerRight = d.getBoundingClientRect().right;
    handle.setPointerCapture(e.pointerId);
    document.body.style.userSelect = 'none';
    e.preventDefault();
  });
  handle.addEventListener('pointermove', (e) => {
    if (!dragging) return;
    let w = Math.round(rtl() ? drawerRight - e.clientX : e.clientX - drawerLeft);
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
