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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NumberSeriesServiceTest {

    @Mock
    private NumberSeriesRepository numberSeriesRepository;

    @InjectMocks
    private NumberSeriesService numberSeriesService;

    private UUID organisationId;

    @BeforeEach
    void setUp() {
        organisationId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Prefix validation: Valid alphanumeric, slash, and hyphen prefixes")
    void testValidPrefixes() {
        NumberSeriesService.validatePrefixAndWidth("INV", 4);
        NumberSeriesService.validatePrefixAndWidth("SEW", 5);
        NumberSeriesService.validatePrefixAndWidth("A/1", 4);
        NumberSeriesService.validatePrefixAndWidth("B-2", 4);
    }

    @Test
    @DisplayName("Prefix validation: Invalid characters throw IllegalArgumentException")
    void testInvalidPrefixCharacters() {
        assertThatThrownBy(() -> NumberSeriesService.validatePrefixAndWidth("IN_V", 4))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Prefix must be 1 to 6 characters");

        assertThatThrownBy(() -> NumberSeriesService.validatePrefixAndWidth("INV@1", 4))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> NumberSeriesService.validatePrefixAndWidth("", 4))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> NumberSeriesService.validatePrefixAndWidth("TOOLONG1", 4))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Rule 46: Format exceeding 16 characters is rejected before issue")
    void testRule46TotalLengthExceeded() {
        // Prefix with 6 chars + width 4: 6 + 1 + 5 + 1 + 4 = 17 chars > 16
        assertThatThrownBy(() -> NumberSeriesService.validatePrefixAndWidth("ABCDEF", 4))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds Rule 46 ceiling of 16 characters");

        // Prefix with 5 chars + width 5: 5 + 1 + 5 + 1 + 5 = 17 chars > 16
        assertThatThrownBy(() -> NumberSeriesService.validatePrefixAndWidth("ABCDE", 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds Rule 46 ceiling of 16 characters");
    }

    @Test
    @DisplayName("Number formatting: Formats with zero-padding and FY")
    void testFormatNumber() {
        String formatted = NumberSeriesService.formatNumber("INV", 2024, 1, 4);
        assertThat(formatted).isEqualTo("INV/24-25/0001");
        assertThat(formatted.length()).isEqualTo(14);
        assertThat(formatted.length()).isLessThanOrEqualTo(16);

        String seq42 = NumberSeriesService.formatNumber("SEW", 2026, 42, 5);
        assertThat(seq42).isEqualTo("SEW/26-27/00042");
        assertThat(seq42.length()).isEqualTo(15);
        assertThat(seq42.length()).isLessThanOrEqualTo(16);
    }

    @Test
    @DisplayName("Allocate next number: Creates series on first issue and returns sequence 1")
    void testAllocateNextNumber_FirstTime() {
        LocalDate date = LocalDate.of(2024, 6, 15);
        int fy = 2024;

        when(numberSeriesRepository.findForUpdate(organisationId, DocumentType.INVOICE, fy))
                .thenReturn(Optional.empty());

        NumberSeries savedSeries = new NumberSeries(
                organisationId,
                DocumentType.INVOICE,
                fy,
                "INV",
                1,
                4
        );
        UUID seriesId = UUID.randomUUID();
        savedSeries.setId(seriesId);

        when(numberSeriesRepository.save(any(NumberSeries.class)))
                .thenReturn(savedSeries);

        AllocatedNumber allocated = numberSeriesService.allocateNextNumber(
                organisationId,
                DocumentType.INVOICE,
                date,
                "INV"
        );

        assertThat(allocated.seriesId()).isEqualTo(seriesId);
        assertThat(allocated.sequenceNumber()).isEqualTo(1);
        assertThat(allocated.formattedNumber()).isEqualTo("INV/24-25/0001");
        assertThat(allocated.financialYear()).isEqualTo(2024);
        assertThat(allocated.financialYearShort()).isEqualTo("24-25");

        // Asserts next number was incremented to 2
        assertThat(savedSeries.getNextNumber()).isEqualTo(2);
    }

    @Test
    @DisplayName("Allocate next number: Increments existing sequence consecutively")
    void testAllocateNextNumber_ExistingSeries() {
        LocalDate date = LocalDate.of(2024, 8, 20);
        int fy = 2024;

        NumberSeries existing = new NumberSeries(
                organisationId,
                DocumentType.INVOICE,
                fy,
                "INV",
                42,
                4
        );
        UUID seriesId = UUID.randomUUID();
        existing.setId(seriesId);

        when(numberSeriesRepository.findForUpdate(organisationId, DocumentType.INVOICE, fy))
                .thenReturn(Optional.of(existing));

        AllocatedNumber allocated = numberSeriesService.allocateNextNumber(
                organisationId,
                DocumentType.INVOICE,
                date,
                "INV"
        );

        assertThat(allocated.sequenceNumber()).isEqualTo(42);
        assertThat(allocated.formattedNumber()).isEqualTo("INV/24-25/0042");
        assertThat(existing.getNextNumber()).isEqualTo(43);
    }

    @Test
    @DisplayName("Preview next number: Non-mutating preview matches expected format")
    void testPreviewNextNumber() {
        LocalDate date = LocalDate.of(2025, 2, 10); // FY 24-25
        int fy = 2024;

        NumberSeries existing = new NumberSeries(
                organisationId,
                DocumentType.INVOICE,
                fy,
                "FAST",
                10,
                4
        );

        when(numberSeriesRepository.findByOrganisationIdAndDocumentTypeAndFinancialYear(organisationId, DocumentType.INVOICE, fy))
                .thenReturn(Optional.of(existing));

        String preview = numberSeriesService.previewNextNumber(organisationId, DocumentType.INVOICE, date, "FAST");
        assertThat(preview).isEqualTo("FAST/24-25/0010");
        // Ensure not mutated
        assertThat(existing.getNextNumber()).isEqualTo(10);
    }

    @Test
    @DisplayName("Configure series: Supports custom starting sequence for mid-year migration")
    void testConfigureSeries_MidYearMigration() {
        int fy = 2024;
        when(numberSeriesRepository.findForUpdate(organisationId, DocumentType.INVOICE, fy))
                .thenReturn(Optional.empty());

        when(numberSeriesRepository.save(any(NumberSeries.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        NumberSeries configured = numberSeriesService.configureSeries(
                organisationId,
                DocumentType.INVOICE,
                fy,
                "INV",
                150,
                4
        );

        assertThat(configured.getNextNumber()).isEqualTo(150);
        assertThat(configured.getPrefix()).isEqualTo("INV");
    }

    @Test
    @DisplayName("Configure series: Rejects decreasing sequence if invoices already issued")
    void testConfigureSeries_CannotDecreaseSequence() {
        int fy = 2024;
        NumberSeries existing = new NumberSeries(
                organisationId,
                DocumentType.INVOICE,
                fy,
                "INV",
                25, // already issued 24 invoices
                4
        );

        when(numberSeriesRepository.findForUpdate(organisationId, DocumentType.INVOICE, fy))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> numberSeriesService.configureSeries(
                organisationId,
                DocumentType.INVOICE,
                fy,
                "INV",
                10, // attempting to decrease to 10
                4
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot decrease sequence number from 25 to 10");
    }
}
