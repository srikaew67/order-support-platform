# Kubernetes deployment

`kustomization.yaml` assembles a single `order-support` namespace, nonsecret configuration, PostgreSQL and RabbitMQ StatefulSets with persistent claims, Redis, four application Deployments, seven internal Services, an Nginx Ingress, and ingress-only NetworkPolicies. The example Secret and optional order-service HPA are deliberately outside the base Kustomization.

## Prerequisites and secrets

- A Kubernetes cluster with a default StorageClass, an Nginx Ingress controller, and a CNI that enforces NetworkPolicy. The Ingress controller must run in a namespace labeled `kubernetes.io/metadata.name=ingress-nginx`, or the `allow-frontend-ingress` policy must be adjusted to its namespace.
- `kubectl`, Python 3 with PyYAML for the checked manifest script, and access to the four application images. For private images, create a `registry-pull` image-pull Secret in this namespace. Jenkins uses the same Deployment and container names when setting commit-tagged images.
- Create a real `order-support-secrets` Secret before applying the workloads. Copy `secret.example.yaml` to ignored `secret.local.yaml`, replace all three placeholder values with private values, and keep file permissions at `0600`. The JWT signing key should contain at least 32 random bytes; `openssl rand -hex 32` yields a suitable 64-character value. The example is not part of Kustomize and must never be applied as-is.

```bash
kubectl apply -f infra/kubernetes/namespace.yaml
umask 077
cp infra/kubernetes/secret.example.yaml infra/kubernetes/secret.local.yaml
# Edit secret.local.yaml and replace every REPLACE_WITH value.
kubectl apply -f infra/kubernetes/secret.local.yaml
kubectl -n order-support create secret docker-registry registry-pull \
  --docker-server=YOUR_REGISTRY_HOST --docker-username=YOUR_USER --docker-password=YOUR_TOKEN
```

The Secret file is excluded by `.gitignore`. Create the image-pull Secret through your cluster's secret management process in production; avoid leaving registry tokens in shell history. The committed ConfigMap uses database `order_support` and user `order_support`. If these change, update `POSTGRES_DB`, `POSTGRES_USER`, and `SPRING_DATASOURCE_URL` together before first startup. The three APIs read the same private JWT key. Redis is a disposable cache; PostgreSQL and RabbitMQ use persistent claims. The dependency manifests use one replica each and are suited to a local or small demonstration cluster, not a highly available production data tier.

## Validate and deploy

Run the offline structural check first. It renders Kustomize and checks namespace, service selectors, probes, CPU/memory resources, Secret references, image-pull configuration, and persistent claims. An optional independent schema check uses Kubeconform. A client dry run and rollout check require a reachable API server.

```bash
scripts/verify-kubernetes.sh --offline
kubectl apply --dry-run=client -k infra/kubernetes
kubectl apply -k infra/kubernetes
scripts/verify-kubernetes.sh --cluster
```

`--cluster` checks the applied Secret and pull Secret, waits for all seven workloads, then verifies Angular and all three API health endpoints through a temporary frontend port-forward. Set `SMOKE_BASE_URL` to an externally reachable frontend URL to check through the Ingress instead. The Ingress uses `order-support.local`; point that host at your controller or change the manifest to your assigned hostname. Add TLS through your ingress controller and a real certificate before exposing the service beyond a local cluster.

For kind or Minikube without a private registry, build the four `order-support/<service>:local` images, load them into the cluster, and create a local-only empty image-pull Secret. For a kind cluster named `order-support`:

```bash
JWT_SECRET="$(openssl rand -hex 32)" IMAGE_TAG=local docker compose build \
  order-service support-service notification-service frontend
kind load docker-image --name order-support \
  order-support/order-service:local order-support/support-service:local \
  order-support/notification-service:local order-support/frontend:local
kubectl -n order-support create secret generic registry-pull \
  --type=kubernetes.io/dockerconfigjson --from-literal=.dockerconfigjson='{"auths":{}}'
```

For Minikube, use `minikube image load` for each image instead of `kind load docker-image`. `imagePullPolicy: IfNotPresent` uses the loaded images. The Jenkins pipeline instead publishes SHA-tagged images to the trusted registry, applies this Kustomization, and updates each Deployment image.

Apply `hpa-order-service.example.yaml` only when a metrics server is present and CPU-based scaling is useful. It is not included in the base stack. Use `kubectl -n order-support describe pod <name>` and `kubectl -n order-support logs <pod>` for rollout failures; common causes are missing Secrets, an unavailable image registry, or a StorageClass that cannot bind the two claims. Back up PostgreSQL and RabbitMQ data before deleting their claims.
