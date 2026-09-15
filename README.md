# Elicit Author

Copyright © 2026 The Regents of the University of Michigan

## About

Elicit Author is the survey **authoring** module of the
[Elicit Software](https://github.com/ElicitSoftware/) platform. It lets researchers,
educators, and other survey designers build survey structure (steps, sections, questions),
branching logic, dynamic tokens, and dimensional/ontology metadata through a visual interface.

Structural survey elements (steps, sections, questions, select groups/items, relationships)
are versioned in the shared schema using a Kimball Type 2 pattern: authors edit a **draft**,
and **publishing** closes the current version and opens a new one under the same durable key
— it does not overwrite what past respondents already saw. "Publish" here is a real state
transition (`is_draft` → published, a new `version` row), not just "save to the database." See
[`docs/scd-contract.md`](docs/scd-contract.md) for the contract and
[`docs/vision.md`](docs/vision.md) for the full mission, scope, and constraints that drive this
project's [AIUP](https://unifiedprocess.ai/) artifacts under [`docs/`](docs/).

Author has no database migrations of its own — it reads and writes the schema that Survey's
`db/migration` scripts own.

Authentication and authorization are controlled by an OpenID Connect (OIDC) provider — Keycloak
for local development and testing, though any OIDC-compliant server works in production.

## Stack

- Java 25, Quarkus 3.39.2, Maven build
- Vaadin 25.2.6 (Flow, server-side UI)
- Hibernate ORM with Panache (JPA) against the shared `survey` database
- PostgreSQL
- OIDC / Keycloak authorization
- OpenTelemetry tracing, SmallRye Health, Micrometer/Prometheus metrics
- Docker for deployment

## Getting Started

### Prerequisites

- Java 25
- A running Postgres instance with the Survey schema, and a running Keycloak (or other OIDC
  provider) — see the [Survey](https://github.com/ElicitSoftware/) and
  [FHHS](https://github.com/ElicitSoftware/FHHS) repos for the shared platform's docker-compose
  setup.

### Running in dev mode

```shell script
./mvnw quarkus:dev
```

Dev mode listens on port `8082` (Survey defaults to `8080`, Admin's dev profile uses `8081`) so
all three apps can run side by side. The Quarkus Dev UI is available at
<http://localhost:8082/q/dev/>.

### Packaging

```shell script
./mvnw package
```

Produces `target/quarkus-app/quarkus-run.jar`, runnable with
`java -jar target/quarkus-app/quarkus-run.jar`.

### Building the Docker image

```shell script
./buildDockerImage.sh
```

## Configuration

Key environment variables (see `src/main/resources/application.properties`):

| Variable | Purpose |
|---|---|
| `ELICIT_DB_PASSWORD` | Password for the `author_user` Postgres role |
| `OIDC_CLIENT_SECRET` | Client secret for the `elicit-author` OIDC client |
| `CORS_ALLOWED_ORIGINS` | Allowed CORS origins, if this app is called cross-origin |
| `LOG_LEVEL` / `ELICIT_LOG_LEVEL` | Root / `com.elicitsoftware.*` log levels |

## Status

This module is at the scaffolding stage: it builds, boots, and is wired into the shared
platform's database and OIDC conventions, but does not yet contain authoring domain code
(entities, views). That follows the AIUP workflow described in [`CLAUDE.md`](CLAUDE.md) —
`/requirements` → `/entity-model` → `/use-case-spec` — driven from
[`docs/vision.md`](docs/vision.md).

## Related Guides

- [REST](https://quarkus.io/guides/rest): Build RESTful web services and APIs using Jakarta REST
- [Keycloak Authorization](https://quarkus.io/guides/security-keycloak-authorization): Policy enforcer using Keycloak-managed permissions
- [OpenID Connect](https://quarkus.io/guides/security-openid-connect): Secure applications with OIDC / OAuth 2.0
- [Hibernate ORM with Panache](https://quarkus.io/guides/hibernate-orm-panache): Simplified JPA/Hibernate data access
- [Hibernate Validator](https://quarkus.io/guides/validation): Bean validation using Jakarta Validation annotations
- [OpenTelemetry](https://quarkus.io/guides/opentelemetry): Trace services
- [SmallRye Health](https://quarkus.io/guides/smallrye-health): Monitor service health
- [Micrometer Registry Prometheus](https://quarkus.io/guides/micrometer): Prometheus metrics
- [Logging JSON](https://quarkus.io/guides/logging#json-logging): JSON console log formatting
- [JDBC Driver - PostgreSQL](https://quarkus.io/guides/datasource): Connect to PostgreSQL via JDBC

## License

[PolyForm Noncommercial License 1.0.0](https://polyformproject.org/licenses/noncommercial/1.0.0)
