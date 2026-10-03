#!/usr/bin/env python3
"""Check the rendered Kustomize workload contract before deployment."""

import sys

import yaml


def require(condition, message):
    if not condition:
        raise SystemExit(message)


objects = list(yaml.safe_load_all(sys.stdin))
resources = {(item["kind"], item["metadata"]["name"]): item for item in objects}
require(len(resources) == len(objects), "Duplicate Kubernetes kind/name")
require(("Namespace", "order-support") in resources, "Missing namespace")
require(not any(item["kind"] == "Secret" for item in objects), "A Secret was rendered from committed files")
require(all(item["metadata"].get("namespace") == "order-support"
            for item in objects if item["kind"] != "Namespace"), "Resource outside order-support namespace")

apps = ("order-service", "support-service", "notification-service", "frontend")
dependencies = ("postgres", "redis", "rabbitmq")
for name in (*apps, *dependencies):
    require(("Service", name) in resources, f"Missing Service {name}")
for name in (*apps, "redis"):
    require(("Deployment", name) in resources, f"Missing Deployment {name}")
for name in ("postgres", "rabbitmq"):
    require(("StatefulSet", name) in resources, f"Missing StatefulSet {name}")

for name in (*apps, *dependencies):
    kind = "StatefulSet" if name in ("postgres", "rabbitmq") else "Deployment"
    workload = resources[(kind, name)]
    pod = workload["spec"]["template"]
    require(workload["spec"]["selector"]["matchLabels"].items()
            <= pod["metadata"]["labels"].items(), f"Selector mismatch for {name}")
    service = resources[("Service", name)]
    require(service["spec"]["selector"].items() <= pod["metadata"]["labels"].items(),
            f"Service selector mismatch for {name}")
    container = pod["spec"]["containers"][0]
    require(container["name"] == name, f"Container name mismatch for {name}")
    container_ports = {port["name"] for port in container.get("ports", [])}
    require(all(port.get("targetPort") in container_ports for port in service["spec"]["ports"]),
            f"Service port does not target a named container port on {name}")
    require(all(probe in container for probe in ("startupProbe", "readinessProbe", "livenessProbe")),
            f"Missing probe on {name}")
    resources_spec = container.get("resources", {})
    require(all(resources_spec.get(group, {}).get(item)
                for group in ("requests", "limits") for item in ("cpu", "memory")),
            f"Missing CPU/memory request or limit on {name}")
    if name in apps:
        require(container["image"] == f"order-support/{name}:local", f"Unexpected image on {name}")
        require(pod["spec"].get("imagePullSecrets") == [{"name": "registry-pull"}],
                f"Missing image pull Secret on {name}")
        require(pod["spec"].get("securityContext", {}).get("runAsNonRoot") is True,
                f"{name} must run nonroot")
    if name != "frontend" and name in apps:
        secret_env = {entry["name"]: entry.get("valueFrom", {}).get("secretKeyRef", {})
                      for entry in container.get("env", [])}
        for variable, key in (("JWT_SECRET", "JWT_SECRET"),
                              ("SPRING_DATASOURCE_PASSWORD", "POSTGRES_PASSWORD"),
                              ("RABBITMQ_PASSWORD", "RABBITMQ_PASSWORD")):
            require(secret_env.get(variable) == {"name": "order-support-secrets", "key": key},
                    f"{name} does not reference {variable} from Secret")

for name in ("postgres", "rabbitmq"):
    claims = resources[("StatefulSet", name)]["spec"].get("volumeClaimTemplates", [])
    require(claims and claims[0]["spec"]["resources"]["requests"].get("storage"),
            f"Missing persistent volume claim for {name}")

require(("NetworkPolicy", "default-deny-ingress") in resources, "Missing default deny ingress")
for name in (*apps, *dependencies):
    require(("NetworkPolicy", f"allow-{name}-ingress") in resources,
            f"Missing ingress rule for {name}")
ingress = resources.get(("Ingress", "order-support"))
require(ingress is not None, "Missing frontend Ingress")
require(ingress["spec"]["rules"][0]["http"]["paths"][0]["backend"]["service"]["name"] == "frontend",
        "Ingress must target the frontend proxy")
print(f"Validated {len(objects)} rendered Kubernetes objects, probes, resources, Secrets, and selectors")
