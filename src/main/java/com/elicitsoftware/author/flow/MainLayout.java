package com.elicitsoftware.author.flow;

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

import com.elicitsoftware.author.util.BrandUtil;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationListener;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;

/**
 * Application shell: branded header (same markup and CSS contract as Admin and Survey) plus a
 * drawer. Outside a survey the drawer holds the application navigation; inside a survey a view
 * may replace it with the structure tree (UC-006) via {@link #setDrawerContent(Component)}.
 */
@PermitAll
public class MainLayout extends AppLayout implements AfterNavigationListener {

    @Inject
    SecurityIdentity identity;

    @Inject
    BrandUtil brandUtil;

    private Component drawerContent;

    public MainLayout() {
        // Default constructor for Vaadin
    }

    @PostConstruct
    public void init() {
        createHeader();
        showApplicationNavigation();
    }

    private void createHeader() {
        BrandUtil.BrandInfo brandInfo = brandUtil.detectCurrentBrand();

        Div headerContainer = new Div();
        headerContainer.addClassName("branded-header");
        if (brandInfo != null && brandInfo.getCssClass() != null && !brandInfo.getCssClass().isEmpty()) {
            headerContainer.addClassName(brandInfo.getCssClass());
        }

        headerContainer.add(new DrawerToggle());

        // Icon-only mark: the full horizontal lockup is illegible at header height.
        try {
            Image logo = new Image();
            logo.setSrc(brandUtil.getIconResourcePath(brandInfo));
            logo.setAlt(brandInfo.getDisplayName() + " Logo");
            logo.addClassName("logo");
            Div logoContainer = new Div(logo);
            logoContainer.addClassName("logo-container");
            headerContainer.add(logoContainer);
        } catch (Exception e) {
            // Logo not available, continue without it
        }

        String appTitle = brandInfo != null ? brandUtil.getApplicationTitle(brandInfo, "Author") : "Elicit Author";
        Anchor title = new Anchor("/", appTitle);
        title.addClassName("brand-title");
        headerContainer.add(title);

        addToNavbar(headerContainer);
    }

    /** Standard navigation: what an author can do outside a survey. */
    public void showApplicationNavigation() {
        SideNav nav = new SideNav();
        if (identity != null && identity.hasRole(com.elicitsoftware.author.security.AuthorRoles.AUTHOR)) {
            nav.addItem(new SideNavItem("Surveys", SurveysView.class, VaadinIcon.LIST.create()));
            nav.addItem(new SideNavItem("Import definition", ImportDefinitionView.class, VaadinIcon.UPLOAD.create()));
        }
        nav.addItem(new SideNavItem("Logout", LogoutView.class, VaadinIcon.LOCK.create()));
        setDrawerContent(nav);
    }

    /** Replaces the drawer content (used by the survey editor to show the structure tree). */
    public void setDrawerContent(Component content) {
        if (drawerContent != null) {
            remove(drawerContent);
        }
        Scroller scroller = new Scroller(content);
        scroller.addClassName("drawer-scroller");
        drawerContent = scroller;
        addToDrawer(scroller);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        getUI().ifPresent(ui -> ui.addAfterNavigationListener(this));
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        getContent().scrollIntoView();
    }
}
