# Jenkins pipeline

Use `infra/jenkins/Jenkinsfile` as the script path for a Jenkins Multibranch Pipeline. It checks out the selected commit, scans Git history and the working tree for secrets, tests the publish helper, runs all backend unit/integration tests and Angular tests/build, builds four Docker images, and runs the isolated full-stack Compose smoke test. Only a successful `main` branch build with `PUBLISH_IMAGES=true` can push images. Deployment is additionally gated by `DEPLOY_TO_K8S=true`.

## Agent and Jenkins setup

- Provide an agent labeled `docker-java21-node22` with Java 21, Node.js 22/npm, Chrome for `ChromeHeadless`, Python 3, Docker Engine/Compose v2, and Gitleaks v8 with the `git` and `dir` commands. The Maven wrapper downloads Maven 3.9.9 on first use. The agent needs network access to Maven Central, npm, and the image registry.
- Install the Jenkins Pipeline, Credentials Binding, JUnit, and Artifact Manager plugins. Configure the job as a Multibranch Pipeline so `BRANCH_NAME` identifies `main` for publishing.
- For Docker Hub, store the approved namespace (for example, `tnk67`) as a Jenkins **secret text** credential named `dockerhub-namespace`; the pipeline publishes to `tnk67/order-support-<service>`. Store the Docker Hub username and Personal Access Token under `container-registry`. Create the four repositories before publishing: `order-support-order-service`, `order-support-support-service`, `order-support-notification-service`, and `order-support-frontend`. The pipeline only creates a previously absent full-SHA tag and records the pushed digest; Docker Hub Free does not provide registry-enforced tag immutability. For deployment, store a kubeconfig **file** credential under `kubeconfig` and install `kubectl` on the agent. Do not put credential values in the repository or job parameters.
- Grant the Jenkins agent Docker access only in a trusted build environment. Protect `main` and restrict who can change this Jenkinsfile or trigger publish/deploy. The release registry is fixed by the administrator-controlled credential; build parameters cannot redirect login to another host.

## Parameters and images

| Parameter | Default | Purpose |
| --- | --- | --- |
| `PUBLISH_IMAGES` | `false` | Push all four images after every check passes; only permitted on `main`. |
| `DEPLOY_TO_K8S` | `false` | Apply Kustomize manifests, update images, wait for rollouts, and run deployed smoke. Requires publishing. |
| `SMOKE_BASE_URL` | empty | Public frontend base URL, required for deployment verification. |

The pipeline builds local images as `order-support/<service>:sha-<full-40-character-commit-SHA>`. Services are `order-service`, `support-service`, `notification-service`, and `frontend`. The Compose smoke test starts these exact local image IDs with `--no-build` and checks each running container's image ID. After that passes, the publish helper retags the same image IDs under `<dockerhub-namespace>/order-support-<service>:sha-<full-SHA>` and pushes them.

Before any push, the helper checks all four remote tags. It continues only when every inspection explicitly reports `manifest unknown` or `no such manifest` for that exact target; an existing tag, authorization error, network failure, or any other ambiguous response stops publication. Registry-side tag immutability remains required to close the race between inspection and push. The administrator-controlled `registry-immutable-tags` credential is the pipeline gate for that policy.

## Deployment contract

Deployment uses the fixed `order-support` namespace from `infra/kubernetes/kustomization.yaml`. Its Deployments and containers use the four service names above. The namespace, cluster dependencies, configuration, and secrets must already be prepared for the application; see `infra/kubernetes/README.md`. Jenkins applies the manifests, sets the four commit-tagged images, waits up to five minutes per rollout, then runs `scripts/smoke-deployed.py` through `SMOKE_BASE_URL`. This deployed check is read-only: it verifies Angular route fallback, three API health endpoints, and the public catalog. The pre-publish Compose smoke performs the full registration, order, ticket, and notification flow in an isolated database.

Backend JUnit XML files are published to Jenkins. Test, build, secret-scan, and smoke logs and Gitleaks SARIF reports are archived from `reports/`. A failed scan, test, build, or Compose smoke stops the pipeline before registry login, push, or deployment. Run `infra/jenkins/test-publish-images.sh` locally to verify that the publish helper blocks existing tags and registry errors before a push. There is no local Jenkins server in this repository, so the pipeline must also be checked with the Jenkins Declarative Pipeline linter in the target installation before enabling publish/deploy.
