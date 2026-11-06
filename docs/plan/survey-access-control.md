# Per-survey access control

Tracking issue: [#96](https://github.com/ElicitSoftware/Author/issues/96). Written 2026-11-06,
before any of the AIUP artifacts or code it names existed; the artifacts under `docs/` take
precedence over this document once they are written.

## Context

Today anyone holding `elicit_author` can open, edit, export and delete every survey in the
authoring database. There is no user table, no owner column and no check narrower than the
role (FR-033, UC-002 BR-001). The goal is to limit which users can alter which surveys, using
the same identity model as Admin (OIDC principal matched to a local user row, with an optional
local-table role fallback), plus a new `elicit_author_admin` role and screens to manage the
user/survey relationships.

Decisions already made:

1. **Author-owned schema.** Author gets its own Flyway migrations creating an `author` schema
   that exists only in the authoring database. C-005 is narrowed to "no migrations of the
   `survey` schema".
2. **Ungranted surveys are hidden**: absent from the list, URLs answer "not found". No
   read-only mode.
3. **`elicit_author_admin` is manage-only.** It opens the access screens and nothing else. To
   edit a survey an author admin also needs `elicit_author` and a grant, which they may give
   themselves.
4. **No backfill.** A new deployment has no surveys; whoever creates or imports one is given a
   grant row for it. Everything else is denied by default.

## Design

### 1. Schema and Flyway

New migration `src/main/resources/db/migration/V0.0.1__Create_Author_Schema.sql` (Flyway
creates the schema itself):

| Table | Columns | Notes |
| --- | --- | --- |
| `author.users` | `id`, `username` (unique), `first_name`, `last_name`, `email`, `active`, `created_at`, `last_seen_at` | `username` = OIDC principal name, as in Admin. Names nullable (some identity providers send none). |
| `author.user_roles` | `user_id`, `role_name` | PK on both; CHECK `role_name IN ('elicit_author','elicit_author_admin','elicit_admin')`. Used only in DATABASE mode. |
| `author.user_surveys` | `user_id`, `survey_key uuid`, `granted_by`, `granted_at` | PK `(user_id, survey_key)`; index on `survey_key`. |

- **Grants key on `survey_key`, with no foreign key to `survey.surveys`.** The key is the
  survey's durable identity and is never reused; a cross-schema FK would make Author's
  migration depend on the preview Survey having migrated first, and would force an ordering
  between two Flyway instances in tests. Access is resolved by a join, so a dangling grant is
  inert; `SurveyService.delete` removes a survey's grants in its own transaction.
- Grants `USAGE` on the schema and DML on the three tables to `survey_user`.

Configuration, mirroring Admin's `owner` block (`Admin/src/main/resources/application.properties`,
the `quarkus.datasource.owner.*` and `quarkus.flyway.owner.*` lines):

- `pom.xml`: move `quarkus-flyway` and `quarkus-flyway-postgresql` from test to compile scope;
  rewrite the comment above them; regenerate and commit `Notice.txt`.
- `src/main/resources/application.properties`: a named `owner` datasource (`elicit_owner`,
  `${ELICIT_OWNER_DB_PASSWORD}`), `quarkus.flyway.owner.schemas=author`,
  `create-schemas=true`, `table=flyway_author_history`, `locations=db/migration`,
  `migrate-at-start=true`; `%dev` URL and password; add
  `com.elicitsoftware.author.access.model` to `quarkus.hibernate-orm.packages`;
  `elicit.authorization.mode=${ELICIT_AUTHORIZATION:OIDC}`.
- Tests keep the schema mirror untouched. The existing `%test.quarkus.flyway.*` block (default
  datasource, `db/schema-mirror,db/test`) stays; `PostgresTestResource.start()` additionally
  supplies the `owner` datasource URL and credentials (`elicit_owner` already exists in
  `db/testcontainers-init.sql`). Hibernate `validate` then covers the new tables at every boot.
- Deployment: add `ELICIT_OWNER_DB_PASSWORD` and the `owner` JDBC URL to the `author` service
  in `docker-compose.yml`, `docker-compose-full.yml`, `e2e_multisite/site1/docker-compose.yml`
  and `e2e_multisite_multilingual/usa/docker-compose.yml`; add the password to
  `diagnostics/RequiredConfigCheck.java`.

### 2. Identity

- **Local row:** `author.users.username` matches `identity.getPrincipal().getName()` exactly
  (the value already written to `published_by`).
- **Provisioning** (new `access/UserRegistrar`): an upsert run once per Vaadin session from a
  UI-init listener beside `NavigationControlAccessCheckerInitializer`, and lazily when a
  creator is granted. Only for identities holding `elicit_author` or `elicit_author_admin`.
  Names and email come from the ID token where present (the `OidcDiagnosticsView` pattern).
- **Pre-registration:** an author admin can add a username before that person's first sign-in.
- **Authorization mode:** port Admin's `AuthorizationModeConfig` and `UserRoleDatabaseLookup`
  (pointed at `author.user_roles`), and reshape
  `security/RoleSecurityIdentityAugmentor.java` to Admin's logic: a role from OIDC always
  wins; only a roleless identity in DATABASE mode gets roles from the local table.
- **`security/AuthorRoles.java`:** add `AUTHOR_ADMIN = "elicit_author_admin"`; `ALL` becomes
  the three roles.
- **`security/CurrentUser.java`:** add `isAuthorAdmin()`, `isSystemAdmin()`, `roleSource()`,
  `displayName()`, `userId()`.
- **Inactive users:** grants ignored in both modes; no local roles given.

### 3. Enforcement (service layer, not views)

Admin enforces its department scoping only in views; this does not copy that. New package
`com.elicitsoftware.author.access`:

- `SurveyAccess` (`@RequestScoped`): the current user's granted survey ids, loaded once per
  request; `canAlter(int)`, `require(int)`.
- `SurveyGrants`: `grantCreator(UUID)`, `grant`, `revoke`, `removeAllFor(UUID)`.
- `@SurveyGuarded` (class-level interceptor binding), `@SurveyId` (parameter annotation),
  `@NotSurveyBound("reason")` (explicit opt-out), and `SurveyGuardInterceptor`.
  - Every intercepted call requires `elicit_author`.
  - The survey is taken from a `@SurveyId` parameter, a `Survey` parameter, or a
    `StructuralElement` parameter.
  - A method that names no survey and has no opt-out throws: a forgotten annotation fails
    closed.
  - Denial raises the existing "survey is gone" error, which views already handle.
- Applied to every service bean in `author/survey` and `author/definition` (about 60 mutators
  across `SurveyService`, `ElementService`, `DesignerService`, `ListService`, `RuleService`,
  `ReportService`, `ReportMarkService`, `ReportingService`, `TranslationService`,
  `TranslationHandoff`, `SurveyDefinitionExporter`).

Specific paths:

- `SurveyService.list()` filters to granted surveys; `find` and `findByKey` return empty when
  not granted, so the seven survey views' existing reroute to not-found delivers decision 2
  with no view change.
- `SurveyService.create` and `SurveyDefinitionImporter.importFile` grant the creator inside
  the same transaction.
- `SurveyDefinitionImporter.rejectIfPresent` currently names the existing survey
  (`error.import.alreadyOpen`). It keeps that message only when the user can open that
  survey; otherwise a new message says the survey exists and to ask an author administrator.
- `SurveyService.delete` removes the survey's grants.
- **Shared reporting tags:** `ReportingService.updateTag` rewrites a tag across every survey
  that uses it. It is refused when the tag is used by a survey the author cannot alter (the
  message gives a count, no names). Dimensions stay a shared vocabulary.
- Move the static `TokenSamples.load(surveyId)` into a guarded bean; static methods are not
  intercepted.

Gate tests:

- `access/SurveyGuardCoverageTest`: every bean in the two packages is guarded or on an
  explicit exception list; every public method resolves a survey or carries the opt-out; one
  live assertion that a self-invoked guarded method is intercepted.
- `access/ViewsUseServicesTest`: nothing under `flow/` reads survey entities directly.

### 4. Management interface

All gated on `elicit_author_admin`, reading through a separate `access/AccessAdminService`
that never goes through `SurveyService`. Modelled on Admin's `UsersView`/`EditUserView` and
Author's System views.

| View | Route | Purpose |
| --- | --- | --- |
| `flow/access/AccessSurveysView` | `access/surveys` | Every survey with its author count; a "No authors" filter surfaces orphans; a dialog assigns authors. No link into the survey. |
| `flow/access/AccessUsersView` | `access/users` | Grid of users (username, name, active, last seen, survey count); "Add user". |
| `flow/access/AccessUserView` | `access/users/:id?` | Edit a user: active flag and a multi-select of surveys. Role assignment shown only in DATABASE mode. |

- `flow/MainLayout.java`: a new "Access" nav section beside the System section.
- Landing: `/` becomes role-aware, so an author admin without `elicit_author` lands on
  `access/surveys` and an `elicit_admin`-only user on `system` (today both land on the
  access-restricted page).
- `keycloak/elicit-realm.json`: add the `elicit_author_admin` client role, map it to `admin`,
  and add an `authoradmin` user holding only that role to exercise manage-only.

### Defaults chosen

- **First author admin in DATABASE mode:** a property,
  `elicit.author.bootstrap-admin=${ELICIT_AUTHOR_BOOTSTRAP_ADMIN:}`, applied at startup when no
  active user holds the role. Admin instead ships a seeded `admin` account.
- **Assigning `elicit_admin` locally:** offered in the role picker only when the acting user
  holds `elicit_admin`.
- **Last author admin:** cannot be deactivated or demoted in DATABASE mode.
- **Only author admins grant access.** A granted author cannot share a survey with others.
- **Column name `active`** on `author.users`, for parity with Admin (the terminology rule
  against `active` is about survey versioning).

## AIUP artifacts (written before any code, each PR's first commit)

Use the `aiup-core` skills: `/requirements`, `/use-case-diagram`, `/use-case-spec UC-0NN`,
`/entity-model`. Validate each spec with the skill's `validate_use_case.py --strict`.

`docs/requirements.md`:

| ID | Change |
| --- | --- |
| FR-070 (new) | Limit Surveys to Granted Authors |
| FR-071 (new) | Manage Survey Access (`elicit_author_admin`) |
| FR-072 (new) | Manage Author Users |
| FR-073 (new) | Assign Roles Locally (DATABASE mode) |
| NFR-020 (new) | Access enforced below the interface (the two gate tests) |
| C-027 (new) | Access data is local: never in a `.elicit` file, never at a site |
| C-028 (new) | Scope of access control (see Limitations) |
| C-005 | Narrowed to "No Migrations of the Survey Schema" |
| C-008 | Adds `elicit_author_admin` and the Admin-style local role fallback |
| FR-033 | Role plus grant |
| FR-003, FR-043, FR-046 | Import pointer only when openable; diagnostics cover the `author` schema and role source |

`docs/use_cases.puml` and `docs/use_cases/`: new actor "Author Administrator" and an "Access
management" group.

| ID | Use case |
| --- | --- |
| UC-050 (new) | Restrict Surveys to Granted Authors |
| UC-051 (new) | Manage Survey Access |
| UC-052 (new) | Manage Author Users |
| UC-053 (new) | Assign Roles from the Local Database |
| UC-001, UC-002 | Roles and BR-001 ("single authoring role") reworded; A3 realised |
| UC-003, UC-005 | Creator is granted on create and on import; the already-present message |
| UC-008 | Grants never travel in the file |
| UC-023 | New rule for shared tags |
| UC-035, UC-038 | Diagnostics additions |
| UC-047 | Delete removes grants |

Also: `docs/entity_model.md` (reword the "does not propose a new schema" header; add an
"Author-owned schema" section and diagram nodes), `docs/vision.md` (target users; the C-005
passage), the manual's "Signing in" section plus a new chapter, `README.md`, `CLAUDE.md`,
`AGENTS.md`.

## Tests

New: `AuthorSchemaGrantsTest`, `RoleSecurityIdentityAugmentorDatabaseModeTest` (with a
`DatabaseAuthorizationTestProfile`, as in Admin), `UserRegistrarTest`, `BootstrapAdminTest`,
`SurveyAccessTest` (user A creates; user B is denied on list, find, each service family,
export, delete and re-import; revocation; inactive user; admin without `elicit_author`),
`AccessAdminServiceTest`, view tests for the three access views, the two gate tests, and an
exporter assertion that no access data appears in the file. All cite the UC and BR IDs.

Existing tests that change:

- 23 of the 47 `@QuarkusTest` classes carry no `@TestSecurity` and call services directly;
  each gets a class-level test identity.
- A `test/FixtureGrantCallback` (`QuarkusTestBeforeEachCallback`) grants the `V9001` fixture
  surveys to the test's principal, so tests that already have one need no edit.
- `MainLayoutTest` (new section, landing), `DisplayedStringsSweepTest` (routes for the new
  role), `DisplayedStringsCoverageTest.EXTRA_FILES`, `EntityMappingSmokeTest` (three entities).

## Sequencing

Four pull requests, each with its own sub-issue referencing #96, branch
`feature/<issue>-<slug>` from `main`, docs commit first, no AI co-author trailer. Grants
accumulate before they are enforced, so `main` stays usable.

1. **Author-owned schema and local identity** (FR-072, FR-073, C-005, C-008, C-027; UC-052,
   UC-053): pom, Flyway, entities, registrar, augmentor, compose files, realm. No UI strings.
2. **Manage survey access** (FR-071; UC-051): the three views, nav section, landing, and
   recording creator grants on create and import. Needs a same-named umbrella branch carrying
   the new keys in `elicit-i18n/author/translations_es_419.properties` and `_ar.properties`,
   and a regenerated `i18n/TRANSLATION_REQUEST.md`.
3. **Enforce grants** (FR-070, NFR-020, C-028; UC-050 and the amendments): interceptor,
   annotations, importer, delete, tag rule, test infrastructure, gate tests. A small umbrella
   branch for the new messages.
4. **Manual and end-to-end:** the manual chapter and screenshots; confirm
   `AuthorToRespondentE2ETest` still passes as `author`.

## Verification

- **Unit and integration:** run the suite through the Quarkus Dev MCP
  (`quarkus_callTool` → `devui-testing_runTests`), then `mvn -B clean verify` once for the
  70% coverage gate, as CI does.
- **Running stack:** `docker compose up -d db keycloak author-survey`, start Author in dev
  mode (the `Author` run configuration), then:
  1. Confirm the `author` schema and `flyway_author_history` exist
     (`docker compose exec db psql -U survey -d author -c '\dt author.*'`).
  2. Sign in as `author`, create a survey, and confirm a row in `author.user_surveys`.
  3. Sign in as a second author: the survey is absent from the list and its URL answers
     not-found.
  4. Sign in as `authoradmin`: only the Access section is shown; grant the second author;
     confirm they can now open and edit the survey.
  5. Import a `.elicit` whose key belongs to a hidden survey and confirm the message names
     nothing.
  6. Export a survey and confirm the file carries no access data.
  7. Restart with `ELICIT_AUTHORIZATION=DATABASE` and `ELICIT_AUTHOR_BOOTSTRAP_ADMIN` set, and
     confirm a roleless Keycloak user gets roles from `author.user_roles`.
- The realm change only loads into a fresh Keycloak, so `keycloak/db` has to be cleared first.

## Limitations and open points

- **The preview Survey (port 8085) is outside this control.** It renders any survey in the
  authoring database to anyone with an access code, with no sign-in. "Hidden" holds inside
  Author only; C-028 records it, and the preview should be restricted at the network.
- **Name collisions leak existence:** survey names are unique, so creating a survey with a
  hidden survey's name still reports "name in use". Accepted and documented.
- **Existing development databases:** their surveys have no grants and are reachable only
  through the Access screen until an author admin assigns them.
- **Renamed users:** matching is on the principal name, as in Admin; renaming a Keycloak user
  detaches their grants.
- **To confirm in step 1:** that Quarkus intercepts self-invocation on these beans (the gate
  test asserts it), and that the signed-in identity is available inside the upload callbacks
  in `ImportDefinitionView` and `TranslationsView`.
