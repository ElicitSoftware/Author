# Elicit Author as a Desktop Tool

**Status:** research / proposal, 2026-09-19. Nothing here is implemented.

Source: `pom.xml`, `src/main/resources/application.properties`,
`src/test/resources/application.properties`, the schema mirror under
`src/test/resources/db/schema-mirror`, `docs/vision.md`, `docs/requirements.md`,
and `docs/entity_model.md`.

## Question

Can Author be delivered as a self-contained desktop application with an
embedded H2 database, rather than a Quarkus service that needs PostgreSQL and
Keycloak?

## Short Answer

Yes. Author is the right module for it. It is a single-user working-copy tool
whose only output the rest of the platform consumes is the `.elicit` export
file (see `docs/requirements.md`, lifecycle requirements). A desktop edition
therefore never needs to reach a server database, Keycloak, or any other
Elicit service. Three things stand in the way, none of which is a redesign:

1. Author has no schema of its own; it maps onto Survey's PostgreSQL-only
   `survey` schema. A desktop edition needs a small portable Flyway set.
2. Datasource kind and OIDC are build-time choices in Quarkus, so the desktop
   edition is a separate Maven profile, not a runtime switch.
3. `jpackage` cannot cross-build, so installers are produced per platform,
   and macOS and Windows installers must be signed to open without warnings.

## Why Author Fits

- Author edits working copies in its own database and publishes by exporting
  a `.elicit` file that Admin installs at each site. The server-side database
  is an implementation detail, not a shared resource.
- The entity set is small: 15 tables (`surveys`, `steps`, `sections`,
  `questions`, `steps_sections`, `sections_questions`, `select_groups`,
  `select_items`, `relationships`, `metadata`, `ontology`, `dimensions`,
  `question_types`, `action_types`, `operator_types`).
- No entity carries an owner or "created by" column, so there is no user
  identity to fabricate for the data itself.
- Element keys are generated in Java with `UUID.randomUUID()`
  (`SurveyService`, `ElementService`, `ElicitFormat`), not by the database.

## 1. Database: H2 Replaces PostgreSQL

### What Author depends on today

Author ships no Flyway migrations. Tests replay a verbatim mirror of Survey's
migrations (V001 through V015) into a Testcontainers PostgreSQL, and
production expects the `survey` schema to already exist. Those scripts use
constructs H2 does not support:

| Construct | Count in mirror | Needed by Author? |
| --- | --- | --- |
| `GRANT` to `survey_user` / `surveyadmin_user` / `surveyreport_user` | 73 | No. Single local user. |
| `CREATE TRIGGER ... scd_close_predecessor` (plpgsql) | 8, on the authoring tables | No, see below. |
| `insert_fact_respondent` / `update_fact_respondent` triggers | 2, on `respondents` | No. Respondent tables are not Author's. |
| `gen_random_uuid()` column defaults | 5 | No. Keys are set in Java. |
| `::numeric`, `::timestamptz` casts | 8 | Only inside the trigger bodies. |
| Reporting schema, dimensional tables, views, post-survey actions | V002, V004, V009 | No. |

**The SCD trigger is a no-op in Author.** `survey.scd_close_predecessor` only
acts when a newly inserted row shares its durable id with an existing current
row. Per `docs/entity_model.md`, in Author every element has exactly one row
(`version = 0`, epoch `effective_from`, sentinel `effective_to`); version rows
are created only by Admin's update service at deployed sites. The trigger can
therefore be dropped from the desktop schema without changing behavior.

### What H2 handles unchanged

Running H2 with `MODE=PostgreSQL` covers the remaining PostgreSQL-flavoured
SQL Author actually issues:

- The `survey` schema name on every `@Table` and `@SequenceGenerator`.
- One sequence per table, `allocationSize = 1`.
- `nextval('survey.surveys_seq')` and `nextval('survey.select_groups_seq')`
  as native queries in `SurveyDefinitionImporter`.
- `now()`, `uuid`, `timestamp with time zone`, `boolean`.
- The native row queries in `SurveyDefinitionExporter.rows(...)` (plain
  `SELECT ... WHERE ... = :sid`).

Each of these must be confirmed by running the existing Author test suite
against H2, which is the first concrete task.

### Proposed approach

Add a desktop-only Flyway location, `db/desktop`, holding a portable DDL for
the 15 authoring tables, their sequences, foreign keys, unique constraints on
element keys, and the reference data that V003 populates (`question_types`,
`action_types`, `operator_types`, base `ontology` / `dimensions` rows). Keep
column names and types identical to Survey's so `SurveyDefinitionExporter`
and `SurveyDefinitionImporter` need no change and the exported `.elicit`
file is byte-for-byte what the server edition would produce.

Suggested datasource settings for the desktop profile:

```
quarkus.datasource.db-kind=h2
quarkus.datasource.jdbc.url=jdbc:h2:file:${user.home}/.elicit-author/author;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH
quarkus.flyway.locations=db/desktop
quarkus.flyway.schemas=survey
quarkus.flyway.migrate-at-start=true
quarkus.hibernate-orm.schema-management.strategy=validate
```

`validate` keeps the desktop DDL honest against the entity mappings, exactly
as the test profile does today.

### Alternative: embedded PostgreSQL

The `zonky.io` embedded-postgres library bundles real PostgreSQL binaries per
platform and starts them in-process. It would let the desktop edition reuse
Survey's migrations verbatim with zero SQL rewrite. Costs: roughly 30 to
50 MB of binaries per platform, a one to two second startup delay, and a
process the launcher must clean up on crash. H2 is the better default for a
single-file, pure-Java tool. Embedded PostgreSQL is the fallback if the
portable DDL turns up more PostgreSQL dependence than the table above
suggests.

### One database or one file per survey?

Two product shapes are possible and the choice affects the installer:

- **Application database.** One H2 file under the user's home directory holds
  all surveys. The `SurveysView` list is the entry point, as today. Simplest;
  recommended first.
- **Document model.** Each survey project is its own H2 file (or the `.elicit`
  file itself is the save format and H2 is only a working cache). Author gains
  open / save / save-as semantics and the installer registers a file extension
  so double-clicking a project opens Author. More work; revisit once the
  application-database edition exists.

## 2. Build Profile: Datasource and OIDC Are Build-Time

Quarkus fixes `quarkus.datasource.db-kind` and the presence of the OIDC
extension at build time. The desktop edition is therefore a Maven profile
(`desktop`) layered on the existing Vaadin `production` profile that:

- Swaps `quarkus-jdbc-postgresql` and `quarkus-flyway-postgresql` for
  `quarkus-jdbc-h2`.
- Removes `quarkus-oidc`, `quarkus-keycloak-authorization`,
  `quarkus-opentelemetry`, `quarkus-micrometer-registry-prometheus`,
  `quarkus-container-image-docker`, and `quarkus-logging-json`.
- Sets `quarkus.package.jar.type=uber-jar` so `jpackage` has one artifact.
- Binds HTTP to loopback: `quarkus.http.host=127.0.0.1`, fixed port with a
  fallback if busy.
- Points `brand.file.system.path` at a brand directory inside the bundle.

### No Keycloak, but the code still expects a user

Keycloak answers two questions on every request: who is this, and do they
hold `elicit_author`. On a desktop the answer is always "the person at the
keyboard, yes". What has to exist in the desktop profile:

- **A stand-in `SecurityIdentity`.** `CurrentUser`, the views'
  `@RolesAllowed`, and `NavigationControlAccessCheckerInitializer` all read
  `SecurityIdentity`. A small `HttpAuthenticationMechanism` that authenticates
  every loopback request as a fixed local principal carrying `elicit_author`
  keeps every view and service unchanged. `RoleSecurityIdentityAugmentor`,
  `UnauthorizedView`, and `AccessDeniedErrorView` become dead code in this
  profile and do no harm.
- **Hide logout.** `MainLayout` adds a `Logout` nav item pointing at
  `LogoutView`, and the OIDC `/logout` and `/callback` paths are configured
  in `application.properties`. Both should be absent in the desktop build
  rather than left pointing at a Keycloak that is not there.
- **No ownership fields to fill.** None of the 15 entities records a creating
  or editing user, so no substitute identity needs to be persisted.

The only credential-like thing left is the H2 file. It can stay unencrypted,
or use H2's built-in file password if the surveys being authored are
sensitive.

## 3. Compilation and Delivery

### Compile

```
./mvnw -Pproduction,desktop clean package
jpackage --input target --main-jar author-desktop-runner.jar \
  --name "Elicit Author" --app-version 3.0.0 \
  --type dmg   # or msi / deb / rpm
```

Vaadin's production build bundles the frontend into the jar; Quarkus's
uber-jar gives a single runnable artifact; `jpackage` (shipped with JDK 25)
runs `jlink` under the hood so the installer carries a trimmed Java runtime
and the user installs nothing else. Expect an installer of roughly 100 MB.

GraalVM native is not recommended: a JVM Quarkus app opens in a second or
two, and native adds a fragile build step for no user-visible gain.

### Launcher

A thin `main` (or a Quarkus `@QuarkusMain`) that:

1. Starts Quarkus on the loopback port.
2. Opens the default browser on `http://127.0.0.1:<port>/` via
   `java.awt.Desktop.browse` (falling back to `open` / `xdg-open` /
   `rundll32` when headless).
3. Provides a way to quit: a system tray icon or a "Quit" nav item that calls
   `Quarkus.asyncExit()`.

H2's file lock already prevents a second instance from opening the same
database; the launcher should detect that and focus the browser tab instead of
failing. Wrapping the UI in an embedded WebView window is possible later but
is not required; JavaFX's WebKit is a poor fit for Vaadin 25 web components.

### Per-platform builds

`jpackage` cannot cross-build: a `.dmg` is produced on macOS, an `.msi` on
Windows, a `.deb` / `.rpm` on Linux. Proposed pipeline: a GitHub Actions
release workflow in the Author repo, triggered by a version tag, with a
three-runner matrix (`macos-latest`, `windows-latest`, `ubuntu-latest`). Each
runner builds, packages, and uploads its installer as a GitHub Release asset.

Hydraulic Conveyor packages all three targets from one machine and adds
auto-update, but is a paid product for closed-source software. Worth knowing
if the matrix becomes a burden.

### Signing

| Platform | Requirement | Without it |
| --- | --- | --- |
| macOS | Apple Developer ID certificate plus notarization | Gatekeeper blocks the app; most users have no override. |
| Windows | Authenticode code-signing certificate | SmartScreen warning on every install; users can click through. |
| Linux | None | n/a |

Both certificates are a one-time organizational setup and become two secrets
in the release workflow.

### Delivery

GitHub Releases on the Author repo, installers as release assets, and a short
download page linking the latest tag. No hosting required.

### Upgrades

The H2 file lives under the user's home directory, not inside the app bundle,
and the desktop Flyway migrations run at startup, so installing a newer
version over the old one migrates existing surveys on first launch.
`jpackage` has no built-in updater. Minimum viable: a startup check against
the GitHub Releases API that shows a "new version available" link. A real
auto-updater is a later step (Conveyor is the shortcut if wanted).

## Open Decisions

- Application database vs document model (see section 1).
- App name, bundle identifier, and vendor string for `jpackage` and Apple
  notarization; these are hard to change after the first release.
- Whether the desktop build lives in this repo's `pom.xml` as a profile or as
  a separate `desktop/` module that depends on the Author jar. A profile is
  less ceremony; a module keeps the server `pom.xml` untouched.
- Whether to encrypt the H2 file.

## Proposed Order of Work

1. Write `db/desktop` migrations for the 15 authoring tables and reference
   data; add an H2 test profile and get the existing Author test suite green
   against it. This is the largest and most uncertain step.
2. Add the `desktop` Maven profile: H2 datasource, loopback binding, extension
   swaps, loopback authentication mechanism, hidden logout.
3. Add the launcher and a local `jpackage` build; verify on macOS first.
4. Add the GitHub Actions matrix, signing secrets, and Release upload.
5. Add the startup version check.
6. Revisit the document model and file-association question.
