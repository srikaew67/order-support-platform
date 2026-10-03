# Jenkins pipeline

Use `infra/jenkins/Jenkinsfile` as the script path for a Jenkins Multibranch Pipeline. It checks out the selected commit, scans Git history and the working tree for secrets, runs all backend unit/integration tests and Angular tests/build, builds four Docker images, and runs the isolated full-stack Compose smoke test. Only a successful `main` branch build with `PUBLISH_IMAGES=true` can push images. Deployment is additionally gated by `DEPLOY_TO_K8S=true`.

## Agent and Jenkins setup

- Provide an agent labeled `docker-java21-node22` with Java 21, Node.js 22/npm, Chrome for `ChromeHeadless`, Python 3, Docker Engine/Compose v2, and Gitleaks v8 with the `git` and `dir` commands. The Maven wrapper downloads Maven 3.9.9 on first use. The agent needs network access to Maven Central, npm, and the image registry.
- Install the Jenkins Pipeline, Credentials Binding, JUnit, and Artifact Manager plugins. Configure the job as a Multibranch Pipeline so `BRANCH_NAME` identifies `main` for publishing.
- Store registry username/password under the Jenkins credential ID `container-registry`. For deployment, store a kubeconfig **file** credential under `kubeconfig` and install `kubectl` on the agent. Do not put either value in the repository or job parameters.
- Grant the Jenkins agent Docker access only in a trusted build environment. Protect `main`, restrict who can change this Jenkinsfile or trigger publish/deploy, and configure immutable tags in the registry.

## Parameters and images

| Parameter | Default | Purpose |
| --- | --- | --- |
| `PUBLISH_IMAGES` | `false` | Push all four images after every check passes; only permitted on `main`. |
| `DEPLOY_TO_K8S` | `false` | Apply Kustomize manifests, update images, wait for rollouts, and run deployed smoke. Requires publishing. |
| `REGISTRY_HOST` | `registry.example.com` | Registry host and optional port. Replace before publishing. |
| `REGISTRY_NAMESPACE` | `order-support` | Registry repository namespace. |
| `KUBE_NAMESPACE` | `order-support` | Target Kubernetes namespace. |
| `SMOKE_BASE_URL` | empty | Public frontend base URL, required for deployment verification. |

The pipeline tags each image as `<REGISTRY_HOST>/<REGISTRY_NAMESPACE>/<service>:sha-<full-40-character-commit-SHA>`. Services are `order-service`, `support-service`, `notification-service`, and `frontend`. The publish stage refuses to overwrite an existing SHA tag; enable registry-side tag immutability as a second guard.

## Deployment contract

Deployment requires `infra/kubernetes/kustomization.yaml` from Task 9. Its Deployments and containers must use the four service names above. The target namespace, cluster dependencies, configuration, and secrets must already be prepared for the application. Jenkins applies the manifests, sets the four commit-tagged images, waits up to five minutes per rollout, then runs `scripts/smoke-deployed.py` through `SMOKE_BASE_URL`. This deployed check is read-only: it verifies Angular route fallback, three API health endpoints, and the public catalog. The pre-publish Compose smoke performs the full registration, order, ticket, and notification flow in an isolated database.

Backend JUnit XML files are published to Jenkins. Test, build, secret-scan, and smoke logs and Gitleaks SARIF reports are archived from `reports/`. A failed scan, test, build, or Compose smoke stops the pipeline before registry login, push, or deployment. There is no local Jenkins server in this repository, so the pipeline must also be checked with the Jenkins Declarative Pipeline linter in the target installation before enabling publish/deploy.
