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
        add(new H3("Database"));
        add(new Paragraph("Author owns no migrations. The survey schema in its database is created by the "
                + "Survey preview instance that shares the database, so a missing schema means that instance "
                + "has not started against this database yet."));

        add(new H4("Connection"));
        DatabaseDiagnostics.ConnectionReport connection = database.checkConnection();
        Grid<Row> connectionGrid = new Grid<>();
        connectionGrid.setId(CONNECTION_GRID_ID);
        connectionGrid.setItems(List.of(
                new Row("State", connection.result().isUp() ? "OK" : "Failed"),
                new Row("Configured user", connection.configuredUser()),
                new Row("Connected as", valueOrDash(connection.connectedAs())),
                new Row("Database", valueOrDash(connection.database())),
                new Row("Server", connection.result().detail()),
                new Row("Round trip", connection.result().durationMs() + " ms")));
        connectionGrid.addColumn(Row::label).setHeader("Item").setAutoWidth(true);
        connectionGrid.addColumn(Row::value).setHeader("Value").setFlexGrow(1);
        connectionGrid.setAllRowsVisible(true);
        connectionGrid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_WRAP_CELL_CONTENT);
        add(connectionGrid);

        add(new H4("Survey schema"));
        DatabaseDiagnostics.MigrationReport migration = database.surveyMigration();
        Grid<Row> schemaGrid = new Grid<>();
        schemaGrid.setId(SCHEMA_GRID_ID);
        schemaGrid.setItems(List.of(
                new Row("Schema present", database.checkSchema().detail()),
                new Row("Survey migration (survey.flyway_history)", !migration.installed() ? "not installed"
                        : migration.version() == null ? valueOrDash(migration.installedOn()).equals("-") ? "empty"
                        : migration.installedOn() : migration.version()
                        + (migration.success() ? " applied " : " FAILED ") + valueOrDash(migration.installedOn())),
                new Row("Kimball durable-key sequences", database.checkDurableSequences().detail()),
                new Row("Surveys authored", Long.toString(database.surveyCount()))));
        schemaGrid.addColumn(Row::label).setHeader("Item").setAutoWidth(true);
        schemaGrid.addColumn(Row::value).setHeader("Value").setFlexGrow(1);
        schemaGrid.setAllRowsVisible(true);
        schemaGrid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_WRAP_CELL_CONTENT);
        add(schemaGrid);
    }

    private static String valueOrDash(String value) {
        return value != null ? value : "-";
    }

    @Override
    public String getPageTitle() {
        return "System Database";
    }
}
