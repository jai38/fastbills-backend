package com.fastbills.api.receipt;

import com.fastbills.api.auth.AuthenticatedUser;
import com.fastbills.api.receipt.dto.CreateReceiptRequest;
import com.fastbills.api.receipt.dto.ReceiptFilter;
import com.fastbills.api.receipt.dto.ReceiptResponse;
import com.fastbills.api.receipt.dto.ReverseReceiptRequest;
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
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Receipts", description = "Manual payment recording, reversals, and receipts register")
public class ReceiptController {

    private final ReceiptService receiptService;

    public ReceiptController(ReceiptService receiptService) {
        this.receiptService = receiptService;
    }

    @PostMapping("/invoices/{invoiceId}/receipts")
    @Operation(summary = "Record receipt", description = "Records a payment receipt against an issued invoice")
    public ResponseEntity<ReceiptResponse> recordReceipt(
            @PathVariable UUID invoiceId,
            @Valid @RequestBody CreateReceiptRequest request,
            @AuthenticationPrincipal AuthenticatedUser user,
            HttpServletRequest servletRequest
    ) {
        ReceiptResponse response = receiptService.recordReceipt(
                user.organisationId(),
                invoiceId,
                request,
                user.userId(),
                servletRequest.getRemoteAddr()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/invoices/{invoiceId}/receipts")
    @Operation(summary = "Get invoice receipts", description = "Retrieves all receipts for a specific invoice ordered chronologically")
    public ResponseEntity<List<ReceiptResponse>> getInvoiceReceipts(
            @PathVariable UUID invoiceId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        List<ReceiptResponse> responses = receiptService.getReceiptsForInvoice(user.organisationId(), invoiceId);
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/receipts/{receiptId}/reverse")
    @Operation(summary = "Reverse receipt", description = "Reverses a recorded receipt with a mandatory reason and creates linked reversal entry")
    public ResponseEntity<ReceiptResponse> reverseReceipt(
            @PathVariable UUID receiptId,
            @Valid @RequestBody ReverseReceiptRequest request,
            @AuthenticationPrincipal AuthenticatedUser user,
            HttpServletRequest servletRequest
    ) {
        ReceiptResponse response = receiptService.reverseReceipt(
                user.organisationId(),
                receiptId,
                request,
                user.userId(),
                servletRequest.getRemoteAddr()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/receipts")
    @Operation(summary = "List receipts register", description = "Paginated receipts register with date range, method, and status filtering")
    public ResponseEntity<Page<ReceiptResponse>> listReceipts(
            @RequestParam(required = false) UUID invoiceId,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(required = false) Boolean isReversed,
            @PageableDefault(size = 50, sort = "paymentDate", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        ReceiptFilter filter = new ReceiptFilter(invoiceId, customerId, fromDate, toDate, paymentMethod, isReversed);
        Page<ReceiptResponse> page = receiptService.listReceipts(user.organisationId(), filter, pageable);
        return ResponseEntity.ok(page);
    }
}
