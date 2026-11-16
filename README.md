# Elicit Author

Copyright © 2026 Elicit Software LLC

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

- Java 25, Quarkus 3.40.1 LTS, Maven build
- Vaadin 25.3.0 (Flow, server-side UI)
- Hibernate ORM with Panache (JPA) against a local `survey` database
- PostgreSQL
- OIDC / Keycloak authorization
- OpenTelemetry tracing, SmallRye Health, Micrometer/Prometheus metrics
- Docker for deployment

## Getting Started

### Prerequisites

- Java 25
- Docker, and **the Elicit umbrella checkout around this one**
  ([ElicitSoftware/Elicit](https://github.com/ElicitSoftware/Elicit)), whose
  `cloneAllProjects.sh` clones Author to `Elicit/Author/`. The umbrella holds everything Author
  shares with the platform: the stack (`docker-compose.yml`, with Postgres carrying a dedicated
  `author` database next to the site's `survey` database, Keycloak with the test user `author` /
  `author`, and the `author-survey` preview instance), the curated translations in
  `../elicit-i18n` (the image carries English only; the dev and test profiles read the same
  directory), the brand, the sample definitions and the end-to-end suites. All images, Author's
  included, come from the umbrella's `buildDockerImages.sh`; `./buildDockerImage.sh` here builds
  Author's alone.

### End-to-end tests

The platform's Playwright suites drive Author together with Admin and Survey, and live in the
umbrella (`../e2e-tests`, `../e2e_multisite`, `../e2e_multisite_multilingual`).

### Running in dev mode

Start what Author calls from the umbrella's stack, then Author itself:

```shell script
(cd .. && docker compose up -d db keycloak author-survey)
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

This also typesets the **author's manual** and packages it with the image; see
[`docs/manual/README.md`](docs/manual/README.md). It needs Docker (the manual is typeset
in a TeX Live container); `SKIP_MANUAL=1 ./buildDockerImage.sh` builds without it, and the
application then simply does not offer the link.

### The author's manual

`docs/manual/` holds *Building a Survey*, a LaTeX manual with step-by-step instructions and
screenshots, stamped with the version and build date of the image it ships in. Signed-in
authors open it from the **Manual** link in the header or **Manual (PDF)** in the navigation,
which serve it from `/api/manual`.

## Configuration

Key environment variables (see `src/main/resources/application.properties`):

| Variable | Purpose |
|---|---|
| `ELICIT_DB_PASSWORD` | Password for the `survey_user` Postgres role |
| `OIDC_CLIENT_SECRET` | Client secret for the `elicit-author` OIDC client |
| `ELICIT_SERVICE_TOKEN` | The deployment's service token, at least 32 characters; sent as a bearer credential on Author's call to the preview Survey, which is given the same value |
| `CORS_ALLOWED_ORIGINS` | Allowed CORS origins, if this app is called cross-origin |
| `LOG_LEVEL` / `ELICIT_LOG_LEVEL` | Root / `com.elicitsoftware.*` log levels |

## Status

Milestone 1 delivered the branded OIDC shell, entities over the `survey` schema, survey
list/create/edit, `.elicit` import and export with validation, and a structure navigation tree.
Milestone 2 adds the **designer** (`survey/{id}/design`): a flow-chart board where steps are lanes,
sections are collapsible cards and questions are rows, each with a glyph that is its drag handle —
drag from the palette to create, within a container to reorder, into another container to copy;
every drag has a menu equivalent. A guide page (`guide`, "How surveys work") explains the model.
Rules are drawn as arrows between elements, and the survey's **Analysis** page
(`survey/{id}/analysis`) manages the analysis tags, dimensions and assignments that drive the
site's star schema, with a preview of the tables and columns they produce; the same tags are
reachable from a question's or section's menu on the board. The **Reports** page
(`survey/{id}/reports`) lists the report services a survey offers its respondents — every new
survey starts with the built-in respondent report — and the report marks that say which questions
the respondent and administrator reports show. The printable
[author's manual](docs/manual/README.md) ships inside the image alongside that guide. A survey can
also start from an AI draft: [`docs/ai/AUTHORING_FROM_AI.md`](docs/ai/AUTHORING_FROM_AI.md)
explains the survey model and the `.elicit` record format to an AI agent, and Author offers that
same document on its **Draft with AI** page (`ai-draft`, beside **New survey** on the surveys
list), so an author with no checkout can copy it into an AI session and import the draft it
writes. Author sends nothing to any AI service — the author carries the specification out and the
draft back. Work follows the AIUP workflow described
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
