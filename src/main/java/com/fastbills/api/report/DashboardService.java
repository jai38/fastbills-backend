package com.fastbills.api.report;

import com.fastbills.api.customer.Customer;
import com.fastbills.api.customer.CustomerRepository;
import com.fastbills.api.invoice.Invoice;
import com.fastbills.api.invoice.InvoiceRepository;
import com.fastbills.api.invoice.InvoiceService;
import com.fastbills.api.invoice.InvoiceState;
import com.fastbills.api.receipt.AgeingBucket;
import com.fastbills.api.receipt.Receipt;
import com.fastbills.api.receipt.ReceiptRepository;
import com.fastbills.api.report.dto.DashboardMetricsResponse;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final InvoiceRepository invoiceRepository;
    private final ReceiptRepository receiptRepository;
    private final CustomerRepository customerRepository;

    public DashboardService(
            InvoiceRepository invoiceRepository,
            ReceiptRepository receiptRepository,
            CustomerRepository customerRepository
    ) {
        this.invoiceRepository = invoiceRepository;
        this.receiptRepository = receiptRepository;
        this.customerRepository = customerRepository;
    }

    public DashboardMetricsResponse getDashboardMetrics(UUID organisationId) {
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());

        // 1. Fetch all issued invoices
        Specification<Invoice> issuedSpec = (root, query, cb) -> cb.and(
                cb.equal(root.get("organisationId"), organisationId),
                cb.equal(root.get("state"), InvoiceState.ISSUED)
        );
        List<Invoice> issuedInvoices = invoiceRepository.findAll(issuedSpec);

        List<UUID> invoiceIds = issuedInvoices.stream().map(Invoice::getId).toList();
        Map<UUID, BigDecimal> receiptsMap = receiptRepository.sumActiveAmountsByInvoiceIds(organisationId, invoiceIds).stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> (BigDecimal) row[1]
                ));

        BigDecimal totalOutstanding = BigDecimal.ZERO.setScale(2);
        BigDecimal totalOverdue = BigDecimal.ZERO.setScale(2);
        BigDecimal crossed45Days = BigDecimal.ZERO.setScale(2);

        BigDecimal currentBucket = BigDecimal.ZERO.setScale(2);
        BigDecimal d1To30Bucket = BigDecimal.ZERO.setScale(2);
        BigDecimal d31To45Bucket = BigDecimal.ZERO.setScale(2);
        BigDecimal d46To60Bucket = BigDecimal.ZERO.setScale(2);
        BigDecimal d61To90Bucket = BigDecimal.ZERO.setScale(2);
        BigDecimal over90Bucket = BigDecimal.ZERO.setScale(2);

        List<DashboardMetricsResponse.TopUnpaidInvoiceItem> unpaidItems = new ArrayList<>();

        // Customers map for unpaid invoices
        Set<UUID> customerIds = issuedInvoices.stream().map(Invoice::getCustomerId).collect(Collectors.toSet());
        Map<UUID, Customer> customerMap = customerRepository.findAllById(customerIds).stream()
                .collect(Collectors.toMap(Customer::getId, c -> c));

        int invoicesThisMonthCount = 0;
        BigDecimal invoicesThisMonthTotal = BigDecimal.ZERO.setScale(2);

        for (Invoice inv : issuedInvoices) {
            // Month raised stats
            if (!inv.getInvoiceDate().isBefore(startOfMonth) && !inv.getInvoiceDate().isAfter(endOfMonth)) {
                invoicesThisMonthCount++;
                invoicesThisMonthTotal = invoicesThisMonthTotal.add(inv.getGrandTotal());
            }

            BigDecimal received = receiptsMap.getOrDefault(inv.getId(), BigDecimal.ZERO);
            BigDecimal balance = inv.getGrandTotal().subtract(received).max(BigDecimal.ZERO);

            if (balance.compareTo(BigDecimal.ZERO) > 0) {
                totalOutstanding = totalOutstanding.add(balance);

                AgeingBucket bucket = AgeingBucket.calculate(inv.getDueDate());
                switch (bucket) {
                    case CURRENT -> currentBucket = currentBucket.add(balance);
                    case DAYS_1_TO_30 -> {
                        d1To30Bucket = d1To30Bucket.add(balance);
                        totalOverdue = totalOverdue.add(balance);
                    }
                    case DAYS_31_TO_45 -> {
                        d31To45Bucket = d31To45Bucket.add(balance);
                        totalOverdue = totalOverdue.add(balance);
                    }
                    case DAYS_46_TO_60 -> {
                        d46To60Bucket = d46To60Bucket.add(balance);
                        totalOverdue = totalOverdue.add(balance);
                        crossed45Days = crossed45Days.add(balance);
                    }
                    case DAYS_61_TO_90 -> {
                        d61To90Bucket = d61To90Bucket.add(balance);
                        totalOverdue = totalOverdue.add(balance);
                        crossed45Days = crossed45Days.add(balance);
                    }
                    case OVER_90 -> {
                        over90Bucket = over90Bucket.add(balance);
                        totalOverdue = totalOverdue.add(balance);
                        crossed45Days = crossed45Days.add(balance);
                    }
                }

                Customer cust = customerMap.get(inv.getCustomerId());
                String custName = cust != null ? cust.getLegalName() : "Unknown Customer";
                long daysOverdue = inv.getDueDate() != null && today.isAfter(inv.getDueDate())
                        ? ChronoUnit.DAYS.between(inv.getDueDate(), today)
                        : 0;

                unpaidItems.add(new DashboardMetricsResponse.TopUnpaidInvoiceItem(
                        inv.getId(),
                        inv.getInvoiceNumber(),
                        inv.getCustomerId(),
                        custName,
                        inv.getInvoiceDate(),
                        inv.getDueDate(),
                        daysOverdue,
                        inv.getGrandTotal(),
                        balance,
                        InvoiceService.derivePaymentStatus(inv, received),
                        bucket.name()
                ));
            }
        }

        // Sort unpaid invoices by dueDate ascending (oldest first) and take top 10
        unpaidItems.sort(Comparator.comparing(DashboardMetricsResponse.TopUnpaidInvoiceItem::dueDate));
        List<DashboardMetricsResponse.TopUnpaidInvoiceItem> top10Unpaid = unpaidItems.stream().limit(10).toList();

        // 2. Receipts received this month
        Specification<Receipt> receiptMonthSpec = (root, query, cb) -> cb.and(
                cb.equal(root.get("organisationId"), organisationId),
                cb.equal(root.get("isReversed"), false),
                cb.greaterThanOrEqualTo(root.get("paymentDate"), startOfMonth),
                cb.lessThanOrEqualTo(root.get("paymentDate"), endOfMonth)
        );
        List<Receipt> monthReceipts = receiptRepository.findAll(receiptMonthSpec);
        BigDecimal receivedThisMonth = monthReceipts.stream()
                .map(Receipt::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        DashboardMetricsResponse.AgeingSummary ageingSummary = new DashboardMetricsResponse.AgeingSummary(
                currentBucket.setScale(2, RoundingMode.HALF_UP),
                d1To30Bucket.setScale(2, RoundingMode.HALF_UP),
                d31To45Bucket.setScale(2, RoundingMode.HALF_UP),
                d46To60Bucket.setScale(2, RoundingMode.HALF_UP),
                d61To90Bucket.setScale(2, RoundingMode.HALF_UP),
                over90Bucket.setScale(2, RoundingMode.HALF_UP)
        );

        return new DashboardMetricsResponse(
                totalOutstanding.setScale(2, RoundingMode.HALF_UP),
                totalOverdue.setScale(2, RoundingMode.HALF_UP),
                crossed45Days.setScale(2, RoundingMode.HALF_UP),
                receivedThisMonth,
                invoicesThisMonthCount,
                invoicesThisMonthTotal.setScale(2, RoundingMode.HALF_UP),
                ageingSummary,
                top10Unpaid
        );
    }
}
