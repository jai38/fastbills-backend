# FastBills Architecture

This document describes the architectural patterns, package layout, boundaries, and guarantees of the `fastbills-api` service.

## Core Architectural Guarantees

1. **Strict Multitenancy by Default**
   - Every business table possesses an `organisation_id` foreign key.
   - Multitenancy is enforced using Hibernate 6 `@TenantId` coupled with a thread-scoped `TenantContext`.
   - Security filters populate `TenantContext` upon JWT validation.
   - Cross-tenant data access is blocked at the ORM and query level.

2. **Server-Owned Arithmetic**
   - All financial and tax arithmetic is performed in a pure Java module (`com.fastbills.api.tax`).
   - The tax package contains zero Spring, Hibernate, or database dependencies.
   - Client-submitted totals, tax splits, or taxable values are rejected.

3. **Immutable Issued Invoices**
   - Once transitioning from `DRAFT` to `ISSUED`, invoice rows and invoice line rows cannot be edited or deleted.
   - Enforced at both the service layer and via database constraints.

4. **Derived Payment Status**
   - No table contains an `is_paid` or payment status column.
   - Payment status is derived dynamically by aggregating non-reversed receipts against the invoice's frozen grand total.

## Package Responsibilities

Root package: `com.fastbills.api`

- **`config/`**: Spring application configuration, including:
  - `JacksonConfig`: Serializes `BigDecimal` as JSON strings and configures UTC `Instant`/`LocalDate`.
  - `OpenApiConfig`: SpringDoc OpenAPI 3 configuration for client code generation.
  - `SecurityConfig`: Stateless JWT authentication filter and password encoder (BCrypt).
  - `ClockConfig`: Injectable `Clock` bean set to UTC/Asia-Kolkata.
- **`common/`**:
  - `error/`: Uniform `ApiError` envelope and `GlobalExceptionHandler`.
  - `tenant/`: `TenantContext` holding the authenticated caller's `organisationId`.
  - `money/`: Big decimal utilities and Indian number formatting helpers.
- **`auth/`**: Registration (organization + owner user), login, JWT generation, refresh tokens.
- **`organisation/`**: Profile management, bank accounts, logo upload.
- **`user/`**: Staff user creation and management within an organization.
- **`customer/`**: Customer management with soft archival and GSTIN consistency checks.
- **`numbering/`**: Sequential invoice and credit note series allocation with pessimistic database locks.
- **`invoice/`**: Draft invoice creation, line management, issue lifecycle, cancellation, and queries.
- **`tax/`**: Pure tax calculation engine adhering strictly to Indian GST laws.
- **`receipt/`**: Receipt recording, receipt reversals, and dynamic payment status derivation.
- **`creditnote/`**: Credit note numbering, line items, and invoice linkage.
- **`pdf/`**: Server-side HTML template rendering using OpenHTMLtoPDF.
- **`report/`**: Statutory reports (Invoice register, Revenue summary, Tax slab summary, Ageing, Receipts register).
- **`audit/`**: Append-only audit logger capturing before/after states of all financial transactions.

## Transaction and Error Boundaries

- Service layer methods use `@Transactional` to demarcate business unit-of-work boundaries.
- Number allocation (`SELECT ... FOR UPDATE`) is executed within the issuing transaction.
- Exceptions thrown in services map to structured `ApiError` responses with stable error codes and human-readable messages.
