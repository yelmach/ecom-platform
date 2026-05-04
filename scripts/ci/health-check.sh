#!/usr/bin/env sh
set -eu

: "${DEPLOY_HOST:?DEPLOY_HOST is required}"

echo "Checking gateway on https://$DEPLOY_HOST:8443/actuator/health"
for attempt in $(seq 1 6); do
  if curl -kfsS "https://$DEPLOY_HOST:8443/actuator/health" | grep -q '"status":"UP"'; then
    echo "Gateway is healthy on attempt $attempt."
    break
  fi

  if [ "$attempt" -eq 6 ]; then
    echo 'Gateway health check did not succeed in time.'
    exit 1
  fi

  echo "Gateway not ready yet (attempt $attempt/6). Waiting 5 seconds..."
  sleep 5
done

echo "Checking frontend on https://$DEPLOY_HOST:4200"
for attempt in $(seq 1 6); do
  if curl -kfsS "https://$DEPLOY_HOST:4200" > /dev/null; then
    echo "Frontend is reachable on attempt $attempt."
    exit 0
  fi

  if [ "$attempt" -eq 6 ]; then
    echo 'Frontend health check did not succeed in time.'
    exit 1
  fi

  echo "Frontend not ready yet (attempt $attempt/6). Waiting 5 seconds..."
  sleep 5
done
