# ADR 0003: Derived Payment Status

## Status
Accepted

## Context
Indian B2B trade frequently involves multiple part-payments, cheques that bounce or clear late, and cross-reconciliations. A static `is_paid` boolean or `payment_status` enum column on an invoice fails when partial payments occur or receipts are reversed, leading to state desynchronization.

## Decision
1. Invoices contain no `status`, `payment_status`, or `is_paid` columns in the database schema.
2. Money received is tracked exclusively as discrete records in the `receipt` table.
3. Payment status is derived on demand by summing all non-reversed receipts against the invoice's frozen `grand_total`:
   - `received == 0 && today <= due_date` -> `UNPAID`
   - `received == 0 && today > due_date` -> `OVERDUE`
   - `0 < received < grand_total && today <= due_date` -> `PARTIALLY_PAID`
   - `0 < received < grand_total && today > due_date` -> `PARTIALLY_PAID_OVERDUE`
   - `received >= grand_total` -> `PAID`
   - `state == CANCELLED` -> `CANCELLED`

## Consequences
- Partial payments and receipt reversals are handled naturally without data corruption.
- Balances and statuses are mathematically guaranteed to be accurate and consistent with the receipt ledger.
