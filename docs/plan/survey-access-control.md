# Per-survey access control

Tracking issue: [#96](https://github.com/ElicitSoftware/Author/issues/96). Written 2026-11-06,
before any of the AIUP artifacts or code it names existed; the artifacts under `docs/` take
precedence over this document once they are written.

## Context

Today anyone holding `elicit_author` can open, edit, export and delete every survey in the
authoring database. There is no user table, no owner column and no check narrower than the
role (FR-033, UC-002 BR-001). The goal is to limit which users can alter which surveys, using
the same identity model as Admin (OIDC principal matched to a local user row, with an optional
local-table role fallback), plus screens to manage the user/survey relationships, gated on
`elicit_author_admin`. That is Author's existing administrator role, renamed from `elicit_admin`
so it is not confused with Admin's role of that name; it gates the System section too.

Decisions already made:

1. **Author-owned schema.** Author gets its own Flyway migrations creating an `author` schema
   that exists only in the authoring database. C-005 is narrowed to "no migrations of the
   `survey` schema".
2. **Ungranted surveys are hidden**: absent from the list, URLs answer "not found". No
   read-only mode.
3. **`elicit_author_admin` is manage-only.** It opens the access screens and the System
   section, and nothing else. To
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
| `author.user_roles` | `user_id`, `role_name` | PK on both; CHECK `role_name IN ('elicit_author','elicit_author_admin')`. Used only in DATABASE mode. |
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
  in the umbrella's `docker-compose.yml`, `e2e_multisite/site1/docker-compose.yml` and
  `e2e_multisite_multilingual/usa/docker-compose.yml` (Elicit#72 moved all three there); add the password to
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
- **`security/AuthorRoles.java`:** `AUTHOR_ADMIN = "elicit_author_admin"` exists already (the
  renamed `ADMIN`); `ALL` becomes the two roles.
- **`security/CurrentUser.java`:** add `isAuthorAdmin()`, `roleSource()`,
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
  `access/surveys` (today it lands on the access-restricted page).
- `keycloak/elicit-realm.json`: the `elicit_author_admin` client role exists and is mapped to
  `admin` already (renamed from `elicit_admin`); add an
  `authoradmin` user holding only that role to exercise manage-only.

### 5. The preview Survey

The preview (`author-survey`, port 8085) is a Survey instance over the authoring database with
`accessCode.autoRegister=true`: its login page lists every survey in a picker and any typed code
enters any of them. Hiding a survey in Author while the preview shows it is not the separation
asked for. Survey has no sign-in and cannot know who an author is, so the preview is made an
**issued** thing: Author decides who may preview what, and the preview only renders what Author
hands it. It rests on Survey's survey-per-host change
(`Survey/docs/plans/survey-per-host.md`, FR-036 / UC-015 there): a host offers one survey, a
code for another survey is refused on it, and once any survey is mapped a host with no mapping
offers none.

- **Every survey gets a preview host.** `SurveyService.create`, `SurveyDefinitionImporter` and
  `SurveyService.delete` call Survey's `POST`/`DELETE /api/surveys/{key}/host` through
  `PreviewSurveyClient`, at `author.preview.survey.url`, in the same place they grant the creator
  or remove grants. The host is `<slug>.<author.preview.host-suffix>`; a new property
  `author.preview.host-suffix` (e.g. `preview.survey.app.med.umich.edu`) names the suffix, and
  the bare preview host then shows "no surveys". A failed call is reported like the schema drop
  in UC-047 BR-008: the survey operation succeeds, the preview is noted as not configured.
- **Author mints the preview respondent.** A new guarded method, `SurveyService.preview(survey)`
  (FR-030, until now Open), inserts a `survey.respondents` row for that survey with a random
  access code, as Admin's own `AccessCodeService` does, and returns
  `https://<host>/#/login/<code>`. The designer's "Preview as respondent" button and the Reports
  page link (UC-048 A5, today a link to the preview root) open it in a new tab. Being guarded, it
  is only reachable for a survey the author is granted, so a minted code is the only door and an
  author holds codes only for their own surveys.
- **The preview runs with `accessCode.autoRegister=false`** in the umbrella's
  `docker-compose.yml` and the two e2e compose files; `author.preview.survey.url` keeps the
  internal address for the three calls (host set, host clear, schema drop), and the browser-facing
  link is built from the host suffix, so the property no longer has to be browser-reachable.
- **What still shows**: a visitor who guesses another survey's preview host sees its login page
  and title, and nothing more. That is the same class of leak as the name-collision item below;
  deriving the slug from the survey key rather than the name closes it, at the cost of
  unreadable hosts. Preview answers already live in the authoring database and go with the
  survey on delete.
- **Site requirements**: a wildcard DNS record and certificate for `*.<suffix>`, and the proxy
  routing that wildcard to the preview instance with `X-Forwarded-Host`. Where a wildcard is not
  available, the fallback is a Survey change (a `/login/<code>` link that resolves the survey
  from the code and hides the picker) and is not part of this plan.

Artifacts: FR-030 moves from Open to the design above and gains the host and minted-code rules;
a new UC-054 "Preview a Survey as a Respondent" (actor: survey author; preconditions: granted;
business rules: minted code, one host per survey, auto-registration off); UC-047 adds the host
clear beside the schema drop; UC-048 A5 points at the minted link; C-028 is narrowed to the name
leak. Tests: `SurveyAccessTest` gains "user B cannot mint a preview for A's survey";
`PreviewSurveyClientTest` covers host set, clear and an unreachable preview; the
`AuthorToRespondentE2ETest` journey enters the preview through the minted link instead of a typed
code.

### 6. Any OIDC provider, Keycloak as the worked example

Keycloak is the bundled evaluation provider, not a requirement (C-008). A site may bring
Microsoft Entra ID or any other OIDC provider, and the identity model above is chosen so that it
does not care which. What differs by provider is configuration, and the plan commits to naming
it in the configuration reference and the manual chapter:

- **Where roles are in the token.** Nothing sets `quarkus.oidc.roles.role-claim-path`, so
  Quarkus reads Keycloak's `realm_access` and `resource_access` claims and the generic `groups`
  claim. Entra emits app roles in a `roles` claim and fills `groups` with object IDs, omitting it
  above 200 groups. A site that manages Elicit's roles in such a provider sets
  `quarkus.oidc.roles.role-claim-path` (to `roles` on Entra) on Author and on Admin; the role
  names (`elicit_author`, `elicit_author_admin`) are then app-role values. The
  `keycloak/elicit-realm.json` step in section 4 is the bundled example only.
- **DATABASE mode is the expected route on a non-Keycloak provider**, not a corner case. Without
  the claim path every such user arrives roleless and reaches the `author.user_roles` lookup, and
  `ELICIT_AUTHOR_BOOTSTRAP_ADMIN` is then the first-run path rather than a fallback. The manual
  chapter leads with it for that audience, with the claim-path setting beside it for sites that
  prefer provider-managed roles. The mechanism is Admin's unchanged: a provider role always
  wins, and the table is consulted only when the provider supplies none.
- **One username across Admin and Author.** Both key the local user on the principal name,
  which Quarkus takes from `upn`, then `preferred_username`, then `sub`. That is right: Entra's
  `sub` is pairwise per app registration, so it would name the same person differently in the
  two apps, while `preferred_username` is the same in both. A site pins the same
  `quarkus.oidc.token.principal-claim` on Admin and Author, and the manual says so.
- **Username matching is exact in Admin** (`u.username = :username` in
  `UserRoleDatabaseLookup`, and the registrar's upsert follows it). Entra does not guarantee
  that a UPN's casing in the token matches what an author administrator pre-registers. Author
  does not diverge from Admin on its own: either both compare case-insensitively (an Admin
  change, tracked there) or both document exact matching. Until that is decided Author matches
  exactly, and pre-registration keeps the casing the provider sends.
- **Silent refresh.** `quarkus.oidc.token.refresh-expired=true` exists for Keycloak's
  five-minute ID tokens and needs a refresh token. Entra issues one only when `offline_access`
  is requested, so `quarkus.oidc.authentication.scopes=offline_access` is a documented site
  setting; without it an Entra session ends at token expiry, the mid-edit failure the setting
  was added to prevent.
- **Redirect registration.** Author's callback is the fixed path `/callback`, so a provider
  needs the exact URIs `https://<author host>/callback` and, for sign-out, the post-logout URI
  `https://<author host>/`. Wildcards such as `http://localhost:8084/*` are a Keycloak
  convenience; Entra accepts none and has no "web origins".
- **Optional claims.** Names and email come from `given_name`, `family_name` and `email`, which
  an Entra tenant must enable as optional claims; the nullable columns in section 1 already
  tolerate their absence.

### Defaults chosen

- **First author admin in DATABASE mode:** a property,
  `elicit.author.bootstrap-admin=${ELICIT_AUTHOR_BOOTSTRAP_ADMIN:}`, applied at startup when no
  active user holds the role. Admin instead ships a seeded `admin` account.
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
passage), the manual's "Signing in" section plus a new chapter written for any provider (section
6: DATABASE mode first, then the claim path, the principal claim, `offline_access` and the exact
redirect URIs, with Keycloak and Entra as the two worked examples), `README.md`, `CLAUDE.md`,
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
5. **Issued preview** (FR-030, UC-054, C-028): after Survey's survey-per-host change has shipped
   in the image the preview runs. Host suffix property, host calls on create/import/delete,
   `SurveyService.preview`, the two buttons, auto-registration off in the compose files, the
   e2e journey through the minted link. A small umbrella branch for the button strings.

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

## Target deployment check (2026-11-06, not being implemented now)

Reviewed against the intended site: `author.survey.med.umich.edu` behind Microsoft Entra ID,
authors limited to the surveys they are authorized for, sibling hosts for Admin and for each
survey, and no data shared between surveys. The full table is in Survey's plan
(`Survey/docs/plans/survey-per-host.md`, "Target deployment"). For Author the verdict is **yes**
once PRs 1-3 land with the section 6 settings, with three leaks accepted and on record: unique
survey names report "name in use" for a hidden survey; reporting tags and dimensions stay a
shared vocabulary across authors (section 3); and a guessed preview host shows that survey's
title (section 5). Previews live on their own wildcard, not on the production survey hosts. The
gaps that review found are elsewhere: Admin has no per-survey limit (below), one database role
reads every survey's report schema (Survey), and a French-only survey needs a French chrome
bundle, French as its base language and French in `author.content.languages`.

## Limitations and open points

- **The preview Survey (port 8085) is outside this control until PR 5.** Through PRs 1-4 it
  renders any survey in the authoring database to anyone with an access code, with no sign-in,
  so "hidden" holds inside Author only; C-028 records it and the preview should be restricted at
  the network meanwhile. Section 5 closes it once Survey's survey-per-host change is available,
  leaving only the title of a survey whose preview host is guessed.
- **Name collisions leak existence:** survey names are unique, so creating a survey with a
  hidden survey's name still reports "name in use". Accepted and documented.
- **Existing development databases:** their surveys have no grants and are reachable only
  through the Access screen until an author admin assigns them.
- **Renamed users:** matching is on the principal name, as in Admin; renaming a user in the
  provider detaches their grants. Matching is also case-sensitive until Admin and Author decide
  otherwise together (section 6).
- **Admin's own per-survey limit** is a separate gap: `survey.user_surveys` exists since Admin's
  first migration and is mapped on `User.surveys`, but nothing reads or writes it. The design
  here (grants keyed on the survey, creator granted on apply, hidden rather than read-only,
  enforced in services, managed by the role that already manages users) is its template, with
  the existing foreign key on `survey_id` kept because Admin shares Survey's database. That is
  an Admin issue and plan.
- **To confirm in step 1:** that Quarkus intercepts self-invocation on these beans (the gate
  test asserts it), and that the signed-in identity is available inside the upload callbacks
  in `ImportDefinitionView` and `TranslationsView`.
