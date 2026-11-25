package com.elicitsoftware.author.diagnostics;

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

import com.elicitsoftware.test.PostgresTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The connection is probed and the survey schema state is read.
 *
 * <p>Traceability: UC-035 (Diagnose Database), A2.</p>
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class DatabaseDiagnosticsTest {

    @Inject
    DatabaseDiagnostics database;

    /** UC-035 step 2: the connection answers with the server version and the connected user. */
    @Test
    void probesTheConnection() {
        DatabaseDiagnostics.ConnectionReport report = database.checkConnection();

        assertTrue(report.result().isUp(), report.result().detail());
        assertNotNull(report.connectedAs());
        assertNotNull(report.database());
        assertTrue(report.result().detail().startsWith("PostgreSQL"), "the detail carries the server version");
    }

    /** UC-035 steps 3 to 5 and A2: the schema mirror is present; the history table may not be. */
    @Test
    void readsSchemaStateWithoutThrowing() {
        assertTrue(database.checkSchema().isUp(), database.checkSchema().detail());
        DatabaseDiagnostics.MigrationReport migration = database.surveyMigration();
        assertNotNull(migration);
        assertNotNull(database.checkDurableSequences().detail());
        assertTrue(database.surveyCount() >= 0);
    }
}
