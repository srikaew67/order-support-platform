# Simulated incident: duplicate notification delivery

**Status:** Reproduced in automated verification; this is a documented failure scenario, not a claimed production outage.

## Symptom and impact

RabbitMQ delivered the same `OrderCreated` event twice after a consumer acknowledgment was interrupted. Without a deduplication boundary, the customer could see two notifications for one order. Business state in `order-service` remained correct, but duplicate customer messages would reduce trust and complicate support investigation.

## Timeline to reproduce

1. Create an order. `order-service` commits its order and outbox row, then publishes the event with a stable `event_id` and correlation ID.
2. Deliver the event once to `notification-service`, then deliver the identical payload again, representing broker redelivery before an acknowledgment is recorded.
3. Query notifications for the customer and inspect the `event_id` unique constraint. The duplicate-event integration test in `NotificationProcessorIT` verifies that only one record exists.

The complete Compose smoke verifies the first delivery through the frontend API. The duplicate case is covered by the notification-service integration test and listener tests; the smoke script does not inject a broker duplicate.

## Root cause and recovery

RabbitMQ provides at-least-once delivery in this setup. A consumer can persist a notification and lose its acknowledgment, or a publisher can retry an event whose confirmation was lost. Therefore a second delivery is expected behavior, not a broker defect. The dangerous assumption would be treating delivery as exactly once.

`notification-service` parses and validates the versioned event, then uses `event_id` as an idempotency key. A unique database constraint prevents a second row. Its listener checks a concurrent duplicate-key collision in a fresh transaction and acknowledges a verified duplicate; unrelated persistence failures use bounded retries and then dead-lettering. The order and ticket outboxes keep a stable event ID during replay. No manual deletion is needed for a verified duplicate.

## Prevention and detection

- Preserve the event ID when replaying an outbox row; generate it once when the business transaction commits.
- Keep the unique notification `event_id` index and the duplicate integration tests in CI.
- Trace delivery failures with the response/event correlation ID and listener failure logs. Inspect the primary and dead-letter RabbitMQ queues when notifications lag. Correlation-aware HTTP access logging across the APIs is a remaining observability improvement.
- Alert on persistent pending outbox rows, dead-letter growth and failed consumer retries in a production deployment. Those alerts are recommended operational work; this repository does not provision a monitoring stack.
- If a message is dead-lettered, inspect its payload, version and exception before replaying it. Replaying a valid event is safe under the idempotency constraint; malformed events need correction at their source.

The same reliability boundary matters during a RabbitMQ outage: the API transaction commits an outbox row, the dispatcher leaves it pending, and scheduled replay sends it after the broker recovers. Redis outage follows a different path: product reads fall back to PostgreSQL. Both paths have focused tests in the repository.
