#!/usr/bin/env bash
set -euo pipefail

new_tag=${1:?Provide the new image tag}
cd "${2:-.}"
new_compose=${3:-$PWD/docker-compose.yml}
current=$PWD/.deploy/current
rollback=$PWD/.deploy/rollback

# Run Compose with the selected folder's tag and the shared .env.
compose() {
    local folder=$1
    shift
    IMAGE_TAG="$(cat "$folder/image-tag")" docker compose --env-file .env \
        --project-directory "$PWD" -f "$folder/docker-compose.yml" "$@"
}

# Check the frontend and product API.
check_url() {
    curl -fsS --max-time 5 "$1/healthz" | grep -qx ok &&
    curl -fsS --max-time 5 -o /dev/null -w '%{content_type}' "$1/products" | grep -q '^application/json'
}

# Allow about a minute for local requests to start working.
healthy() {
    local address deadline=$((SECONDS + 60))
    address=$(compose "$1" port frontend 6100) || return 1
    while ((SECONDS < deadline)); do
        if check_url "http://$address"; then return 0; fi
        sleep 5
    done
    return 1
}

# 1. Save the current version, then prepare the new version.
if [[ ! -f $current/image-tag ]]; then
    echo 'Set up .deploy/current with your working Compose file and image tag first.' >&2
    exit 1
fi

mkdir -p "$rollback"
cp "$current/docker-compose.yml" "$current/image-tag" "$rollback/"
cp "$new_compose" "$current/docker-compose.yml"
printf '%s\n' "$new_tag" > "$current/image-tag"

# 2. Pull everything before changing any running containers.
if ! compose "$current" pull; then
    cp "$rollback/docker-compose.yml" "$rollback/image-tag" "$current/"
    echo 'Image pull failed; running containers were not changed.' >&2
    exit 1
fi

# 3. Deploy the whole stack and check local health.
if compose "$current" up -d --no-build --pull never --remove-orphans --wait --wait-timeout 300 && healthy "$current"; then
    echo "DEPLOYMENT SUCCEEDED: $new_tag"

    # Public failures are warnings, not reasons to roll back.
    if gateway=$(compose "$current" ps -q gateway-service) &&
       public_url=$(docker inspect --format '{{range .Config.Env}}{{println .}}{{end}}' "$gateway" | sed -n 's/^CORS_ALLOWED_ORIGINS=//p') &&
       check_url "${public_url%/}"; then
        echo 'Public checks passed.'
    else
        echo 'WARNING: public checks failed; deployment remains successful.'
    fi
    exit 0
fi

# 4. If deployment failed, run the saved rollback version once.
echo 'Deployment failed. Rolling back.'
if compose "$rollback" up -d --no-build --pull never --remove-orphans --wait --wait-timeout 300 && healthy "$rollback"; then
    cp "$rollback/docker-compose.yml" "$rollback/image-tag" "$current/"
    echo 'ROLLBACK SUCCEEDED'
else
    echo 'ROLLBACK FAILED' >&2
fi
exit 1
