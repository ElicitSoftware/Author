package com.elicitsoftware.author.diagnostics;

/*-
 * ***LICENSE_START***
 * Elicit Author
 * %%
 * Copyright (C) 2025 - 2026 The Regents of the University of Michigan - Rogel Cancer Center
 * Copyright (C) 2026 Elicit Software LLC
 * %%
 * PolyForm Noncommercial License 1.0.0
 * <https://polyformproject.org/licenses/noncommercial/1.0.0>
 * ***LICENSE_END***
 */

import com.elicitsoftware.author.i18n.Translations;
import com.elicitsoftware.model.Survey;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Probes Author's database connection and reads the state of the {@code survey} schema it
 * depends on (UC-035).
 * <p>
 * Author owns no migrations (C-005): the schema in its database is created by the Survey
 * module running as the preview instance. So the questions here are whether the connection
 * works, whether that schema is present, and which Survey migration version created it.
 */
@ApplicationScoped
public class DatabaseDiagnostics {

    /** The Kimball durable-key sequence the V3 schema carries. */
    static final String DURABLE_SEQUENCE = "questions_durable_seq";

    /** The Survey module's Flyway history table, in the {@code survey} schema. */
    static final String SURVEY_HISTORY_TABLE = "flyway_history";

    /**
     * Outcome of probing the connection.
     *
     * @param configuredUser the username the configuration names
     * @param result         the probe outcome; the detail carries the server version when up
     * @param connectedAs    the user the server reports, or null when the probe failed
     * @param database       the database the server reports, or null when the probe failed
     */
    public record ConnectionReport(String configuredUser, CheckResult result, String connectedAs, String database) {
    }

    /**
     * The Survey module's latest migration in this database.
     *
     * @param version     the latest version applied, or null when the table is absent
     * @param installedOn when it was applied, as the server formats it, or null
     * @param success     whether that migration succeeded
     * @param installed   whether the history table exists at all (UC-035 A2)
     */
    public record MigrationReport(String version, String installedOn, boolean success, boolean installed) {
    }

    @Inject
    DataSource dataSource;

    @ConfigProperty(name = "quarkus.datasource.username", defaultValue = "")
    String configuredUser;

    public DatabaseDiagnostics() {
        // CDI managed bean
    }

    public ConnectionReport checkConnection() {
        long start = System.nanoTime();
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT current_user, current_database(), version()")) {
            rs.next();
            long ms = (System.nanoTime() - start) / 1_000_000;
            return new ConnectionReport(configuredUser, CheckResult.up("database connection", rs.getString(3), ms),
                    rs.getString(1), rs.getString(2));
        } catch (SQLException | RuntimeException e) {
            long ms = (System.nanoTime() - start) / 1_000_000;
            return new ConnectionReport(configuredUser, CheckResult.down("database connection", e.getMessage(), ms),
                    null, null);
        }
    }

    /** Whether the {@code survey} schema and its central table exist (UC-035 step 3). */
    public CheckResult checkSchema() {
        long start = System.nanoTime();
        try (Connection connection = dataSource.getConnection()) {
            if (!tableExists(connection, "surveys")) {
                return CheckResult.down("survey schema", Translations.get("systemDatabaseView.schema.missing"), elapsed(start));
            }
            return CheckResult.up("survey schema", Translations.get("systemDatabaseView.schema.exists"), elapsed(start));
        } catch (SQLException | RuntimeException e) {
            return CheckResult.down("survey schema", e.getMessage(), elapsed(start));
        }
    }

    /** The Survey module's latest migration, from its history table in this database. */
    public MigrationReport surveyMigration() {
        try (Connection connection = dataSource.getConnection()) {
            if (!tableExists(connection, SURVEY_HISTORY_TABLE)) {
                return new MigrationReport(null, null, false, false);
            }
            String sql = "SELECT version, installed_on::text, success FROM survey." + SURVEY_HISTORY_TABLE
                    + " WHERE version IS NOT NULL ORDER BY installed_rank DESC LIMIT 1";
            try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(sql)) {
                if (rs.next()) {
                    return new MigrationReport(rs.getString(1), rs.getString(2), rs.getBoolean(3), true);
                }
                return new MigrationReport(null, null, false, true);
            }
        } catch (SQLException | RuntimeException e) {
            // Typically "permission denied": the table is there but Author's user may not read it.
            return new MigrationReport(null, Translations.get("systemDatabaseView.migration.unreadable", configuredUser,
                    String.valueOf(e.getMessage())), false, true);
        }
    }

    /** Whether the Kimball durable-key sequences exist (UC-035 step 4). */
    public CheckResult checkDurableSequences() {
        long start = System.nanoTime();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT 1 FROM pg_catalog.pg_class c JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace"
                             + " WHERE n.nspname = 'survey' AND c.relkind = 'S' AND c.relname = ?")) {
            statement.setString(1, DURABLE_SEQUENCE);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return CheckResult.up("Kimball durable-key sequences",
                            Translations.get("systemDatabaseView.durableSequences.exists", DURABLE_SEQUENCE), elapsed(start));
                }
                return CheckResult.down("Kimball durable-key sequences",
                        Translations.get("systemDatabaseView.durableSequences.missing", DURABLE_SEQUENCE), elapsed(start));
            }
        } catch (SQLException | RuntimeException e) {
            return CheckResult.down("Kimball durable-key sequences", e.getMessage(), elapsed(start));
        }
    }

    @Transactional
    public long surveyCount() {
        return Survey.count();
    }

    /**
     * Existence check through {@code pg_catalog}, not {@code information_schema}: the latter hides
     * every table the connected user has no privilege on, and Author's user is not granted the
     * Flyway history table, which would make an installed schema look absent.
     */
    private static boolean tableExists(Connection connection, String table) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM pg_catalog.pg_tables WHERE schemaname = 'survey' AND tablename = ?")) {
            statement.setString(1, table);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static long elapsed(long start) {
        return (System.nanoTime() - start) / 1_000_000;
    }
}
