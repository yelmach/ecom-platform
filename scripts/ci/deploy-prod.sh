#!/usr/bin/env sh
set -eu

: "${DEPLOY_DIR:?DEPLOY_DIR is required}"
: "${RELEASE_ENV_FILE:?RELEASE_ENV_FILE is required}"

test -f .release.env
mkdir -p "$DEPLOY_DIR"
mkdir -p "$DEPLOY_DIR/scripts/ci"

rsync -a docker-compose.prod.yml Makefile "$DEPLOY_DIR/"
rsync -a --delete scripts/ci/ "$DEPLOY_DIR/scripts/ci/"

cp .release.env "$RELEASE_ENV_FILE"

cd "$DEPLOY_DIR"

docker compose --env-file backend/docker.env --env-file .release.env -f docker-compose.prod.yml pull
docker compose --env-file backend/docker.env --env-file .release.env -f docker-compose.prod.yml up -d --remove-orphans
