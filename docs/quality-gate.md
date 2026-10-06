# Final quality-gate evidence

Verified on 2026-10-03 in the local workspace. This page records commands and observed outcomes; the repeatable scripts remain the source of truth. The system Java executable was absent on this host, so the backend run used the workspace's JDK 21 at `.tools/jdk-21.0.12.1+1` as `JAVA_HOME`. A clean checkout can use any installed Java 21 JDK.

| Gate | Command or method | Observed result |
| --- | --- | --- |
| Backend tests | In each `backend/{order-service,support-service,notification-service}` directory, `../mvnw -B -ntp '-Dtest=*Test,*IT,*Tests' test` | Order 43, support 17, notification 13 tests; zero failures/errors/skips; all three Maven builds succeeded. |
| Angular tests/build | `cd frontend && npm ci && npm test -- --watch=false --browsers=ChromeHeadless && npm run build` | 31 ChromeHeadless tests passed; production build succeeded. |
| Compose end-to-end | `scripts/verify-e2e.sh` | Angular route, registration/login, product list/detail, order create/detail, linked ticket create/detail/list and RabbitMQ `OrderCreated` notification passed. The disposable stack was removed after screenshots. |
| Kubernetes manifests | `scripts/verify-kubernetes.sh --offline` with workspace `kubectl` | 25 rendered objects validated for namespace, selectors, probes, resources, Secret references, claims and Ingress. |
| Secret scan | Gitleaks v8.24.3 `git` on history and `dir` on a clean snapshot of tracked/new source | No leaks found. The snapshot excludes ignored local dependencies/build output. |
| Publish helper | `infra/jenkins/test-publish-images.sh` | Missing tag, existing tag, network failure, unauthorized response and invalid target cases pass. |

`kubectl apply --dry-run=client -k infra/kubernetes` could not validate against an API server on this host: the command tried `http://localhost:8080/openapi/v2` and received connection refused. No kind, Minikube or live Kubernetes rollout was available. Jenkins Declarative validation, registry push and deployment also require an external controller, configured credentials and cluster; their behavior is represented by pipeline code, local helper tests and deployment manifests rather than a claimed live release.

The screenshots in the root README come from the completed disposable Compose flow. They show a real Angular registration page and a product catalog row; they are UI evidence, not a substitute for the automated order, ticket and notification checks.
