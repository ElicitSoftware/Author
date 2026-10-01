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

import com.elicitsoftware.author.diagnostics.DatabaseDiagnostics;
import com.elicitsoftware.author.security.AuthorRoles;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;

import java.util.List;

/**
 * Database diagnostics (UC-035): the connection, the presence of the {@code survey} schema
 * Author depends on but never creates (C-005), the Survey migration that created it, and what
 * is stored. Reads only.
 */
@Route(value = "system/database", layout = MainLayout.class)
@RolesAllowed(AuthorRoles.ADMIN)
public class SystemDatabaseView extends VerticalLayout implements HasDynamicTitle {

    static final String CONNECTION_GRID_ID = "system-db-connection";
    static final String SCHEMA_GRID_ID = "system-db-schema";

    record Row(String label, String value) {
    }

    @Inject
    DatabaseDiagnostics database;

    public SystemDatabaseView() {
        setWidthFull();
        setPadding(true);
    }

    @PostConstruct
    void init() {
        add(new H3(getTranslation("systemDatabaseView.title")));
        add(new Paragraph(getTranslation("systemDatabaseView.intro")));

        add(new H4(getTranslation("systemDatabaseView.connection")));
        DatabaseDiagnostics.ConnectionReport connection = database.checkConnection();
        Grid<Row> connectionGrid = new Grid<>();
        connectionGrid.setId(CONNECTION_GRID_ID);
        connectionGrid.setItems(List.of(
                new Row(getTranslation("systemDatabaseView.row.state"), getTranslation(connection.result().isUp() ? "system.badge.ok" : "system.badge.failed")),
                new Row(getTranslation("systemDatabaseView.row.configuredUser"), connection.configuredUser()),
                new Row(getTranslation("systemDatabaseView.row.connectedAs"), valueOrDash(connection.connectedAs())),
                new Row(getTranslation("systemDatabaseView.row.database"), valueOrDash(connection.database())),
                new Row(getTranslation("systemDatabaseView.row.server"), connection.result().detail()),
                new Row(getTranslation("systemDatabaseView.row.roundTrip"), SystemBadges.millis(connection.result().durationMs()))));
        connectionGrid.addColumn(Row::label).setHeader(getTranslation("system.grid.item")).setAutoWidth(true);
        connectionGrid.addColumn(Row::value).setHeader(getTranslation("system.grid.value")).setFlexGrow(1);
        connectionGrid.setAllRowsVisible(true);
        connectionGrid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_WRAP_CELL_CONTENT);
        add(connectionGrid);

        add(new H4(getTranslation("systemDatabaseView.surveySchema")));
        DatabaseDiagnostics.MigrationReport migration = database.surveyMigration();
        Grid<Row> schemaGrid = new Grid<>();
        schemaGrid.setId(SCHEMA_GRID_ID);
        schemaGrid.setItems(List.of(
                new Row(getTranslation("systemDatabaseView.row.schemaPresent"), database.checkSchema().detail()),
                new Row(getTranslation("systemDatabaseView.row.surveyMigration"), migrationState(migration)),
                new Row(getTranslation("systemDatabaseView.row.durableSequences"), database.checkDurableSequences().detail()),
                new Row(getTranslation("systemDatabaseView.row.surveysAuthored"), Long.toString(database.surveyCount()))));
        schemaGrid.addColumn(Row::label).setHeader(getTranslation("system.grid.item")).setAutoWidth(true);
        schemaGrid.addColumn(Row::value).setHeader(getTranslation("system.grid.value")).setFlexGrow(1);
        schemaGrid.setAllRowsVisible(true);
        schemaGrid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_WRAP_CELL_CONTENT);
        add(schemaGrid);
    }

    /** The migration cell: not installed, empty, unreadable (carried in installedOn), or version plus outcome. */
    private String migrationState(DatabaseDiagnostics.MigrationReport migration) {
        if (!migration.installed()) {
            return getTranslation("systemDatabaseView.migration.notInstalled");
        }
        if (migration.version() == null) {
            return migration.installedOn() == null ? getTranslation("systemDatabaseView.migration.empty") : migration.installedOn();
        }
        return getTranslation(migration.success() ? "systemDatabaseView.migration.applied" : "systemDatabaseView.migration.failed",
                migration.version(), valueOrDash(migration.installedOn()));
    }

    private static String valueOrDash(String value) {
        return value != null ? value : "-";
    }

    @Override
    public String getPageTitle() {
        return getTranslation("systemDatabaseView.pageTitle");
    }
}
