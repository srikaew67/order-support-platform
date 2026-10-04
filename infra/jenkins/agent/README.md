# Local Jenkins build agent

Build the image from the repository root:

```bash
docker build -t order-support/jenkins-agent:local -f infra/jenkins/agent/Dockerfile .
```

Create a Jenkins permanent node named `local-ci-agent`, with remote root directory
`/home/jenkins/agent`, label `docker-java21-node22`, and the inbound-agent launch
method. Jenkins shows an agent secret after the node is created.

Run the node, replacing only the displayed secret:

```bash
docker run -d --name jenkins-order-support-agent --restart unless-stopped \
  --add-host=host.docker.internal:host-gateway \
  -e JENKINS_URL=http://host.docker.internal:8088/ \
  -e JENKINS_AGENT_NAME=local-ci-agent \
  -e JENKINS_SECRET=REPLACE_WITH_JENKINS_AGENT_SECRET \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v jenkins-order-support-agent-workspace:/home/jenkins/agent \
  order-support/jenkins-agent:local
```

The Docker socket is intentionally mounted only for this local demonstration agent;
do not use this pattern for untrusted repositories or multi-tenant Jenkins agents.
