#!/usr/bin/env bash
# Refresh the test-only mirror of Survey's greenfield Flyway migrations.
#
# Author owns no production migrations (C-005). Its @QuarkusTest suite replays a byte-for-byte
# copy of ../Survey/src/main/resources/db/migration into a throwaway Testcontainers Postgres so
# entity mappings are verified against the real schema. SchemaMirrorFreshnessTest fails when
# this copy drifts from the sibling checkout; run this script to catch up.
set -euo pipefail
HERE="$(cd "$(dirname "$0")/.." && pwd)"
SRC="${SURVEY_MIGRATIONS:-$HERE/../Survey/src/main/resources/db/migration}"
DST="$HERE/src/test/resources/db/schema-mirror"
if [ ! -d "$SRC" ]; then
  echo "Survey migrations not found at $SRC (set SURVEY_MIGRATIONS to override)" >&2
  exit 1
fi
mkdir -p "$DST"
rsync -a --delete --exclude 'SOURCE.txt' "$SRC/" "$DST/"
{
  echo "Mirror of $(cd "$SRC" && pwd)"
  echo "Survey commit: $(git -C "$SRC" rev-parse HEAD 2>/dev/null || echo unknown)"
  echo "Synced: $(date -u +%Y-%m-%dT%H:%M:%SZ)"
} > "$DST/SOURCE.txt"
echo "Synced $(ls "$DST"/*.sql | wc -l | tr -d ' ') migration files into $DST"
