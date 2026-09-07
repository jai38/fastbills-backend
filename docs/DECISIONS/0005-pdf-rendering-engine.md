# 5. PDF Rendering Engine & Rupee Symbol Handling

Date: 2026-09-07

## Status

Accepted

## Context

Under Indian GST Rule 46, an issued tax invoice is an immutable legal instrument. Per `DOMAIN.md` Section 8:
- Invoices must be rendered server-side from an HTML template and stored in object storage keyed by invoice ID.
- The document must be generated exactly once at the moment of issue (`ISSUED` state) and served from storage thereafter so that downloads years later are byte-identical.
- The document must never be rendered in the browser (client environments vary in print stylesheet handling, fonts, and zoom levels).
- Standard PDFBox fonts (Helvetica, Times, Courier) only support WinAnsi (ISO-8859-1) encoding and lack the Indian Rupee symbol `₹` (Unicode U+20B9). Attempting to render `₹` with standard fonts causes unprintable character boxes or encoding crashes.

## Decision

1. **Engine Choice:** We choose **OpenHTMLtoPDF** (core + PDFBox) combined with **Thymeleaf** template engine.
   - OpenHTMLtoPDF translates modern HTML/CSS directly into clean, vectorized PDF documents.
   - Thymeleaf provides clean context separation and server-side template evaluation.
2. **Font & Rupee Symbol (`₹`):**
   - We embed a TrueType font containing the U+20B9 glyph (e.g. `Noto Sans` or Arial/FreeSans) registered into OpenHTMLtoPDF's `PdfRendererBuilder.useFont()`.
   - All monetary figures formatted with the Rupee symbol (`₹`) render crisply as vector glyphs without fallback artifacts.
3. **Storage & Immutability:**
   - On `issueInvoice()`, the PDF is generated and saved via `StorageService` under object key `invoices/{organisationId}/{invoiceId}.pdf`.
   - The `pdf_object_key` is frozen onto the `invoices` row.
   - Subsequent `GET /api/v1/invoices/{id}/pdf` requests stream directly from storage without re-rendering.
4. **Draft & Cancelled Documents:**
   - Draft invoices have no stored PDF.
   - Cancelled invoices retain their original issued PDF (or carry a prominent "CANCELLED" watermark/stamp).

## Consequences

- Byte stability is guaranteed across client operating systems, browsers, and mobile devices.
- No reliance on external headless browser binaries (e.g. Chromium / Puppeteer), keeping container images small (< 250MB), memory footprint low, and cold starts fast.
