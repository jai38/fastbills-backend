package com.fastbills.api.creditnote;

import com.fastbills.api.auth.AuthenticatedUser;
import com.fastbills.api.creditnote.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Credit Notes", description = "Credit Note lifecycle, Section 34 & Rule 53 GST compliance")
public class CreditNoteController {

    private final CreditNoteService creditNoteService;

    public CreditNoteController(CreditNoteService creditNoteService) {
        this.creditNoteService = creditNoteService;
    }

    @PostMapping("/invoices/{invoiceId}/credit-notes")
    @Operation(summary = "Issue credit note against invoice", description = "Issues a statutory GST Credit Note against an ISSUED invoice with Rule 53 numbering and tax recalculation")
    public ResponseEntity<CreditNoteResponse> createCreditNote(
            @PathVariable UUID invoiceId,
            @Valid @RequestBody CreateCreditNoteRequest request,
            @AuthenticationPrincipal AuthenticatedUser user,
            HttpServletRequest httpRequest
    ) {
        CreditNoteResponse response = creditNoteService.createCreditNote(
                user.organisationId(),
                invoiceId,
                request,
                user.userId(),
                httpRequest.getRemoteAddr()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/invoices/{invoiceId}/credit-notes")
    @Operation(summary = "Get invoice credit notes", description = "Retrieves all credit notes issued against a specific invoice")
    public ResponseEntity<List<CreditNoteResponse>> getInvoiceCreditNotes(
            @PathVariable UUID invoiceId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        List<CreditNoteResponse> response = creditNoteService.getInvoiceCreditNotes(user.organisationId(), invoiceId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/credit-notes")
    @Operation(summary = "List credit notes", description = "Returns a paginated list of credit notes with filtering options")
    public ResponseEntity<Page<CreditNoteSummaryResponse>> listCreditNotes(
            @RequestParam(required = false) CreditNoteState state,
            @RequestParam(required = false) UUID originalInvoiceId,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 25, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        CreditNoteFilter filter = new CreditNoteFilter(state, originalInvoiceId, customerId, fromDate, toDate, search);
        Page<CreditNoteSummaryResponse> page = creditNoteService.listCreditNotes(user.organisationId(), filter, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/credit-notes/{id}")
    @Operation(summary = "Get credit note details", description = "Retrieves full credit note details including line items")
    public ResponseEntity<CreditNoteResponse> getCreditNote(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        CreditNoteResponse response = creditNoteService.getCreditNote(user.organisationId(), id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/credit-notes/{id}/pdf")
    @Operation(summary = "Download credit note PDF", description = "Streams the Rule 53 GST compliant PDF for a credit note")
    public ResponseEntity<org.springframework.core.io.Resource> getCreditNotePdf(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        CreditNotePdfResource pdf = creditNoteService.getCreditNotePdf(user.organisationId(), id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + pdf.filename() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf.resource());
    }

    @PostMapping("/credit-notes/{id}/cancel")
    @Operation(summary = "Cancel credit note", description = "Cancels an issued credit note with a required reason")
    public ResponseEntity<CreditNoteResponse> cancelCreditNote(
            @PathVariable UUID id,
            @Valid @RequestBody CancelCreditNoteRequest request,
            @AuthenticationPrincipal AuthenticatedUser user,
            HttpServletRequest httpRequest
    ) {
        CreditNoteResponse response = creditNoteService.cancelCreditNote(
                user.organisationId(),
                id,
                request,
                user.userId(),
                httpRequest.getRemoteAddr()
        );
        return ResponseEntity.ok(response);
    }
}
