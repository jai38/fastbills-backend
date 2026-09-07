package com.fastbills.api.creditnote;

import com.fastbills.api.creditnote.pdf.CreditNotePdfService;
import com.fastbills.api.customer.Customer;
import com.fastbills.api.customer.CustomerRepository;
import com.fastbills.api.invoice.Invoice;
import com.fastbills.api.invoice.InvoiceState;
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

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CreditNotePdfServiceTest {

    @Mock
    private StorageService storageService;

    @Mock
    private OrganisationRepository organisationRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CreditNoteRepository creditNoteRepository;

    private CreditNotePdfService creditNotePdfService;

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

        creditNotePdfService = new CreditNotePdfService(
                templateEngine,
                storageService,
                organisationRepository,
                customerRepository,
                creditNoteRepository
        );
    }

    @Test
    @DisplayName("Generate Rule 53 PDF for Intra-state Credit Note (CGST + SGST, Rupee Symbol)")
    void testGeneratePdfIntraStateCreditNote() {
        Organisation org = new Organisation("FastBills Technologies Pvt Ltd");
        org.setId(UUID.randomUUID());
        org.setTradeName("FastBills");
        org.setGstin("29AABCF1234F1Z5");
        org.setPan("AABCF1234F");
        org.setAddressLine1("100 Feet Road, Indiranagar");
        org.setCity("Bangalore");
        org.setStateCode("29");
        org.setPincode("560038");

        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setLegalName("Acme Global Solutions India Pvt Ltd");
        customer.setGstin("29AAACG7890H1ZX");
        customer.setBillingAddressLine1("7th Main, Indiranagar");
        customer.setBillingCity("Bangalore");
        customer.setBillingStateCode("29");
        customer.setBillingPincode("560038");
        customer.setIsRegistered(true);

        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setOrganisationId(org.getId());
        invoice.setCustomerId(customer.getId());
        invoice.setInvoiceNumber("INV/2026-27/0001");
        invoice.setInvoiceDate(LocalDate.of(2026, 9, 1));
        invoice.setSupplyType(SupplyType.INTRA_STATE);
        invoice.setPlaceOfSupplyStateCode("29");
        invoice.setState(InvoiceState.ISSUED);
        invoice.setGrandTotal(new BigDecimal("11800.00"));

        CreditNote cn = new CreditNote();
        cn.setId(UUID.randomUUID());
        cn.setOrganisationId(org.getId());
        cn.setOriginalInvoice(invoice);
        cn.setCreditNoteNumber("CN/2026-27/0001");
        cn.setCreditNoteDate(LocalDate.of(2026, 9, 7));
        cn.setReason(CreditNoteReason.SALES_RETURN);
        cn.setReasonNotes("Goods damaged during transit");
        cn.setTaxableValue(new BigDecimal("1000.00"));
        cn.setTotalCgst(new BigDecimal("90.00"));
        cn.setTotalSgst(new BigDecimal("90.00"));
        cn.setTotalIgst(BigDecimal.ZERO);
        cn.setTotalCess(BigDecimal.ZERO);
        cn.setTotalTax(new BigDecimal("180.00"));
        cn.setRoundOff(BigDecimal.ZERO);
        cn.setGrandTotal(new BigDecimal("1180.00"));
        cn.setState(CreditNoteState.ISSUED);

        CreditNoteLine line = new CreditNoteLine();
        line.setId(UUID.randomUUID());
        line.setItemDescription("Damaged Software Box");
        line.setHsnSacCode("998313");
        line.setQuantity(new BigDecimal("1.000"));
        line.setUnitPrice(new BigDecimal("1000.00"));
        line.setTaxableValue(new BigDecimal("1000.00"));
        line.setTaxCategory(TaxCategory.TAXABLE);
        line.setTaxRatePercent(new BigDecimal("18.00"));
        line.setCgstRatePercent(new BigDecimal("9.00"));
        line.setCgstAmount(new BigDecimal("90.00"));
        line.setSgstRatePercent(new BigDecimal("9.00"));
        line.setSgstAmount(new BigDecimal("90.00"));
        line.setIgstRatePercent(BigDecimal.ZERO);
        line.setIgstAmount(BigDecimal.ZERO);
        line.setTotalTaxAmount(new BigDecimal("180.00"));
        line.setLineTotal(new BigDecimal("1180.00"));
        cn.addLine(line);

        byte[] pdfBytes = creditNotePdfService.generatePdf(cn, org, customer);

        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(1000);

        String header = new String(pdfBytes, 0, 5, StandardCharsets.US_ASCII);
        assertThat(header).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("Generate Rule 53 PDF for Inter-state Credit Note (IGST)")
    void testGeneratePdfInterStateCreditNote() {
        Organisation org = new Organisation("FastBills Technologies Pvt Ltd");
        org.setId(UUID.randomUUID());
        org.setGstin("29AABCF1234F1Z5");
        org.setStateCode("29");

        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setLegalName("Mumbai Corp");
        customer.setGstin("27AAACM1234M1Z1");
        customer.setBillingStateCode("27");
        customer.setIsRegistered(true);

        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setOrganisationId(org.getId());
        invoice.setCustomerId(customer.getId());
        invoice.setInvoiceNumber("INV/2026-27/0002");
        invoice.setInvoiceDate(LocalDate.of(2026, 9, 2));
        invoice.setSupplyType(SupplyType.INTER_STATE);
        invoice.setPlaceOfSupplyStateCode("27");
        invoice.setState(InvoiceState.ISSUED);
        invoice.setGrandTotal(new BigDecimal("23600.00"));

        CreditNote cn = new CreditNote();
        cn.setId(UUID.randomUUID());
        cn.setOrganisationId(org.getId());
        cn.setOriginalInvoice(invoice);
        cn.setCreditNoteNumber("CN/2026-27/0002");
        cn.setCreditNoteDate(LocalDate.of(2026, 9, 7));
        cn.setReason(CreditNoteReason.POST_SALE_DISCOUNT);
        cn.setTaxableValue(new BigDecimal("2000.00"));
        cn.setTotalCgst(BigDecimal.ZERO);
        cn.setTotalSgst(BigDecimal.ZERO);
        cn.setTotalIgst(new BigDecimal("360.00"));
        cn.setTotalCess(BigDecimal.ZERO);
        cn.setTotalTax(new BigDecimal("360.00"));
        cn.setRoundOff(BigDecimal.ZERO);
        cn.setGrandTotal(new BigDecimal("2360.00"));
        cn.setState(CreditNoteState.ISSUED);

        CreditNoteLine line = new CreditNoteLine();
        line.setId(UUID.randomUUID());
        line.setItemDescription("Post-sale Volume Rebate");
        line.setHsnSacCode("998313");
        line.setQuantity(new BigDecimal("1.000"));
        line.setUnitPrice(new BigDecimal("2000.00"));
        line.setTaxableValue(new BigDecimal("2000.00"));
        line.setTaxCategory(TaxCategory.TAXABLE);
        line.setTaxRatePercent(new BigDecimal("18.00"));
        line.setIgstRatePercent(new BigDecimal("18.00"));
        line.setIgstAmount(new BigDecimal("360.00"));
        line.setTotalTaxAmount(new BigDecimal("360.00"));
        line.setLineTotal(new BigDecimal("2360.00"));
        cn.addLine(line);

        byte[] pdfBytes = creditNotePdfService.generatePdf(cn, org, customer);

        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(1000);

        String header = new String(pdfBytes, 0, 5, StandardCharsets.US_ASCII);
        assertThat(header).isEqualTo("%PDF-");
    }
}
