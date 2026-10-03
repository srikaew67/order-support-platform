#!/usr/bin/env bash
set -euo pipefail

root_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$root_dir"
kubectl_bin="${KUBECTL:-kubectl}"
mode="${1:---offline}"
[[ "$mode" == --offline || "$mode" == --cluster ]] || {
  echo "Usage: scripts/verify-kubernetes.sh [--offline|--cluster]" >&2; exit 2;
}
command -v "$kubectl_bin" >/dev/null || { echo "kubectl is required" >&2; exit 1; }

"$kubectl_bin" kustomize infra/kubernetes | python3 infra/kubernetes/check-manifests.py
if [[ "$mode" == --offline ]]; then
  echo "Offline Kubernetes manifest verification passed"
  exit 0
fi

"$kubectl_bin" apply --dry-run=client -k infra/kubernetes >/dev/null
"$kubectl_bin" -n order-support get secret order-support-secrets -o json | python3 -c '
import base64, json, sys
data = json.load(sys.stdin)["data"]
for key in ("POSTGRES_PASSWORD", "RABBITMQ_PASSWORD", "JWT_SECRET"):
    value = base64.b64decode(data[key]).decode()
    assert value and not value.startswith("REPLACE_"), f"{key} is a placeholder"
assert len(base64.b64decode(data["JWT_SECRET"])) >= 32, "JWT_SECRET is too short"
'
"$kubectl_bin" -n order-support get secret registry-pull >/dev/null

for workload in statefulset/postgres deployment/redis statefulset/rabbitmq \
  deployment/order-service deployment/support-service deployment/notification-service deployment/frontend; do
  "$kubectl_bin" -n order-support rollout status "$workload" --timeout=300s
done

if [[ -n "${SMOKE_BASE_URL:-}" ]]; then
  python3 scripts/smoke-deployed.py --base-url "$SMOKE_BASE_URL"
else
  port="$(python3 -c 'import socket; s=socket.socket(); s.bind(("127.0.0.1", 0)); print(s.getsockname()[1]); s.close()')"
  "$kubectl_bin" -n order-support port-forward --address 127.0.0.1 svc/frontend "$port:8080" \
    > /tmp/cdg-kubernetes-port-forward.log 2>&1 &
  forward_pid=$!
  trap 'kill "$forward_pid" 2>/dev/null || true' EXIT
  for attempt in {1..30}; do
    if python3 -c 'import sys,urllib.request; urllib.request.urlopen(sys.argv[1],timeout=2)' \
        "http://127.0.0.1:$port/healthz" >/dev/null 2>&1; then
      break
    fi
    sleep 1
  done
  python3 scripts/smoke-deployed.py --base-url "http://127.0.0.1:$port"
fi
echo "Kubernetes rollout and health verification passed"
