package com.fastbills.api.series;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

/** Checklist section C (numbering) edge cases: C1, C2, C3, C5, C7, C8, C9. Pure unit tests, no DB. */
@ExtendWith(MockitoExtension.class)
class NumberSeriesEdgeCaseTest {

    @Mock
    private NumberSeriesRepository repo;

    @InjectMocks
    private NumberSeriesService service;

    private UUID orgId;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
    }

    @Test
    @DisplayName("C7: 31 Mar belongs to the prior FY, 1 Apr starts the new one")
    void financialYearBoundary() {
        assertThat(FinancialYear.fromDate(LocalDate.of(2027, 3, 31))).isEqualTo(2026);
        assertThat(FinancialYear.fromDate(LocalDate.of(2027, 4, 1))).isEqualTo(2027);
        assertThat(FinancialYear.fromDate(LocalDate.of(2027, 2, 10))).isEqualTo(2026);
        assertThat(FinancialYear.formatShort(LocalDate.of(2027, 2, 10))).isEqualTo("26-27");
    }

    @Test
    @DisplayName("C7: century-style rollover prints correct short FY")
    void shortFyAcrossDecade() {
        assertThat(FinancialYear.formatShort(2029)).isEqualTo("29-30");
        assertThat(FinancialYear.formatShort(2099)).isEqualTo("99-00");
    }

    @Test
    @DisplayName("C7: backdated document uses the prior FY series, not the current one")
    void backdatedUsesPriorSeries() {
        LocalDate backdated = LocalDate.of(2026, 3, 31); // FY 2025
        NumberSeries prior = new NumberSeries(orgId, DocumentType.INVOICE, 2025, "INV", 7, 4);
        when(repo.findForUpdate(orgId, DocumentType.INVOICE, 2025)).thenReturn(Optional.of(prior));

        AllocatedNumber n = service.allocateNextNumber(orgId, DocumentType.INVOICE, backdated, "INV");

        assertThat(n.formattedNumber()).isEqualTo("INV/25-26/0007");
        assertThat(prior.getNextNumber()).isEqualTo(8);
    }

    @Test
    @DisplayName("C3: five allocations in order are consecutive with no gaps")
    void consecutiveNoGaps() {
        NumberSeries s = new NumberSeries(orgId, DocumentType.INVOICE, 2026, "INV", 1, 4);
        when(repo.findForUpdate(orgId, DocumentType.INVOICE, 2026)).thenReturn(Optional.of(s));
        LocalDate d = LocalDate.of(2026, 11, 19);

        for (int i = 1; i <= 5; i++) {
            AllocatedNumber n = service.allocateNextNumber(orgId, DocumentType.INVOICE, d, "INV");
            assertThat(n.sequenceNumber()).isEqualTo(i);
            assertThat(n.formattedNumber()).isEqualTo(String.format("INV/26-27/%04d", i));
        }
        assertThat(s.getNextNumber()).isEqualTo(6);
    }

    @Test
    @DisplayName("C5: preview with no series creates nothing and shows number 1")
    void previewDoesNotCreateSeries() {
        when(repo.findByOrganisationIdAndDocumentTypeAndFinancialYear(orgId, DocumentType.INVOICE, 2026))
                .thenReturn(Optional.empty());

        String preview = service.previewNextNumber(orgId, DocumentType.INVOICE, LocalDate.of(2026, 11, 19), null);

        assertThat(preview).isEqualTo("INV/26-27/0001");
        verify(repo, never()).save(any(NumberSeries.class));
    }

    @Test
    @DisplayName("C9: credit notes use their own series row, separate from invoices")
    void creditNoteSeparateSeries() {
        NumberSeries cn = new NumberSeries(orgId, DocumentType.CREDIT_NOTE, 2026, "CN", 3, 4);
        when(repo.findForUpdate(orgId, DocumentType.CREDIT_NOTE, 2026)).thenReturn(Optional.of(cn));

        AllocatedNumber n = service.allocateNextNumber(orgId, DocumentType.CREDIT_NOTE, LocalDate.of(2026, 11, 19), "CN");

        assertThat(n.formattedNumber()).isEqualTo("CN/26-27/0003");
        verify(repo, never()).findForUpdate(orgId, DocumentType.INVOICE, 2026);
    }

    @Test
    @DisplayName("C2: six-char prefix is accepted only at width 3; 7 chars always rejected")
    void prefixLengthLimits() {
        NumberSeriesService.validatePrefixAndWidth("ABCDEF", 3); // 6+1+5+1+3 = 16
        assertThatThrownBy(() -> NumberSeriesService.validatePrefixAndWidth("ABCDEFG", 3))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("C2: width outside 3..6 rejected")
    void widthLimits() {
        assertThatThrownBy(() -> NumberSeriesService.validatePrefixAndWidth("INV", 2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> NumberSeriesService.validatePrefixAndWidth("INV", 7))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("C1: allocation refuses to emit a number longer than 16 chars when sequence overflows width")
    void sequenceOverflowRejected() {
        // 5-char prefix, width 4 => 16 chars; sequence 10000 would make 17
        assertThatThrownBy(() -> NumberSeriesService.formatNumber("ABCDE", 2026, 10000, 4))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("exceeds Rule 46");
    }

    @Test
    @DisplayName("C8: onboarding start number continues an existing series and sets the next number")
    void onboardingStartNumber() {
        when(repo.findForUpdate(orgId, DocumentType.INVOICE, 2026)).thenReturn(Optional.empty());
        when(repo.save(any(NumberSeries.class))).thenAnswer(i -> i.getArgument(0));

        NumberSeries s = service.configureSeries(orgId, DocumentType.INVOICE, 2026, "INV", 412, 4);
        assertThat(s.getNextNumber()).isEqualTo(412);
    }

    @Test
    @DisplayName("C8: starting sequence below 1 rejected")
    void startBelowOneRejected() {
        assertThatThrownBy(() -> service.configureSeries(orgId, DocumentType.INVOICE, 2026, "INV", 0, 4))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
