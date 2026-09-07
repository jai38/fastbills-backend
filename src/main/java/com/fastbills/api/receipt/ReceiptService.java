package com.fastbills.api.receipt;

import com.fastbills.api.customer.Customer;
import com.fastbills.api.customer.CustomerRepository;
import com.fastbills.api.invoice.AuditLog;
import com.fastbills.api.invoice.AuditLogRepository;
import com.fastbills.api.invoice.Invoice;
import com.fastbills.api.invoice.InvoiceRepository;
import com.fastbills.api.invoice.InvoiceState;
import com.fastbills.api.receipt.dto.CreateReceiptRequest;
import com.fastbills.api.receipt.dto.ReceiptFilter;
import com.fastbills.api.receipt.dto.ReceiptResponse;
import com.fastbills.api.receipt.dto.ReverseReceiptRequest;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
public class ReceiptService {

    private final ReceiptRepository receiptRepository;
    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final AuditLogRepository auditLogRepository;

    public ReceiptService(
            ReceiptRepository receiptRepository,
            InvoiceRepository invoiceRepository,
            CustomerRepository customerRepository,
            AuditLogRepository auditLogRepository
    ) {
        this.receiptRepository = receiptRepository;
        this.invoiceRepository = invoiceRepository;
        this.customerRepository = customerRepository;
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Records a payment receipt against an issued invoice.
     */
    @Transactional
    public ReceiptResponse recordReceipt(
            UUID organisationId,
            UUID invoiceId,
            CreateReceiptRequest request,
            UUID actorUserId,
            String clientIp
    ) {
        Invoice invoice = invoiceRepository.findByIdAndOrganisationId(invoiceId, organisationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));

        if (invoice.getState() == InvoiceState.DRAFT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot record receipts against draft invoices");
        }
        if (invoice.getState() == InvoiceState.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot record receipts against cancelled invoices");
        }

        BigDecimal activeReceived = receiptRepository.sumActiveAmountByInvoiceId(organisationId, invoiceId);
        BigDecimal grandTotal = invoice.getGrandTotal() != null ? invoice.getGrandTotal() : BigDecimal.ZERO;
        BigDecimal remainingBalance = grandTotal.subtract(activeReceived);

        if (remainingBalance.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invoice is already fully paid");
        }

        if (request.amount().compareTo(remainingBalance) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Receipt amount (" + request.amount() + ") exceeds outstanding balance (" + remainingBalance + ")");
        }

        Receipt receipt = new Receipt(
                organisationId,
                invoiceId,
                invoice.getCustomerId(),
                request.amount(),
                request.paymentDate(),
                request.paymentMethod(),
                request.referenceNumber(),
                request.notes(),
                actorUserId
        );

        Receipt saved = receiptRepository.save(receipt);

        auditLogRepository.save(new AuditLog(
                organisationId,
                actorUserId,
                "RECEIPT_RECORDED",
                "RECEIPT",
                saved.getId(),
                null,
                "{\"amount\":\"" + saved.getAmount() + "\",\"method\":\"" + saved.getPaymentMethod() + "\",\"invoiceId\":\"" + invoiceId + "\"}",
                clientIp
        ));

        Customer customer = customerRepository.findByIdAndOrganisationId(invoice.getCustomerId(), organisationId)
                .orElse(null);
        String customerLegalName = customer != null ? customer.getLegalName() : "";

        return toReceiptResponse(saved, invoice.getInvoiceNumber(), customerLegalName);
    }

    /**
     * Reverses a receipt, creating a linked reversing receipt record and updating the audit log.
     */
    @Transactional
    public ReceiptResponse reverseReceipt(
            UUID organisationId,
            UUID receiptId,
            ReverseReceiptRequest request,
            UUID actorUserId,
            String clientIp
    ) {
        Receipt receipt = receiptRepository.findByIdAndOrganisationId(receiptId, organisationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Receipt not found"));

        if (Boolean.TRUE.equals(receipt.getIsReversed())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Receipt is already reversed");
        }

        Instant now = Instant.now();

        // 1. Create linked reversing row
        Receipt reversalReceipt = new Receipt(
                organisationId,
                receipt.getInvoiceId(),
                receipt.getCustomerId(),
                receipt.getAmount(),
                receipt.getPaymentDate(),
                receipt.getPaymentMethod(),
                receipt.getReferenceNumber(),
                "Reversal of receipt " + receipt.getId() + ": " + request.reason(),
                actorUserId
        );
        reversalReceipt.setIsReversed(true);
        reversalReceipt.setReversalReason(request.reason());
        reversalReceipt.setReversedAt(now);
        reversalReceipt.setReversedByUserId(actorUserId);
        reversalReceipt.setReversedByReceiptId(receipt.getId());

        Receipt savedReversal = receiptRepository.save(reversalReceipt);

        // 2. Mark original receipt as reversed and link to reversing row
        receipt.setIsReversed(true);
        receipt.setReversalReason(request.reason());
        receipt.setReversedAt(now);
        receipt.setReversedByUserId(actorUserId);
        receipt.setReversedByReceiptId(savedReversal.getId());

        Receipt savedOriginal = receiptRepository.save(receipt);

        auditLogRepository.save(new AuditLog(
                organisationId,
                actorUserId,
                "RECEIPT_REVERSED",
                "RECEIPT",
                savedOriginal.getId(),
                "{\"isReversed\":false}",
                "{\"isReversed\":true,\"reason\":\"" + request.reason() + "\",\"reversingReceiptId\":\"" + savedReversal.getId() + "\"}",
                clientIp
        ));

        Invoice invoice = invoiceRepository.findByIdAndOrganisationId(savedOriginal.getInvoiceId(), organisationId).orElse(null);
        String invoiceNumber = invoice != null ? invoice.getInvoiceNumber() : "";
        Customer customer = invoice != null ? customerRepository.findByIdAndOrganisationId(invoice.getCustomerId(), organisationId).orElse(null) : null;
        String customerLegalName = customer != null ? customer.getLegalName() : "";

        return toReceiptResponse(savedOriginal, invoiceNumber, customerLegalName);
    }

    /**
     * Lists receipts for a specific invoice in chronological order.
     */
    @Transactional(readOnly = true)
    public List<ReceiptResponse> getReceiptsForInvoice(UUID organisationId, UUID invoiceId) {
        Invoice invoice = invoiceRepository.findByIdAndOrganisationId(invoiceId, organisationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));

        Customer customer = customerRepository.findByIdAndOrganisationId(invoice.getCustomerId(), organisationId)
                .orElse(null);
        String customerLegalName = customer != null ? customer.getLegalName() : "";

        List<Receipt> receipts = receiptRepository.findByOrganisationIdAndInvoiceIdOrderByPaymentDateAscCreatedAtAsc(
                organisationId,
                invoiceId
        );

        return receipts.stream()
                .map(r -> toReceiptResponse(r, invoice.getInvoiceNumber(), customerLegalName))
                .toList();
    }

    /**
     * Paginated receipts register across all invoices with filters.
     */
    @Transactional(readOnly = true)
    public Page<ReceiptResponse> listReceipts(UUID organisationId, ReceiptFilter filter, Pageable pageable) {
        Specification<Receipt> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organisationId"), organisationId));

            if (filter != null) {
                if (filter.invoiceId() != null) {
                    predicates.add(cb.equal(root.get("invoiceId"), filter.invoiceId()));
                }
                if (filter.customerId() != null) {
                    predicates.add(cb.equal(root.get("customerId"), filter.customerId()));
                }
                if (filter.fromDate() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("paymentDate"), filter.fromDate()));
                }
                if (filter.toDate() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("paymentDate"), filter.toDate()));
                }
                if (filter.paymentMethod() != null) {
                    predicates.add(cb.equal(root.get("paymentMethod"), filter.paymentMethod()));
                }
                if (filter.isReversed() != null) {
                    predicates.add(cb.equal(root.get("isReversed"), filter.isReversed()));
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Receipt> page = receiptRepository.findAll(spec, pageable);

        // Fetch invoice numbers and customer names in batches
        Set<UUID> invoiceIds = new HashSet<>();
        Set<UUID> customerIds = new HashSet<>();
        for (Receipt r : page.getContent()) {
            invoiceIds.add(r.getInvoiceId());
            customerIds.add(r.getCustomerId());
        }

        Map<UUID, String> invoiceNumbers = new HashMap<>();
        if (!invoiceIds.isEmpty()) {
            invoiceRepository.findAllById(invoiceIds).forEach(inv -> invoiceNumbers.put(inv.getId(), inv.getInvoiceNumber()));
        }

        Map<UUID, String> customerNames = new HashMap<>();
        if (!customerIds.isEmpty()) {
            customerRepository.findAllById(customerIds).forEach(c -> customerNames.put(c.getId(), c.getLegalName()));
        }

        return page.map(r -> toReceiptResponse(
                r,
                invoiceNumbers.getOrDefault(r.getInvoiceId(), ""),
                customerNames.getOrDefault(r.getCustomerId(), "")
        ));
    }

    /**
     * Gets sum of active receipts for an invoice.
     */
    @Transactional(readOnly = true)
    public BigDecimal getActiveReceiptsSum(UUID organisationId, UUID invoiceId) {
        return receiptRepository.sumActiveAmountByInvoiceId(organisationId, invoiceId);
    }

    /**
     * Batch gets sum of active receipts for multiple invoices.
     */
    @Transactional(readOnly = true)
    public Map<UUID, BigDecimal> getActiveReceiptsMap(UUID organisationId, Collection<UUID> invoiceIds) {
        if (invoiceIds == null || invoiceIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Object[]> rows = receiptRepository.sumActiveAmountsByInvoiceIds(organisationId, invoiceIds);
        Map<UUID, BigDecimal> result = new HashMap<>();
        for (Object[] row : rows) {
            UUID invId = (UUID) row[0];
            BigDecimal sum = (BigDecimal) row[1];
            result.put(invId, sum);
        }
        return result;
    }

    private ReceiptResponse toReceiptResponse(Receipt r, String invoiceNumber, String customerLegalName) {
        return new ReceiptResponse(
                r.getId(),
                r.getOrganisationId(),
                r.getInvoiceId(),
                invoiceNumber,
                r.getCustomerId(),
                customerLegalName,
                r.getAmount(),
                r.getPaymentDate(),
                r.getPaymentMethod(),
                r.getReferenceNumber(),
                r.getNotes(),
                r.getIsReversed(),
                r.getReversalReason(),
                r.getReversedAt(),
                r.getReversedByUserId(),
                r.getReversedByReceiptId(),
                r.getCreatedByUserId(),
                r.getCreatedAt()
        );
    }
}
