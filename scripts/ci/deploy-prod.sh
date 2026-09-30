#!/usr/bin/env sh
set -eu

: "${DEPLOY_DIR:?DEPLOY_DIR is required}"
: "${RELEASE_ENV_FILE:?RELEASE_ENV_FILE is required}"

test -f .release.env
mkdir -p "$DEPLOY_DIR"
mkdir -p "$DEPLOY_DIR/scripts/ci"
test -f "$DEPLOY_DIR/.env" || {
  echo "Missing $DEPLOY_DIR/.env; provision production configuration before deploying." >&2
  exit 1
}

rsync -a docker-compose.yml "$DEPLOY_DIR/"
rsync -a --delete scripts/ci/ "$DEPLOY_DIR/scripts/ci/"

cp .release.env "$RELEASE_ENV_FILE"

cd "$DEPLOY_DIR"

docker compose --env-file .env --env-file .release.env -f docker-compose.yml pull
docker compose --env-file .env --env-file .release.env -f docker-compose.yml up -d --remove-orphans
