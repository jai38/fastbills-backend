# Operating Manual: fastbills-api

Authoritative business rules are in `docs/DOMAIN.md`. Never deviate from them.

## Non-Negotiable Invariants

1. **An issued invoice is immutable.** Once leaving `DRAFT`, no field may ever update and the row may never be deleted. Corrections require a credit note or cancellation.
2. **Payment status is derived, never stored.** No `status` or `is_paid` column. Receipts are rows. Status is derived by summing active receipts against `grand_total`.
3. **Every query is tenant scoped.** Every business table carries `organisation_id`. Never read or write without scoping to the authenticated caller's tenant.
4. **Money is never a floating-point number.** In PostgreSQL: `NUMERIC(15,2)`. In Java: `BigDecimal` constructed from String or `BigDecimal.valueOf`. Quantities: `NUMERIC(15,3)`. Tax rates: `NUMERIC(5,2)`. JSON serialization: formatted as strings. Rounding mode: explicit `RoundingMode.HALF_UP`.
5. **Invoice numbers come from a database-enforced series.** Sequence allocation happens inside the issuing transaction using row-level locking (`SELECT ... FOR UPDATE`).
6. **Every money-mutating action writes an append-only audit row.** Actor, timestamp, entity, before and after JSON. Never modified, never deleted.

## Build and Test Commands

```bash
# Build project
./mvnw clean compile

# Run all tests (requires Docker for Testcontainers PostgreSQL)
./mvnw clean test

# Run single test
./mvnw test -Dtest=FastbillsApiApplicationTests

# Run local application
./mvnw spring-boot:run
```

## Package Layout (`com.fastbills.api`)

Organized by feature, never technical layer:
- `config/`: Security, Jackson, OpenAPI, storage, clock.
- `common/`: Standard error envelope, money helpers, audit types, tenant context, pagination.
- `auth/`: Registration, login, token issue/refresh, password reset.
- `organisation/`: Profile, logo upload, bank details, invoice prefix.
- `user/`: Staff management within an organisation.
- `customer/`: Customer CRUD and archival.
- `numbering/`: Number series, row locking allocation, FY calculation.
- `invoice/`: Draft editing, issue, cancel, query, ageing.
- `tax/`: GST computation. Pure Java, zero Spring/database dependencies.
- `receipt/`: Receipt entry, reversal, derived payment status.
- `creditnote/`: Credit note series, issue, lines.
- `pdf/`: Template rendering and storage.
- `report/`: Five statutory/business reports and CSV/XLSX export.
- `audit/`: Append-only audit logger.

## Money and Tax Rules

- Java: Always `BigDecimal`. Explicit `RoundingMode.HALF_UP` on every scale operation.
- Server recomputes all monetary values from `quantity`, `unit_price`, `discount_percent`/`discount_amount`, and `tax_rate_percent`.
- Reject client-submitted `supply_type`, `taxable_value`, tax amounts, or `grand_total`.

## Database Migrations (Flyway)

- Append-only in `src/main/resources/db/migration/`.
- Never edit or delete an existing migration file. Fix forward with a new migration.
- Always include rollback/reversible considerations.

## What Requires Stopping to Ask

- Any feature on the out-of-scope list in `DOMAIN.md` section 1.
- Anything marked `// VERIFY:` from `DOMAIN.md` section 11.
- Proposed changes to the six non-negotiable invariants.

## Mistakes to Avoid

- Never use `double` or `float` anywhere.
- Never compare customer's billing state instead of place of supply.
- Never drop leading zero on state codes (e.g. `'01'`).
- Never allow an issued invoice to be edited.
- Never reuse a cancelled invoice number.
- Never restart series at 1 for onboarding customers with existing series.
- Never roll financial year on current date instead of invoice date.
- Never store payment status as a column.
- Never delete a customer who has invoices (use archival).
- Never forget tenant scoping on any query.
