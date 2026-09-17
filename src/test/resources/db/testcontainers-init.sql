---
-- ***LICENSE_START***
-- Elicit Author
-- %%
-- Copyright (C) 2026 The Regents of the University of Michigan - Rogel Cancer Center
-- %%
-- PolyForm Noncommercial License 1.0.0
-- <https://polyformproject.org/licenses/noncommercial/1.0.0>
-- ***LICENSE_END***
---

-- Runs once, right after the Testcontainers Postgres reports healthy and before Flyway
-- connects. The mirrored Survey migrations GRANT on individual tables to these roles and
-- create tables in these schemas, but create neither the roles nor the schemas.
CREATE ROLE survey_user LOGIN PASSWORD 'SURVEYPW';
CREATE ROLE surveyadmin_user LOGIN PASSWORD 'SURVEYPW';
CREATE ROLE surveyreport_user LOGIN PASSWORD 'SURVEYPW';
CREATE ROLE elicit_owner LOGIN PASSWORD 'SURVEYPW' CREATEROLE CREATEDB;

ALTER DATABASE survey OWNER TO elicit_owner;

CREATE SCHEMA IF NOT EXISTS survey AUTHORIZATION elicit_owner;
CREATE SCHEMA IF NOT EXISTS surveyreport AUTHORIZATION elicit_owner;

GRANT USAGE ON SCHEMA survey, surveyreport TO survey_user, surveyadmin_user, surveyreport_user, elicit_owner;
