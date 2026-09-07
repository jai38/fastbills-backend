# ADR 0004: Tenant Isolation Strategy

## Status
Accepted

## Context
FastBills is a multi-tenant application where different businesses store proprietary invoices, customer records, and tax filings. Cross-tenant data leakage is unacceptable. We need a strategy that guarantees data isolation without excessive infrastructure overhead.

## Options Considered
1. **Database-per-tenant:** Maximum physical isolation, but high cost, complex connection pooling, and expensive migrations across hundreds of small businesses.
2. **Schema-per-tenant:** Logical separation in PostgreSQL schemas. Requires running Flyway migrations on every tenant schema, creating maintenance friction.
3. **Row-Level Security (PostgreSQL RLS) with session variables:** Strong database-level filter, but requires setting session context on pooled HikariCP connections per checkout/transaction, with risks if statements execute outside transactional boundaries.
4. **Shared Database with Hibernate `@TenantId` and Scoped Context:** Every business table carries `organisation_id`. Hibernate 6 automatically injects tenant filters on all entity queries and automatically sets `organisation_id` on persist. A security interceptor populates `TenantContext` from JWT claims.

## Decision
Adopt **Shared Database with Hibernate `@TenantId` and `TenantContext`**, reinforced by:
- Non-nullable foreign keys `organisation_id REFERENCES organisation(id)` on every tenant entity.
- Automated cross-tenant integration tests that authenticate as Organisation A and assert that requests accessing Organisation B's resources fail with 404/403.

## Consequences
- Single database with standard Flyway migrations.
- Complete ORM-enforced tenant filtering without requiring developers to manually write `WHERE organisation_id = ...`.
- Clean testability and rapid deployment.
