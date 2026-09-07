package com.fastbills.api.invoice;

import com.fastbills.api.customer.Customer;
import com.fastbills.api.customer.CustomerRepository;
import com.fastbills.api.invoice.dto.*;
import com.fastbills.api.organisation.Organisation;
import com.fastbills.api.organisation.OrganisationRepository;
import com.fastbills.api.series.AllocatedNumber;
import com.fastbills.api.series.DocumentType;
import com.fastbills.api.series.FinancialYear;
import com.fastbills.api.series.NumberSeriesService;
import com.fastbills.api.receipt.AgeingBucket;
import com.fastbills.api.receipt.ReceiptRepository;
import com.fastbills.api.tax.*;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fastbills.api.invoice.pdf.InvoicePdfService;
import com.fastbills.api.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InvoiceService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceService.class);

    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final OrganisationRepository organisationRepository;
    private final NumberSeriesService numberSeriesService;
    private final AuditLogRepository auditLogRepository;
    private final ReceiptRepository receiptRepository;
    private final InvoicePdfService invoicePdfService;
    private final StorageService storageService;

    public InvoiceService(
            InvoiceRepository invoiceRepository,
            CustomerRepository customerRepository,
            OrganisationRepository organisationRepository,
            NumberSeriesService numberSeriesService,
            AuditLogRepository auditLogRepository,
            ReceiptRepository receiptRepository,
            InvoicePdfService invoicePdfService,
            StorageService storageService
    ) {
        this.invoiceRepository = invoiceRepository;
        this.customerRepository = customerRepository;
        this.organisationRepository = organisationRepository;
        this.numberSeriesService = numberSeriesService;
        this.auditLogRepository = auditLogRepository;
        this.receiptRepository = receiptRepository;
        this.invoicePdfService = invoicePdfService;
        this.storageService = storageService;
    }

    /**
     * Creates a new draft invoice.
     */
    @Transactional
    public InvoiceResponse createDraft(UUID organisationId, UUID actorUserId, CreateDraftInvoiceRequest request) {
        Organisation org = getOrganisation(organisationId);
        Customer customer = getCustomer(organisationId, request.customerId());

        if (Boolean.TRUE.equals(customer.getIsArchived())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot create an invoice for an archived customer");
        }

        Invoice invoice = new Invoice();
        invoice.setOrganisationId(organisationId);
        invoice.setCustomerId(customer.getId());
        invoice.setState(InvoiceState.DRAFT);
        invoice.setInvoiceDate(request.invoiceDate());
        invoice.setDeliveryOrAcceptanceDate(request.deliveryOrAcceptanceDate());

        // Derived credit days
        int creditDays = request.agreedCreditDays() != null
                ? request.agreedCreditDays()
                : (customer.getCreditDays() != null ? customer.getCreditDays() : org.getDefaultCreditDays());
        invoice.setAgreedCreditDays(creditDays);

        // Derived place of supply
        String pos = request.placeOfSupplyStateCode() != null && !request.placeOfSupplyStateCode().trim().isEmpty()
                ? request.placeOfSupplyStateCode().trim()
                : (customer.getDefaultPlaceOfSupplyStateCode() != null ? customer.getDefaultPlaceOfSupplyStateCode() : customer.getBillingStateCode());
        invoice.setPlaceOfSupplyStateCode(pos);

        // Compute supply type
        SupplyType supplyType = GstTaxEngine.determineSupplyType(org.getStateCode(), pos);
        invoice.setSupplyType(supplyType);

        invoice.setIsReverseCharge(Boolean.TRUE.equals(request.isReverseCharge()));
        invoice.setPoNumber(request.poNumber());
        invoice.setPoDate(request.poDate());
        invoice.setNotes(request.notes());
        invoice.setTermsText(request.termsText() != null ? request.termsText() : org.getDefaultTermsText());

        // Calculate due date
        LocalDate refDate = invoice.getDeliveryOrAcceptanceDate() != null
                ? invoice.getDeliveryOrAcceptanceDate()
                : invoice.getInvoiceDate();
        invoice.setDueDate(refDate.plusDays(creditDays));

        // Process lines and calculate totals
        boolean isSupplierRegistered = org.getGstin() != null && !org.getGstin().trim().isEmpty();
        processAndCalculateLines(invoice, request.lines(), org.getStateCode(), pos, isSupplierRegistered);

        Invoice saved = invoiceRepository.save(invoice);
        auditLogRepository.save(new AuditLog(
                organisationId,
                actorUserId,
                "INVOICE_DRAFT_CREATED",
                "INVOICE",
                saved.getId(),
                null,
                "{\"state\":\"DRAFT\",\"customerId\":\"" + customer.getId() + "\"}",
                null
        ));

        return toInvoiceResponse(saved, customer, org);
    }

    /**
     * Updates an existing draft invoice.
     */
    @Transactional
    public InvoiceResponse updateDraft(UUID organisationId, UUID invoiceId, UUID actorUserId, UpdateDraftInvoiceRequest request) {
        Invoice invoice = invoiceRepository.findByIdAndOrganisationIdWithLines(invoiceId, organisationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));

        if (invoice.getState() != InvoiceState.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot modify an invoice in state: " + invoice.getState() + ". Issued invoices are immutable.");
        }

        Organisation org = getOrganisation(organisationId);
        Customer customer = getCustomer(organisationId, request.customerId());

        if (Boolean.TRUE.equals(customer.getIsArchived())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot update invoice with an archived customer");
        }

        invoice.setCustomerId(customer.getId());
        invoice.setInvoiceDate(request.invoiceDate());
        invoice.setDeliveryOrAcceptanceDate(request.deliveryOrAcceptanceDate());

        int creditDays = request.agreedCreditDays() != null
                ? request.agreedCreditDays()
                : (customer.getCreditDays() != null ? customer.getCreditDays() : org.getDefaultCreditDays());
        invoice.setAgreedCreditDays(creditDays);

        String pos = request.placeOfSupplyStateCode() != null && !request.placeOfSupplyStateCode().trim().isEmpty()
                ? request.placeOfSupplyStateCode().trim()
                : (customer.getDefaultPlaceOfSupplyStateCode() != null ? customer.getDefaultPlaceOfSupplyStateCode() : customer.getBillingStateCode());
        invoice.setPlaceOfSupplyStateCode(pos);

        SupplyType supplyType = GstTaxEngine.determineSupplyType(org.getStateCode(), pos);
        invoice.setSupplyType(supplyType);

        invoice.setIsReverseCharge(Boolean.TRUE.equals(request.isReverseCharge()));
        invoice.setPoNumber(request.poNumber());
        invoice.setPoDate(request.poDate());
        invoice.setNotes(request.notes());
        invoice.setTermsText(request.termsText() != null ? request.termsText() : org.getDefaultTermsText());

        LocalDate refDate = invoice.getDeliveryOrAcceptanceDate() != null
                ? invoice.getDeliveryOrAcceptanceDate()
                : invoice.getInvoiceDate();
        invoice.setDueDate(refDate.plusDays(creditDays));

        invoice.clearLines();
        boolean isSupplierRegistered = org.getGstin() != null && !org.getGstin().trim().isEmpty();
        processAndCalculateLines(invoice, request.lines(), org.getStateCode(), pos, isSupplierRegistered);

        Invoice saved = invoiceRepository.save(invoice);
        auditLogRepository.save(new AuditLog(
                organisationId,
                actorUserId,
                "INVOICE_DRAFT_UPDATED",
                "INVOICE",
                saved.getId(),
                null,
                "{\"state\":\"DRAFT\"}",
                null
        ));

        return toInvoiceResponse(saved, customer, org);
    }

    /**
     * Deletes a draft invoice.
     */
    @Transactional
    public void deleteDraft(UUID organisationId, UUID invoiceId, UUID actorUserId) {
        Invoice invoice = invoiceRepository.findByIdAndOrganisationId(invoiceId, organisationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));

        if (invoice.getState() != InvoiceState.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only draft invoices can be deleted. An issued invoice must be cancelled.");
        }

        invoiceRepository.delete(invoice);
        auditLogRepository.save(new AuditLog(
                organisationId,
                actorUserId,
                "INVOICE_DRAFT_DELETED",
                "INVOICE",
                invoiceId,
                "{\"state\":\"DRAFT\"}",
                null,
                null
        ));
    }

    /**
     * Issues an invoice: freezes totals, allocates invoice number atomically, derives due date & collection reference.
     */
    @Transactional
    public InvoiceResponse issueInvoice(UUID organisationId, UUID invoiceId, UUID actorUserId, String clientIp) {
        Invoice invoice = invoiceRepository.findByIdAndOrganisationIdWithLines(invoiceId, organisationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));

        if (invoice.getState() == InvoiceState.ISSUED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invoice is already issued");
        }
        if (invoice.getState() == InvoiceState.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot issue a cancelled invoice");
        }
        if (invoice.getLines().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one line item is required to issue an invoice");
        }

        Organisation org = getOrganisation(organisationId);
        Customer customer = getCustomer(organisationId, invoice.getCustomerId());

        if (Boolean.TRUE.equals(customer.getIsArchived())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot issue an invoice for an archived customer");
        }

        // Recompute and freeze all lines and totals via pure tax engine
        boolean isSupplierRegistered = org.getGstin() != null && !org.getGstin().trim().isEmpty();
        List<TaxLineInput> taxInputs = new ArrayList<>();
        for (InvoiceLine line : invoice.getLines()) {
            taxInputs.add(new TaxLineInput(
                    line.getLineNumber(),
                    line.getItemDescription(),
                    line.getHsnOrSacCode(),
                    Boolean.TRUE.equals(line.getIsService()),
                    line.getQuantity(),
                    line.getUnitOfMeasure(),
                    line.getUnitPrice(),
                    line.getDiscountPercent(),
                    line.getDiscountAmount(),
                    line.getTaxRatePercent(),
                    line.getCessRatePercent(),
                    line.getCessAmount(),
                    line.getTaxCategory()
            ));
        }

        TaxInvoiceCalculation calc = GstTaxEngine.calculateInvoice(
                org.getStateCode(),
                invoice.getPlaceOfSupplyStateCode(),
                isSupplierRegistered,
                invoice.getIsReverseCharge(),
                taxInputs
        );

        // Apply calculated line totals
        for (int i = 0; i < invoice.getLines().size(); i++) {
            InvoiceLine line = invoice.getLines().get(i);
            TaxLineCalculation lineCalc = calc.lines().get(i);

            line.setGrossAmount(lineCalc.grossAmount());
            line.setDiscountAmount(lineCalc.discountAmount());
            line.setTaxableValue(lineCalc.taxableValue());
            line.setCgstRatePercent(lineCalc.cgstRatePercent());
            line.setCgstAmount(lineCalc.cgstAmount());
            line.setSgstRatePercent(lineCalc.sgstRatePercent());
            line.setSgstAmount(lineCalc.sgstAmount());
            line.setIgstRatePercent(lineCalc.igstRatePercent());
            line.setIgstAmount(lineCalc.igstAmount());
            line.setCessAmount(lineCalc.cessAmount());
            line.setLineTotal(lineCalc.lineTotal());
        }

        // Apply calculated invoice totals
        invoice.setSubtotalBeforeDiscount(calc.subtotalBeforeDiscount());
        invoice.setTotalDiscount(calc.totalDiscount());
        invoice.setTaxableValue(calc.taxableValue());
        invoice.setTotalCgst(calc.totalCgst());
        invoice.setTotalSgst(calc.totalSgst());
        invoice.setTotalIgst(calc.totalIgst());
        invoice.setTotalCess(calc.totalCess());
        invoice.setTotalTax(calc.totalTax());
        invoice.setRoundOff(calc.roundOff());
        invoice.setGrandTotal(calc.grandTotal());
        invoice.setAmountInWords(calc.amountInWords());

        // Allocate consecutive invoice number via SELECT ... FOR UPDATE
        AllocatedNumber allocated = numberSeriesService.allocateNextNumber(
                organisationId,
                DocumentType.INVOICE,
                invoice.getInvoiceDate(),
                org.getInvoicePrefix()
        );

        invoice.setInvoiceNumber(allocated.formattedNumber());
        invoice.setSeriesId(allocated.seriesId());
        invoice.setFinancialYear(allocated.financialYear());

        // Derive and freeze collection reference and due date
        invoice.setCollectionReference(CollectionReferenceGenerator.generate(allocated.sequenceNumber()));
        LocalDate refDate = invoice.getDeliveryOrAcceptanceDate() != null
                ? invoice.getDeliveryOrAcceptanceDate()
                : invoice.getInvoiceDate();
        invoice.setDueDate(refDate.plusDays(invoice.getAgreedCreditDays()));

        // Mark as ISSUED
        invoice.setState(InvoiceState.ISSUED);
        invoice.setIssuedAt(java.time.Instant.now());
        invoice.setIssuedByUserId(actorUserId);

        // Render and store immutable PDF once at issue (ADR 0005)
        try {
            String pdfKey = invoicePdfService.generateAndStorePdf(invoice, org, customer);
            invoice.setPdfObjectKey(pdfKey);
        } catch (Exception e) {
            log.error("Failed to generate and store PDF on issue for invoice: {}", invoice.getInvoiceNumber(), e);
            throw new RuntimeException("Could not generate invoice PDF: " + e.getMessage(), e);
        }

        Invoice saved = invoiceRepository.save(invoice);

        auditLogRepository.save(new AuditLog(
                organisationId,
                actorUserId,
                "INVOICE_ISSUED",
                "INVOICE",
                saved.getId(),
                "{\"state\":\"DRAFT\"}",
                "{\"state\":\"ISSUED\",\"invoiceNumber\":\"" + saved.getInvoiceNumber() + "\",\"grandTotal\":\"" + saved.getGrandTotal() + "\"}",
                clientIp
        ));

        return toInvoiceResponse(saved, customer, org);
    }

    /**
     * Cancels an issued invoice (Owner only).
     */
    @Transactional
    public InvoiceResponse cancelInvoice(
            UUID organisationId,
            UUID invoiceId,
            UUID actorUserId,
            String userRole,
            String reason,
            String clientIp
    ) {
        if (!"OWNER".equalsIgnoreCase(userRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only organisation owners may cancel an invoice");
        }

        Invoice invoice = invoiceRepository.findByIdAndOrganisationIdWithLines(invoiceId, organisationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));

        if (invoice.getState() == InvoiceState.DRAFT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Draft invoices cannot be cancelled; they can be deleted");
        }
        if (invoice.getState() == InvoiceState.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invoice is already cancelled");
        }

        if (receiptRepository.existsByOrganisationIdAndInvoiceIdAndIsReversedFalse(organisationId, invoiceId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "An invoice with any non-reversed receipt against it cannot be cancelled: reverse the receipts first, or issue a credit note instead.");
        }

        invoice.setState(InvoiceState.CANCELLED);
        invoice.setCancelledAt(java.time.Instant.now());
        invoice.setCancelledByUserId(actorUserId);
        invoice.setCancellationReason(reason);

        Invoice saved = invoiceRepository.save(invoice);
        Customer customer = getCustomer(organisationId, invoice.getCustomerId());
        Organisation org = getOrganisation(organisationId);

        auditLogRepository.save(new AuditLog(
                organisationId,
                actorUserId,
                "INVOICE_CANCELLED",
                "INVOICE",
                saved.getId(),
                "{\"state\":\"ISSUED\"}",
                "{\"state\":\"CANCELLED\",\"reason\":\"" + reason + "\"}",
                clientIp
        ));

        return toInvoiceResponse(saved, customer, org);
    }

    /**
     * Retrieves single invoice details.
     */
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(UUID organisationId, UUID invoiceId) {
        Invoice invoice = invoiceRepository.findByIdAndOrganisationIdWithLines(invoiceId, organisationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));
        Customer customer = getCustomer(organisationId, invoice.getCustomerId());
        Organisation org = getOrganisation(organisationId);
        return toInvoiceResponse(invoice, customer, org);
    }

    /**
     * Paginated and filtered search of invoices.
     */
    @Transactional(readOnly = true)
    public Page<InvoiceSummaryResponse> listInvoices(UUID organisationId, InvoiceFilter filter, Pageable pageable) {
        Specification<Invoice> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organisationId"), organisationId));

            if (filter != null) {
                if (filter.state() != null) {
                    predicates.add(cb.equal(root.get("state"), filter.state()));
                }
                if (filter.customerId() != null) {
                    predicates.add(cb.equal(root.get("customerId"), filter.customerId()));
                }
                if (filter.fromDate() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("invoiceDate"), filter.fromDate()));
                }
                if (filter.toDate() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("invoiceDate"), filter.toDate()));
                }
                if (filter.search() != null && !filter.search().trim().isEmpty()) {
                    String pattern = "%" + filter.search().trim().toLowerCase() + "%";
                    predicates.add(cb.or(
                            cb.like(cb.lower(root.get("invoiceNumber")), pattern),
                            cb.like(cb.lower(root.get("collectionReference")), pattern),
                            cb.like(cb.lower(root.get("poNumber")), pattern)
                    ));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Invoice> page = invoiceRepository.findAll(spec, pageable);
        Map<UUID, String> customerNames = new HashMap<>();

        List<UUID> invoiceIds = page.getContent().stream().map(Invoice::getId).toList();
        Map<UUID, BigDecimal> activeReceiptsMap = new HashMap<>();
        if (!invoiceIds.isEmpty()) {
            List<Object[]> rows = receiptRepository.sumActiveAmountsByInvoiceIds(organisationId, invoiceIds);
            for (Object[] row : rows) {
                activeReceiptsMap.put((UUID) row[0], (BigDecimal) row[1]);
            }
        }

        return page.map(inv -> {
            String customerName = customerNames.computeIfAbsent(inv.getCustomerId(), cid -> {
                return customerRepository.findById(cid).map(Customer::getLegalName).orElse("Unknown Customer");
            });

            String preview = null;
            if (inv.getState() == InvoiceState.DRAFT) {
                preview = numberSeriesService.previewNextNumber(
                        organisationId,
                        DocumentType.INVOICE,
                        inv.getInvoiceDate(),
                        null
                );
            }

            BigDecimal received = activeReceiptsMap.getOrDefault(inv.getId(), BigDecimal.ZERO.setScale(2));
            BigDecimal grandTotal = inv.getGrandTotal() != null ? inv.getGrandTotal() : BigDecimal.ZERO.setScale(2);
            BigDecimal balance = grandTotal.subtract(received);
            String paymentStatus = derivePaymentStatus(inv, received);
            String ageingBucket = inv.getState() == InvoiceState.ISSUED
                    ? AgeingBucket.calculate(inv.getDueDate(), LocalDate.now()).name()
                    : null;

            return new InvoiceSummaryResponse(
                    inv.getId(),
                    inv.getInvoiceNumber(),
                    preview,
                    inv.getInvoiceDate(),
                    inv.getDueDate(),
                    inv.getCustomerId(),
                    customerName,
                    inv.getGrandTotal(),
                    received,
                    balance,
                    inv.getState(),
                    paymentStatus,
                    ageingBucket,
                    inv.getCollectionReference()
            );
        });
    }

    // --- Private Helper Methods ---

    private Organisation getOrganisation(UUID organisationId) {
        return organisationRepository.findById(organisationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organisation not found"));
    }

    private Customer getCustomer(UUID organisationId, UUID customerId) {
        return customerRepository.findByIdAndOrganisationId(customerId, organisationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));
    }

    private void processAndCalculateLines(
            Invoice invoice,
            List<InvoiceLineRequest> lineRequests,
            String supplierState,
            String posState,
            boolean isSupplierRegistered
    ) {
        if (lineRequests == null || lineRequests.isEmpty()) {
            invoice.setSubtotalBeforeDiscount(BigDecimal.ZERO.setScale(2));
            invoice.setTotalDiscount(BigDecimal.ZERO.setScale(2));
            invoice.setTaxableValue(BigDecimal.ZERO.setScale(2));
            invoice.setTotalCgst(BigDecimal.ZERO.setScale(2));
            invoice.setTotalSgst(BigDecimal.ZERO.setScale(2));
            invoice.setTotalIgst(BigDecimal.ZERO.setScale(2));
            invoice.setTotalCess(BigDecimal.ZERO.setScale(2));
            invoice.setTotalTax(BigDecimal.ZERO.setScale(2));
            invoice.setRoundOff(BigDecimal.ZERO.setScale(2));
            invoice.setGrandTotal(BigDecimal.ZERO.setScale(2));
            invoice.setAmountInWords("Zero Rupees Only");
            return;
        }

        List<TaxLineInput> taxInputs = new ArrayList<>();
        int lineNum = 1;
        for (InvoiceLineRequest req : lineRequests) {
            taxInputs.add(new TaxLineInput(
                    lineNum++,
                    req.itemDescription(),
                    req.hsnOrSacCode(),
                    Boolean.TRUE.equals(req.isService()),
                    req.quantity(),
                    req.unitOfMeasure() != null ? req.unitOfMeasure() : "NOS",
                    req.unitPrice(),
                    req.discountPercent(),
                    req.discountAmount(),
                    req.taxRatePercent(),
                    null,
                    null,
                    req.taxCategory() != null ? req.taxCategory() : TaxCategory.TAXABLE
            ));
        }

        TaxInvoiceCalculation calc = GstTaxEngine.calculateInvoice(
                supplierState,
                posState,
                isSupplierRegistered,
                invoice.getIsReverseCharge(),
                taxInputs
        );

        invoice.clearLines();
        for (TaxLineCalculation l : calc.lines()) {
            InvoiceLine line = new InvoiceLine();
            line.setLineNumber(l.lineNumber());
            line.setItemDescription(l.itemDescription());
            line.setHsnOrSacCode(l.hsnOrSacCode());
            line.setIsService(l.isService());
            line.setQuantity(l.quantity());
            line.setUnitOfMeasure(l.unitOfMeasure());
            line.setUnitPrice(l.unitPrice());
            line.setGrossAmount(l.grossAmount());
            line.setDiscountPercent(l.discountPercent());
            line.setDiscountAmount(l.discountAmount());
            line.setTaxableValue(l.taxableValue());
            line.setTaxCategory(l.taxCategory());
            line.setTaxRatePercent(l.taxRatePercent());
            line.setCgstRatePercent(l.cgstRatePercent());
            line.setCgstAmount(l.cgstAmount());
            line.setSgstRatePercent(l.sgstRatePercent());
            line.setSgstAmount(l.sgstAmount());
            line.setIgstRatePercent(l.igstRatePercent());
            line.setIgstAmount(l.igstAmount());
            line.setCessAmount(l.cessAmount());
            line.setLineTotal(l.lineTotal());
            invoice.addLine(line);
        }

        invoice.setSubtotalBeforeDiscount(calc.subtotalBeforeDiscount());
        invoice.setTotalDiscount(calc.totalDiscount());
        invoice.setTaxableValue(calc.taxableValue());
        invoice.setTotalCgst(calc.totalCgst());
        invoice.setTotalSgst(calc.totalSgst());
        invoice.setTotalIgst(calc.totalIgst());
        invoice.setTotalCess(calc.totalCess());
        invoice.setTotalTax(calc.totalTax());
        invoice.setRoundOff(calc.roundOff());
        invoice.setGrandTotal(calc.grandTotal());
        invoice.setAmountInWords(calc.amountInWords());
    }

    private InvoiceResponse toInvoiceResponse(Invoice inv, Customer customer, Organisation org) {
        String preview = null;
        if (inv.getState() == InvoiceState.DRAFT) {
            preview = numberSeriesService.previewNextNumber(
                    inv.getOrganisationId(),
                    DocumentType.INVOICE,
                    inv.getInvoiceDate(),
                    org.getInvoicePrefix()
            );
        }

        List<InvoiceLineResponse> lineResponses = inv.getLines().stream().map(l -> new InvoiceLineResponse(
                l.getId(),
                l.getLineNumber(),
                l.getItemDescription(),
                l.getHsnOrSacCode(),
                Boolean.TRUE.equals(l.getIsService()),
                l.getQuantity(),
                l.getUnitOfMeasure(),
                l.getUnitPrice(),
                l.getGrossAmount(),
                l.getDiscountPercent(),
                l.getDiscountAmount(),
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
                l.getLineTotal()
        )).collect(Collectors.toList());

        // Compute slab summaries for display
        Map<BigDecimal, List<InvoiceLine>> byRate = new TreeMap<>();
        for (InvoiceLine l : inv.getLines()) {
            byRate.computeIfAbsent(l.getTaxRatePercent(), k -> new ArrayList<>()).add(l);
        }
        List<TaxSlabSummary> slabs = new ArrayList<>();
        for (Map.Entry<BigDecimal, List<InvoiceLine>> entry : byRate.entrySet()) {
            BigDecimal rate = entry.getKey();
            BigDecimal taxable = BigDecimal.ZERO.setScale(2);
            BigDecimal cgst = BigDecimal.ZERO.setScale(2);
            BigDecimal sgst = BigDecimal.ZERO.setScale(2);
            BigDecimal igst = BigDecimal.ZERO.setScale(2);
            BigDecimal cess = BigDecimal.ZERO.setScale(2);

            for (InvoiceLine l : entry.getValue()) {
                taxable = taxable.add(l.getTaxableValue());
                cgst = cgst.add(l.getCgstAmount());
                sgst = sgst.add(l.getSgstAmount());
                igst = igst.add(l.getIgstAmount());
                cess = cess.add(l.getCessAmount());
            }
            slabs.add(new TaxSlabSummary(rate, taxable, cgst, sgst, igst, cess, cgst.add(sgst).add(igst).add(cess)));
        }

        BigDecimal activeReceived = receiptRepository.sumActiveAmountByInvoiceId(inv.getOrganisationId(), inv.getId());
        if (activeReceived == null) {
            activeReceived = BigDecimal.ZERO.setScale(2);
        }
        BigDecimal grandTotal = inv.getGrandTotal() != null ? inv.getGrandTotal() : BigDecimal.ZERO.setScale(2);
        BigDecimal balanceAmount = grandTotal.subtract(activeReceived);
        String paymentStatus = derivePaymentStatus(inv, activeReceived);
        String ageingBucket = inv.getState() == InvoiceState.ISSUED
                ? AgeingBucket.calculate(inv.getDueDate(), LocalDate.now()).name()
                : null;
        String fyShort = inv.getFinancialYear() != null ? FinancialYear.formatShort(inv.getFinancialYear()) : null;

        return new InvoiceResponse(
                inv.getId(),
                inv.getOrganisationId(),
                customer.getId(),
                customer.getLegalName(),
                customer.getGstin(),
                inv.getSeriesId(),
                inv.getInvoiceNumber(),
                preview,
                inv.getFinancialYear(),
                fyShort,
                inv.getInvoiceDate(),
                inv.getDueDate(),
                inv.getPlaceOfSupplyStateCode(),
                inv.getSupplyType(),
                inv.getIsReverseCharge(),
                inv.getDeliveryOrAcceptanceDate(),
                inv.getAgreedCreditDays(),
                inv.getCollectionReference(),
                inv.getPoNumber(),
                inv.getPoDate(),
                inv.getNotes(),
                inv.getTermsText(),
                inv.getSubtotalBeforeDiscount(),
                inv.getTotalDiscount(),
                inv.getTaxableValue(),
                inv.getTotalCgst(),
                inv.getTotalSgst(),
                inv.getTotalIgst(),
                inv.getTotalCess(),
                inv.getTotalTax(),
                inv.getRoundOff(),
                inv.getGrandTotal(),
                inv.getAmountInWords(),
                inv.getState(),
                paymentStatus,
                activeReceived,
                balanceAmount,
                ageingBucket,
                inv.getIssuedAt(),
                inv.getIssuedByUserId(),
                inv.getCancelledAt(),
                inv.getCancelledByUserId(),
                inv.getCancellationReason(),
                inv.getPdfObjectKey(),
                inv.getCreatedAt(),
                inv.getUpdatedAt(),
                lineResponses,
                slabs
        );
    }

    /**
     * Derives payment status according to DOMAIN.md Section 4 line 219.
     * State CANCELLED -> CANCELLED
     * received = 0 and today <= due_date -> UNPAID
     * received = 0 and today > due_date -> OVERDUE
     * 0 < received < grand_total and today <= due_date -> PARTIALLY_PAID
     * 0 < received < grand_total and today > due_date -> PARTIALLY_PAID_OVERDUE
     * received >= grand_total -> PAID
     */
    public static String derivePaymentStatus(Invoice inv, BigDecimal receivedAmount) {
        if (inv.getState() == InvoiceState.CANCELLED) {
            return "CANCELLED";
        }
        if (inv.getState() == InvoiceState.DRAFT) {
            return "DRAFT";
        }

        BigDecimal received = receivedAmount != null ? receivedAmount : BigDecimal.ZERO;
        BigDecimal grandTotal = inv.getGrandTotal() != null ? inv.getGrandTotal() : BigDecimal.ZERO;
        LocalDate today = LocalDate.now();
        boolean isOverdue = inv.getDueDate() != null && today.isAfter(inv.getDueDate());

        if (received.compareTo(BigDecimal.ZERO) <= 0) {
            return isOverdue ? "OVERDUE" : "UNPAID";
        } else if (received.compareTo(grandTotal) < 0) {
            return isOverdue ? "PARTIALLY_PAID_OVERDUE" : "PARTIALLY_PAID";
        } else {
            return "PAID";
        }
    }

    /**
     * Loads the immutable PDF resource for an invoice, or generates it if needed.
     */
    @Transactional(readOnly = true)
    public InvoicePdfResource getInvoicePdf(UUID organisationId, UUID invoiceId) {
        Invoice invoice = invoiceRepository.findByIdAndOrganisationIdWithLines(invoiceId, organisationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));

        Organisation org = getOrganisation(organisationId);
        Customer customer = getCustomer(organisationId, invoice.getCustomerId());

        String safeNumber = invoice.getInvoiceNumber() != null
                ? invoice.getInvoiceNumber().replace('/', '_').replace('\\', '_')
                : "DRAFT_" + invoice.getId().toString().substring(0, 8);
        String filename = safeNumber + ".pdf";

        if (invoice.getPdfObjectKey() != null && !invoice.getPdfObjectKey().isBlank()) {
            try {
                Resource resource = storageService.load(invoice.getPdfObjectKey());
                if (resource.exists()) {
                    return new InvoicePdfResource(resource, filename);
                }
            } catch (Exception e) {
                log.warn("Stored PDF not found for key {}, regenerating on-the-fly...", invoice.getPdfObjectKey());
            }
        }

        // Generate on demand (e.g. drafts or legacy records)
        byte[] pdfBytes = invoicePdfService.generatePdf(invoice, org, customer);
        ByteArrayResource resource = new ByteArrayResource(pdfBytes);
        return new InvoicePdfResource(resource, filename);
    }
}
