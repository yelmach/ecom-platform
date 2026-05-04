#!/usr/bin/env sh
set -eu

: "${DEPLOY_DIR:?DEPLOY_DIR is required}"
: "${RELEASE_ENV_FILE:?RELEASE_ENV_FILE is required}"

test -f .release.env
mkdir -p "$DEPLOY_DIR"

rsync -a --delete \
  --exclude '.git/' \
  --exclude '.release.env' \
  --exclude '.last-successful-release.env' \
  --exclude 'backend/docker.env' \
  --exclude 'backend/certs/' \
  --exclude 'backend/keys/' \
  --exclude 'frontend/node_modules/' \
  --exclude 'frontend/coverage/' \
  --exclude 'frontend/reports/' \
  ./ "$DEPLOY_DIR/"

cp .release.env "$RELEASE_ENV_FILE"

cd "$DEPLOY_DIR"

docker compose --env-file backend/docker.env --env-file .release.env -f docker-compose.prod.yml pull
docker compose --env-file backend/docker.env --env-file .release.env -f docker-compose.prod.yml up -d --remove-orphans
