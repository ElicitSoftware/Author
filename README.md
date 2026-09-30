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

- Java 25, Quarkus 3.39.5, Maven build
- Vaadin 25.2.7 (Flow, server-side UI)
- Hibernate ORM with Panache (JPA) against a local `survey` database
- PostgreSQL
- OIDC / Keycloak authorization
- OpenTelemetry tracing, SmallRye Health, Micrometer/Prometheus metrics
- Docker for deployment

## Getting Started

### Prerequisites

- Java 25
- Docker. [`docker-compose.yml`](docker-compose.yml) in this repo runs the whole platform:
  Postgres with a dedicated `author` database next to the site's `survey` database, Keycloak
  (realm from [`keycloak/elicit-realm.json`](keycloak/elicit-realm.json), test user `author` /
  `author`), the `author-survey` preview instance, and Survey, Admin, FHHS and Pedigree so the
  end-to-end suites have a site to drive.
- **An Elicit umbrella checkout around this one.** Author is a private repository and is no
  longer cloned by the umbrella's `cloneAllProjects.sh`, but it is still expected to sit at
  `Elicit/Author/`. Shared assets are not duplicated here: the compose file mounts
  `../elicit-brand` and `../sftp_ssh`, and the end-to-end suites install
  `../../FHHS/family-history-survey.elicit`. The Survey, Admin, FHHS and Pedigree images come
  from the umbrella's `buildDockerImages.sh`; the Author image from `./buildDockerImage.sh`.

### End-to-end tests

Three Playwright suites live here and drive the stack above through a browser. They are
opt-in (`-DskipTests=false`) and need it running:

| Suite | What it covers |
| --- | --- |
| [`e2e-tests/`](e2e-tests) | Author → Admin → Survey on one site, plus the respondent journey, CSV registration and search filters |
| [`e2e_multisite/`](e2e_multisite) | Authoring at a master site and applying the definition at a second one |
| [`e2e_multisite_multilingual/`](e2e_multisite_multilingual) | The same across three sites in English, Spanish and Arabic |

`e2e_multisite` and `e2e_multisite_multilingual` compile the Author page objects out of
`e2e-tests/src/test/java`, so the three move together. Each has its own `run.sh`.

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
same tags are reachable from a question's or section's menu on the board. The printable
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
