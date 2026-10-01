#!/bin/bash
# Export the 'elicit' realm from Keycloak 26.2.3 using the H2 database files from the running container
# This script copies the H2 files from the running container, prompts to stop the container, and then exports the realm.

set -e

# Resolved from this repo's compose project rather than hard-coded. The name differs between
# stacks -- `elicit-author-keycloak-1` here, `elicit-keycloak-1` in the umbrella -- and a
# hard-coded one either finds nothing or, worse, exports the umbrella's realm into this repo.
COMPOSE_FILE="$(cd "$(dirname "$0")/.." && pwd)/docker-compose.yml"
CONTAINER_NAME="${KEYCLOAK_CONTAINER:-$(docker compose -f "$COMPOSE_FILE" ps -q keycloak 2>/dev/null | head -1)}"
if [ -z "$CONTAINER_NAME" ]; then
	echo "Keycloak is not running for $COMPOSE_FILE." >&2
	echo "Start it with 'docker compose up -d keycloak' from the repository root," >&2
	echo "or set KEYCLOAK_CONTAINER to the container to export from." >&2
	exit 1
fi

# Set export directory and filename with date
EXPORT_DIR="$(pwd)"
H2_DIR="$(pwd)/db"
REALM_NAME="elicit"
DATE_STR=$(date +"%m_%d_%Y")
EXPORT_FILE="$EXPORT_DIR/${REALM_NAME}_${DATE_STR}.json"
mkdir -p "$EXPORT_DIR"
mkdir -p "$H2_DIR"

echo "Copying H2 database files from running container ($CONTAINER_NAME)..."
docker cp "$CONTAINER_NAME":/opt/keycloak/data/h2/keycloakdb.mv.db "$H2_DIR/keycloakdb.mv.db"
docker cp "$CONTAINER_NAME":/opt/keycloak/data/h2/keycloakdb.trace.db "$H2_DIR/keycloakdb.trace.db" 2>/dev/null || true

echo "Please stop the Keycloak container ($CONTAINER_NAME) now to ensure database consistency, then press Enter to continue."
read -r

docker run --rm \
	-v "$H2_DIR:/h2" \
	-v "$EXPORT_DIR:/opt/keycloak/data/export" \
	quay.io/keycloak/keycloak:26.2.3 \
	export \
		--db-url="jdbc:h2:file:/h2/keycloakdb;NON_KEYWORDS=VALUE" \
		--realm "$REALM_NAME" \
		--users same_file \
		--file "/opt/keycloak/data/export/${REALM_NAME}_${DATE_STR}.json"