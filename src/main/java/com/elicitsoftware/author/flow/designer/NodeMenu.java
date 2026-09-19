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

import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;

/**
 * The per-element "⋮" menu: every drag action has an entry here (NFR-012), so the board is
 * usable from the keyboard and on touch devices.
 */
public class NodeMenu extends MenuBar {

    private final SubMenu items;

    public NodeMenu(String accessibleName) {
        addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE, MenuBarVariant.LUMO_SMALL);
        addClassName("designer-node-menu");
        MenuItem root = addItem(VaadinIcon.ELLIPSIS_DOTS_V.create());
        root.setAriaLabel(accessibleName);
        items = root.getSubMenu();
        // The menu sits inside a clickable header/row: its clicks and keys must not select or
        // toggle the element (which would rebuild the board and destroy the open menu).
        getElement().addEventListener("click", e -> { }).stopPropagation();
        getElement().addEventListener("keydown", e -> { }).stopPropagation();
    }

    public MenuItem item(String label, Runnable action) {
        return items.addItem(label, e -> action.run());
    }

    public MenuItem item(String label, Runnable action, boolean enabled) {
        MenuItem item = item(label, action);
        item.setEnabled(enabled);
        return item;
    }

    /** Menu entries by label, for tests. */
    public MenuItem find(String label) {
        return items.getItems().stream().filter(i -> label.equals(i.getText())).findFirst().orElse(null);
    }
}
