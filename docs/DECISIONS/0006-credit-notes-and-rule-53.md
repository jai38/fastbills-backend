# ADR 0006: Credit Notes & Rule 53 Compliance

## Status
Accepted

## Context
Under Section 34 of the Central Goods and Services Tax (CGST) Act and Rule 53 of the CGST Rules, a supplier who has issued a tax invoice for supply of goods or services may issue a credit note to the recipient when:
1. The taxable value or tax charged in that tax invoice exceeds the taxable value or tax payable.
2. The goods supplied are returned by the recipient.
3. The goods or services or both supplied are found to be deficient.
4. Post-issuance discounts or price corrections are agreed upon.

Furthermore, Rule 53 requires credit notes to carry distinct consecutive serial numbers unique for a financial year (max 16 characters), reference the original invoice number and date, state the reason for issuance, and provide tax and line-item breakdowns.

## Decision
1. **Lifecycle & Preconditions:**
   - Credit notes may only be issued against invoices in `ISSUED` state. Draft or cancelled invoices cannot be credited.
   - Cumulative credit limit: The sum of active (`ISSUED`) credit notes issued against an invoice must not exceed the original invoice `grand_total`. Over-crediting is blocked with `422 Unprocessable Entity`.
2. **Tax Engine & Line Items:**
   - Credit note lines reuse the pure `GstTaxEngine` logic. Taxes (CGST, SGST, IGST, cess) and line roundings (`HALF_UP`) are computed using the original supply type and place of supply.
3. **Numbering Series:**
   - Credit notes utilize a dedicated sequence series (`CRN/2026-27/0001`) with row-level locking (`SELECT ... FOR UPDATE`), strictly complying with the Rule 53 16-character alphanumeric limit.
4. **Cancellation:**
   - Cancelled credit notes do not restore sequence numbers. Cancellation requires an explicit statutory reason and reverses the credit against the original invoice.
5. **PDF Rendering:**
   - Credit note PDFs are rendered server-side via OpenHTMLtoPDF using `credit-note-template.html`, featuring embedded TrueType `FreeSans` font with Unicode Rupee symbol (`₹`) support.

## Consequences
- Full statutory compliance with Section 34 of CGST Act and Rule 53.
- Complete auditability and financial integrity without risk of over-crediting or calculation drift.
