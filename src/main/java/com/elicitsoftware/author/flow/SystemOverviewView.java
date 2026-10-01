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
        add(new H3(getTranslation("systemOverviewView.title")));
        add(new Paragraph(getTranslation("systemOverviewView.intro")));

        add(new H4(getTranslation("systemOverviewView.running")));
        add(rows(BUILD_GRID_ID, List.of(
                new Row(getTranslation("systemOverviewView.row.application"), buildInfo.applicationName()),
                new Row(getTranslation("systemOverviewView.row.version"), buildInfo.version()),
                new Row(getTranslation("systemOverviewView.row.built"), buildInfo.buildTimestamp()),
                new Row(getTranslation("systemOverviewView.row.profile"), String.join(", ", buildInfo.profiles())),
                new Row(getTranslation("systemOverviewView.row.started"), buildInfo.startedAt().toString()),
                new Row(getTranslation("systemOverviewView.row.uptime"), formatUptime(buildInfo.uptime())))));

        add(new H4(getTranslation("systemOverviewView.health")));
        DatabaseDiagnostics.ConnectionReport connection = database.checkConnection();
        add(areas(List.of(
                new Area(getTranslation("systemOverviewView.area.database"), connection.result(), SystemDatabaseView.class),
                new Area(getTranslation("systemOverviewView.area.surveySchema"), connection.result().isUp() ? database.checkSchema()
                        : CheckResult.unknown("Survey schema", getTranslation("systemOverviewView.area.surveySchema.notChecked")), // i18n:ignore
                        SystemDatabaseView.class),
                new Area(getTranslation("systemOverviewView.area.branding"), brandSummary(), SystemBrandingView.class),
                new Area(getTranslation("systemOverviewView.area.connections"), connectionsSummary(), SystemConnectionsView.class))));

        add(new H4(getTranslation("systemOverviewView.requiredSettings")));
        Grid<RequiredConfigCheck.RequiredSetting> config = new Grid<>();
        config.setId(CONFIG_GRID_ID);
        config.setItems(requiredConfig.requiredSettings());
        config.addColumn(RequiredConfigCheck.RequiredSetting::property).setHeader(getTranslation("systemOverviewView.grid.setting")).setAutoWidth(true);
        config.addColumn(RequiredConfigCheck.RequiredSetting::envVar).setHeader(getTranslation("systemOverviewView.grid.suppliedBy")).setAutoWidth(true);
        config.addColumn(RequiredConfigCheck.RequiredSetting::purpose).setHeader(getTranslation("systemOverviewView.grid.purpose")).setFlexGrow(1);
        config.addComponentColumn(s -> SystemBadges.presence(s.present())).setHeader(getTranslation("system.grid.state")).setAutoWidth(true);
        config.setAllRowsVisible(true);
        config.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_WRAP_CELL_CONTENT);
        add(config);
    }

    private CheckResult brandSummary() {
        try {
            return CheckResult.up("Branding", brand.report().summary(), 0); // i18n:ignore
        } catch (RuntimeException e) {
            return CheckResult.down("Branding", e.getMessage(), 0); // i18n:ignore
        }
    }

    private CheckResult connectionsSummary() {
        int count = connections.targets().size();
        return CheckResult.unknown("Connections", getTranslation(count == 1 // i18n:ignore
                ? "systemOverviewView.connectionsSummary.one" : "systemOverviewView.connectionsSummary.many",
                Integer.toString(count)));
    }

    private Grid<Row> rows(String id, List<Row> rows) {
        Grid<Row> grid = new Grid<>();
        grid.setId(id);
        grid.setItems(rows);
        grid.addColumn(Row::label).setHeader(getTranslation("system.grid.item")).setAutoWidth(true);
        grid.addColumn(Row::value).setHeader(getTranslation("system.grid.value")).setFlexGrow(1);
        grid.setAllRowsVisible(true);
        grid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_WRAP_CELL_CONTENT);
        return grid;
    }

    private Grid<Area> areas(List<Area> areas) {
        Grid<Area> grid = new Grid<>();
        grid.setId(HEALTH_GRID_ID);
        grid.setItems(areas);
        grid.addComponentColumn(a -> new RouterLink(a.name(), a.screen())).setHeader(getTranslation("systemOverviewView.grid.area")).setAutoWidth(true);
        grid.addComponentColumn(a -> SystemBadges.status(a.result())).setHeader(getTranslation("system.grid.state")).setAutoWidth(true);
        grid.addColumn(a -> a.result().detail()).setHeader(getTranslation("system.grid.detail")).setFlexGrow(1);
        grid.setAllRowsVisible(true);
        grid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_WRAP_CELL_CONTENT);
        return grid;
    }

    String formatUptime(Duration uptime) {
        String days = Long.toString(uptime.toDays());
        String hours = Long.toString(uptime.toHoursPart());
        String minutes = Long.toString(uptime.toMinutesPart());
        if (uptime.toDays() > 0) {
            return getTranslation("systemOverviewView.uptime.days", days, hours, minutes);
        }
        if (uptime.toHoursPart() > 0) {
            return getTranslation("systemOverviewView.uptime.hours", hours, minutes);
        }
        return getTranslation("systemOverviewView.uptime.minutes", minutes, Integer.toString(uptime.toSecondsPart()));
    }

    @Override
    public String getPageTitle() {
        return getTranslation("systemOverviewView.pageTitle");
    }
}
