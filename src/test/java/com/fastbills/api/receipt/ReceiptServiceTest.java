package com.fastbills.api.receipt;

import com.fastbills.api.customer.Customer;
import com.fastbills.api.customer.CustomerRepository;
import com.fastbills.api.invoice.AuditLogRepository;
import com.fastbills.api.invoice.Invoice;
import com.fastbills.api.invoice.InvoiceRepository;
import com.fastbills.api.invoice.InvoiceService;
import com.fastbills.api.invoice.InvoiceState;
import com.fastbills.api.receipt.dto.CreateReceiptRequest;
import com.fastbills.api.receipt.dto.ReceiptResponse;
import com.fastbills.api.receipt.dto.ReverseReceiptRequest;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReceiptServiceTest {

    @Mock
    private ReceiptRepository receiptRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private ReceiptService receiptService;

    private UUID organisationId;
    private UUID customerId;
    private UUID invoiceId;
    private UUID actorUserId;
    private Invoice issuedInvoice;
    private Customer customer;

    @BeforeEach
    void setUp() {
        organisationId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        invoiceId = UUID.randomUUID();
        actorUserId = UUID.randomUUID();

        issuedInvoice = new Invoice();
        issuedInvoice.setId(invoiceId);
        issuedInvoice.setOrganisationId(organisationId);
        issuedInvoice.setCustomerId(customerId);
        issuedInvoice.setInvoiceNumber("INV/26-27/0001");
        issuedInvoice.setState(InvoiceState.ISSUED);
        issuedInvoice.setInvoiceDate(LocalDate.now().minusDays(5));
        issuedInvoice.setDueDate(LocalDate.now().plusDays(25));
        issuedInvoice.setGrandTotal(new BigDecimal("250000.00"));

        customer = new Customer();
        customer.setId(customerId);
        customer.setOrganisationId(organisationId);
        customer.setLegalName("Apex Industrial Systems Pvt Ltd");
    }

    @Test
    @DisplayName("DOMAIN.md Part-payment sequence: 2 lakh NEFT then 50k cash asserts balance and status")
    void testPartPaymentSequenceFromDomainSpec() {
        when(invoiceRepository.findByIdAndOrganisationId(invoiceId, organisationId)).thenReturn(Optional.of(issuedInvoice));
        when(customerRepository.findByIdAndOrganisationId(customerId, organisationId)).thenReturn(Optional.of(customer));
        when(receiptRepository.save(any(Receipt.class))).thenAnswer(invocation -> {
            Receipt r = invocation.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        // Step 1: Initial state (0 received) -> UNPAID
        String status0 = InvoiceService.derivePaymentStatus(issuedInvoice, BigDecimal.ZERO);
        assertThat(status0).isEqualTo("UNPAID");

        // Step 2: First payment: 2,00,000 via NEFT
        when(receiptRepository.sumActiveAmountByInvoiceId(organisationId, invoiceId))
                .thenReturn(BigDecimal.ZERO); // no prior payments

        CreateReceiptRequest neftPayment = new CreateReceiptRequest(
                new BigDecimal("200000.00"),
                LocalDate.now(),
                PaymentMethod.NEFT,
                "NEFT-UTR-987654321",
                "Two lakh advance payment via NEFT"
        );

        ReceiptResponse resp1 = receiptService.recordReceipt(organisationId, invoiceId, neftPayment, actorUserId, "127.0.0.1");
        assertThat(resp1).isNotNull();
        assertThat(resp1.amount()).isEqualByComparingTo("200000.00");
        assertThat(resp1.paymentMethod()).isEqualTo(PaymentMethod.NEFT);

        // Derive status after first payment: received 2,00,000 on 2,50,000 -> PARTIALLY_PAID
        String status1 = InvoiceService.derivePaymentStatus(issuedInvoice, new BigDecimal("200000.00"));
        assertThat(status1).isEqualTo("PARTIALLY_PAID");

        // Step 3: Second payment: 50,000 via Cash
        when(receiptRepository.sumActiveAmountByInvoiceId(organisationId, invoiceId))
                .thenReturn(new BigDecimal("200000.00")); // already received 2,00,000

        CreateReceiptRequest cashPayment = new CreateReceiptRequest(
                new BigDecimal("50000.00"),
                LocalDate.now(),
                PaymentMethod.CASH,
                null,
                "Fifty thousand settlement in cash"
        );

        ReceiptResponse resp2 = receiptService.recordReceipt(organisationId, invoiceId, cashPayment, actorUserId, "127.0.0.1");
        assertThat(resp2).isNotNull();
        assertThat(resp2.amount()).isEqualByComparingTo("50000.00");
        assertThat(resp2.paymentMethod()).isEqualTo(PaymentMethod.CASH);

        // Derive status after second payment: received 2,50,000 on 2,50,000 -> PAID
        String status2 = InvoiceService.derivePaymentStatus(issuedInvoice, new BigDecimal("250000.00"));
        assertThat(status2).isEqualTo("PAID");
    }

    @Test
    @DisplayName("Overdue status derivations for unpaid and partially paid invoices past due date")
    void testOverdueStatusDerivations() {
        issuedInvoice.setDueDate(LocalDate.now().minusDays(10)); // Overdue by 10 days

        // 0 received and past due -> OVERDUE
        assertThat(InvoiceService.derivePaymentStatus(issuedInvoice, BigDecimal.ZERO)).isEqualTo("OVERDUE");

        // Partial payment and past due -> PARTIALLY_PAID_OVERDUE
        assertThat(InvoiceService.derivePaymentStatus(issuedInvoice, new BigDecimal("100000.00"))).isEqualTo("PARTIALLY_PAID_OVERDUE");

        // Full payment and past due -> PAID
        assertThat(InvoiceService.derivePaymentStatus(issuedInvoice, new BigDecimal("250000.00"))).isEqualTo("PAID");
    }

    @Test
    @DisplayName("Ageing bucket calculation matches DOMAIN.md 45-day threshold")
    void testAgeingBuckets() {
        LocalDate today = LocalDate.now();

        // Current (due today or in future)
        assertThat(AgeingBucket.calculate(today, today)).isEqualTo(AgeingBucket.CURRENT);
        assertThat(AgeingBucket.calculate(today.plusDays(10), today)).isEqualTo(AgeingBucket.CURRENT);

        // 1 to 30 days past due
        assertThat(AgeingBucket.calculate(today.minusDays(1), today)).isEqualTo(AgeingBucket.DAYS_1_TO_30);
        assertThat(AgeingBucket.calculate(today.minusDays(30), today)).isEqualTo(AgeingBucket.DAYS_1_TO_30);

        // 31 to 45 days past due (critical 45-day statutory threshold)
        assertThat(AgeingBucket.calculate(today.minusDays(31), today)).isEqualTo(AgeingBucket.DAYS_31_TO_45);
        assertThat(AgeingBucket.calculate(today.minusDays(45), today)).isEqualTo(AgeingBucket.DAYS_31_TO_45);

        // 46 to 60 days past due
        assertThat(AgeingBucket.calculate(today.minusDays(46), today)).isEqualTo(AgeingBucket.DAYS_46_TO_60);
        assertThat(AgeingBucket.calculate(today.minusDays(60), today)).isEqualTo(AgeingBucket.DAYS_46_TO_60);

        // 61 to 90 days past due
        assertThat(AgeingBucket.calculate(today.minusDays(61), today)).isEqualTo(AgeingBucket.DAYS_61_TO_90);
        assertThat(AgeingBucket.calculate(today.minusDays(90), today)).isEqualTo(AgeingBucket.DAYS_61_TO_90);

        // Over 90 days
        assertThat(AgeingBucket.calculate(today.minusDays(91), today)).isEqualTo(AgeingBucket.OVER_90);
    }

    @Test
    @DisplayName("Receipt reversal creates linked reversal row and updates audit log")
    void testReceiptReversalFlow() {
        UUID receiptId = UUID.randomUUID();
        Receipt activeReceipt = new Receipt(
                organisationId,
                invoiceId,
                customerId,
                new BigDecimal("50000.00"),
                LocalDate.now(),
                PaymentMethod.UPI,
                "UPI-TXN-1234",
                "Payment via UPI",
                actorUserId
        );
        activeReceipt.setId(receiptId);

        when(receiptRepository.findByIdAndOrganisationId(receiptId, organisationId)).thenReturn(Optional.of(activeReceipt));
        when(receiptRepository.save(any(Receipt.class))).thenAnswer(invocation -> {
            Receipt r = invocation.getArgument(0);
            if (r.getId() == null) {
                r.setId(UUID.randomUUID());
            }
            return r;
        });
        when(invoiceRepository.findByIdAndOrganisationId(invoiceId, organisationId)).thenReturn(Optional.of(issuedInvoice));
        when(customerRepository.findByIdAndOrganisationId(customerId, organisationId)).thenReturn(Optional.of(customer));

        ReverseReceiptRequest reverseReq = new ReverseReceiptRequest("Bank reported transaction bounced / returned");
        ReceiptResponse reversed = receiptService.reverseReceipt(organisationId, receiptId, reverseReq, actorUserId, "127.0.0.1");

        assertThat(reversed.isReversed()).isTrue();
        assertThat(reversed.reversalReason()).isEqualTo("Bank reported transaction bounced / returned");
        assertThat(reversed.reversedByUserId()).isEqualTo(actorUserId);
        assertThat(reversed.reversedByReceiptId()).isNotNull();

        verify(auditLogRepository).save(argThat(log ->
                "RECEIPT_REVERSED".equals(log.getAction()) && "RECEIPT".equals(log.getEntityType())
        ));
    }

    @Test
    @DisplayName("Refuse reversal of an already reversed receipt")
    void testRefuseDoubleReversal() {
        UUID receiptId = UUID.randomUUID();
        Receipt reversedReceipt = new Receipt();
        reversedReceipt.setId(receiptId);
        reversedReceipt.setOrganisationId(organisationId);
        reversedReceipt.setIsReversed(true);

        when(receiptRepository.findByIdAndOrganisationId(receiptId, organisationId)).thenReturn(Optional.of(reversedReceipt));

        ReverseReceiptRequest reverseReq = new ReverseReceiptRequest("Another reversal attempt");
        assertThatThrownBy(() -> receiptService.reverseReceipt(organisationId, receiptId, reverseReq, actorUserId, "127.0.0.1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already reversed");
    }

    @Test
    @DisplayName("Refuse recording receipt if amount exceeds remaining balance")
    void testRefuseReceiptExceedingBalance() {
        when(invoiceRepository.findByIdAndOrganisationId(invoiceId, organisationId)).thenReturn(Optional.of(issuedInvoice));
        when(receiptRepository.sumActiveAmountByInvoiceId(organisationId, invoiceId))
                .thenReturn(new BigDecimal("200000.00")); // 50,000 remaining

        CreateReceiptRequest excessPayment = new CreateReceiptRequest(
                new BigDecimal("60000.00"), // 60,000 > 50,000 remaining
                LocalDate.now(),
                PaymentMethod.CARD,
                "CARD-AUTH-999",
                "Attempted overpayment"
        );

        assertThatThrownBy(() -> receiptService.recordReceipt(organisationId, invoiceId, excessPayment, actorUserId, "127.0.0.1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("exceeds outstanding balance");
    }

    @Test
    @DisplayName("Refuse recording receipt on DRAFT or CANCELLED invoice")
    void testRefuseReceiptOnDraftOrCancelledInvoice() {
        issuedInvoice.setState(InvoiceState.DRAFT);
        when(invoiceRepository.findByIdAndOrganisationId(invoiceId, organisationId)).thenReturn(Optional.of(issuedInvoice));

        CreateReceiptRequest payment = new CreateReceiptRequest(
                new BigDecimal("1000.00"),
                LocalDate.now(),
                PaymentMethod.CASH,
                null,
                null
        );

        assertThatThrownBy(() -> receiptService.recordReceipt(organisationId, invoiceId, payment, actorUserId, "127.0.0.1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Cannot record receipts against draft invoices");

        issuedInvoice.setState(InvoiceState.CANCELLED);
        assertThatThrownBy(() -> receiptService.recordReceipt(organisationId, invoiceId, payment, actorUserId, "127.0.0.1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Cannot record receipts against cancelled invoices");
    }
}
