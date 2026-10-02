# Upgrading databases with legacy ticket data

Order-service Flyway V1 created `public.support_tickets` and `public.ticket_comments`. Task 5 moved active tickets to the service-owned `support` schema. Order-service V5 removes the old public tables only when both are empty and no other table has a foreign key to them. If they contain rows, V5 stops startup without deleting them.

1. Back up the database and stop applications that write the public ticket tables.
2. Start support-service so Flyway creates `support.support_tickets`, `support.ticket_comments`, and `support.ticket_outbox`. It can start while the public tables still contain rows.
3. Inspect the legacy data before copying it. The new ticket table has a 200-character subject and 4,000-character description; its allowed statuses are `OPEN`, `IN_PROGRESS`, `RESOLVED`, and `CLOSED`. Check any longer text or other statuses, and decide how to map them. Confirm each `assigned_to` ID identifies a current SUPPORT user. Preserve ticket and comment UUIDs, customer and order IDs, timestamps, and comment authors.
4. Copy ticket rows into `support.support_tickets`, mapping `assigned_to` to `assignee_id`; then copy comments into `support.ticket_comments`, mapping `message` to `body`. The legacy `ticket_number` and `priority` fields have no target columns, so retain them in an export if needed. Verify row counts and representative records in both schemas.
5. After verifying the copy, delete the old comments and tickets in a transaction. Restart order-service; V5 will remove the now-empty public tables. Keep the backup until the customer and agent ticket screens have been checked.

Useful prechecks:

```sql
SELECT count(*) FROM public.support_tickets;
SELECT count(*) FROM public.ticket_comments;
SELECT status, count(*) FROM public.support_tickets GROUP BY status;
SELECT max(length(subject)), max(length(description)) FROM public.support_tickets;
SELECT max(length(message)) FROM public.ticket_comments;
```

Do not edit the old V1 migration or Flyway history to bypass the guard.
