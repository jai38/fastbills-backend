package com.fastbills.api.report;

import com.fastbills.api.customer.Customer;
import com.fastbills.api.customer.CustomerRepository;
import com.fastbills.api.invoice.Invoice;
import com.fastbills.api.invoice.InvoiceLine;
import com.fastbills.api.invoice.InvoiceRepository;
import com.fastbills.api.invoice.InvoiceService;
import com.fastbills.api.invoice.InvoiceState;
import com.fastbills.api.receipt.AgeingBucket;
import com.fastbills.api.receipt.PaymentMethod;
import com.fastbills.api.receipt.ReceiptRepository;
import com.fastbills.api.receipt.ReceiptService;
import com.fastbills.api.receipt.dto.ReceiptFilter;
import com.fastbills.api.receipt.dto.ReceiptResponse;
import com.fastbills.api.report.dto.*;
import com.fastbills.api.tax.IndianStates;
import com.fastbills.api.tax.TaxCategory;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private final InvoiceRepository invoiceRepository;
    private final ReceiptRepository receiptRepository;
    private final CustomerRepository customerRepository;
    private final ReceiptService receiptService;

    public ReportService(
            InvoiceRepository invoiceRepository,
            ReceiptRepository receiptRepository,
            CustomerRepository customerRepository,
            ReceiptService receiptService
    ) {
        this.invoiceRepository = invoiceRepository;
        this.receiptRepository = receiptRepository;
        this.customerRepository = customerRepository;
        this.receiptService = receiptService;
    }

    /**
     * Report 1: Invoice Register
     * Returns all issued and cancelled invoices in the period with derived payment status and balance.
     */
    public List<InvoiceRegisterItem> getInvoiceRegister(UUID organisationId, ReportFilter filter) {
        Specification<Invoice> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organisationId"), organisationId));
            predicates.add(root.get("state").in(InvoiceState.ISSUED, InvoiceState.CANCELLED));

            if (filter.fromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("invoiceDate"), filter.fromDate()));
            }
            if (filter.toDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("invoiceDate"), filter.toDate()));
            }
            if (filter.customerId() != null) {
                predicates.add(cb.equal(root.get("customerId"), filter.customerId()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<Invoice> invoices = invoiceRepository.findAll(spec);
        if (invoices.isEmpty()) {
            return Collections.emptyList();
        }

        // Map customers
        Set<UUID> customerIds = invoices.stream().map(Invoice::getCustomerId).collect(Collectors.toSet());
        Map<UUID, Customer> customerMap = customerRepository.findAllById(customerIds).stream()
                .collect(Collectors.toMap(Customer::getId, c -> c));

        // Map active receipts
        List<UUID> invoiceIds = invoices.stream().map(Invoice::getId).toList();
        Map<UUID, BigDecimal> receiptsMap = receiptRepository.sumActiveAmountsByInvoiceIds(organisationId, invoiceIds).stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> (BigDecimal) row[1]
                ));

        List<InvoiceRegisterItem> items = new ArrayList<>();
        for (Invoice inv : invoices) {
            Customer cust = customerMap.get(inv.getCustomerId());
            String customerName = cust != null ? cust.getLegalName() : "Unknown Customer";
            String customerGstin = cust != null && cust.getGstin() != null ? cust.getGstin() : "-";
            String pos = inv.getPlaceOfSupplyStateCode() + " - " + IndianStates.getStateName(inv.getPlaceOfSupplyStateCode());

            BigDecimal received = receiptsMap.getOrDefault(inv.getId(), BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            BigDecimal balance = inv.getGrandTotal().subtract(received).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            String paymentStatus = InvoiceService.derivePaymentStatus(inv, received);
            String ageingBucket = AgeingBucket.calculate(inv.getDueDate()).name();

            items.add(new InvoiceRegisterItem(
                    inv.getId(),
                    inv.getInvoiceNumber(),
                    inv.getInvoiceDate(),
                    inv.getDueDate(),
                    inv.getCustomerId(),
                    customerName,
                    customerGstin,
                    pos,
                    inv.getSupplyType(),
                    inv.getState(),
                    inv.getTaxableValue(),
                    inv.getTotalCgst(),
                    inv.getTotalSgst(),
                    inv.getTotalIgst(),
                    inv.getTotalCess(),
                    inv.getTotalTax(),
                    inv.getRoundOff(),
                    inv.getGrandTotal(),
                    received,
                    balance,
                    paymentStatus,
                    ageingBucket
            ));
        }

        items.sort(Comparator.comparing(InvoiceRegisterItem::invoiceDate).reversed()
                .thenComparing(Comparator.comparing(InvoiceRegisterItem::invoiceNumber).reversed()));

        return items;
    }

    /**
     * Report 2: Revenue Summary
     * Grouped by month or customer. Excludes drafts and cancelled invoices.
     */
    public List<RevenueSummaryItem> getRevenueSummary(UUID organisationId, ReportFilter filter) {
        Specification<Invoice> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organisationId"), organisationId));
            predicates.add(cb.equal(root.get("state"), InvoiceState.ISSUED));

            if (filter.fromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("invoiceDate"), filter.fromDate()));
            }
            if (filter.toDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("invoiceDate"), filter.toDate()));
            }
            if (filter.customerId() != null) {
                predicates.add(cb.equal(root.get("customerId"), filter.customerId()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<Invoice> invoices = invoiceRepository.findAll(spec);
        if (invoices.isEmpty()) {
            return Collections.emptyList();
        }

        // Map receipts
        List<UUID> invoiceIds = invoices.stream().map(Invoice::getId).toList();
        Map<UUID, BigDecimal> receiptsMap = receiptRepository.sumActiveAmountsByInvoiceIds(organisationId, invoiceIds).stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> (BigDecimal) row[1]
                ));

        boolean groupByCustomer = "CUSTOMER".equalsIgnoreCase(filter.groupBy());

        Map<UUID, Customer> customerMap = Collections.emptyMap();
        if (groupByCustomer) {
            Set<UUID> customerIds = invoices.stream().map(Invoice::getCustomerId).collect(Collectors.toSet());
            customerMap = customerRepository.findAllById(customerIds).stream()
                    .collect(Collectors.toMap(Customer::getId, c -> c));
        }

        Map<String, List<Invoice>> grouped = new LinkedHashMap<>();
        for (Invoice inv : invoices) {
            String key;
            if (groupByCustomer) {
                key = inv.getCustomerId().toString();
            } else {
                key = inv.getInvoiceDate().getYear() + "-" + String.format("%02d", inv.getInvoiceDate().getMonthValue());
            }
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(inv);
        }

        List<RevenueSummaryItem> items = new ArrayList<>();
        for (Map.Entry<String, List<Invoice>> entry : grouped.entrySet()) {
            String key = entry.getKey();
            List<Invoice> groupInvoices = entry.getValue();

            String label;
            if (groupByCustomer) {
                Customer c = customerMap.get(UUID.fromString(key));
                label = c != null ? c.getLegalName() : "Unknown Customer";
            } else {
                String[] parts = key.split("-");
                int year = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);
                String monthName = LocalDate.of(year, month, 1).getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
                label = monthName + " " + year;
            }

            int count = groupInvoices.size();
            BigDecimal taxable = BigDecimal.ZERO.setScale(2);
            BigDecimal cgst = BigDecimal.ZERO.setScale(2);
            BigDecimal sgst = BigDecimal.ZERO.setScale(2);
            BigDecimal igst = BigDecimal.ZERO.setScale(2);
            BigDecimal cess = BigDecimal.ZERO.setScale(2);
            BigDecimal tax = BigDecimal.ZERO.setScale(2);
            BigDecimal grand = BigDecimal.ZERO.setScale(2);
            BigDecimal received = BigDecimal.ZERO.setScale(2);
            BigDecimal balance = BigDecimal.ZERO.setScale(2);

            for (Invoice inv : groupInvoices) {
                taxable = taxable.add(inv.getTaxableValue());
                cgst = cgst.add(inv.getTotalCgst());
                sgst = sgst.add(inv.getTotalSgst());
                igst = igst.add(inv.getTotalIgst());
                cess = cess.add(inv.getTotalCess());
                tax = tax.add(inv.getTotalTax());
                grand = grand.add(inv.getGrandTotal());

                BigDecimal rec = receiptsMap.getOrDefault(inv.getId(), BigDecimal.ZERO);
                received = received.add(rec);
                balance = balance.add(inv.getGrandTotal().subtract(rec).max(BigDecimal.ZERO));
            }

            items.add(new RevenueSummaryItem(
                    key,
                    label,
                    count,
                    taxable.setScale(2, RoundingMode.HALF_UP),
                    cgst.setScale(2, RoundingMode.HALF_UP),
                    sgst.setScale(2, RoundingMode.HALF_UP),
                    igst.setScale(2, RoundingMode.HALF_UP),
                    cess.setScale(2, RoundingMode.HALF_UP),
                    tax.setScale(2, RoundingMode.HALF_UP),
                    grand.setScale(2, RoundingMode.HALF_UP),
                    received.setScale(2, RoundingMode.HALF_UP),
                    balance.setScale(2, RoundingMode.HALF_UP)
            ));
        }

        if (groupByCustomer) {
            items.sort(Comparator.comparing(RevenueSummaryItem::grandTotal).reversed());
        } else {
            items.sort(Comparator.comparing(RevenueSummaryItem::groupKey));
        }

        return items;
    }

    /**
     * Report 3: Tax Summary by Rate Slab
     * Groups invoice lines of issued invoices by rate slab. Excludes drafts and cancelled invoices.
     */
    public List<TaxSummaryItem> getTaxSummaryByRateSlab(UUID organisationId, ReportFilter filter) {
        List<Invoice> invoices = invoiceRepository.findAllByOrganisationIdAndStateWithLines(organisationId, InvoiceState.ISSUED);

        if (filter.fromDate() != null) {
            invoices = invoices.stream().filter(i -> !i.getInvoiceDate().isBefore(filter.fromDate())).toList();
        }
        if (filter.toDate() != null) {
            invoices = invoices.stream().filter(i -> !i.getInvoiceDate().isAfter(filter.toDate())).toList();
        }
        if (filter.customerId() != null) {
            invoices = invoices.stream().filter(i -> i.getCustomerId().equals(filter.customerId())).toList();
        }

        if (invoices.isEmpty()) {
            return Collections.emptyList();
        }

        // Key: category + "_" + ratePercent
        Map<String, List<InvoiceLine>> slabMap = new LinkedHashMap<>();
        for (Invoice inv : invoices) {
            for (InvoiceLine line : inv.getLines()) {
                String cat = line.getTaxCategory() != null ? line.getTaxCategory().name() : "TAXABLE";
                BigDecimal rate = line.getTaxRatePercent() != null ? line.getTaxRatePercent() : BigDecimal.ZERO;
                String key = cat + "_" + rate.setScale(2, RoundingMode.HALF_UP);
                slabMap.computeIfAbsent(key, k -> new ArrayList<>()).add(line);
            }
        }

        List<TaxSummaryItem> items = new ArrayList<>();
        for (Map.Entry<String, List<InvoiceLine>> entry : slabMap.entrySet()) {
            List<InvoiceLine> lines = entry.getValue();
            InvoiceLine first = lines.get(0);
            BigDecimal rate = first.getTaxRatePercent() != null ? first.getTaxRatePercent() : BigDecimal.ZERO;
            String cat = first.getTaxCategory() != null ? first.getTaxCategory().name() : "TAXABLE";

            String label;
            if ("TAXABLE".equalsIgnoreCase(cat)) {
                label = rate.setScale(2, RoundingMode.HALF_UP).toPlainString() + "% GST";
            } else {
                label = cat.replace("_", " ");
            }

            BigDecimal taxable = BigDecimal.ZERO.setScale(2);
            BigDecimal cgst = BigDecimal.ZERO.setScale(2);
            BigDecimal sgst = BigDecimal.ZERO.setScale(2);
            BigDecimal igst = BigDecimal.ZERO.setScale(2);
            BigDecimal cess = BigDecimal.ZERO.setScale(2);
            BigDecimal tax = BigDecimal.ZERO.setScale(2);

            for (InvoiceLine line : lines) {
                taxable = taxable.add(line.getTaxableValue());
                cgst = cgst.add(line.getCgstAmount());
                sgst = sgst.add(line.getSgstAmount());
                igst = igst.add(line.getIgstAmount());
                cess = cess.add(line.getCessAmount());
                tax = tax.add(line.getCgstAmount()).add(line.getSgstAmount()).add(line.getIgstAmount()).add(line.getCessAmount());
            }

            items.add(new TaxSummaryItem(
                    rate.setScale(2, RoundingMode.HALF_UP),
                    cat,
                    label,
                    lines.size(),
                    taxable.setScale(2, RoundingMode.HALF_UP),
                    cgst.setScale(2, RoundingMode.HALF_UP),
                    sgst.setScale(2, RoundingMode.HALF_UP),
                    igst.setScale(2, RoundingMode.HALF_UP),
                    cess.setScale(2, RoundingMode.HALF_UP),
                    tax.setScale(2, RoundingMode.HALF_UP)
            ));
        }

        items.sort(Comparator.comparing(TaxSummaryItem::taxRatePercent));
        return items;
    }

    /**
     * Report 4: Receivables Ageing
     * Groups outstanding balances per customer across the 6 ageing buckets.
     */
    public List<ReceivablesAgeingItem> getReceivablesAgeing(UUID organisationId, ReportFilter filter) {
        Specification<Invoice> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organisationId"), organisationId));
            predicates.add(cb.equal(root.get("state"), InvoiceState.ISSUED));

            if (filter.customerId() != null) {
                predicates.add(cb.equal(root.get("customerId"), filter.customerId()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<Invoice> invoices = invoiceRepository.findAll(spec);
        if (invoices.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> invoiceIds = invoices.stream().map(Invoice::getId).toList();
        Map<UUID, BigDecimal> receiptsMap = receiptRepository.sumActiveAmountsByInvoiceIds(organisationId, invoiceIds).stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> (BigDecimal) row[1]
                ));

        // Filter invoices that have an outstanding balance
        List<Invoice> unpaidInvoices = new ArrayList<>();
        Map<UUID, BigDecimal> balanceMap = new HashMap<>();

        for (Invoice inv : invoices) {
            BigDecimal rec = receiptsMap.getOrDefault(inv.getId(), BigDecimal.ZERO);
            BigDecimal bal = inv.getGrandTotal().subtract(rec);
            if (bal.compareTo(BigDecimal.ZERO) > 0) {
                unpaidInvoices.add(inv);
                balanceMap.put(inv.getId(), bal);
            }
        }

        if (unpaidInvoices.isEmpty()) {
            return Collections.emptyList();
        }

        Set<UUID> customerIds = unpaidInvoices.stream().map(Invoice::getCustomerId).collect(Collectors.toSet());
        Map<UUID, Customer> customerMap = customerRepository.findAllById(customerIds).stream()
                .collect(Collectors.toMap(Customer::getId, c -> c));

        Map<UUID, List<Invoice>> customerInvoices = unpaidInvoices.stream()
                .collect(Collectors.groupingBy(Invoice::getCustomerId));

        List<ReceivablesAgeingItem> items = new ArrayList<>();
        for (Map.Entry<UUID, List<Invoice>> entry : customerInvoices.entrySet()) {
            UUID custId = entry.getKey();
            List<Invoice> custInvs = entry.getValue();
            Customer cust = customerMap.get(custId);
            String name = cust != null ? cust.getLegalName() : "Unknown Customer";
            String gstin = cust != null && cust.getGstin() != null ? cust.getGstin() : "-";

            BigDecimal current = BigDecimal.ZERO.setScale(2);
            BigDecimal d1To30 = BigDecimal.ZERO.setScale(2);
            BigDecimal d31To45 = BigDecimal.ZERO.setScale(2);
            BigDecimal d46To60 = BigDecimal.ZERO.setScale(2);
            BigDecimal d61To90 = BigDecimal.ZERO.setScale(2);
            BigDecimal over90 = BigDecimal.ZERO.setScale(2);
            LocalDate oldestDue = null;

            for (Invoice inv : custInvs) {
                BigDecimal bal = balanceMap.get(inv.getId());
                AgeingBucket bucket = AgeingBucket.calculate(inv.getDueDate());

                switch (bucket) {
                    case CURRENT -> current = current.add(bal);
                    case DAYS_1_TO_30 -> d1To30 = d1To30.add(bal);
                    case DAYS_31_TO_45 -> d31To45 = d31To45.add(bal);
                    case DAYS_46_TO_60 -> d46To60 = d46To60.add(bal);
                    case DAYS_61_TO_90 -> d61To90 = d61To90.add(bal);
                    case OVER_90 -> over90 = over90.add(bal);
                }

                if (inv.getDueDate() != null) {
                    if (oldestDue == null || inv.getDueDate().isBefore(oldestDue)) {
                        oldestDue = inv.getDueDate();
                    }
                }
            }

            BigDecimal totalOut = current.add(d1To30).add(d31To45).add(d46To60).add(d61To90).add(over90);

            items.add(new ReceivablesAgeingItem(
                    custId,
                    name,
                    gstin,
                    totalOut.setScale(2, RoundingMode.HALF_UP),
                    current.setScale(2, RoundingMode.HALF_UP),
                    d1To30.setScale(2, RoundingMode.HALF_UP),
                    d31To45.setScale(2, RoundingMode.HALF_UP),
                    d46To60.setScale(2, RoundingMode.HALF_UP),
                    d61To90.setScale(2, RoundingMode.HALF_UP),
                    over90.setScale(2, RoundingMode.HALF_UP),
                    oldestDue
            ));
        }

        items.sort(Comparator.comparing(ReceivablesAgeingItem::totalOutstanding).reversed());
        return items;
    }

    /**
     * Report 5: Receipts Register
     */
    public List<ReceiptResponse> getReceiptsRegister(
            UUID organisationId,
            ReportFilter filter,
            PaymentMethod method,
            Boolean isReversed
    ) {
        ReceiptFilter rFilter = new ReceiptFilter(
                null,
                filter.customerId(),
                filter.fromDate(),
                filter.toDate(),
                method,
                isReversed
        );
        Page<ReceiptResponse> page = receiptService.listReceipts(organisationId, rFilter, Pageable.unpaged());
        return page.getContent();
    }
}
