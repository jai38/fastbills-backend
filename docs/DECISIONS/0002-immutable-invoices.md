# ADR 0002: Immutable Issued Invoices

## Status
Accepted

## Context
Under Indian GST rules and standard accounting ethics, an issued tax invoice is a legal document. Modifying an issued invoice after it has been delivered creates tax discrepancies, audit failures, and potential fraud.

## Decision
1. Once an invoice transitions from `DRAFT` to `ISSUED`, all fields on the `invoice` and its associated `invoice_line` records become strictly immutable.
2. Invoices in `ISSUED` state may never be deleted.
3. Corrections must occur solely by:
   - Issuing a `credit_note` referencing the original invoice, or
   - Cancelling the invoice (permitted only if no active receipts exist, and requiring Owner authorization and a stated reason).
4. Immutability is enforced at the domain service layer and verified by automated integration tests.

## Consequences
- Trust and audit compliance are guaranteed.
- Financial reports remain stable over time.
