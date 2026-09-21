package com.elicitsoftware.author.flow;

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

import com.elicitsoftware.author.flow.designer.SurveyDesignerView;
import com.elicitsoftware.author.i18n.ElicitI18NProvider;
import com.elicitsoftware.author.i18n.LanguageSwitcher;
import com.elicitsoftware.author.i18n.LocaleSelection;
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
import com.vaadin.quarkus.annotation.VaadinServiceEnabled;
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

    /** Remembers the language the author picks (UC-039). */
    @Inject
    LocaleSelection localeSelection;

    /** Supplies the languages offered by the switcher. */
    @Inject
    @VaadinServiceEnabled
    ElicitI18NProvider i18nProvider;

    private Component drawerContent;
    private StructureNavigationTree structureTree;
    private com.elicitsoftware.author.flow.designer.DesignerToolbox drawerPalette;

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
            logo.setAlt(getTranslation("common.logoAlt", brandInfo.getDisplayName(getLocale())));
            logo.addClassName("logo");
            Div logoContainer = new Div(logo);
            logoContainer.addClassName("logo-container");
            headerContainer.add(logoContainer);
        } catch (Exception e) {
            // Logo not available, continue without it
        }

        String appType = getTranslation("common.appType.author");
        String appTitle = brandInfo == null || brandInfo.isDefaultBrand()
                ? getTranslation("common.appTitle.default", appType)
                : getTranslation("common.appTitle", brandInfo.getDisplayName(getLocale()), appType);
        Anchor title = new Anchor("/", appTitle);
        title.addClassName("brand-title");
        headerContainer.add(title);

        // Language selector (UC-039): every page offers the shipped and mounted languages.
        headerContainer.add(new LanguageSwitcher(localeSelection, i18nProvider));

        addToNavbar(headerContainer);
    }

    /** Standard navigation: what an author can do outside a survey. */
    public void showApplicationNavigation() {
        SideNav nav = new SideNav();
        if (identity != null && identity.hasRole(com.elicitsoftware.author.security.AuthorRoles.AUTHOR)) {
            nav.addItem(new SideNavItem(getTranslation("mainLayout.nav.surveys"), SurveysView.class, VaadinIcon.LIST.create()));
            nav.addItem(new SideNavItem(getTranslation("mainLayout.nav.guide"), SurveyModelGuideView.class, VaadinIcon.OPEN_BOOK.create()));
            nav.addItem(new SideNavItem(getTranslation("mainLayout.nav.importDefinition"), ImportDefinitionView.class, VaadinIcon.UPLOAD.create()));
        }
        if (identity != null && identity.hasRole(com.elicitsoftware.author.security.AuthorRoles.ADMIN)) {
            nav.addItem(createSystemSection());
        }
        nav.addItem(new SideNavItem(getTranslation("mainLayout.nav.logout"), LogoutView.class, VaadinIcon.LOCK.create()));
        setDrawerContent(nav);
    }

    /**
     * The System section: setup and diagnostics for the administrator or operator wiring up a
     * deployment (UC-034 to UC-038). Gated on {@code elicit_admin}, which grants no authoring.
     */
    private SideNavItem createSystemSection() {
        SideNavItem systemSection = new SideNavItem(getTranslation("mainLayout.nav.system"));
        systemSection.setPrefixComponent(VaadinIcon.TOOLS.create());
        systemSection.addItem(new SideNavItem(getTranslation("mainLayout.nav.systemOverview"), SystemOverviewView.class,
                VaadinIcon.DASHBOARD.create()));
        systemSection.addItem(new SideNavItem(getTranslation("mainLayout.nav.systemDatabase"), SystemDatabaseView.class,
                VaadinIcon.DATABASE.create()));
        systemSection.addItem(new SideNavItem(getTranslation("mainLayout.nav.systemBranding"), SystemBrandingView.class,
                VaadinIcon.PAINTBRUSH.create()));
        systemSection.addItem(new SideNavItem(getTranslation("mainLayout.nav.systemConnections"), SystemConnectionsView.class,
                VaadinIcon.CONNECT.create()));
        systemSection.addItem(new SideNavItem(getTranslation("mainLayout.nav.systemOidc"), OidcDiagnosticsView.class,
                VaadinIcon.SHIELD.create()));
        return systemSection;
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

    /** Designer drawer: the tools palette above the structure tree, scrolling together as one surface. */
    private void installDesignerDrawer(com.elicitsoftware.author.flow.designer.DesignerToolbox palette) {
        com.vaadin.flow.component.orderedlayout.VerticalLayout drawer =
                new com.vaadin.flow.component.orderedlayout.VerticalLayout(palette, structureTree);
        drawer.setPadding(false);
        drawer.setSpacing(false);
        drawer.setWidthFull();
        drawer.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.STRETCH);
        Scroller scroller = new Scroller(drawer);
        scroller.addClassName("drawer-scroller");
        scroller.addClassName("designer-drawer");
        scroller.setSizeFull();
        replaceDrawer(scroller);
        drawerPalette = palette;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        getUI().ifPresent(ui -> ui.addAfterNavigationListener(this));
        // Install the mouse-draggable drawer resize handle (drawer-resizer.js) on this app-layout.
        getElement().executeJs("window.__initDrawerResizer && window.__initDrawerResizer(this, "
                + "{title: $0, ariaLabel: $1, smallScreenHeading: $2, smallScreenText: $3})",
                getTranslation("mainLayout.resizer.title"), getTranslation("mainLayout.resizer.aria"),
                getTranslation("mainLayout.smallScreen.heading"), getTranslation("mainLayout.smallScreen.text"));
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
        } else if (activeView instanceof com.elicitsoftware.author.flow.reporting.ReportingView r && r.survey != null) {
            surveyId = r.survey.id;
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
        if (designer != null && (treeRebuilt || drawerPalette != designer.toolbox())) {
            installDesignerDrawer(designer.toolbox());
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
