#!/usr/bin/env bash
set -euo pipefail
set +x

: "${IMAGE_TAG:?A full commit SHA image tag is required}"
: "${DOCKERHUB_NAMESPACE:?The trusted Docker Hub namespace credential is required}"
: "${REGISTRY_USER:?Registry username is required}"
: "${REGISTRY_PASSWORD:?Registry password is required}"
: "${REGISTRY_IMMUTABILITY_CONFIRMED:?Registry immutability confirmation is required}"

[[ "$REGISTRY_IMMUTABILITY_CONFIRMED" == enabled ]] || {
  echo "Registry-side immutable tags have not been confirmed by an administrator" >&2
  exit 1
}

[[ "$IMAGE_TAG" =~ ^sha-[0-9a-f]{40}$ ]] || {
  echo "IMAGE_TAG must contain the full Git commit SHA" >&2
  exit 1
}
[[ "$DOCKERHUB_NAMESPACE" =~ ^[a-z0-9][a-z0-9_-]{1,38}$ ]] || {
  echo "Invalid Docker Hub namespace" >&2
  exit 1
}
registry_host='docker.io'
services=(order-service support-service notification-service frontend)

for service in "${services[@]}"; do
  docker image inspect "order-support/$service:$IMAGE_TAG" >/dev/null || {
    echo "Missing tested local image: $service:$IMAGE_TAG" >&2
    exit 1
  }
done

export DOCKER_CONFIG="$(mktemp -d)"
cleanup() {
  python3 -c 'import shutil,sys; shutil.rmtree(sys.argv[1])' "$DOCKER_CONFIG"
}
trap cleanup EXIT
printf '%s' "$REGISTRY_PASSWORD" | docker login "$registry_host" \
  --username "$REGISTRY_USER" --password-stdin

# Continue only when the registry explicitly reports that every manifest is absent.
# A network, authorization, or other inspection error stops publication.
for service in "${services[@]}"; do
  target="$DOCKERHUB_NAMESPACE/order-support-$service:$IMAGE_TAG"
  result=0
  inspection="$(docker manifest inspect "$target" 2>&1)" || result=$?
  if (( result == 0 )); then
    echo "Immutable image tag already exists: $service:$IMAGE_TAG" >&2
    exit 1
  fi
  if [[ "$inspection" != 'manifest unknown' &&
        "$inspection" != 'manifest unknown: manifest unknown' &&
        "$inspection" != "no such manifest: $target" ]]; then
    echo "Cannot verify that the registry tag is absent: $service:$IMAGE_TAG" >&2
    exit 1
  fi
done

for service in "${services[@]}"; do
  target="$DOCKERHUB_NAMESPACE/order-support-$service:$IMAGE_TAG"
  docker tag "order-support/$service:$IMAGE_TAG" "$target"
  docker push "$target"
done
