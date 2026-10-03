#!/usr/bin/env python3
"""Read-only smoke checks for a deployed frontend and its API proxies."""

import argparse
import json
import urllib.request


def get(base, path):
    with urllib.request.urlopen(base + path, timeout=20) as response:
        assert response.status == 200, f"{path}: HTTP {response.status}"
        return response.read()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", required=True)
    base = parser.parse_args().base_url.rstrip("/")
    assert b"<app-root" in get(base, "/products")
    for prefix in ("orders", "support", "notifications"):
        health = json.loads(get(base, f"/api/{prefix}/actuator/health"))
        assert health["status"] == "UP", f"{prefix} health is {health}"
    catalog = json.loads(get(base, "/api/orders/api/v1/products"))
    assert isinstance(catalog["content"], list)
    print("Deployed frontend, three API health endpoints, and catalog passed")


if __name__ == "__main__":
    main()
