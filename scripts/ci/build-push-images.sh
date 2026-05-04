#!/usr/bin/env sh
set -eu

: "${GHCR_USER:?GHCR_USER is required}"
: "${GHCR_TOKEN:?GHCR_TOKEN is required}"
: "${IMAGE_REGISTRY:?IMAGE_REGISTRY is required}"
: "${IMAGE_TAG:?IMAGE_TAG is required}"

BACKEND_SERVICES="discovery-service gateway-service user-service product-service media-service"
REGISTRY_HOST="${IMAGE_REGISTRY%%/*}"

printf 'IMAGE_REGISTRY=%s\nIMAGE_TAG=%s\n' "$IMAGE_REGISTRY" "$IMAGE_TAG" > .release.env

echo "$GHCR_TOKEN" | docker login "$REGISTRY_HOST" -u "$GHCR_USER" --password-stdin

for service in $BACKEND_SERVICES; do
  image="$IMAGE_REGISTRY/ecom-$service:$IMAGE_TAG"
  context="backend/$service"

  echo "Building $image from $context"
  docker build -t "$image" "$context"

  echo "Pushing $image"
  docker push "$image"
done

frontend_image="$IMAGE_REGISTRY/ecom-frontend:$IMAGE_TAG"

echo "Building $frontend_image from frontend"
docker build -t "$frontend_image" frontend

echo "Pushing $frontend_image"
docker push "$frontend_image"
