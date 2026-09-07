package com.fastbills.api.series;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class NumberSeriesService {

    public static final int MAX_RULE_46_LENGTH = 16;
    public static final int DEFAULT_WIDTH = 4;
    public static final String DEFAULT_PREFIX = "INV";
    private static final Pattern PREFIX_PATTERN = Pattern.compile("^[A-Za-z0-9/-]{1,6}$");

    private final NumberSeriesRepository numberSeriesRepository;

    public NumberSeriesService(NumberSeriesRepository numberSeriesRepository) {
        this.numberSeriesRepository = numberSeriesRepository;
    }

    /**
     * Validates prefix syntax and ensures the combined format can never exceed Rule 46's 16-character limit.
     * Format: {prefix}/{YY-YY}/{sequence} -> length = prefix.length() + 1 + 5 + 1 + width.
     */
    public static void validatePrefixAndWidth(String prefix, int width) {
        if (prefix == null || prefix.trim().isEmpty()) {
            throw new IllegalArgumentException("Invoice prefix is required");
        }
        String cleanPrefix = prefix.trim();
        if (!PREFIX_PATTERN.matcher(cleanPrefix).matches()) {
            throw new IllegalArgumentException("Prefix must be 1 to 6 characters and contain only letters, numbers, slashes (/), and hyphens (-)");
        }
        if (width < 3 || width > 6) {
            throw new IllegalArgumentException("Sequence width must be between 3 and 6 digits");
        }

        int maxInitialLength = cleanPrefix.length() + 1 + 5 + 1 + width;
        if (maxInitialLength > MAX_RULE_46_LENGTH) {
            throw new IllegalArgumentException(String.format(
                    "Prefix '%s' with %d-digit width produces format '%s/YY-YY/%s' (%d chars) which exceeds Rule 46 ceiling of 16 characters",
                    cleanPrefix, width, cleanPrefix, "0".repeat(width), maxInitialLength
            ));
        }
    }

    /**
     * Formats a document number and strictly asserts the Rule 46 16-character ceiling.
     */
    public static String formatNumber(String prefix, int financialYear, int sequence, int width) {
        String fyShort = FinancialYear.formatShort(financialYear);
        String formatted = String.format("%s/%s/%0" + width + "d", prefix.trim(), fyShort, sequence);
        if (formatted.length() > MAX_RULE_46_LENGTH) {
            throw new IllegalStateException(String.format(
                    "Generated document number '%s' (%d chars) exceeds Rule 46 statutory ceiling of 16 characters",
                    formatted, formatted.length()
            ));
        }
        return formatted;
    }

    /**
     * Allocates the next consecutive number inside a transactional row-lock (SELECT ... FOR UPDATE).
     * Guarantees zero gaps and zero duplicates under concurrency.
     */
    @Transactional
    public AllocatedNumber allocateNextNumber(
            UUID organisationId,
            DocumentType documentType,
            LocalDate documentDate,
            String defaultPrefix
    ) {
        if (organisationId == null) {
            throw new IllegalArgumentException("Organisation ID is required for sequence allocation");
        }
        if (documentType == null) {
            throw new IllegalArgumentException("Document type is required for sequence allocation");
        }
        if (documentDate == null) {
            throw new IllegalArgumentException("Document date is required for sequence allocation");
        }

        int fy = FinancialYear.fromDate(documentDate);
        String fyShort = FinancialYear.formatShort(fy);

        // Fetch or create with pessimistic lock
        NumberSeries series = numberSeriesRepository.findForUpdate(organisationId, documentType, fy)
                .orElseGet(() -> {
                    String prefix = (defaultPrefix != null && !defaultPrefix.trim().isEmpty())
                            ? defaultPrefix.trim()
                            : DEFAULT_PREFIX;
                    validatePrefixAndWidth(prefix, DEFAULT_WIDTH);
                    NumberSeries newSeries = new NumberSeries(
                            organisationId,
                            documentType,
                            fy,
                            prefix,
                            1,
                            DEFAULT_WIDTH
                    );
                    return numberSeriesRepository.save(newSeries);
                });

        int allocatedSeq = series.getNextNumber();
        String formattedNumber = formatNumber(series.getPrefix(), fy, allocatedSeq, series.getWidth());

        series.setNextNumber(allocatedSeq + 1);
        numberSeriesRepository.save(series);

        return new AllocatedNumber(series.getId(), allocatedSeq, formattedNumber, fy, fyShort);
    }

    /**
     * Non-mutating preview of the next document number for the given date.
     */
    @Transactional(readOnly = true)
    public String previewNextNumber(
            UUID organisationId,
            DocumentType documentType,
            LocalDate documentDate,
            String defaultPrefix
    ) {
        if (organisationId == null || documentDate == null) {
            return null;
        }
        int fy = FinancialYear.fromDate(documentDate);
        Optional<NumberSeries> opt = numberSeriesRepository.findByOrganisationIdAndDocumentTypeAndFinancialYear(
                organisationId,
                documentType != null ? documentType : DocumentType.INVOICE,
                fy
        );

        if (opt.isPresent()) {
            NumberSeries series = opt.get();
            return formatNumber(series.getPrefix(), fy, series.getNextNumber(), series.getWidth());
        } else {
            String prefix = (defaultPrefix != null && !defaultPrefix.trim().isEmpty())
                    ? defaultPrefix.trim()
                    : DEFAULT_PREFIX;
            validatePrefixAndWidth(prefix, DEFAULT_WIDTH);
            return formatNumber(prefix, fy, 1, DEFAULT_WIDTH);
        }
    }

    /**
     * Allows configuring or setting the starting sequence for an onboarding or mid-year migration.
     */
    @Transactional
    public NumberSeries configureSeries(
            UUID organisationId,
            DocumentType documentType,
            int financialYear,
            String prefix,
            Integer startingSequence,
            Integer width
    ) {
        int actualWidth = width != null ? width : DEFAULT_WIDTH;
        validatePrefixAndWidth(prefix, actualWidth);

        if (startingSequence != null && startingSequence < 1) {
            throw new IllegalArgumentException("Starting sequence number must be greater than or equal to 1");
        }

        Optional<NumberSeries> opt = numberSeriesRepository.findForUpdate(organisationId, documentType, financialYear);
        if (opt.isPresent()) {
            NumberSeries series = opt.get();
            if (startingSequence != null) {
                if (series.getNextNumber() > 1 && startingSequence < series.getNextNumber()) {
                    throw new IllegalStateException(String.format(
                            "Cannot decrease sequence number from %d to %d as invoices have already been issued in FY %s",
                            series.getNextNumber(), startingSequence, FinancialYear.formatShort(financialYear)
                    ));
                }
                series.setNextNumber(startingSequence);
            }
            series.setPrefix(prefix.trim());
            series.setWidth(actualWidth);
            return numberSeriesRepository.save(series);
        } else {
            NumberSeries newSeries = new NumberSeries(
                    organisationId,
                    documentType,
                    financialYear,
                    prefix.trim(),
                    startingSequence != null ? startingSequence : 1,
                    actualWidth
            );
            return numberSeriesRepository.save(newSeries);
        }
    }
}
