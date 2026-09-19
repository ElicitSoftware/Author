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

import com.elicitsoftware.author.flow.designer.SurveyDesignerView;
import com.elicitsoftware.author.survey.StructureNode;
import com.elicitsoftware.author.survey.SurveyStructureQuery;
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
@com.vaadin.flow.component.dependency.JsModule("./drawer-resizer.js")
public class MainLayout extends AppLayout implements AfterNavigationListener {

    @Inject
    SecurityIdentity identity;

    @Inject
    BrandUtil brandUtil;

    @Inject
    SurveyStructureQuery structureQuery;

    private Component drawerContent;
    private StructureNavigationTree structureTree;
    private com.elicitsoftware.author.flow.designer.DesignerPalette drawerPalette;

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
            nav.addItem(new SideNavItem("How surveys work", SurveyModelGuideView.class, VaadinIcon.OPEN_BOOK.create()));
            nav.addItem(new SideNavItem("Import definition", ImportDefinitionView.class, VaadinIcon.UPLOAD.create()));
        }
        nav.addItem(new SideNavItem("Logout", LogoutView.class, VaadinIcon.LOCK.create()));
        setDrawerContent(nav);
    }

    /** Replaces the drawer content (used by the survey editor to show the structure tree). */
    public void setDrawerContent(Component content) {
        Scroller scroller = new Scroller(content);
        scroller.addClassName("drawer-scroller");
        replaceDrawer(scroller);
    }

    /** Swaps the whole drawer body for an already-composed component (no extra Scroller wrap). */
    private void replaceDrawer(Component content) {
        if (drawerContent != null) {
            remove(drawerContent);
        }
        drawerContent = content;
        addToDrawer(content);
    }

    /** Designer drawer: the tools palette fixed above the scrolling structure tree. */
    private void installDesignerDrawer(com.elicitsoftware.author.flow.designer.DesignerPalette palette) {
        Scroller treeScroller = new Scroller(structureTree);
        treeScroller.addClassName("drawer-scroller");
        treeScroller.setWidthFull();
        com.vaadin.flow.component.orderedlayout.VerticalLayout drawer =
                new com.vaadin.flow.component.orderedlayout.VerticalLayout(palette, treeScroller);
        drawer.setPadding(false);
        drawer.setSpacing(false);
        drawer.setSizeFull();
        drawer.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.STRETCH);
        drawer.addClassName("designer-drawer");
        drawer.setFlexGrow(1, treeScroller);
        replaceDrawer(drawer);
        drawerPalette = palette;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        getUI().ifPresent(ui -> ui.addAfterNavigationListener(this));
        // Install the mouse-draggable drawer resize handle (drawer-resizer.js) on this app-layout.
        getElement().executeJs("window.__initDrawerResizer && window.__initDrawerResizer(this)");
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        if (getContent() != null) {
            getContent().scrollIntoView();
        }
        syncDrawer(event == null || event.getActiveChain().isEmpty() ? null : event.getActiveChain().get(0));
    }

    /**
     * Inside a survey the drawer shows that survey's structure tree with the current element
     * highlighted (UC-006); everywhere else the application navigation.
     */
    /** The designer calls this whenever its selection changes (UC-006 BR-003 keeps the tree in step). */
    public void syncWith(SurveyDesignerView designer) {
        syncDrawer(designer);
    }

    void syncDrawer(Object activeView) {
        Integer surveyId = null;
        StructureNode.Kind kind = null;
        Integer durableId = null;
        SurveyDesignerView designer = null;
        if (activeView instanceof SurveyEditorView editor && editor.survey != null) {
            surveyId = editor.survey.id;
        } else if (activeView instanceof ElementView element && element.survey != null) {
            surveyId = element.survey.id;
            kind = element.kind;
            durableId = element.durableId;
        } else if (activeView instanceof SurveyDesignerView d && d.survey() != null) {
            surveyId = d.survey().id;
            designer = d;
            if (d.selected() != null) {
                kind = d.selected().kind();
                durableId = d.selected().durableId();
            }
        }
        if (surveyId == null) {
            if (structureTree != null) {
                structureTree = null;
                drawerPalette = null;
                showApplicationNavigation();
            }
            return;
        }
        boolean treeRebuilt = structureTree == null || structureTree.surveyId() != surveyId;
        if (treeRebuilt) {
            structureTree = new StructureNavigationTree(surveyId, structureQuery, n -> { });
        }
        // The designer drawer carries the tools palette above the tree; other views show the tree alone.
        // Rebuild the drawer only when the tree changed or the active designer's palette changed.
        if (designer != null && (treeRebuilt || drawerPalette != designer.palette())) {
            installDesignerDrawer(designer.palette());
        } else if (designer == null && (treeRebuilt || drawerPalette != null)) {
            drawerPalette = null;
            setDrawerContent(structureTree);
        }
        // On the designer, the tree selects on the board instead of leaving for the details page.
        structureTree.setSelectionHandler(designer == null ? null : designer::selectFromTree);
        structureTree.highlight(kind, durableId);
    }

    public void refreshStructureTree() {
        if (structureTree != null) {
            structureTree.refresh();
        }
    }

    StructureNavigationTree structureTree() {
        return structureTree;
    }
}
