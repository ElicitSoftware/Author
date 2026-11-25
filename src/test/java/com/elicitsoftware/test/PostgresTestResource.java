package com.elicitsoftware.test;

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

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.Map;

/**
 * Starts one throwaway PostgreSQL container for the whole test run and points the default
 * datasource at it.
 *
 * <p>The container runs {@code db/testcontainers-init.sql} as the {@code postgres} superuser
 * (roles, schemas, schema-level USAGE), then Flyway replays the mirrored Survey migrations
 * and Author's fixtures. The application datasource connects as the superuser so Flyway can
 * DDL and so tests never fail on privilege gaps; privilege regressions are Survey's concern.</p>
 */
public class PostgresTestResource implements QuarkusTestResourceLifecycleManager {

    private PostgreSQLContainer container;

    @Override
    public Map<String, String> start() {
        container = new PostgreSQLContainer("postgres:18")
                .withDatabaseName("survey")
                .withUsername("postgres")
                .withPassword("postgres")
                .withInitScript("db/testcontainers-init.sql");
        container.start();
        return Map.of(
                "quarkus.datasource.jdbc.url", container.getJdbcUrl(),
                "quarkus.datasource.username", container.getUsername(),
                "quarkus.datasource.password", container.getPassword());
    }

    @Override
    public void stop() {
        if (container != null) {
            container.stop();
        }
    }
}
