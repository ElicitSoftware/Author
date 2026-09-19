# Elicit Author

Copyright © 2026 The Regents of the University of Michigan

## About

Elicit Author is the survey **authoring** module of the
[Elicit Software](https://github.com/ElicitSoftware/) platform. It lets researchers,
educators, and other survey designers build survey structure (steps, sections, questions),
branching logic, dynamic tokens, and dimensional/ontology metadata through a visual interface.

Authors work on **plain working copies** in Author's own database and **export** a survey as an
`.elicit` definition file (`ELICIT_SURVEY_EXPORT_V1`). A deployed site installs or updates that
file through the Admin app's *Apply Survey Definition*: the first apply installs it, later
applies update it in place, matched by the UUID element keys every survey element carries and
versioned Kimball Type 2 style at the site so past respondents keep what they started with.
Removing an element in Author retires it (closes its validity window) rather than deleting it, so
recorded answers at every site survive. See [`docs/scd-contract.md`](docs/scd-contract.md) for the
contract and [`docs/vision.md`](docs/vision.md) for the mission, scope, and constraints that drive
this project's [AIUP](https://unifiedprocess.ai/) artifacts under [`docs/`](docs/).

Author has no production database migrations of its own — its local database carries the `survey`
schema that Survey's `db/migration` scripts create. `samples/ElicitDesigner.elicit` is an example of
the file Author produces.

Authentication and authorization are controlled by an OpenID Connect (OIDC) provider — Keycloak
for local development and testing, though any OIDC-compliant server works in production.

## Stack

- Java 25, Quarkus 3.39.2, Maven build
- Vaadin 25.2.7 (Flow, server-side UI)
- Hibernate ORM with Panache (JPA) against a local `survey` database
- PostgreSQL
- OIDC / Keycloak authorization
- OpenTelemetry tracing, SmallRye Health, Micrometer/Prometheus metrics
- Docker for deployment

## Getting Started

### Prerequisites

- Java 25
- Docker. The umbrella repo's `docker-compose.yml` (one level up) runs the support services
  Author needs: Postgres with a dedicated `author` database next to the site's `survey`
  database, Keycloak (realm from `../keycloak/elicit-realm.json`, test user `author` /
  `author`), and the `author-survey` preview instance. Author has no compose file of its own.

### Running in dev mode

```shell script
./mvnw quarkus:dev
```

Dev mode listens on port `8084` (Survey `8080`, Admin `8081`, FHHS `8082`, Pedigree `8083` in the
umbrella stack) so the apps can run side by side. The Quarkus Dev UI is available at
<http://localhost:8084/q/dev/>.

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
| `ELICIT_DB_PASSWORD` | Password for the `survey_user` Postgres role |
| `OIDC_CLIENT_SECRET` | Client secret for the `elicit-author` OIDC client |
| `CORS_ALLOWED_ORIGINS` | Allowed CORS origins, if this app is called cross-origin |
| `LOG_LEVEL` / `ELICIT_LOG_LEVEL` | Root / `com.elicitsoftware.*` log levels |

## Status

Milestone 1 delivered the branded OIDC shell, entities over the `survey` schema, survey
list/create/edit, `.elicit` import and export with validation, and a structure navigation tree.
Milestone 2 adds the **designer** (`survey/{id}/design`): a flow-chart board where steps are lanes,
sections are collapsible cards and questions are rows, each with a glyph that is its drag handle —
drag from the palette to create, within a container to reorder, into another container to copy;
every drag has a menu equivalent. A guide page (`guide`, "How surveys work") explains the model.
Rules are drawn as arrows between elements, and the survey's **Reporting** page
(`survey/{id}/reporting`) manages the reporting tags, dimensions and assignments that drive the
site's dimensional reporting schema, with a preview of the tables and columns they produce; the
same tags are reachable from a question's or section's menu on the board. Work follows the AIUP workflow described
in [`CLAUDE.md`](CLAUDE.md); use case specs live in [`docs/use_cases/`](docs/use_cases/).

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
