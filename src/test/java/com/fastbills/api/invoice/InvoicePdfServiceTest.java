package com.fastbills.api.invoice;

import com.fastbills.api.customer.Customer;
import com.fastbills.api.customer.CustomerRepository;
import com.fastbills.api.invoice.pdf.InvoicePdfService;
import com.fastbills.api.organisation.Organisation;
import com.fastbills.api.organisation.OrganisationRepository;
import com.fastbills.api.storage.StorageService;
import com.fastbills.api.tax.SupplyType;
import com.fastbills.api.tax.TaxCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoicePdfServiceTest {

    @Mock
    private StorageService storageService;

    @Mock
    private OrganisationRepository organisationRepository;

    @Mock
    private CustomerRepository customerRepository;

    private InvoicePdfService invoicePdfService;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);

        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(resolver);

        invoicePdfService = new InvoicePdfService(
                templateEngine,
                storageService,
                organisationRepository,
                customerRepository
        );
    }

    @Test
    @DisplayName("Generate PDF for Worked Example A (Intra-state B2B, 18% GST)")
    void testGeneratePdfWorkedExampleA() throws Exception {
        UUID orgId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        Organisation org = new Organisation("FastBills Technologies Pvt Ltd");
        org.setId(orgId);
        org.setTradeName("FastBills");
        org.setGstin("29AABCF1234F1Z5");
        org.setPan("AABCF1234F");
        org.setAddressLine1("100 Feet Road, Indiranagar");
        org.setCity("Bangalore");
        org.setStateCode("29");
        org.setPincode("560038");
        org.setEmail("billing@fastbills.com");
        org.setPhone("+91 80 1234 5678");
        org.setBankName("HDFC Bank");
        org.setBankAccountName("FastBills Technologies Pvt Ltd");
        org.setBankAccountNumber("50200012345678");
        org.setBankIfsc("HDFC0001234");
        org.setBankBranch("Indiranagar, Bangalore");
        org.setUpiId("fastbills@okhdfcbank");

        Customer customer = new Customer(orgId, "Acme Global Solutions India Pvt Ltd", "29");
        customer.setId(customerId);
        customer.setTradeName("Acme India");
        customer.setGstin("29AAACG7890H1ZX");
        customer.setPan("AAACG7890H");
        customer.setIsRegistered(true);
        customer.setBillingAddressLine1("45, MG Road");
        customer.setBillingCity("Bangalore");
        customer.setBillingStateCode("29");
        customer.setBillingPincode("560001");

        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setOrganisationId(orgId);
        invoice.setCustomerId(customerId);
        invoice.setInvoiceNumber("INV/2024-25/0001");
        invoice.setInvoiceDate(LocalDate.of(2026, 9, 7));
        invoice.setDueDate(LocalDate.of(2026, 10, 7));
        invoice.setPlaceOfSupplyStateCode("29");
        invoice.setSupplyType(SupplyType.INTRA_STATE);
        invoice.setCollectionReference("INV/2024-25/0001 - ACME");

        // Line 1: Enterprise Software Subscription (Qty 1 @ 10,000, 18% GST -> 900 CGST + 900 SGST)
        InvoiceLine line = new InvoiceLine();
        line.setInvoice(invoice);
        line.setLineNumber(1);
        line.setItemDescription("Enterprise Cloud ERP Subscription (Annual)");
        line.setHsnOrSacCode("998313");
        line.setIsService(true);
        line.setQuantity(new BigDecimal("1.000"));
        line.setUnitOfMeasure("YR");
        line.setUnitPrice(new BigDecimal("10000.00"));
        line.setDiscountPercent(BigDecimal.ZERO);
        line.setDiscountAmount(BigDecimal.ZERO.setScale(2));
        line.setTaxableValue(new BigDecimal("10000.00"));
        line.setTaxCategory(TaxCategory.TAXABLE);
        line.setTaxRatePercent(new BigDecimal("18.00"));
        line.setCgstRatePercent(new BigDecimal("9.00"));
        line.setCgstAmount(new BigDecimal("900.00"));
        line.setSgstRatePercent(new BigDecimal("9.00"));
        line.setSgstAmount(new BigDecimal("900.00"));
        line.setIgstRatePercent(BigDecimal.ZERO.setScale(2));
        line.setIgstAmount(BigDecimal.ZERO.setScale(2));
        line.setCessRatePercent(BigDecimal.ZERO.setScale(2));
        line.setCessAmount(BigDecimal.ZERO.setScale(2));
        line.setLineTotal(new BigDecimal("11800.00"));

        invoice.getLines().add(line);
        invoice.setSubtotalBeforeDiscount(new BigDecimal("10000.00"));
        invoice.setTotalDiscount(BigDecimal.ZERO.setScale(2));
        invoice.setTaxableValue(new BigDecimal("10000.00"));
        invoice.setTotalCgst(new BigDecimal("900.00"));
        invoice.setTotalSgst(new BigDecimal("900.00"));
        invoice.setTotalIgst(BigDecimal.ZERO.setScale(2));
        invoice.setTotalCess(BigDecimal.ZERO.setScale(2));
        invoice.setTotalTax(new BigDecimal("1800.00"));
        invoice.setRoundOff(BigDecimal.ZERO.setScale(2));
        invoice.setGrandTotal(new BigDecimal("11800.00"));
        invoice.setAmountInWords("Eleven Thousand Eight Hundred Rupees Only");

        byte[] pdfBytes = invoicePdfService.generatePdf(invoice, org, customer);

        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(1000);

        // Verify PDF Magic Bytes %PDF-
        String header = new String(pdfBytes, 0, 5, StandardCharsets.US_ASCII);
        assertThat(header).isEqualTo("%PDF-");

        // Save sample PDF to target for inspection
        File outputDir = new File("target/sample-invoices");
        outputDir.mkdirs();
        File sampleFile = new File(outputDir, "worked_example_a.pdf");
        try (FileOutputStream fos = new FileOutputStream(sampleFile)) {
            fos.write(pdfBytes);
        }
        assertThat(sampleFile.exists()).isTrue();
    }

    @Test
    @DisplayName("Generate PDF for Worked Example B (Inter-state B2B, Multi-slab 5%, 12%, 18%, 28% + Cess)")
    void testGeneratePdfWorkedExampleB() throws Exception {
        UUID orgId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        Organisation org = new Organisation("FastBills Technologies Pvt Ltd");
        org.setId(orgId);
        org.setGstin("29AABCF1234F1Z5"); // Karnataka (29)
        org.setStateCode("29");

        Customer customer = new Customer(orgId, "Apex Retail Enterprises", "27"); // Maharashtra (27)
        customer.setId(customerId);
        customer.setGstin("27AABCA5678B1Z2");
        customer.setBillingStateCode("27");
        customer.setIsRegistered(true);

        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setOrganisationId(orgId);
        invoice.setCustomerId(customerId);
        invoice.setInvoiceNumber("INV/2024-25/0002");
        invoice.setInvoiceDate(LocalDate.of(2026, 9, 7));
        invoice.setDueDate(LocalDate.of(2026, 10, 7));
        invoice.setPlaceOfSupplyStateCode("27");
        invoice.setSupplyType(SupplyType.INTER_STATE);

        // Line 1: 5% item
        InvoiceLine line1 = new InvoiceLine();
        line1.setInvoice(invoice);
        line1.setLineNumber(1);
        line1.setItemDescription("Organic Edible Oil (5L)");
        line1.setHsnOrSacCode("1512");
        line1.setQuantity(new BigDecimal("10.000"));
        line1.setUnitPrice(new BigDecimal("800.00"));
        line1.setTaxableValue(new BigDecimal("8000.00"));
        line1.setTaxCategory(TaxCategory.TAXABLE);
        line1.setTaxRatePercent(new BigDecimal("5.00"));
        line1.setIgstRatePercent(new BigDecimal("5.00"));
        line1.setIgstAmount(new BigDecimal("400.00"));
        line1.setLineTotal(new BigDecimal("8400.00"));
        invoice.getLines().add(line1);

        // Line 2: 12% item
        InvoiceLine line2 = new InvoiceLine();
        line2.setInvoice(invoice);
        line2.setLineNumber(2);
        line2.setItemDescription("Processed Almond Butter (500g)");
        line2.setHsnOrSacCode("2008");
        line2.setQuantity(new BigDecimal("20.000"));
        line2.setUnitPrice(new BigDecimal("450.00"));
        line2.setTaxableValue(new BigDecimal("9000.00"));
        line2.setTaxCategory(TaxCategory.TAXABLE);
        line2.setTaxRatePercent(new BigDecimal("12.00"));
        line2.setIgstRatePercent(new BigDecimal("12.00"));
        line2.setIgstAmount(new BigDecimal("1080.00"));
        line2.setLineTotal(new BigDecimal("10080.00"));
        invoice.getLines().add(line2);

        // Line 3: 18% item
        InvoiceLine line3 = new InvoiceLine();
        line3.setInvoice(invoice);
        line3.setLineNumber(3);
        line3.setItemDescription("Warehouse Inventory Management Software");
        line3.setHsnOrSacCode("998314");
        line3.setQuantity(new BigDecimal("1.000"));
        line3.setUnitPrice(new BigDecimal("25000.00"));
        line3.setTaxableValue(new BigDecimal("25000.00"));
        line3.setTaxCategory(TaxCategory.TAXABLE);
        line3.setTaxRatePercent(new BigDecimal("18.00"));
        line3.setIgstRatePercent(new BigDecimal("18.00"));
        line3.setIgstAmount(new BigDecimal("4500.00"));
        line3.setLineTotal(new BigDecimal("29500.00"));
        invoice.getLines().add(line3);

        // Line 4: 28% item with 12% Cess
        InvoiceLine line4 = new InvoiceLine();
        line4.setInvoice(invoice);
        line4.setLineNumber(4);
        line4.setItemDescription("Luxury Aerated Flavoured Beverage (Cans)");
        line4.setHsnOrSacCode("220210");
        line4.setQuantity(new BigDecimal("50.000"));
        line4.setUnitPrice(new BigDecimal("200.00"));
        line4.setTaxableValue(new BigDecimal("10000.00"));
        line4.setTaxCategory(TaxCategory.TAXABLE);
        line4.setTaxRatePercent(new BigDecimal("28.00"));
        line4.setIgstRatePercent(new BigDecimal("28.00"));
        line4.setIgstAmount(new BigDecimal("2800.00"));
        line4.setCessRatePercent(new BigDecimal("12.00"));
        line4.setCessAmount(new BigDecimal("1200.00"));
        line4.setLineTotal(new BigDecimal("14000.00"));
        invoice.getLines().add(line4);

        invoice.setSubtotalBeforeDiscount(new BigDecimal("52000.00"));
        invoice.setTotalDiscount(BigDecimal.ZERO.setScale(2));
        invoice.setTaxableValue(new BigDecimal("52000.00"));
        invoice.setTotalCgst(BigDecimal.ZERO.setScale(2));
        invoice.setTotalSgst(BigDecimal.ZERO.setScale(2));
        invoice.setTotalIgst(new BigDecimal("8780.00"));
        invoice.setTotalCess(new BigDecimal("1200.00"));
        invoice.setTotalTax(new BigDecimal("9980.00"));
        invoice.setRoundOff(BigDecimal.ZERO.setScale(2));
        invoice.setGrandTotal(new BigDecimal("61980.00"));
        invoice.setAmountInWords("Sixty-One Thousand Nine Hundred and Eighty Rupees Only");

        byte[] pdfBytes = invoicePdfService.generatePdf(invoice, org, customer);

        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(1000);

        String header = new String(pdfBytes, 0, 5, StandardCharsets.US_ASCII);
        assertThat(header).isEqualTo("%PDF-");

        File outputDir = new File("target/sample-invoices");
        outputDir.mkdirs();
        File sampleFile = new File(outputDir, "worked_example_b.pdf");
        try (FileOutputStream fos = new FileOutputStream(sampleFile)) {
            fos.write(pdfBytes);
        }
        assertThat(sampleFile.exists()).isTrue();
    }

    @Test
    @DisplayName("Generate and Store PDF updates storage and returns key")
    void testGenerateAndStorePdf() {
        UUID orgId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        Organisation org = new Organisation("Test Org");
        org.setId(orgId);
        org.setStateCode("27"); // Maharashtra

        Customer customer = new Customer(orgId, "Customer B", "29"); // Karnataka (Inter-state)
        customer.setId(customerId);

        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        invoice.setOrganisationId(orgId);
        invoice.setCustomerId(customerId);
        invoice.setInvoiceNumber("INV/2024-25/0002");
        invoice.setInvoiceDate(LocalDate.now());
        invoice.setDueDate(LocalDate.now().plusDays(30));
        invoice.setPlaceOfSupplyStateCode("29");
        invoice.setSupplyType(SupplyType.INTER_STATE);

        InvoiceLine line = new InvoiceLine();
        line.setInvoice(invoice);
        line.setLineNumber(1);
        line.setItemDescription("Consulting Services");
        line.setQuantity(new BigDecimal("1.000"));
        line.setUnitPrice(new BigDecimal("5000.00"));
        line.setTaxableValue(new BigDecimal("5000.00"));
        line.setTaxCategory(TaxCategory.TAXABLE);
        line.setTaxRatePercent(new BigDecimal("18.00"));
        line.setIgstRatePercent(new BigDecimal("18.00"));
        line.setIgstAmount(new BigDecimal("900.00"));
        line.setLineTotal(new BigDecimal("5900.00"));
        invoice.getLines().add(line);

        invoice.setTaxableValue(new BigDecimal("5000.00"));
        invoice.setTotalIgst(new BigDecimal("900.00"));
        invoice.setTotalTax(new BigDecimal("900.00"));
        invoice.setGrandTotal(new BigDecimal("5900.00"));

        when(storageService.store(any(), any(), any(), any(Long.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        String key = invoicePdfService.generateAndStorePdf(invoice, org, customer);

        assertThat(key).isEqualTo("invoices/" + orgId + "/" + invoiceId + ".pdf");
    }
}
