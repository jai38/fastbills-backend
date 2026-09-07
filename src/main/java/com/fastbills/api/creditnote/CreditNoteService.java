package com.fastbills.api.creditnote;

import com.fastbills.api.customer.Customer;
import com.fastbills.api.customer.CustomerRepository;
import com.fastbills.api.creditnote.dto.*;
import com.fastbills.api.invoice.*;
import com.fastbills.api.organisation.Organisation;
import com.fastbills.api.organisation.OrganisationRepository;
import com.fastbills.api.series.AllocatedNumber;
import com.fastbills.api.series.DocumentType;
import com.fastbills.api.series.NumberSeriesService;
import com.fastbills.api.tax.*;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class CreditNoteService {

    private final CreditNoteRepository creditNoteRepository;
    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final OrganisationRepository organisationRepository;
    private final NumberSeriesService numberSeriesService;
    private final AuditLogRepository auditLogRepository;
    private final com.fastbills.api.creditnote.pdf.CreditNotePdfService creditNotePdfService;
    private final com.fastbills.api.storage.StorageService storageService;

    public CreditNoteService(
            CreditNoteRepository creditNoteRepository,
            InvoiceRepository invoiceRepository,
            CustomerRepository customerRepository,
            OrganisationRepository organisationRepository,
            NumberSeriesService numberSeriesService,
            AuditLogRepository auditLogRepository,
            com.fastbills.api.creditnote.pdf.CreditNotePdfService creditNotePdfService,
            com.fastbills.api.storage.StorageService storageService
    ) {
        this.creditNoteRepository = creditNoteRepository;
        this.invoiceRepository = invoiceRepository;
        this.customerRepository = customerRepository;
        this.organisationRepository = organisationRepository;
        this.numberSeriesService = numberSeriesService;
        this.auditLogRepository = auditLogRepository;
        this.creditNotePdfService = creditNotePdfService;
        this.storageService = storageService;
    }

    /**
     * Issues a GST Credit Note against an issued invoice per Section 34 & Rule 53.
     */
    public CreditNoteResponse createCreditNote(
            UUID organisationId,
            UUID invoiceId,
            CreateCreditNoteRequest request,
            UUID userId,
            String ipAddress
    ) {
        Invoice invoice = invoiceRepository.findByIdAndOrganisationIdWithLines(invoiceId, organisationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));

        if (invoice.getState() != InvoiceState.ISSUED) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Credit notes can only be issued against ISSUED invoices. Current invoice state: " + invoice.getState());
        }

        if (request.creditNoteDate().isBefore(invoice.getInvoiceDate())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Credit note date (" + request.creditNoteDate() + ") cannot be earlier than the original invoice date (" + invoice.getInvoiceDate() + ")");
        }

        Customer customer = customerRepository.findByIdAndOrganisationId(invoice.getCustomerId(), organisationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));

        // 1. Map input lines to TaxLineInput for calculation
        List<TaxLineInput> calcInputs = new ArrayList<>();
        int lineCounter = 1;
        for (CreateCreditNoteRequest.CreditNoteLineRequest lineReq : request.lines()) {
            TaxCategory category = lineReq.taxCategory() != null ? lineReq.taxCategory() : TaxCategory.TAXABLE;
            BigDecimal taxRate = lineReq.taxRatePercent() != null ? lineReq.taxRatePercent() : BigDecimal.ZERO;
            BigDecimal cessRate = lineReq.cessRatePercent() != null ? lineReq.cessRatePercent() : BigDecimal.ZERO;

            calcInputs.add(new TaxLineInput(
                    lineCounter++,
                    lineReq.itemDescription(),
                    lineReq.hsnSacCode(),
                    false,
                    lineReq.quantity(),
                    "NOS",
                    lineReq.unitPrice(),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    taxRate,
                    cessRate,
                    null,
                    category
            ));
        }

        Organisation org = organisationRepository.findById(organisationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organisation not found"));

        // 2. Perform exact tax calculation using pure tax engine
        TaxInvoiceCalculation calc = GstTaxEngine.calculateInvoice(
                org.getStateCode() != null ? org.getStateCode() : "29",
                invoice.getPlaceOfSupplyStateCode() != null ? invoice.getPlaceOfSupplyStateCode() : "29",
                org.getGstin() != null && !org.getGstin().isBlank(),
                false,
                calcInputs
        );

        // 3. Verify credit note grand total does not exceed uncredited amount of original invoice
        BigDecimal existingCredited = creditNoteRepository.sumActiveAmountByInvoiceId(organisationId, invoice.getId());
        if (existingCredited == null) {
            existingCredited = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal proposedGrandTotal = calc.grandTotal();
        BigDecimal remainingCreditAvailable = invoice.getGrandTotal().subtract(existingCredited);

        if (proposedGrandTotal.compareTo(remainingCreditAvailable) > 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, String.format(
                    "Credit note grand total (₹ %s) exceeds remaining credit available on invoice (₹ %s). Invoice total: ₹ %s, already credited: ₹ %s.",
                    proposedGrandTotal.toPlainString(),
                    remainingCreditAvailable.toPlainString(),
                    invoice.getGrandTotal().toPlainString(),
                    existingCredited.toPlainString()
            ));
        }

        // 4. Allocate number series via pessimistic row lock
        AllocatedNumber allocated = numberSeriesService.allocateNextNumber(
                organisationId,
                DocumentType.CREDIT_NOTE,
                request.creditNoteDate(),
                "CN"
        );

        // 5. Build CreditNote entity
        CreditNote cn = new CreditNote();
        cn.setOrganisationId(organisationId);
        cn.setOriginalInvoice(invoice);
        cn.setCreditNoteNumber(allocated.formattedNumber());
        cn.setFinancialYear(allocated.financialYear());
        cn.setCreditNoteDate(request.creditNoteDate());
        cn.setReason(request.reason());
        cn.setReasonNotes(request.reasonNotes());
        cn.setTaxableValue(calc.taxableValue());
        cn.setTotalCgst(calc.totalCgst());
        cn.setTotalSgst(calc.totalSgst());
        cn.setTotalIgst(calc.totalIgst());
        cn.setTotalCess(calc.totalCess());
        cn.setTotalTax(calc.totalTax());
        cn.setRoundOff(calc.roundOff());
        cn.setGrandTotal(calc.grandTotal());
        cn.setState(CreditNoteState.ISSUED);
        cn.setCreatedByUserId(userId);
        cn.setCreatedAt(Instant.now());
        cn.setUpdatedAt(Instant.now());

        // 6. Build lines
        for (int i = 0; i < request.lines().size(); i++) {
            CreateCreditNoteRequest.CreditNoteLineRequest lineReq = request.lines().get(i);
            TaxLineCalculation calculated = calc.lines().get(i);

            BigDecimal lineTax = calculated.cgstAmount()
                    .add(calculated.sgstAmount())
                    .add(calculated.igstAmount())
                    .add(calculated.cessAmount());

            CreditNoteLine line = new CreditNoteLine();
            line.setOriginalInvoiceLineId(lineReq.originalInvoiceLineId());
            line.setItemDescription(lineReq.itemDescription());
            line.setHsnSacCode(lineReq.hsnSacCode());
            line.setQuantity(lineReq.quantity());
            line.setUnitPrice(lineReq.unitPrice());
            line.setTaxableValue(calculated.taxableValue());
            line.setTaxCategory(calculated.taxCategory());
            line.setTaxRatePercent(calculated.taxRatePercent());
            line.setCgstRatePercent(calculated.cgstRatePercent());
            line.setCgstAmount(calculated.cgstAmount());
            line.setSgstRatePercent(calculated.sgstRatePercent());
            line.setSgstAmount(calculated.sgstAmount());
            line.setIgstRatePercent(calculated.igstRatePercent());
            line.setIgstAmount(calculated.igstAmount());
            line.setCessRatePercent(lineReq.cessRatePercent() != null ? lineReq.cessRatePercent() : BigDecimal.ZERO);
            line.setCessAmount(calculated.cessAmount());
            line.setTotalTaxAmount(lineTax);
            line.setLineTotal(calculated.lineTotal());
            line.setSortOrder(i);

            cn.addLine(line);
        }

        CreditNote saved = creditNoteRepository.save(cn);

        // 7. Audit log
        auditLogRepository.save(new AuditLog(
                organisationId,
                userId,
                "ISSUED",
                "CREDIT_NOTE",
                saved.getId(),
                null,
                "Issued credit note " + saved.getCreditNoteNumber() + " against invoice " + invoice.getInvoiceNumber() + " for ₹ " + saved.getGrandTotal(),
                ipAddress
        ));

        return toResponse(saved, customer);
    }

    /**
     * Cancels an issued credit note (reversing its impact). Sequence number is not reused.
     */
    public CreditNoteResponse cancelCreditNote(
            UUID organisationId,
            UUID creditNoteId,
            CancelCreditNoteRequest request,
            UUID userId,
            String ipAddress
    ) {
        CreditNote cn = creditNoteRepository.findByIdAndOrganisationIdWithLines(creditNoteId, organisationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Credit note not found"));

        if (cn.getState() == CreditNoteState.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Credit note is already cancelled");
        }

        cn.setState(CreditNoteState.CANCELLED);
        cn.setCancellationReason(request.reason());
        cn.setCancelledAt(Instant.now());
        cn.setCancelledByUserId(userId);
        cn.setUpdatedAt(Instant.now());

        CreditNote saved = creditNoteRepository.save(cn);

        auditLogRepository.save(new AuditLog(
                organisationId,
                userId,
                "CANCELLED",
                "CREDIT_NOTE",
                saved.getId(),
                "ISSUED",
                "Cancelled credit note: " + request.reason(),
                ipAddress
        ));

        Customer customer = customerRepository.findByIdAndOrganisationId(saved.getOriginalInvoice().getCustomerId(), organisationId)
                .orElse(null);

        return toResponse(saved, customer);
    }

    @Transactional(readOnly = true)
    public CreditNoteResponse getCreditNote(UUID organisationId, UUID id) {
        CreditNote cn = creditNoteRepository.findByIdAndOrganisationIdWithLines(id, organisationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Credit note not found"));

        Customer customer = customerRepository.findByIdAndOrganisationId(cn.getOriginalInvoice().getCustomerId(), organisationId)
                .orElse(null);

        return toResponse(cn, customer);
    }

    @Transactional(readOnly = true)
    public List<CreditNoteResponse> getInvoiceCreditNotes(UUID organisationId, UUID invoiceId) {
        List<CreditNote> creditNotes = creditNoteRepository.findByOrganisationIdAndOriginalInvoiceIdOrderByCreatedAtDesc(
                organisationId, invoiceId
        );

        if (creditNotes.isEmpty()) {
            return Collections.emptyList();
        }

        UUID customerId = creditNotes.get(0).getOriginalInvoice().getCustomerId();
        Customer customer = customerRepository.findByIdAndOrganisationId(customerId, organisationId).orElse(null);

        return creditNotes.stream().map(cn -> toResponse(cn, customer)).toList();
    }

    @Transactional(readOnly = true)
    public Page<CreditNoteSummaryResponse> listCreditNotes(
            UUID organisationId,
            CreditNoteFilter filter,
            Pageable pageable
    ) {
        Specification<CreditNote> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organisationId"), organisationId));

            if (filter.state() != null) {
                predicates.add(cb.equal(root.get("state"), filter.state()));
            }
            if (filter.originalInvoiceId() != null) {
                predicates.add(cb.equal(root.get("originalInvoice").get("id"), filter.originalInvoiceId()));
            }
            if (filter.customerId() != null) {
                predicates.add(cb.equal(root.get("originalInvoice").get("customerId"), filter.customerId()));
            }
            if (filter.fromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("creditNoteDate"), filter.fromDate()));
            }
            if (filter.toDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("creditNoteDate"), filter.toDate()));
            }
            if (filter.search() != null && !filter.search().trim().isEmpty()) {
                String term = "%" + filter.search().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("creditNoteNumber")), term),
                        cb.like(cb.lower(root.get("originalInvoice").get("invoiceNumber")), term)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<CreditNote> page = creditNoteRepository.findAll(spec, pageable);

        Set<UUID> customerIds = page.getContent().stream()
                .map(cn -> cn.getOriginalInvoice().getCustomerId())
                .collect(Collectors.toSet());

        Map<UUID, Customer> customerMap = customerRepository.findAllById(customerIds).stream()
                .collect(Collectors.toMap(Customer::getId, c -> c));

        return page.map(cn -> {
            Customer cust = customerMap.get(cn.getOriginalInvoice().getCustomerId());
            String customerName = cust != null ? cust.getLegalName() : "Unknown Customer";

            return new CreditNoteSummaryResponse(
                    cn.getId(),
                    cn.getCreditNoteNumber(),
                    cn.getCreditNoteDate(),
                    cn.getReason(),
                    cn.getReason().getDescription(),
                    cn.getOriginalInvoice().getId(),
                    cn.getOriginalInvoice().getInvoiceNumber(),
                    cn.getOriginalInvoice().getCustomerId(),
                    customerName,
                    cn.getTaxableValue(),
                    cn.getTotalTax(),
                    cn.getGrandTotal(),
                    cn.getState(),
                    cn.getCreatedAt()
            );
        });
    }

    private CreditNoteResponse toResponse(CreditNote cn, Customer customer) {
        String customerName = customer != null ? customer.getLegalName() : "Unknown Customer";
        String customerGstin = customer != null && customer.getGstin() != null ? customer.getGstin() : "-";

        List<CreditNoteResponse.CreditNoteLineResponse> lineResponses = cn.getLines().stream().map(l ->
                new CreditNoteResponse.CreditNoteLineResponse(
                        l.getId(),
                        l.getOriginalInvoiceLineId(),
                        l.getItemDescription(),
                        l.getHsnSacCode(),
                        l.getQuantity(),
                        l.getUnitPrice(),
                        l.getTaxableValue(),
                        l.getTaxCategory(),
                        l.getTaxRatePercent(),
                        l.getCgstRatePercent(),
                        l.getCgstAmount(),
                        l.getSgstRatePercent(),
                        l.getSgstAmount(),
                        l.getIgstRatePercent(),
                        l.getIgstAmount(),
                        l.getCessRatePercent(),
                        l.getCessAmount(),
                        l.getTotalTaxAmount(),
                        l.getLineTotal(),
                        l.getSortOrder()
                )
        ).toList();

        return new CreditNoteResponse(
                cn.getId(),
                cn.getCreditNoteNumber(),
                cn.getFinancialYear(),
                cn.getCreditNoteDate(),
                cn.getReason(),
                cn.getReason().getDescription(),
                cn.getReasonNotes(),
                cn.getOriginalInvoice().getId(),
                cn.getOriginalInvoice().getInvoiceNumber(),
                cn.getOriginalInvoice().getInvoiceDate(),
                customer != null ? customer.getId() : cn.getOriginalInvoice().getCustomerId(),
                customerName,
                customerGstin,
                cn.getTaxableValue(),
                cn.getTotalCgst(),
                cn.getTotalSgst(),
                cn.getTotalIgst(),
                cn.getTotalCess(),
                cn.getTotalTax(),
                cn.getRoundOff(),
                cn.getGrandTotal(),
                cn.getState(),
                cn.getCancellationReason(),
                cn.getCancelledAt(),
                cn.getCreatedAt(),
                lineResponses
        );
    }

    @Transactional(readOnly = true)
    public CreditNotePdfResource getCreditNotePdf(UUID organisationId, UUID creditNoteId) {
        CreditNote cn = creditNoteRepository.findByIdAndOrganisationIdWithLines(creditNoteId, organisationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Credit note not found"));

        Organisation org = organisationRepository.findById(organisationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organisation not found"));

        Customer customer = customerRepository.findByIdAndOrganisationId(cn.getOriginalInvoice().getCustomerId(), organisationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));

        String safeNumber = cn.getCreditNoteNumber() != null
                ? cn.getCreditNoteNumber().replace('/', '_').replace('\\', '_')
                : "CN_" + cn.getId().toString().substring(0, 8);
        String filename = safeNumber + ".pdf";

        byte[] pdfBytes = creditNotePdfService.generatePdf(cn, org, customer);
        org.springframework.core.io.ByteArrayResource resource = new org.springframework.core.io.ByteArrayResource(pdfBytes);
        return new CreditNotePdfResource(resource, filename);
    }
}
