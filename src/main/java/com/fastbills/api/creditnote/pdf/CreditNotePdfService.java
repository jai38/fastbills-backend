package com.fastbills.api.creditnote.pdf;

import com.fastbills.api.creditnote.CreditNote;
import com.fastbills.api.creditnote.CreditNoteLine;
import com.fastbills.api.creditnote.CreditNoteRepository;
import com.fastbills.api.customer.Customer;
import com.fastbills.api.customer.CustomerRepository;
import com.fastbills.api.invoice.Invoice;
import com.fastbills.api.organisation.Organisation;
import com.fastbills.api.organisation.OrganisationRepository;
import com.fastbills.api.storage.StorageService;
import com.fastbills.api.tax.*;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class CreditNotePdfService {

    private static final Logger log = LoggerFactory.getLogger(CreditNotePdfService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final SpringTemplateEngine templateEngine;
    private final StorageService storageService;
    private final OrganisationRepository organisationRepository;
    private final CustomerRepository customerRepository;
    private final CreditNoteRepository creditNoteRepository;

    public CreditNotePdfService(
            SpringTemplateEngine templateEngine,
            StorageService storageService,
            OrganisationRepository organisationRepository,
            CustomerRepository customerRepository,
            CreditNoteRepository creditNoteRepository
    ) {
        this.templateEngine = templateEngine;
        this.storageService = storageService;
        this.organisationRepository = organisationRepository;
        this.customerRepository = customerRepository;
        this.creditNoteRepository = creditNoteRepository;
    }

    /**
     * Renders a byte[] PDF for the given credit note, org, and customer.
     */
    public byte[] generatePdf(CreditNote creditNote, Organisation org, Customer customer) {
        CreditNotePdfViewModel viewModel = buildViewModel(creditNote, org, customer);

        Context context = new Context();
        context.setVariable("model", viewModel);

        String htmlContent = templateEngine.process("credit-note-template", context);

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();

            // Register Unicode TrueType font supporting Indian Rupee symbol (₹)
            builder.useFont(() -> getClass().getResourceAsStream("/fonts/FreeSans.ttf"),
                    "FreeSans", 400, BaseRendererBuilder.FontStyle.NORMAL, true);
            builder.useFont(() -> getClass().getResourceAsStream("/fonts/FreeSans-Bold.ttf"),
                    "FreeSans", 700, BaseRendererBuilder.FontStyle.NORMAL, true);

            builder.withHtmlContent(htmlContent, null);
            builder.toStream(os);
            builder.run();

            return os.toByteArray();
        } catch (Exception e) {
            log.error("Failed to render credit note PDF for CN: {}", creditNote.getCreditNoteNumber(), e);
            throw new RuntimeException("Failed to render credit note PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Generates and stores the PDF in storageService, returning the object key.
     */
    public String generateAndStorePdf(CreditNote creditNote, Organisation org, Customer customer) {
        byte[] pdfBytes = generatePdf(creditNote, org, customer);
        String objectKey = "credit-notes/" + org.getId() + "/" + creditNote.getId() + ".pdf";

        storageService.store(
                objectKey,
                new ByteArrayInputStream(pdfBytes),
                "application/pdf",
                pdfBytes.length
        );

        return objectKey;
    }

    /**
     * Convenience method to load dependencies and generate/store PDF.
     */
    public String generateAndStorePdf(CreditNote creditNote) {
        Organisation org = organisationRepository.findById(creditNote.getOrganisationId())
                .orElseThrow(() -> new IllegalArgumentException("Organisation not found: " + creditNote.getOrganisationId()));
        Invoice originalInvoice = creditNote.getOriginalInvoice();
        Customer customer = customerRepository.findById(originalInvoice.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + originalInvoice.getCustomerId()));

        return generateAndStorePdf(creditNote, org, customer);
    }

    public CreditNotePdfViewModel buildViewModel(CreditNote creditNote, Organisation org, Customer customer) {
        Invoice invoice = creditNote.getOriginalInvoice();
        boolean isIntraState = invoice.getSupplyType() == SupplyType.INTRA_STATE;

        // 1. Supplier details
        String logoDataUri = null;
        if (org.getLogoObjectKey() != null && !org.getLogoObjectKey().isBlank()) {
            try {
                Resource logoResource = storageService.load(org.getLogoObjectKey());
                if (logoResource.exists()) {
                    try (InputStream in = logoResource.getInputStream()) {
                        byte[] bytes = in.readAllBytes();
                        String base64 = Base64.getEncoder().encodeToString(bytes);
                        String mime = org.getLogoObjectKey().toLowerCase().endsWith(".png") ? "image/png" : "image/jpeg";
                        logoDataUri = "data:" + mime + ";base64," + base64;
                    }
                }
            } catch (Exception e) {
                log.warn("Could not load organisation logo for Credit Note PDF: {}", e.getMessage());
            }
        }

        CreditNotePdfViewModel.SupplierDetails supplier = new CreditNotePdfViewModel.SupplierDetails(
                org.getLegalName(),
                org.getTradeName(),
                org.getGstin() != null ? org.getGstin() : "",
                org.getPan() != null ? org.getPan() : "",
                org.getAddressLine1() != null ? org.getAddressLine1() : "",
                org.getAddressLine2(),
                org.getCity() != null ? org.getCity() : "",
                org.getStateCode() != null ? org.getStateCode() : "",
                IndianStates.getStateName(org.getStateCode()),
                org.getPincode() != null ? org.getPincode() : "",
                org.getPhone(),
                org.getEmail(),
                logoDataUri
        );

        // 2. Customer details
        CreditNotePdfViewModel.CustomerDetails customerDetails = new CreditNotePdfViewModel.CustomerDetails(
                customer.getLegalName(),
                customer.getTradeName(),
                customer.getGstin(),
                customer.getPan(),
                customer.getIsRegistered() != null && customer.getIsRegistered(),
                customer.getBillingAddressLine1(),
                customer.getBillingAddressLine2(),
                customer.getBillingCity(),
                customer.getBillingStateCode(),
                IndianStates.getStateName(customer.getBillingStateCode()),
                customer.getBillingPincode(),
                customer.getShippingAddressLine1(),
                customer.getShippingAddressLine2(),
                customer.getShippingCity(),
                customer.getShippingStateCode(),
                IndianStates.getStateName(customer.getShippingStateCode()),
                customer.getShippingPincode(),
                customer.getPhone(),
                customer.getEmail()
        );

        // 3. Credit Note metadata
        CreditNotePdfViewModel.CreditNoteDetails creditNoteDetails = new CreditNotePdfViewModel.CreditNoteDetails(
                creditNote.getCreditNoteNumber(),
                creditNote.getCreditNoteDate() != null ? creditNote.getCreditNoteDate().format(DATE_FMT) : "",
                invoice.getInvoiceNumber(),
                invoice.getInvoiceDate() != null ? invoice.getInvoiceDate().format(DATE_FMT) : "",
                creditNote.getReason() != null ? creditNote.getReason().getDescription() : "",
                creditNote.getReasonNotes(),
                invoice.getPlaceOfSupplyStateCode(),
                IndianStates.getStateName(invoice.getPlaceOfSupplyStateCode()),
                creditNote.getState().name()
        );

        // 4. Line Items
        List<CreditNotePdfViewModel.LineItemDetails> lineItems = new ArrayList<>();
        List<TaxLineInput> calcInputs = new ArrayList<>();

        int lineNum = 1;
        for (CreditNoteLine line : creditNote.getLines()) {
            int num = lineNum++;
            lineItems.add(new CreditNotePdfViewModel.LineItemDetails(
                    num,
                    line.getItemDescription(),
                    line.getHsnSacCode() != null ? line.getHsnSacCode() : "-",
                    line.getQuantity().stripTrailingZeros().toPlainString(),
                    IndianCurrencyFormatter.format(line.getUnitPrice()),
                    IndianCurrencyFormatter.format(line.getTaxableValue()),
                    line.getTaxRatePercent() != null ? line.getTaxRatePercent().stripTrailingZeros().toPlainString() : "0",
                    line.getCgstRatePercent() != null ? line.getCgstRatePercent().stripTrailingZeros().toPlainString() : "0",
                    IndianCurrencyFormatter.format(line.getCgstAmount()),
                    line.getSgstRatePercent() != null ? line.getSgstRatePercent().stripTrailingZeros().toPlainString() : "0",
                    IndianCurrencyFormatter.format(line.getSgstAmount()),
                    line.getIgstRatePercent() != null ? line.getIgstRatePercent().stripTrailingZeros().toPlainString() : "0",
                    IndianCurrencyFormatter.format(line.getIgstAmount()),
                    IndianCurrencyFormatter.format(line.getCessAmount()),
                    IndianCurrencyFormatter.format(line.getLineTotal())
            ));

            calcInputs.add(new TaxLineInput(
                    num,
                    line.getItemDescription(),
                    line.getHsnSacCode(),
                    false,
                    line.getQuantity(),
                    "NOS",
                    line.getUnitPrice(),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    line.getTaxRatePercent(),
                    line.getCessRatePercent(),
                    null,
                    line.getTaxCategory() != null ? line.getTaxCategory() : TaxCategory.TAXABLE
            ));
        }

        // 5. Rate Slabs calculation
        TaxInvoiceCalculation taxCalc = GstTaxEngine.calculateInvoice(
                org.getStateCode() != null ? org.getStateCode() : "29",
                invoice.getPlaceOfSupplyStateCode() != null ? invoice.getPlaceOfSupplyStateCode() : "29",
                org.getGstin() != null && !org.getGstin().isBlank(),
                false,
                calcInputs
        );

        List<CreditNotePdfViewModel.RateSlabDetails> rateSlabs = new ArrayList<>();
        for (TaxSlabSummary slab : taxCalc.slabSummaries()) {
            rateSlabs.add(new CreditNotePdfViewModel.RateSlabDetails(
                    slab.taxRatePercent().stripTrailingZeros().toPlainString(),
                    IndianCurrencyFormatter.format(slab.taxableValue()),
                    IndianCurrencyFormatter.format(slab.cgstAmount()),
                    IndianCurrencyFormatter.format(slab.sgstAmount()),
                    IndianCurrencyFormatter.format(slab.igstAmount()),
                    IndianCurrencyFormatter.format(slab.cessAmount()),
                    IndianCurrencyFormatter.format(slab.totalTax())
            ));
        }

        // 6. Totals
        String words = IndianNumberToWords.convert(creditNote.getGrandTotal());

        CreditNotePdfViewModel.TotalsDetails totals = new CreditNotePdfViewModel.TotalsDetails(
                IndianCurrencyFormatter.format(creditNote.getTaxableValue()),
                IndianCurrencyFormatter.format(creditNote.getTotalCgst()),
                IndianCurrencyFormatter.format(creditNote.getTotalSgst()),
                IndianCurrencyFormatter.format(creditNote.getTotalIgst()),
                IndianCurrencyFormatter.format(creditNote.getTotalCess()),
                IndianCurrencyFormatter.format(creditNote.getTotalTax()),
                (creditNote.getRoundOff().compareTo(BigDecimal.ZERO) >= 0 ? "+" : "") + IndianCurrencyFormatter.format(creditNote.getRoundOff()),
                IndianCurrencyFormatter.format(creditNote.getGrandTotal()),
                words
        );

        return new CreditNotePdfViewModel(
                "CREDIT NOTE",
                supplier,
                customerDetails,
                creditNoteDetails,
                lineItems,
                rateSlabs,
                totals,
                isIntraState
        );
    }
}
