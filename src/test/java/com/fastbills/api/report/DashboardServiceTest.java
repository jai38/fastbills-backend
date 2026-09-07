package com.fastbills.api.report;

import com.fastbills.api.customer.Customer;
import com.fastbills.api.customer.CustomerRepository;
import com.fastbills.api.invoice.Invoice;
import com.fastbills.api.invoice.InvoiceRepository;
import com.fastbills.api.invoice.InvoiceState;
import com.fastbills.api.receipt.PaymentMethod;
import com.fastbills.api.receipt.Receipt;
import com.fastbills.api.receipt.ReceiptRepository;
import com.fastbills.api.report.dto.DashboardMetricsResponse;
import com.fastbills.api.tax.SupplyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private ReceiptRepository receiptRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private DashboardService dashboardService;

    private UUID organisationId;
    private UUID customerId;
    private Customer customer;

    @BeforeEach
    void setUp() {
        organisationId = UUID.randomUUID();
        customerId = UUID.randomUUID();

        customer = new Customer();
        customer.setId(customerId);
        customer.setOrganisationId(organisationId);
        customer.setLegalName("Apex Logistics Ltd");
        customer.setGstin("27BBBCU9603R1ZM");
    }

    private Invoice createInvoice(String invoiceNumber, LocalDate date, LocalDate dueDate, BigDecimal total) {
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setOrganisationId(organisationId);
        invoice.setCustomerId(customerId);
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setInvoiceDate(date);
        invoice.setDueDate(dueDate);
        invoice.setState(InvoiceState.ISSUED);
        invoice.setSupplyType(SupplyType.INTRA_STATE);
        invoice.setGrandTotal(total);
        return invoice;
    }

    @Test
    @DisplayName("Dashboard aggregates 5 KPIs, ageing visualizer, and oldest unpaid invoices accurately")
    void testGetDashboardMetrics() {
        LocalDate today = LocalDate.now();

        // Inv 1: Current month, not overdue, total 1000
        Invoice inv1 = createInvoice("INV-001", today.withDayOfMonth(1), today.plusDays(10), new BigDecimal("1000.00"));

        // Inv 2: Overdue 20 days (DAYS_1_TO_30), total 2000, 500 received -> 1500 balance
        Invoice inv2 = createInvoice("INV-002", today.minusDays(30), today.minusDays(20), new BigDecimal("2000.00"));

        // Inv 3: Overdue 60 days (DAYS_46_TO_60, crossed 45 days), total 5000, 0 received -> 5000 balance
        Invoice inv3 = createInvoice("INV-003", today.minusDays(75), today.minusDays(60), new BigDecimal("5000.00"));

        when(invoiceRepository.findAll(any(Specification.class))).thenReturn(List.of(inv1, inv2, inv3));
        when(receiptRepository.sumActiveAmountsByInvoiceIds(eq(organisationId), anyList())).thenReturn(List.<Object[]>of(
                new Object[]{inv2.getId(), new BigDecimal("500.00")}
        ));

        // Receipts this month
        Receipt r1 = new Receipt();
        r1.setId(UUID.randomUUID());
        r1.setAmount(new BigDecimal("1500.00"));
        r1.setPaymentDate(today.withDayOfMonth(5));
        when(receiptRepository.findAll(any(Specification.class))).thenReturn(List.of(r1));

        when(customerRepository.findAllById(anySet())).thenReturn(List.of(customer));

        DashboardMetricsResponse metrics = dashboardService.getDashboardMetrics(organisationId);

        // Balances:
        // inv1: 1000.00 (not overdue)
        // inv2: 1500.00 (overdue 20 days)
        // inv3: 5000.00 (overdue 60 days)
        // Total Outstanding: 1000 + 1500 + 5000 = 7500.00
        assertThat(metrics.totalOutstanding()).isEqualByComparingTo("7500.00");

        // Total Overdue: 1500 + 5000 = 6500.00
        assertThat(metrics.totalOverdue()).isEqualByComparingTo("6500.00");

        // Crossed 45 Days: inv3 = 5000.00
        assertThat(metrics.crossed45Days()).isEqualByComparingTo("5000.00");

        // Received This Month: 1500.00
        assertThat(metrics.receivedThisMonth()).isEqualByComparingTo("1500.00");

        // Invoices Raised This Month: only inv1 has invoiceDate in current month
        assertThat(metrics.invoicesRaisedThisMonthCount()).isEqualTo(1);
        assertThat(metrics.invoicesRaisedThisMonthTotal()).isEqualByComparingTo("1000.00");

        // Ageing Summary
        assertThat(metrics.ageingSummary().current()).isEqualByComparingTo("1000.00");
        assertThat(metrics.ageingSummary().days1To30()).isEqualByComparingTo("1500.00");
        assertThat(metrics.ageingSummary().days46To60()).isEqualByComparingTo("5000.00");

        // Top Unpaid Invoices: should be sorted oldest due date first (inv3, then inv2, then inv1)
        assertThat(metrics.topUnpaidInvoices()).hasSize(3);
        assertThat(metrics.topUnpaidInvoices().get(0).invoiceNumber()).isEqualTo("INV-003");
        assertThat(metrics.topUnpaidInvoices().get(0).daysOverdue()).isEqualTo(60);
        assertThat(metrics.topUnpaidInvoices().get(1).invoiceNumber()).isEqualTo("INV-002");
        assertThat(metrics.topUnpaidInvoices().get(2).invoiceNumber()).isEqualTo("INV-001");
    }
}
