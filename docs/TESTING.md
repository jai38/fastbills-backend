# FastBills Testing Strategy

Testing ensures adherence to non-negotiable invariants, mathematical accuracy of tax calculations, and tenant data isolation.

## Test Categorization

1. **Unit Tests (Fast, no Spring context)**
   - Located in package `com.fastbills.api.tax`.
   - Pure arithmetic tests verifying GST intra-state and inter-state calculations, rate slabs, and half-up rounding.
   - Asserted to the exact paise against Worked Examples A and B in `DOMAIN.md`.

2. **Integration Tests (Spring Context + Testcontainers)**
   - Run against a live PostgreSQL 16 container via Testcontainers.
   - Never use in-memory databases (like H2), because we rely on PostgreSQL-specific `NUMERIC` semantics, triggers, and `SELECT ... FOR UPDATE` row locking.
   - Verifies Flyway schema execution, repository persistence, and API controllers.

3. **Specialized Test Suites**
   - **Cross-Tenant Test Suite:** Authenticates as Organisation A and verifies that all read/update/delete operations against Organisation B's resources return 404 or 403.
   - **Concurrency Number Allocation Test:** Spawns concurrent threads attempting to issue invoices simultaneously and asserts that generated invoice numbers are strictly sequential, gapless, and duplicate-free.

## Running Tests

```bash
# Run all tests (requires Docker daemon for Testcontainers)
./mvnw clean test

# Run pure tax tests (does not require Docker)
./mvnw test -Dtest=GstTaxEngineTest
```
