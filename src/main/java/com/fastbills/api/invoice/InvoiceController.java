package com.fastbills.api.invoice;

import com.fastbills.api.auth.AuthenticatedUser;
import com.fastbills.api.invoice.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invoices")
@Tag(name = "Invoices", description = "Invoice lifecycle, Rule 46 GST compliance, and immutable ledger operations")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @PostMapping("/drafts")
    @Operation(summary = "Create draft invoice", description = "Creates a new editable draft invoice without consuming a number")
    public ResponseEntity<InvoiceResponse> createDraft(
            @Valid @RequestBody CreateDraftInvoiceRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        InvoiceResponse response = invoiceService.createDraft(user.organisationId(), user.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/drafts/{id}")
    @Operation(summary = "Update draft invoice", description = "Updates an editable draft invoice. Fails if already issued.")
    public ResponseEntity<InvoiceResponse> updateDraft(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDraftInvoiceRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        InvoiceResponse response = invoiceService.updateDraft(user.organisationId(), id, user.userId(), request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/drafts/{id}")
    @Operation(summary = "Delete draft invoice", description = "Permanently deletes a draft invoice. Fails if already issued.")
    public ResponseEntity<Void> deleteDraft(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        invoiceService.deleteDraft(user.organisationId(), id, user.userId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/issue")
    @Operation(summary = "Issue invoice", description = "Freezes lines and totals, consumes the next consecutive number, and makes invoice immutable")
    public ResponseEntity<InvoiceResponse> issueInvoice(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user,
            HttpServletRequest httpRequest
    ) {
        InvoiceResponse response = invoiceService.issueInvoice(
                user.organisationId(),
                id,
                user.userId(),
                httpRequest.getRemoteAddr()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel issued invoice", description = "Cancels an issued invoice with a required reason (Owner only). Sequence number is not reused.")
    public ResponseEntity<InvoiceResponse> cancelInvoice(
            @PathVariable UUID id,
            @Valid @RequestBody CancelInvoiceRequest request,
            @AuthenticationPrincipal AuthenticatedUser user,
            HttpServletRequest httpRequest
    ) {
        InvoiceResponse response = invoiceService.cancelInvoice(
                user.organisationId(),
                id,
                user.userId(),
                user.role().name(),
                request.reason(),
                httpRequest.getRemoteAddr()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get invoice details", description = "Retrieves full invoice details including lines and tax slab breakdown")
    public ResponseEntity<InvoiceResponse> getInvoice(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        InvoiceResponse response = invoiceService.getInvoice(user.organisationId(), id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/pdf")
    @Operation(summary = "Download invoice PDF", description = "Streams the Rule 46 GST compliant PDF for an invoice")
    public ResponseEntity<org.springframework.core.io.Resource> getInvoicePdf(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        InvoicePdfResource pdf = invoiceService.getInvoicePdf(user.organisationId(), id);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + pdf.filename() + "\"")
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(pdf.resource());
    }

    @GetMapping
    @Operation(summary = "List invoices", description = "Returns a paginated list of invoices with filters for state, customer, date range, and search query")
    public ResponseEntity<Page<InvoiceSummaryResponse>> listInvoices(
            @RequestParam(required = false) InvoiceState state,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 25, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        InvoiceFilter filter = new InvoiceFilter(state, customerId, fromDate, toDate, search);
        Page<InvoiceSummaryResponse> page = invoiceService.listInvoices(user.organisationId(), filter, pageable);
        return ResponseEntity.ok(page);
    }
}
