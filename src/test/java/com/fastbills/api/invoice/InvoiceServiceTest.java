package com.fastbills.api.invoice;

import com.fastbills.api.customer.Customer;
import com.fastbills.api.customer.CustomerRepository;
import com.fastbills.api.invoice.dto.*;
import com.fastbills.api.organisation.Organisation;
import com.fastbills.api.organisation.OrganisationRepository;
import com.fastbills.api.series.AllocatedNumber;
import com.fastbills.api.series.DocumentType;
import com.fastbills.api.series.NumberSeriesService;
import com.fastbills.api.tax.SupplyType;
import com.fastbills.api.tax.TaxCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private OrganisationRepository organisationRepository;

    @Mock
    private NumberSeriesService numberSeriesService;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private com.fastbills.api.receipt.ReceiptRepository receiptRepository;

    @Mock
    private com.fastbills.api.invoice.pdf.InvoicePdfService invoicePdfService;

    @Mock
    private com.fastbills.api.storage.StorageService storageService;

    @InjectMocks
    private InvoiceService invoiceService;

    private UUID organisationId;
    private UUID customerId;
    private UUID actorUserId;
    private Organisation organisation;
    private Customer customer;

    @BeforeEach
    void setUp() {
        organisationId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        actorUserId = UUID.randomUUID();

        organisation = new Organisation();
        organisation.setId(organisationId);
        organisation.setLegalName("Apex Fab Ltd");
        organisation.setGstin("27AAPCA1234A1Z5");
        organisation.setStateCode("27");
        organisation.setInvoicePrefix("INV");
        organisation.setDefaultCreditDays(30);
        organisation.setDefaultTermsText("Payment due within terms");

        customer = new Customer();
        customer.setId(customerId);
        customer.setOrganisationId(organisationId);
        customer.setLegalName("Bharat Dynamics");
        customer.setBillingStateCode("27");
        customer.setIsArchived(false);
        customer.setCreditDays(15);
    }

    @Test
    @DisplayName("Create draft: Successfully creates draft invoice with intra-state taxes and derived due date")
    void testCreateDraft_Success() {
        when(organisationRepository.findById(organisationId)).thenReturn(Optional.of(organisation));
        when(customerRepository.findByIdAndOrganisationId(customerId, organisationId)).thenReturn(Optional.of(customer));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        InvoiceLineRequest line = new InvoiceLineRequest(
                1,
                "CNC Milling Services",
                "9988",
                true,
                new BigDecimal("10.000"),
                "HRS",
                new BigDecimal("1000.00"),
                BigDecimal.ZERO,
                null,
                TaxCategory.TAXABLE,
                new BigDecimal("18.00")
        );

        CreateDraftInvoiceRequest req = new CreateDraftInvoiceRequest(
                customerId,
                LocalDate.of(2024, 7, 1),
                null,
                null,
                "27",
                false,
                "PO-99",
                LocalDate.of(2024, 6, 28),
                "Customer project Alpha",
                null,
                List.of(line)
        );

        InvoiceResponse response = invoiceService.createDraft(organisationId, actorUserId, req);

        assertThat(response.state()).isEqualTo(InvoiceState.DRAFT);
        assertThat(response.supplyType()).isEqualTo(SupplyType.INTRA_STATE);
        assertThat(response.agreedCreditDays()).isEqualTo(15); // Derived from customer
        assertThat(response.dueDate()).isEqualTo(LocalDate.of(2024, 7, 16)); // 1 + 15
        assertThat(response.taxableValue()).isEqualByComparingTo("10000.00");
        assertThat(response.totalCgst()).isEqualByComparingTo("900.00");
        assertThat(response.totalSgst()).isEqualByComparingTo("900.00");
        assertThat(response.grandTotal()).isEqualByComparingTo("11800.00");

        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Create draft: Rejects creating invoice for an archived customer")
    void testCreateDraft_ArchivedCustomer_ThrowsException() {
        customer.setIsArchived(true);
        when(organisationRepository.findById(organisationId)).thenReturn(Optional.of(organisation));
        when(customerRepository.findByIdAndOrganisationId(customerId, organisationId)).thenReturn(Optional.of(customer));

        CreateDraftInvoiceRequest req = new CreateDraftInvoiceRequest(
                customerId,
                LocalDate.now(),
                null,
                null,
                "27",
                false,
                null,
                null,
                null,
                null,
                List.of()
        );

        assertThatThrownBy(() -> invoiceService.createDraft(organisationId, actorUserId, req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Cannot create an invoice for an archived customer");
    }

    @Test
    @DisplayName("Update draft: Throws conflict if invoice is already issued (immutability)")
    void testUpdateDraft_IssuedInvoice_ThrowsConflict() {
        UUID invoiceId = UUID.randomUUID();
        Invoice issuedInvoice = new Invoice();
        issuedInvoice.setId(invoiceId);
        issuedInvoice.setOrganisationId(organisationId);
        issuedInvoice.setState(InvoiceState.ISSUED);

        when(invoiceRepository.findByIdAndOrganisationIdWithLines(invoiceId, organisationId))
                .thenReturn(Optional.of(issuedInvoice));

        UpdateDraftInvoiceRequest req = new UpdateDraftInvoiceRequest(
                customerId,
                LocalDate.now(),
                null,
                null,
                "27",
                false,
                null,
                null,
                null,
                null,
                List.of()
        );

        assertThatThrownBy(() -> invoiceService.updateDraft(organisationId, invoiceId, actorUserId, req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Issued invoices are immutable");
    }

    @Test
    @DisplayName("Issue invoice: Freezes totals, allocates consecutive number, generates collection reference")
    void testIssueInvoice_Success() {
        UUID invoiceId = UUID.randomUUID();
        Invoice draft = new Invoice();
        draft.setId(invoiceId);
        draft.setOrganisationId(organisationId);
        draft.setCustomerId(customerId);
        draft.setState(InvoiceState.DRAFT);
        draft.setInvoiceDate(LocalDate.of(2024, 8, 1));
        draft.setPlaceOfSupplyStateCode("27");
        draft.setAgreedCreditDays(30);

        InvoiceLine line = new InvoiceLine();
        line.setLineNumber(1);
        line.setItemDescription("Laser cutting");
        line.setQuantity(new BigDecimal("5.000"));
        line.setUnitOfMeasure("NOS");
        line.setUnitPrice(new BigDecimal("2000.00"));
        line.setTaxRatePercent(new BigDecimal("18.00"));
        line.setTaxCategory(TaxCategory.TAXABLE);
        draft.addLine(line);

        when(invoiceRepository.findByIdAndOrganisationIdWithLines(invoiceId, organisationId))
                .thenReturn(Optional.of(draft));
        when(organisationRepository.findById(organisationId)).thenReturn(Optional.of(organisation));
        when(customerRepository.findByIdAndOrganisationId(customerId, organisationId)).thenReturn(Optional.of(customer));

        UUID seriesId = UUID.randomUUID();
        when(numberSeriesService.allocateNextNumber(eq(organisationId), eq(DocumentType.INVOICE), eq(draft.getInvoiceDate()), eq("INV")))
                .thenReturn(new AllocatedNumber(seriesId, 1, "INV/24-25/0001", 2024, "24-25"));

        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));
        when(invoicePdfService.generateAndStorePdf(any(Invoice.class), any(Organisation.class), any(Customer.class)))
                .thenReturn("invoices/" + organisationId + "/" + invoiceId + ".pdf");

        InvoiceResponse response = invoiceService.issueInvoice(organisationId, invoiceId, actorUserId, "127.0.0.1");

        assertThat(response.state()).isEqualTo(InvoiceState.ISSUED);
        assertThat(response.invoiceNumber()).isEqualTo("INV/24-25/0001");
        assertThat(response.collectionReference()).matches("^1-[23456789ABCDEFGHJKMNPQRSTUVWXYZ]{4}$");
        assertThat(response.taxableValue()).isEqualByComparingTo("10000.00");
        assertThat(response.totalCgst()).isEqualByComparingTo("900.00");
        assertThat(response.totalSgst()).isEqualByComparingTo("900.00");
        assertThat(response.grandTotal()).isEqualByComparingTo("11800.00");
        assertThat(response.amountInWords()).contains("Eleven Thousand Eight Hundred Rupees Only");

        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Issue invoice: Rejects invoice date before the latest issued invoice date in the series")
    void testIssueInvoice_RejectsDateBeforeLatestIssuedDate() {
        UUID invoiceId = UUID.randomUUID();
        Invoice draft = new Invoice();
        draft.setId(invoiceId);
        draft.setOrganisationId(organisationId);
        draft.setCustomerId(customerId);
        draft.setState(InvoiceState.DRAFT);
        draft.setInvoiceDate(LocalDate.of(2026, 9, 8));
        draft.setPlaceOfSupplyStateCode("27");
        InvoiceLine line = new InvoiceLine();
        line.setLineNumber(1);
        line.setItemDescription("Test Service");
        line.setQuantity(BigDecimal.ONE);
        line.setUnitPrice(new BigDecimal("100.00"));
        line.setTaxRatePercent(new BigDecimal("18.00"));
        line.setTaxCategory(TaxCategory.TAXABLE);
        draft.addLine(line);

        when(invoiceRepository.findByIdAndOrganisationIdWithLines(invoiceId, organisationId))
                .thenReturn(Optional.of(draft));
        when(organisationRepository.findById(organisationId)).thenReturn(Optional.of(organisation));
        when(customerRepository.findByIdAndOrganisationId(customerId, organisationId)).thenReturn(Optional.of(customer));
        when(invoiceRepository.findLatestIssuedInvoiceDate(eq(organisationId), anyInt()))
                .thenReturn(Optional.of(LocalDate.of(2026, 9, 10)));

        assertThatThrownBy(() -> invoiceService.issueInvoice(organisationId, invoiceId, actorUserId, "127.0.0.1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("cannot be earlier than the latest issued invoice date (10-09-2026)");
    }

    @Test
    @DisplayName("Cancel invoice: Only OWNER role may cancel an issued invoice")
    void testCancelInvoice_StaffRole_Forbidden() {
        UUID invoiceId = UUID.randomUUID();
        assertThatThrownBy(() -> invoiceService.cancelInvoice(
                organisationId,
                invoiceId,
                actorUserId,
                "STAFF",
                "Customer cancelled order",
                "127.0.0.1"
        )).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Only organisation owners may cancel an invoice");
    }

    @Test
    @DisplayName("Cancel invoice: Owner cancels issued invoice successfully with reason")
    void testCancelInvoice_Owner_Success() {
        UUID invoiceId = UUID.randomUUID();
        Invoice issued = new Invoice();
        issued.setId(invoiceId);
        issued.setOrganisationId(organisationId);
        issued.setCustomerId(customerId);
        issued.setState(InvoiceState.ISSUED);
        issued.setInvoiceNumber("INV/24-25/0001");

        when(invoiceRepository.findByIdAndOrganisationIdWithLines(invoiceId, organisationId))
                .thenReturn(Optional.of(issued));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));
        when(customerRepository.findByIdAndOrganisationId(customerId, organisationId)).thenReturn(Optional.of(customer));
        when(organisationRepository.findById(organisationId)).thenReturn(Optional.of(organisation));

        InvoiceResponse response = invoiceService.cancelInvoice(
                organisationId,
                invoiceId,
                actorUserId,
                "OWNER",
                "Dispute over billing items resolved via credit note",
                "127.0.0.1"
        );

        assertThat(response.state()).isEqualTo(InvoiceState.CANCELLED);
        assertThat(response.cancellationReason()).isEqualTo("Dispute over billing items resolved via credit note");
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Cancel invoice: Refuse cancellation if non-reversed receipts exist")
    void testCancelInvoice_WithActiveReceipts_ThrowsConflict() {
        UUID invoiceId = UUID.randomUUID();
        Invoice issued = new Invoice();
        issued.setId(invoiceId);
        issued.setOrganisationId(organisationId);
        issued.setState(InvoiceState.ISSUED);

        when(invoiceRepository.findByIdAndOrganisationIdWithLines(invoiceId, organisationId))
                .thenReturn(Optional.of(issued));
        when(receiptRepository.existsByOrganisationIdAndInvoiceIdAndIsReversedFalse(organisationId, invoiceId))
                .thenReturn(true);

        assertThatThrownBy(() -> invoiceService.cancelInvoice(
                organisationId,
                invoiceId,
                actorUserId,
                "OWNER",
                "Try cancel",
                "127.0.0.1"
        )).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("non-reversed receipt");
    }

    @Test
    @DisplayName("Derived payment status: Correctly computes UNPAID, OVERDUE, PARTIALLY_PAID, and PAID")
    void testDerivedPaymentStatus() {
        Invoice inv = new Invoice();
        inv.setState(InvoiceState.ISSUED);
        inv.setGrandTotal(new BigDecimal("10000.00"));
        inv.setDueDate(LocalDate.now().plusDays(5));

        // 0 received, not overdue -> UNPAID
        assertThat(InvoiceService.derivePaymentStatus(inv, BigDecimal.ZERO)).isEqualTo("UNPAID");

        // 0 received, overdue -> OVERDUE
        inv.setDueDate(LocalDate.now().minusDays(1));
        assertThat(InvoiceService.derivePaymentStatus(inv, BigDecimal.ZERO)).isEqualTo("OVERDUE");

        // Partially received, not overdue -> PARTIALLY_PAID
        inv.setDueDate(LocalDate.now().plusDays(5));
        assertThat(InvoiceService.derivePaymentStatus(inv, new BigDecimal("4000.00"))).isEqualTo("PARTIALLY_PAID");

        // Partially received, overdue -> PARTIALLY_PAID_OVERDUE
        inv.setDueDate(LocalDate.now().minusDays(1));
        assertThat(InvoiceService.derivePaymentStatus(inv, new BigDecimal("4000.00"))).isEqualTo("PARTIALLY_PAID_OVERDUE");

        // Fully received -> PAID
        assertThat(InvoiceService.derivePaymentStatus(inv, new BigDecimal("10000.00"))).isEqualTo("PAID");

        // Cancelled invoice -> CANCELLED
        inv.setState(InvoiceState.CANCELLED);
        assertThat(InvoiceService.derivePaymentStatus(inv, new BigDecimal("10000.00"))).isEqualTo("CANCELLED");
    }

    @Test
    @DisplayName("Get Invoice PDF: Returns stored resource when available")
    void testGetInvoicePdf_Stored() {
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        invoice.setOrganisationId(organisationId);
        invoice.setCustomerId(customerId);
        invoice.setInvoiceNumber("INV/24-25/0001");
        invoice.setPdfObjectKey("invoices/" + organisationId + "/" + invoiceId + ".pdf");

        org.springframework.core.io.Resource mockResource = mock(org.springframework.core.io.Resource.class);
        when(mockResource.exists()).thenReturn(true);

        when(invoiceRepository.findByIdAndOrganisationIdWithLines(invoiceId, organisationId))
                .thenReturn(Optional.of(invoice));
        when(organisationRepository.findById(organisationId)).thenReturn(Optional.of(organisation));
        when(customerRepository.findByIdAndOrganisationId(customerId, organisationId)).thenReturn(Optional.of(customer));
        when(storageService.load(invoice.getPdfObjectKey())).thenReturn(mockResource);

        var pdfResource = invoiceService.getInvoicePdf(organisationId, invoiceId);
        assertThat(pdfResource).isNotNull();
        assertThat(pdfResource.filename()).isEqualTo("INV_24-25_0001.pdf");
        assertThat(pdfResource.resource()).isEqualTo(mockResource);
    }

    @Test
    @DisplayName("Get Invoice PDF: Generates on demand when not pre-stored")
    void testGetInvoicePdf_OnDemand() {
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        invoice.setOrganisationId(organisationId);
        invoice.setCustomerId(customerId);
        invoice.setInvoiceNumber("INV/24-25/0002");

        when(invoiceRepository.findByIdAndOrganisationIdWithLines(invoiceId, organisationId))
                .thenReturn(Optional.of(invoice));
        when(organisationRepository.findById(organisationId)).thenReturn(Optional.of(organisation));
        when(customerRepository.findByIdAndOrganisationId(customerId, organisationId)).thenReturn(Optional.of(customer));
        when(invoicePdfService.generatePdf(any(), any(), any())).thenReturn("%PDF-test".getBytes());

        var pdfResource = invoiceService.getInvoicePdf(organisationId, invoiceId);
        assertThat(pdfResource).isNotNull();
        assertThat(pdfResource.filename()).isEqualTo("INV_24-25_0002.pdf");
        assertThat(pdfResource.resource().exists()).isTrue();
    }
}
