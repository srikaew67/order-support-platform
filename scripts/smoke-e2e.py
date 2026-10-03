#!/usr/bin/env python3
"""Exercise the public API through the Compose frontend proxy."""

import argparse
import json
import time
import urllib.error
import urllib.request
import uuid


def request(base, method, path, body=None, token=None, correlation=None, expected=200):
    headers = {"Accept": "application/json"}
    if body is not None:
        headers["Content-Type"] = "application/json"
    if token:
        headers["Authorization"] = f"Bearer {token}"
    if correlation:
        headers["X-Correlation-ID"] = correlation
    data = json.dumps(body).encode() if body is not None else None
    call = urllib.request.Request(base + path, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(call, timeout=10) as response:
            raw = response.read()
            status = response.status
    except urllib.error.HTTPError as error:
        raw = error.read()
        status = error.code
    if status != expected:
        raise AssertionError(f"{method} {path}: expected {expected}, got {status}: {raw[:500]!r}")
    return json.loads(raw) if raw else None


def main():
    args = argparse.ArgumentParser()
    args.add_argument("--base-url", required=True)
    args.add_argument("--product-id", required=True)
    options = args.parse_args()
    base = options.base_url.rstrip("/")
    with urllib.request.urlopen(base + "/products", timeout=10) as response:
        assert response.status == 200 and b"<app-root" in response.read()
    print("Angular page and route fallback passed", flush=True)
    run_id = uuid.uuid4().hex[:12]
    email = f"smoke-{run_id}@example.test"
    password = f"Smoke-{run_id}-pass"

    registered = request(base, "POST", "/api/orders/api/v1/auth/register",
                         {"email": email, "password": password, "displayName": "Smoke Customer"},
                         expected=201)
    assert registered["role"] == "CUSTOMER"
    logged_in = request(base, "POST", "/api/orders/api/v1/auth/login",
                        {"email": email, "password": password})
    token = logged_in["accessToken"]
    assert token
    print("Customer registration and login passed", flush=True)

    catalog = request(base, "GET", "/api/orders/api/v1/products")
    assert any(product["id"] == options.product_id for product in catalog["content"])
    product = request(base, "GET", f"/api/orders/api/v1/products/{options.product_id}")
    assert product["stockQuantity"] >= 1
    print("Product listing and detail passed", flush=True)

    correlation = f"smoke-{run_id}"
    order = request(base, "POST", "/api/orders/api/v1/orders",
                    {"items": [{"productId": options.product_id, "quantity": 1}]},
                    token=token, correlation=correlation, expected=201)
    assert order["status"] == "PENDING" and order["items"][0]["productId"] == options.product_id
    order_id = order["id"]
    assert request(base, "GET", f"/api/orders/api/v1/orders/{order_id}", token=token)["id"] == order_id
    print("Order creation and detail passed", flush=True)

    ticket = request(base, "POST", "/api/support/api/v1/tickets",
                     {"orderId": order_id, "subject": "Smoke support request",
                      "description": "End-to-end smoke ticket"}, token=token, expected=201)
    assert ticket["orderId"] == order_id and ticket["status"] == "OPEN"
    ticket_id = ticket["id"]
    assert request(base, "GET", f"/api/support/api/v1/tickets/{ticket_id}", token=token)["id"] == ticket_id
    assert any(item["id"] == ticket_id for item in request(
        base, "GET", "/api/support/api/v1/tickets", token=token)["content"])
    print("Support ticket creation, detail, and list passed", flush=True)

    deadline = time.monotonic() + 60
    while time.monotonic() < deadline:
        notifications = request(base, "GET", "/api/notifications/api/v1/notifications", token=token)
        matching = [item for item in notifications["content"]
                    if item["eventType"] == "OrderCreated" and item["subjectId"] == order_id]
        if matching:
            assert matching[0]["correlationId"] == correlation
            detail = request(base, "GET", "/api/notifications/api/v1/notifications/"
                             + matching[0]["id"], token=token)
            assert detail["eventId"] == matching[0]["eventId"]
            print("RabbitMQ order event delivered to the customer notification API", flush=True)
            return
        time.sleep(2)
    raise AssertionError("OrderCreated notification was not delivered within 60 seconds")


if __name__ == "__main__":
    main()
