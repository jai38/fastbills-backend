# FastBills API Reference

The API serves JSON over HTTPS and adheres to REST conventions. OpenAPI 3 documentation is available at `/v3/api-docs` and Swagger UI at `/swagger-ui.html`.

## Standard Error Response Format

All API errors return a standard JSON envelope:

```json
{
  "code": "VALIDATION_FAILED",
  "message": "Input validation failed for the requested resource",
  "timestamp": "2026-09-07T04:25:00Z",
  "fieldErrors": [
    {
      "field": "gstin",
      "message": "GSTIN must be 15 alphanumeric characters matching state code"
    }
  ]
}
```

### Standard Error Codes
- `VALIDATION_FAILED` (400): Request body or parameter validation error.
- `AUTHENTICATION_FAILED` (401): Missing or invalid credentials/token.
- `ACCESS_DENIED` (403): User lacks permission for the requested action.
- `NOT_FOUND` (404): Resource not found within tenant scope.
- `INVOICE_IMMUTABLE` (409): Attempted modification of an issued invoice.
- `INVOICE_CANCEL_NOT_ALLOWED` (409): Invoices with active receipts cannot be cancelled.
- `INTERNAL_SERVER_ERROR` (500): Unexpected server failure.

## Endpoints

### Health Check
- **GET** `/actuator/health`
  - Response (200): `{"status": "UP"}`
- **GET** `/api/v1/health`
  - Response (200): `{"status": "UP", "version": "0.0.1-SNAPSHOT"}`

### Authentication (`/api/v1/auth`)

- **POST** `/api/v1/auth/register`
  - Creates a new tenant organisation and its initial `OWNER` user.
  - Request:
    ```json
    {
      "legalName": "Acme Fabrications Ltd",
      "tradeName": "Acme Fab",
      "email": "owner@acmefab.com",
      "password": "Password123!",
      "fullName": "Ramesh Kumar",
      "gstin": "27ABCDE1234F1Z5",
      "stateCode": "27",
      "pan": "ABCDE1234F"
    }
    ```
  - Response (201):
    ```json
    {
      "accessToken": "eyJhbGciOiJIUzI1NiIsIn...",
      "refreshToken": "d8e3b719-75c1-4...",
      "tokenType": "Bearer",
      "expiresInSeconds": 3600,
      "user": {
        "id": "c56a4180-65aa-42ec-a945-5fd21dec0538",
        "email": "owner@acmefab.com",
        "fullName": "Ramesh Kumar",
        "role": "OWNER"
      },
      "organisation": {
        "id": "076e7f6d-757a-4290-ac58-c9878430d18a",
        "legalName": "Acme Fabrications Ltd",
        "tradeName": "Acme Fab",
        "gstin": "27ABCDE1234F1Z5",
        "stateCode": "27"
      }
    }
    ```

- **POST** `/api/v1/auth/login`
  - Authenticates user credentials and issues access and refresh tokens.
  - Request: `{"email": "owner@acmefab.com", "password": "Password123!"}`
  - Response (200): Same shape as registration response.

- **POST** `/api/v1/auth/refresh`
  - Exchanges a valid refresh token for a new access token and rotated refresh token.
  - Request: `{"refreshToken": "d8e3b719-75c1-4..."}`
  - Response (200): Rotated `accessToken` and `refreshToken`.

- **GET** `/api/v1/auth/me`
  - Returns authenticated user and tenant organisation details.
  - Headers: `Authorization: Bearer <accessToken>`
  - Response (200): `{"userId": "...", "organisationId": "...", "email": "...", "fullName": "...", "role": "OWNER"}`

### Organisation (`/api/v1/organisation`)

- **GET** `/api/v1/organisation/profile`
  - Returns profile for the currently authenticated tenant.
  - Headers: `Authorization: Bearer <accessToken>`
  - Response (200): Full `Organisation` object.

- **PUT** `/api/v1/organisation/profile`
  - Updates organisation profile, bank details, and numbering configuration.
  - Request: Profile fields (`legalName`, `tradeName`, `pan`, `addressLine1`, `city`, `stateCode`, `bankAccountName`, `bankAccountNumber`, `bankIfsc`, etc.).
  - Response (200): Updated `Organisation` object.

- **POST** `/api/v1/organisation/logo`
  - Uploads tenant branding logo (PNG/JPEG, max 2MB).
  - Multi-part form data: `file`.
  - Response (200): `{"logoUrl": "/api/v1/organisation/logo/org-id.png"}`.

### Customers (`/api/v1/customers`)

- **GET** `/api/v1/customers`
  - Returns paginated list of customers for the authenticated tenant.
  - Query params: `page`, `size`, `search`, `includeArchived` (default: `false`).
  - Response (200): `PageResponse<CustomerResponse>`.

- **POST** `/api/v1/customers`
  - Creates a new customer with GSTIN validation and state code derivation.
  - Request: `legalName`, `tradeName`, `gstin`, `pan`, `billingAddress`, `shippingAddress`, `stateCode`, `creditDays`.
  - Response (201): `CustomerResponse`.

- **GET** `/api/v1/customers/{id}`
  - Retrieves customer by UUID.
  - Response (200): `CustomerResponse`.

- **PUT** `/api/v1/customers/{id}`
  - Updates customer profile and address information.
  - Response (200): `CustomerResponse`.

- **DELETE** `/api/v1/customers/{id}`
  - Soft-deletes / archives customer (preserves statutory invoice history per Rule 46).
  - Response (204): No content.

### Invoices (`/api/v1/invoices`)

- **GET** `/api/v1/invoices`
  - Lists invoices with multi-criteria filtering and server-side pagination.
  - Query params: `page`, `size`, `sort`, `state` (`DRAFT`, `ISSUED`, `CANCELLED`), `customerId`, `fromDate`, `toDate`, `search`.
  - Response (200): `PageResponse<InvoiceSummaryResponse>`.

- **POST** `/api/v1/invoices`
  - Creates a new DRAFT invoice.
  - Request: `customerId`, `invoiceDate`, `supplyType`, `placeOfSupply`, `lines` array.
  - Response (201): `InvoiceResponse`.

- **GET** `/api/v1/invoices/{id}`
  - Retrieves full invoice representation with line items and derived payment status.
  - Response (200): `InvoiceResponse`.

- **PUT** `/api/v1/invoices/{id}`
  - Updates draft invoice lines, customer, or metadata.
  - Blocked with `409 INVOICE_IMMUTABLE` if invoice state is `ISSUED` or `CANCELLED`.
  - Response (200): `InvoiceResponse`.

- **DELETE** `/api/v1/invoices/{id}`
  - Deletes a draft invoice. Blocked with `409` if issued.
  - Response (204): No content.

- **POST** `/api/v1/invoices/{id}/issue`
  - Issues invoice: consumes number series sequence (`SELECT ... FOR UPDATE`), recalculates and freezes tax amounts via `GstTaxEngine`, sets due date, computes derived payment status, and generates telephone collection reference.
  - Response (200): `InvoiceResponse` with state `ISSUED`.

- **POST** `/api/v1/invoices/{id}/cancel`
  - Cancels issued invoice (Owner only). Requires cancellation reason.
  - Blocked with `409 INVOICE_CANCEL_NOT_ALLOWED` if invoice has active receipts.
  - Response (200): `InvoiceResponse` with state `CANCELLED`.

- **GET** `/api/v1/invoices/{id}/pdf`
  - Streams Rule 46 compliant statutory invoice PDF (OpenHTMLtoPDF, UTF-8 Rupee symbol).
  - Headers: `Content-Type: application/pdf`, `Content-Disposition: inline; filename="INV-xxxx.pdf"`.
  - Response (200): PDF binary stream.

### Receipts & Payments (`/api/v1/receipts`)

- **POST** `/api/v1/invoices/{invoiceId}/receipts`
  - Records a payment receipt against an issued invoice.
  - Request: `amount`, `paymentDate`, `paymentMethod` (`CASH`, `UPI`, `NEFT`, `RTGS`, `IMPS`, `CHEQUE`, `CARD`), `referenceNumber`, `notes`.
  - Response (201): `ReceiptResponse`. Recomputes derived invoice payment status.

- **POST** `/api/v1/receipts/{receiptId}/reverse`
  - Reverses an active receipt, creating a linked reversing receipt.
  - Request: `reason`.
  - Response (200): Reversal `ReceiptResponse`. Re-evaluates invoice payment status.

### Credit Notes (`/api/v1/credit-notes` & `/api/v1/invoices/{id}/credit-notes`)

- **GET** `/api/v1/invoices/{invoiceId}/credit-notes`
  - Retrieves all credit notes issued against a specific invoice.
  - Response (200): `List<CreditNoteResponse>`.

- **POST** `/api/v1/invoices/{invoiceId}/credit-notes`
  - Issues a statutory credit note under Section 34 of the CGST Act & Rule 53.
  - Validates that total credited value does not exceed invoice grand total.
  - Request: `reason` (`DEFICIENCY_IN_SERVICE`, `CORRECTION_IN_INVOICE`, `POST_ISSUANCE_DISCOUNT`, `GOODS_RETURNED`), `lines` array (quantity, rate, taxable, tax).
  - Response (201): `CreditNoteResponse`.

- **GET** `/api/v1/credit-notes`
  - Lists tenant credit notes with search and status filtering (`ISSUED`, `CANCELLED`).
  - Response (200): `PageResponse<CreditNoteSummaryResponse>`.

- **GET** `/api/v1/credit-notes/{id}`
  - Retrieves full credit note details with line items.
  - Response (200): `CreditNoteResponse`.

- **POST** `/api/v1/credit-notes/{id}/cancel`
  - Cancels credit note, reversing the credited amount against original invoice.
  - Request: `reason`.
  - Response (200): Updated `CreditNoteResponse`.

- **GET** `/api/v1/credit-notes/{id}/pdf`
  - Streams Rule 53 compliant statutory credit note PDF.
  - Response (200): PDF binary stream.

### Reports (`/api/v1/reports`)

All reports support `?export=csv` (RFC 4180) and `?export=xlsx` (native styled Excel via Apache POI):
- **GET** `/api/v1/reports/invoice-register`: Full register of invoices with derived payment status and ageing buckets.
- **GET** `/api/v1/reports/revenue-summary`: Monthly and per-customer revenue breakdown excluding drafts and cancellations.
- **GET** `/api/v1/reports/tax-summary`: GSTR-1 rate slab tax breakdown (0%, 5%, 12%, 18%, 28%, Nil, Exempt).
- **GET** `/api/v1/reports/receivables-ageing`: Customer receivables distributed across 6 ageing buckets highlighting the 45-day MSME threshold.
- **GET** `/api/v1/reports/receipts-register`: Chronological receipt register with reversal audit trail.

### Executive Dashboard (`/api/v1/dashboard`)

- **GET** `/api/v1/dashboard/metrics`
  - Returns 5 core financial KPIs, 6-bucket ageing totals, and top 10 oldest unpaid invoices.
  - Response (200): `DashboardMetricsResponse`.


