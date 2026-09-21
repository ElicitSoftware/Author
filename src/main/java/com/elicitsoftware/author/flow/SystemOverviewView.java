package com.elicitsoftware.author.flow;

/*-
 * ***LICENSE_START***
 * Elicit Survey
 * %%
 * Copyright (C) 2025 - 2026 The Regents of the University of Michigan - Rogel Cancer Center
 * %%
 * PolyForm Noncommercial License 1.0.0
 * <https://polyformproject.org/licenses/noncommercial/1.0.0>
 * ***LICENSE_END***
 */

import com.elicitsoftware.author.diagnostics.BrandDiagnostics;
import com.elicitsoftware.author.diagnostics.BuildInfo;
import com.elicitsoftware.author.diagnostics.CheckResult;
import com.elicitsoftware.author.diagnostics.ConnectionChecks;
import com.elicitsoftware.author.diagnostics.DatabaseDiagnostics;
import com.elicitsoftware.author.diagnostics.RequiredConfigCheck;
import com.elicitsoftware.author.security.AuthorRoles;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;

import java.time.Duration;
import java.util.List;

/**
 * System overview (UC-034): what is running, whether it is healthy, and whether every required
 * setting is present. Read-only; every summary is re-established when the screen opens.
 */
@Route(value = "system", layout = MainLayout.class)
@RolesAllowed(AuthorRoles.ADMIN)
public class SystemOverviewView extends VerticalLayout implements HasDynamicTitle {

    static final String BUILD_GRID_ID = "system-build-grid";
    static final String HEALTH_GRID_ID = "system-health-grid";
    static final String CONFIG_GRID_ID = "system-config-grid";

    record Row(String label, String value) {
    }

    record Area(String name, CheckResult result, Class<? extends Component> screen) {
    }

    @Inject
    BuildInfo buildInfo;

    @Inject
    DatabaseDiagnostics database;

    @Inject
    BrandDiagnostics brand;

    @Inject
    ConnectionChecks connections;

    @Inject
    RequiredConfigCheck requiredConfig;

    public SystemOverviewView() {
        setWidthFull();
        setPadding(true);
    }

    @PostConstruct
    void init() {
        add(new H3("System Overview"));
        add(new Paragraph("What this deployment is running, whether it is healthy, and whether every "
                + "required setting is present. Settings are shown as present or absent, never by value; "
                + "they are startup configuration and cannot be changed here."));

        add(new H4("Running"));
        add(rows(BUILD_GRID_ID, List.of(
                new Row("Application", buildInfo.applicationName()),
                new Row("Version", buildInfo.version()),
                new Row("Built", buildInfo.buildTimestamp()),
                new Row("Profile", String.join(", ", buildInfo.profiles())),
                new Row("Started", buildInfo.startedAt().toString()),
                new Row("Uptime", formatUptime(buildInfo.uptime())))));

        add(new H4("Health"));
        DatabaseDiagnostics.ConnectionReport connection = database.checkConnection();
        add(areas(List.of(
                new Area("Database connection", connection.result(), SystemDatabaseView.class),
                new Area("Survey schema", connection.result().isUp() ? database.checkSchema()
                        : CheckResult.unknown("Survey schema", "not checked: the connection failed"), SystemDatabaseView.class),
                new Area("Branding", brandSummary(), SystemBrandingView.class),
                new Area("Connections", connectionsSummary(), SystemConnectionsView.class))));

        add(new H4("Required settings"));
        Grid<RequiredConfigCheck.RequiredSetting> config = new Grid<>();
        config.setId(CONFIG_GRID_ID);
        config.setItems(requiredConfig.requiredSettings());
        config.addColumn(RequiredConfigCheck.RequiredSetting::property).setHeader("Setting").setAutoWidth(true);
        config.addColumn(RequiredConfigCheck.RequiredSetting::envVar).setHeader("Supplied by").setAutoWidth(true);
        config.addColumn(RequiredConfigCheck.RequiredSetting::purpose).setHeader("Purpose").setFlexGrow(1);
        config.addComponentColumn(s -> SystemBadges.presence(s.present())).setHeader("State").setAutoWidth(true);
        config.setAllRowsVisible(true);
        config.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_WRAP_CELL_CONTENT);
        add(config);
    }

    private CheckResult brandSummary() {
        try {
            return CheckResult.up("Branding", brand.report().summary(), 0);
        } catch (RuntimeException e) {
            return CheckResult.down("Branding", e.getMessage(), 0);
        }
    }

    private CheckResult connectionsSummary() {
        int count = connections.targets().size();
        return CheckResult.unknown("Connections", count + " outbound target" + (count == 1 ? "" : "s")
                + " configured; open Connections to check them");
    }

    private static Grid<Row> rows(String id, List<Row> rows) {
        Grid<Row> grid = new Grid<>();
        grid.setId(id);
        grid.setItems(rows);
        grid.addColumn(Row::label).setHeader("Item").setAutoWidth(true);
        grid.addColumn(Row::value).setHeader("Value").setFlexGrow(1);
        grid.setAllRowsVisible(true);
        grid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_WRAP_CELL_CONTENT);
        return grid;
    }

    private static Grid<Area> areas(List<Area> areas) {
        Grid<Area> grid = new Grid<>();
        grid.setId(HEALTH_GRID_ID);
        grid.setItems(areas);
        grid.addComponentColumn(a -> new RouterLink(a.name(), a.screen())).setHeader("Area").setAutoWidth(true);
        grid.addComponentColumn(a -> SystemBadges.status(a.result())).setHeader("State").setAutoWidth(true);
        grid.addColumn(a -> a.result().detail()).setHeader("Detail").setFlexGrow(1);
        grid.setAllRowsVisible(true);
        grid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_WRAP_CELL_CONTENT);
        return grid;
    }

    static String formatUptime(Duration uptime) {
        long days = uptime.toDays();
        long hours = uptime.toHoursPart();
        long minutes = uptime.toMinutesPart();
        if (days > 0) {
            return days + " d " + hours + " h " + minutes + " min";
        }
        if (hours > 0) {
            return hours + " h " + minutes + " min";
        }
        return minutes + " min " + uptime.toSecondsPart() + " s";
    }

    @Override
    public String getPageTitle() {
        return "System Overview";
    }
}
