package com.fastbills.api.report;

import com.fastbills.api.customer.Customer;
import com.fastbills.api.customer.CustomerRepository;
import com.fastbills.api.invoice.*;
import com.fastbills.api.receipt.PaymentMethod;
import com.fastbills.api.receipt.ReceiptRepository;
import com.fastbills.api.receipt.ReceiptService;
import com.fastbills.api.receipt.dto.ReceiptResponse;
import com.fastbills.api.report.dto.*;
import com.fastbills.api.tax.SupplyType;
import com.fastbills.api.tax.TaxCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private ReceiptRepository receiptRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ReceiptService receiptService;

    @InjectMocks
    private ReportService reportService;

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
        customer.setLegalName("Acme Industries Pvt Ltd");
        customer.setGstin("27AABCU9603R1ZM");
        customer.setBillingStateCode("27");
    }

    private Invoice createTestInvoice(
            String invoiceNumber,
            LocalDate date,
            LocalDate dueDate,
            InvoiceState state,
            BigDecimal taxable,
            BigDecimal cgst,
            BigDecimal sgst,
            BigDecimal igst,
            BigDecimal grandTotal
    ) {
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setOrganisationId(organisationId);
        invoice.setCustomerId(customerId);
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setInvoiceDate(date);
        invoice.setDueDate(dueDate);
        invoice.setState(state);
        invoice.setSupplyType(SupplyType.INTRA_STATE);
        invoice.setPlaceOfSupplyStateCode("27");
        invoice.setTaxableValue(taxable);
        invoice.setTotalCgst(cgst);
        invoice.setTotalSgst(sgst);
        invoice.setTotalIgst(igst);
        invoice.setTotalCess(BigDecimal.ZERO);
        invoice.setTotalTax(cgst.add(sgst).add(igst));
        invoice.setRoundOff(BigDecimal.ZERO);
        invoice.setGrandTotal(grandTotal);
        return invoice;
    }

    @Test
    @DisplayName("Invoice Register derives correct payment status, balance and customer details")
    void testGetInvoiceRegister() {
        Invoice inv1 = createTestInvoice("INV-001", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 15),
                InvoiceState.ISSUED, new BigDecimal("1000.00"), new BigDecimal("90.00"), new BigDecimal("90.00"),
                BigDecimal.ZERO, new BigDecimal("1180.00"));

        Invoice inv2 = createTestInvoice("INV-002", LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 16),
                InvoiceState.ISSUED, new BigDecimal("2000.00"), new BigDecimal("180.00"), new BigDecimal("180.00"),
                BigDecimal.ZERO, new BigDecimal("2360.00"));

        when(invoiceRepository.findAll(any(Specification.class))).thenReturn(List.of(inv1, inv2));
        when(customerRepository.findAllById(anySet())).thenReturn(List.of(customer));
        // inv1 has 1180 received (PAID), inv2 has 1000 received (PARTIALLY_PAID)
        when(receiptRepository.sumActiveAmountsByInvoiceIds(eq(organisationId), anyList())).thenReturn(List.of(
                new Object[]{inv1.getId(), new BigDecimal("1180.00")},
                new Object[]{inv2.getId(), new BigDecimal("1000.00")}
        ));

        ReportFilter filter = new ReportFilter(null, null, null, null);
        List<InvoiceRegisterItem> results = reportService.getInvoiceRegister(organisationId, filter);

        assertThat(results).hasSize(2);

        InvoiceRegisterItem item1 = results.stream().filter(r -> r.invoiceNumber().equals("INV-001")).findFirst().orElseThrow();
        assertThat(item1.paymentStatus()).isEqualTo("PAID");
        assertThat(item1.receivedAmount()).isEqualByComparingTo("1180.00");
        assertThat(item1.balanceAmount()).isEqualByComparingTo("0.00");
        assertThat(item1.customerLegalName()).isEqualTo("Acme Industries Pvt Ltd");

        InvoiceRegisterItem item2 = results.stream().filter(r -> r.invoiceNumber().equals("INV-002")).findFirst().orElseThrow();
        assertThat(item2.paymentStatus()).isEqualTo("PARTIALLY_PAID");
        assertThat(item2.receivedAmount()).isEqualByComparingTo("1000.00");
        assertThat(item2.balanceAmount()).isEqualByComparingTo("1360.00");
    }

    @Test
    @DisplayName("Revenue Summary excludes drafts and cancelled invoices and groups by month")
    void testGetRevenueSummaryByMonth() {
        Invoice inv1 = createTestInvoice("INV-001", LocalDate.of(2026, 8, 15), LocalDate.of(2026, 8, 30),
                InvoiceState.ISSUED, new BigDecimal("5000.00"), new BigDecimal("450.00"), new BigDecimal("450.00"),
                BigDecimal.ZERO, new BigDecimal("5900.00"));

        Invoice inv2 = createTestInvoice("INV-002", LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 25),
                InvoiceState.ISSUED, new BigDecimal("10000.00"), new BigDecimal("900.00"), new BigDecimal("900.00"),
                BigDecimal.ZERO, new BigDecimal("11800.00"));

        when(invoiceRepository.findAll(any(Specification.class))).thenReturn(List.of(inv1, inv2));
        when(receiptRepository.sumActiveAmountsByInvoiceIds(eq(organisationId), anyList())).thenReturn(Collections.emptyList());

        ReportFilter filter = new ReportFilter(null, null, null, "month");
        List<RevenueSummaryItem> summary = reportService.getRevenueSummary(organisationId, filter);

        assertThat(summary).hasSize(2);
        RevenueSummaryItem aug = summary.stream().filter(s -> s.groupKey().equals("2026-08")).findFirst().orElseThrow();
        assertThat(aug.invoiceCount()).isEqualTo(1);
        assertThat(aug.taxableValue()).isEqualByComparingTo("5000.00");
        assertThat(aug.grandTotal()).isEqualByComparingTo("5900.00");

        RevenueSummaryItem sep = summary.stream().filter(s -> s.groupKey().equals("2026-09")).findFirst().orElseThrow();
        assertThat(sep.invoiceCount()).isEqualTo(1);
        assertThat(sep.taxableValue()).isEqualByComparingTo("10000.00");
        assertThat(sep.grandTotal()).isEqualByComparingTo("11800.00");
    }

    @Test
    @DisplayName("Tax Summary by Rate Slab correctly aggregates across invoice lines")
    void testGetTaxSummaryByRateSlab() {
        Invoice inv = createTestInvoice("INV-001", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 15),
                InvoiceState.ISSUED, new BigDecimal("3000.00"), new BigDecimal("270.00"), new BigDecimal("270.00"),
                BigDecimal.ZERO, new BigDecimal("3540.00"));

        InvoiceLine line18 = new InvoiceLine();
        line18.setTaxCategory(TaxCategory.TAXABLE);
        line18.setTaxRatePercent(new BigDecimal("18.00"));
        line18.setTaxableValue(new BigDecimal("2000.00"));
        line18.setCgstAmount(new BigDecimal("180.00"));
        line18.setSgstAmount(new BigDecimal("180.00"));
        line18.setIgstAmount(BigDecimal.ZERO);
        line18.setCessAmount(BigDecimal.ZERO);

        InvoiceLine line5 = new InvoiceLine();
        line5.setTaxCategory(TaxCategory.TAXABLE);
        line5.setTaxRatePercent(new BigDecimal("5.00"));
        line5.setTaxableValue(new BigDecimal("1000.00"));
        line5.setCgstAmount(new BigDecimal("25.00"));
        line5.setSgstAmount(new BigDecimal("25.00"));
        line5.setIgstAmount(BigDecimal.ZERO);
        line5.setCessAmount(BigDecimal.ZERO);

        inv.setLines(List.of(line18, line5));

        when(invoiceRepository.findAllByOrganisationIdAndStateWithLines(organisationId, InvoiceState.ISSUED))
                .thenReturn(List.of(inv));

        ReportFilter filter = new ReportFilter(null, null, null, null);
        List<TaxSummaryItem> summary = reportService.getTaxSummaryByRateSlab(organisationId, filter);

        assertThat(summary).hasSize(2);
        TaxSummaryItem item5 = summary.stream().filter(s -> s.taxRatePercent().compareTo(new BigDecimal("5.00")) == 0).findFirst().orElseThrow();
        assertThat(item5.taxableValue()).isEqualByComparingTo("1000.00");
        assertThat(item5.totalTax()).isEqualByComparingTo("50.00");

        TaxSummaryItem item18 = summary.stream().filter(s -> s.taxRatePercent().compareTo(new BigDecimal("18.00")) == 0).findFirst().orElseThrow();
        assertThat(item18.taxableValue()).isEqualByComparingTo("2000.00");
        assertThat(item18.totalTax()).isEqualByComparingTo("360.00");
    }

    @Test
    @DisplayName("Receivables Ageing categorizes unpaid balances into ageing buckets")
    void testGetReceivablesAgeing() {
        LocalDate today = LocalDate.now();
        // Due today -> CURRENT
        Invoice invCurrent = createTestInvoice("INV-CUR", today, today, InvoiceState.ISSUED,
                new BigDecimal("1000.00"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("1000.00"));

        // Due 10 days ago -> DAYS_1_TO_30
        Invoice inv1To30 = createTestInvoice("INV-1-30", today.minusDays(15), today.minusDays(10), InvoiceState.ISSUED,
                new BigDecimal("2000.00"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("2000.00"));

        // Due 50 days ago -> DAYS_46_TO_60
        Invoice inv46To60 = createTestInvoice("INV-46-60", today.minusDays(60), today.minusDays(50), InvoiceState.ISSUED,
                new BigDecimal("3000.00"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("3000.00"));

        when(invoiceRepository.findAll(any(Specification.class))).thenReturn(List.of(invCurrent, inv1To30, inv46To60));
        when(receiptRepository.sumActiveAmountsByInvoiceIds(eq(organisationId), anyList())).thenReturn(Collections.emptyList());
        when(customerRepository.findAllById(anySet())).thenReturn(List.of(customer));

        ReportFilter filter = new ReportFilter(null, null, null, null);
        List<ReceivablesAgeingItem> ageing = reportService.getReceivablesAgeing(organisationId, filter);

        assertThat(ageing).hasSize(1);
        ReceivablesAgeingItem item = ageing.get(0);
        assertThat(item.customerLegalName()).isEqualTo("Acme Industries Pvt Ltd");
        assertThat(item.totalOutstanding()).isEqualByComparingTo("6000.00");
        assertThat(item.current()).isEqualByComparingTo("1000.00");
        assertThat(item.days1To30()).isEqualByComparingTo("2000.00");
        assertThat(item.days46To60()).isEqualByComparingTo("3000.00");
        assertThat(item.oldestDueDate()).isEqualTo(today.minusDays(50));
    }

    @Test
    @DisplayName("Receipts Register passes parameters to receiptService")
    void testGetReceiptsRegister() {
        ReceiptResponse response = new ReceiptResponse(
                UUID.randomUUID(),
                organisationId,
                UUID.randomUUID(),
                "INV-001",
                customerId,
                "Acme Industries Pvt Ltd",
                new BigDecimal("1180.00"),
                LocalDate.of(2026, 9, 5),
                PaymentMethod.NEFT,
                "REF-12345",
                null,
                false,
                null,
                null,
                null,
                null,
                null,
                null
        );

        when(receiptService.listReceipts(eq(organisationId), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response)));

        ReportFilter filter = new ReportFilter(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), customerId, null);
        List<ReceiptResponse> results = reportService.getReceiptsRegister(organisationId, filter, PaymentMethod.NEFT, false);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).invoiceNumber()).isEqualTo("INV-001");
        assertThat(results.get(0).amount()).isEqualByComparingTo("1180.00");
    }
}
