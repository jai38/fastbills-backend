package com.fastbills.api.creditnote;

import com.fastbills.api.creditnote.dto.CancelCreditNoteRequest;
import com.fastbills.api.creditnote.dto.CreateCreditNoteRequest;
import com.fastbills.api.creditnote.dto.CreditNoteResponse;
import com.fastbills.api.creditnote.pdf.CreditNotePdfService;
import com.fastbills.api.customer.Customer;
import com.fastbills.api.customer.CustomerRepository;
import com.fastbills.api.invoice.AuditLogRepository;
import com.fastbills.api.invoice.Invoice;
import com.fastbills.api.invoice.InvoiceLine;
import com.fastbills.api.invoice.InvoiceRepository;
import com.fastbills.api.invoice.InvoiceState;
import com.fastbills.api.organisation.Organisation;
import com.fastbills.api.organisation.OrganisationRepository;
import com.fastbills.api.series.AllocatedNumber;
import com.fastbills.api.series.DocumentType;
import com.fastbills.api.series.NumberSeriesService;
import com.fastbills.api.storage.StorageService;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreditNoteServiceTest {

    @Mock
    private CreditNoteRepository creditNoteRepository;

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
    private CreditNotePdfService creditNotePdfService;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private CreditNoteService creditNoteService;

    private UUID organisationId;
    private UUID customerId;
    private UUID invoiceId;
    private UUID actorUserId;
    private Organisation organisation;
    private Customer customer;
    private Invoice invoice;

    @BeforeEach
    void setUp() {
        organisationId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        invoiceId = UUID.randomUUID();
        actorUserId = UUID.randomUUID();

        organisation = new Organisation("FastBills Technologies Pvt Ltd");
        organisation.setId(organisationId);
        organisation.setGstin("29AABCF1234F1Z5");
        organisation.setStateCode("29");

        customer = new Customer();
        customer.setId(customerId);
        customer.setLegalName("Acme Corp");
        customer.setGstin("29AAACG7890H1ZX");
        customer.setBillingStateCode("29");
        customer.setIsRegistered(true);

        invoice = new Invoice();
        invoice.setId(invoiceId);
        invoice.setOrganisationId(organisationId);
        invoice.setCustomerId(customerId);
        invoice.setInvoiceNumber("INV/2026-27/0001");
        invoice.setInvoiceDate(LocalDate.of(2026, 9, 1));
        invoice.setDueDate(LocalDate.of(2026, 9, 30));
        invoice.setSupplyType(SupplyType.INTRA_STATE);
        invoice.setPlaceOfSupplyStateCode("29");
        invoice.setState(InvoiceState.ISSUED);
        invoice.setTaxableValue(new BigDecimal("10000.00"));
        invoice.setTotalCgst(new BigDecimal("900.00"));
        invoice.setTotalSgst(new BigDecimal("900.00"));
        invoice.setTotalIgst(BigDecimal.ZERO);
        invoice.setTotalTax(new BigDecimal("1800.00"));
        invoice.setRoundOff(BigDecimal.ZERO);
        invoice.setGrandTotal(new BigDecimal("11800.00"));

        InvoiceLine line = new InvoiceLine();
        line.setId(UUID.randomUUID());
        line.setItemDescription("Software License");
        line.setQuantity(new BigDecimal("1.000"));
        line.setUnitPrice(new BigDecimal("10000.00"));
        line.setTaxableValue(new BigDecimal("10000.00"));
        line.setTaxRatePercent(new BigDecimal("18.00"));
        line.setLineTotal(new BigDecimal("11800.00"));
        invoice.addLine(line);
    }

    @Test
    @DisplayName("Successfully issue credit note against ISSUED invoice")
    void testCreateCreditNoteSuccess() {
        when(invoiceRepository.findByIdAndOrganisationIdWithLines(invoiceId, organisationId))
                .thenReturn(Optional.of(invoice));
        when(customerRepository.findByIdAndOrganisationId(customerId, organisationId))
                .thenReturn(Optional.of(customer));
        when(organisationRepository.findById(organisationId))
                .thenReturn(Optional.of(organisation));
        when(creditNoteRepository.sumActiveAmountByInvoiceId(organisationId, invoiceId))
                .thenReturn(BigDecimal.ZERO);
        when(numberSeriesService.allocateNextNumber(eq(organisationId), eq(DocumentType.CREDIT_NOTE), any(LocalDate.class), eq("CN")))
                .thenReturn(new AllocatedNumber(UUID.randomUUID(), 1, "CN/2026-27/0001", 2026, "26-27"));
        when(creditNoteRepository.save(any(CreditNote.class)))
                .thenAnswer(inv -> {
                    CreditNote cn = inv.getArgument(0);
                    cn.setId(UUID.randomUUID());
                    return cn;
                });

        CreateCreditNoteRequest request = new CreateCreditNoteRequest(
                LocalDate.of(2026, 9, 5),
                CreditNoteReason.SALES_RETURN,
                "Partial return of item",
                List.of(
                        new CreateCreditNoteRequest.CreditNoteLineRequest(
                                invoice.getLines().get(0).getId(),
                                "Returned Software License",
                                "998313",
                                new BigDecimal("1.000"),
                                new BigDecimal("5000.00"),
                                TaxCategory.TAXABLE,
                                new BigDecimal("18.00"),
                                BigDecimal.ZERO
                        )
                )
        );

        CreditNoteResponse response = creditNoteService.createCreditNote(
                organisationId, invoiceId, request, actorUserId, "127.0.0.1"
        );

        assertThat(response).isNotNull();
        assertThat(response.creditNoteNumber()).isEqualTo("CN/2026-27/0001");
        assertThat(response.taxableValue()).isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(response.totalCgst()).isEqualByComparingTo(new BigDecimal("450.00"));
        assertThat(response.totalSgst()).isEqualByComparingTo(new BigDecimal("450.00"));
        assertThat(response.totalTax()).isEqualByComparingTo(new BigDecimal("900.00"));
        assertThat(response.grandTotal()).isEqualByComparingTo(new BigDecimal("5900.00"));
        assertThat(response.originalInvoiceNumber()).isEqualTo("INV/2026-27/0001");
        assertThat(response.reason()).isEqualTo(CreditNoteReason.SALES_RETURN);

        verify(auditLogRepository).save(any());
    }

    @Test
    @DisplayName("Reject credit note creation when invoice is DRAFT")
    void testCreateCreditNoteFailsWhenInvoiceDraft() {
        invoice.setState(InvoiceState.DRAFT);
        when(invoiceRepository.findByIdAndOrganisationIdWithLines(invoiceId, organisationId))
                .thenReturn(Optional.of(invoice));

        CreateCreditNoteRequest request = new CreateCreditNoteRequest(
                LocalDate.of(2026, 9, 5),
                CreditNoteReason.SALES_RETURN,
                "Return",
                List.of(
                        new CreateCreditNoteRequest.CreditNoteLineRequest(
                                null, "Item", "998313", BigDecimal.ONE, new BigDecimal("1000.00"),
                                TaxCategory.TAXABLE, new BigDecimal("18.00"), BigDecimal.ZERO
                        )
                )
        );

        assertThatThrownBy(() -> creditNoteService.createCreditNote(organisationId, invoiceId, request, actorUserId, "127.0.0.1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Credit notes can only be issued against ISSUED invoices");
    }

    @Test
    @DisplayName("Reject credit note creation when credit note date is before invoice date")
    void testCreateCreditNoteFailsWhenDateBeforeInvoice() {
        when(invoiceRepository.findByIdAndOrganisationIdWithLines(invoiceId, organisationId))
                .thenReturn(Optional.of(invoice));

        CreateCreditNoteRequest request = new CreateCreditNoteRequest(
                LocalDate.of(2026, 8, 25), // before 2026-09-01
                CreditNoteReason.CORRECTION_IN_INVOICE,
                "Backdated error",
                List.of(
                        new CreateCreditNoteRequest.CreditNoteLineRequest(
                                null, "Item", "998313", BigDecimal.ONE, new BigDecimal("1000.00"),
                                TaxCategory.TAXABLE, new BigDecimal("18.00"), BigDecimal.ZERO
                        )
                )
        );

        assertThatThrownBy(() -> creditNoteService.createCreditNote(organisationId, invoiceId, request, actorUserId, "127.0.0.1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("cannot be earlier than the original invoice date");
    }

    @Test
    @DisplayName("Reject credit note creation when credit amount exceeds uncredited invoice total")
    void testCreateCreditNoteFailsWhenExceedingInvoiceBalance() {
        when(invoiceRepository.findByIdAndOrganisationIdWithLines(invoiceId, organisationId))
                .thenReturn(Optional.of(invoice));
        when(customerRepository.findByIdAndOrganisationId(customerId, organisationId))
                .thenReturn(Optional.of(customer));
        when(organisationRepository.findById(organisationId))
                .thenReturn(Optional.of(organisation));
        when(creditNoteRepository.sumActiveAmountByInvoiceId(organisationId, invoiceId))
                .thenReturn(new BigDecimal("10000.00")); // only 1800 left

        // Attempting to credit 5000 + 18% = 5900 > 1800
        CreateCreditNoteRequest request = new CreateCreditNoteRequest(
                LocalDate.of(2026, 9, 10),
                CreditNoteReason.DEFICIENCY_IN_SERVICE,
                "Excess credit",
                List.of(
                        new CreateCreditNoteRequest.CreditNoteLineRequest(
                                null, "Refund", "998313", BigDecimal.ONE, new BigDecimal("5000.00"),
                                TaxCategory.TAXABLE, new BigDecimal("18.00"), BigDecimal.ZERO
                        )
                )
        );

        assertThatThrownBy(() -> creditNoteService.createCreditNote(organisationId, invoiceId, request, actorUserId, "127.0.0.1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("exceeds remaining credit available on invoice");
    }

    @Test
    @DisplayName("Successfully cancel an issued credit note")
    void testCancelCreditNoteSuccess() {
        UUID cnId = UUID.randomUUID();
        CreditNote cn = new CreditNote();
        cn.setId(cnId);
        cn.setOrganisationId(organisationId);
        cn.setOriginalInvoice(invoice);
        cn.setCreditNoteNumber("CN/2026-27/0001");
        cn.setCreditNoteDate(LocalDate.of(2026, 9, 5));
        cn.setReason(CreditNoteReason.SALES_RETURN);
        cn.setTaxableValue(new BigDecimal("5000.00"));
        cn.setGrandTotal(new BigDecimal("5900.00"));
        cn.setState(CreditNoteState.ISSUED);

        when(creditNoteRepository.findByIdAndOrganisationIdWithLines(cnId, organisationId))
                .thenReturn(Optional.of(cn));
        when(creditNoteRepository.save(any(CreditNote.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CancelCreditNoteRequest cancelReq = new CancelCreditNoteRequest("Issued in error");
        CreditNoteResponse cancelled = creditNoteService.cancelCreditNote(
                organisationId, cnId, cancelReq, actorUserId, "127.0.0.1"
        );

        assertThat(cancelled.state()).isEqualTo(CreditNoteState.CANCELLED);
        assertThat(cancelled.cancellationReason()).isEqualTo("Issued in error");
        verify(auditLogRepository).save(any());
    }

    @Test
    @DisplayName("Reject cancellation of already cancelled credit note")
    void testCancelAlreadyCancelledCreditNoteFails() {
        UUID cnId = UUID.randomUUID();
        CreditNote cn = new CreditNote();
        cn.setId(cnId);
        cn.setOrganisationId(organisationId);
        cn.setState(CreditNoteState.CANCELLED);

        when(creditNoteRepository.findByIdAndOrganisationIdWithLines(cnId, organisationId))
                .thenReturn(Optional.of(cn));

        CancelCreditNoteRequest cancelReq = new CancelCreditNoteRequest("Second cancel");
        assertThatThrownBy(() -> creditNoteService.cancelCreditNote(organisationId, cnId, cancelReq, actorUserId, "127.0.0.1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already cancelled");
    }
}
