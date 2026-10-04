#!/usr/bin/env bash
set -euo pipefail

root_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
test_dir="$(mktemp -d)"
trap 'python3 -c '\''import shutil,sys; shutil.rmtree(sys.argv[1])'\'' "$test_dir"' EXIT

cat > "$test_dir/docker" <<'SH'
#!/usr/bin/env bash
printf '%s\n' "$*" >> "$DOCKER_TEST_LOG"
case "$1 $2" in
  'image inspect')
    target="${!#}"
    if [[ "$target" == tnk67/* ]]; then echo "${target%@*}@sha256:test-digest"; else echo 'sha256:test-image'; fi;;
  'manifest inspect')
    case "$MANIFEST_MODE" in
      missing) echo 'manifest unknown: manifest unknown' >&2; exit 1;;
      dockerhub_missing) echo "no such manifest: $3" >&2; exit 1;;
      existing) echo '{}';;
      network) echo 'connection refused' >&2; exit 1;;
      unauthorized) echo 'unauthorized: authentication required' >&2; exit 1;;
    esac;;
  'login '*) cat >/dev/null;;
  'tag '*) ;;
  'push '*) ;;
  *) echo "Unexpected docker call: $*" >&2; exit 2;;
esac
SH
chmod +x "$test_dir/docker"

export PATH="$test_dir:$PATH"
export DOCKER_TEST_LOG="$test_dir/docker.log"
export IMAGE_TAG="sha-$(printf 'a%.0s' {1..40})"
export DOCKERHUB_NAMESPACE='tnk67'
export REGISTRY_USER='test-user'
export REGISTRY_PASSWORD='test-password'

for mode in missing dockerhub_missing existing network unauthorized; do
  export MANIFEST_MODE="$mode"
  : > "$DOCKER_TEST_LOG"
  result=0
  "$root_dir/infra/jenkins/publish-images.sh" > "$test_dir/$mode.out" 2>&1 || result=$?
  pushes="$(grep -c '^push ' "$DOCKER_TEST_LOG" || true)"
  if [[ "$mode" == missing || "$mode" == dockerhub_missing ]]; then
    [[ "$result" == 0 && "$pushes" == 4 ]] || {
      echo "Missing-tag case did not publish four tested images" >&2; exit 1;
    }
  else
    [[ "$result" != 0 && "$pushes" == 0 ]] || {
      echo "$mode case was not blocked before push" >&2; exit 1;
    }
  fi
done

DOCKERHUB_NAMESPACE='attacker.example.test/team/$(touch /tmp/unsafe)' \
  "$root_dir/infra/jenkins/publish-images.sh" > "$test_dir/invalid.out" 2>&1 && {
    echo "Invalid release prefix was accepted" >&2; exit 1;
  }

echo 'Publish helper passed both missing-tag forms, existing, network, unauthorized, and invalid-target checks'
