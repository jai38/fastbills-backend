package com.fastbills.api.invoice.pdf;

import com.fastbills.api.customer.Customer;
import com.fastbills.api.customer.CustomerRepository;
import com.fastbills.api.invoice.Invoice;
import com.fastbills.api.invoice.InvoiceLine;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class InvoicePdfService {

    private static final Logger log = LoggerFactory.getLogger(InvoicePdfService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final SpringTemplateEngine templateEngine;
    private final StorageService storageService;
    private final OrganisationRepository organisationRepository;
    private final CustomerRepository customerRepository;

    public InvoicePdfService(
            SpringTemplateEngine templateEngine,
            StorageService storageService,
            OrganisationRepository organisationRepository,
            CustomerRepository customerRepository
    ) {
        this.templateEngine = templateEngine;
        this.storageService = storageService;
        this.organisationRepository = organisationRepository;
        this.customerRepository = customerRepository;
    }

    /**
     * Renders a byte[] PDF for the given invoice, org, and customer.
     */
    public byte[] generatePdf(Invoice invoice, Organisation org, Customer customer) {
        InvoicePdfViewModel viewModel = buildViewModel(invoice, org, customer);

        Context context = new Context();
        context.setVariable("model", viewModel);

        String htmlContent = templateEngine.process("invoice-template", context);

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
            log.error("Failed to render invoice PDF for invoice: {}", invoice.getInvoiceNumber(), e);
            throw new RuntimeException("Failed to render invoice PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Generates and saves the PDF into storageService, returning the object key.
     */
    public String generateAndStorePdf(Invoice invoice, Organisation org, Customer customer) {
        byte[] pdfBytes = generatePdf(invoice, org, customer);
        String objectKey = "invoices/" + org.getId() + "/" + invoice.getId() + ".pdf";

        storageService.store(
                objectKey,
                new ByteArrayInputStream(pdfBytes),
                "application/pdf",
                pdfBytes.length
        );

        return objectKey;
    }

    /**
     * Convenience method to load invoice dependencies and generate/store PDF.
     */
    public String generateAndStorePdf(Invoice invoice) {
        Organisation org = organisationRepository.findById(invoice.getOrganisationId())
                .orElseThrow(() -> new IllegalArgumentException("Organisation not found: " + invoice.getOrganisationId()));
        Customer customer = customerRepository.findById(invoice.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + invoice.getCustomerId()));

        return generateAndStorePdf(invoice, org, customer);
    }

    public InvoicePdfViewModel buildViewModel(Invoice invoice, Organisation org, Customer customer) {
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
                log.warn("Could not load organisation logo for PDF: {}", e.getMessage());
            }
        }

        InvoicePdfViewModel.SupplierDetails supplier = new InvoicePdfViewModel.SupplierDetails(
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
        InvoicePdfViewModel.CustomerDetails customerDetails = new InvoicePdfViewModel.CustomerDetails(
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

        // 3. Invoice details
        InvoicePdfViewModel.InvoiceDetails invoiceDetails = new InvoicePdfViewModel.InvoiceDetails(
                invoice.getInvoiceNumber() != null ? invoice.getInvoiceNumber() : "DRAFT",
                invoice.getInvoiceDate() != null ? invoice.getInvoiceDate().format(DATE_FMT) : "",
                invoice.getDueDate() != null ? invoice.getDueDate().format(DATE_FMT) : "",
                invoice.getPlaceOfSupplyStateCode(),
                IndianStates.getStateName(invoice.getPlaceOfSupplyStateCode()),
                invoice.getSupplyType() != null ? invoice.getSupplyType().name() : "INTRA_STATE",
                (invoice.getIsReverseCharge() != null && invoice.getIsReverseCharge()) ? "Yes" : "No",
                invoice.getPoNumber(),
                invoice.getPoDate() != null ? invoice.getPoDate().format(DATE_FMT) : null,
                invoice.getCollectionReference(),
                invoice.getNotes(),
                invoice.getTermsText()
        );

        // 4. Line Items
        List<InvoicePdfViewModel.LineItemDetails> lineItems = new ArrayList<>();
        List<TaxLineInput> calcInputs = new ArrayList<>();

        int lineNum = 1;
        for (InvoiceLine line : invoice.getLines()) {
            int num = line.getLineNumber() != null ? line.getLineNumber() : lineNum++;
            lineItems.add(new InvoicePdfViewModel.LineItemDetails(
                    num,
                    line.getItemDescription(),
                    line.getHsnOrSacCode() != null ? line.getHsnOrSacCode() : "-",
                    line.getQuantity().toPlainString(),
                    line.getUnitOfMeasure() != null ? line.getUnitOfMeasure() : "NOS",
                    IndianCurrencyFormatter.format(line.getUnitPrice()),
                    IndianCurrencyFormatter.format(line.getDiscountAmount()),
                    IndianCurrencyFormatter.format(line.getTaxableValue()),
                    line.getTaxRatePercent() != null ? line.getTaxRatePercent().toPlainString() : "0",
                    line.getCgstRatePercent() != null ? line.getCgstRatePercent().toPlainString() : "0",
                    IndianCurrencyFormatter.format(line.getCgstAmount()),
                    line.getSgstRatePercent() != null ? line.getSgstRatePercent().toPlainString() : "0",
                    IndianCurrencyFormatter.format(line.getSgstAmount()),
                    line.getIgstRatePercent() != null ? line.getIgstRatePercent().toPlainString() : "0",
                    IndianCurrencyFormatter.format(line.getIgstAmount()),
                    IndianCurrencyFormatter.format(line.getCessAmount()),
                    IndianCurrencyFormatter.format(line.getLineTotal())
            ));

            calcInputs.add(new TaxLineInput(
                    num,
                    line.getItemDescription(),
                    line.getHsnOrSacCode(),
                    line.getIsService() != null && line.getIsService(),
                    line.getQuantity(),
                    line.getUnitOfMeasure(),
                    line.getUnitPrice(),
                    line.getDiscountPercent(),
                    line.getDiscountAmount(),
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
                invoice.getIsReverseCharge() != null && invoice.getIsReverseCharge(),
                calcInputs
        );

        List<InvoicePdfViewModel.RateSlabDetails> rateSlabs = new ArrayList<>();
        for (TaxSlabSummary slab : taxCalc.slabSummaries()) {
            rateSlabs.add(new InvoicePdfViewModel.RateSlabDetails(
                    slab.taxRatePercent().toPlainString(),
                    IndianCurrencyFormatter.format(slab.taxableValue()),
                    IndianCurrencyFormatter.format(slab.cgstAmount()),
                    IndianCurrencyFormatter.format(slab.sgstAmount()),
                    IndianCurrencyFormatter.format(slab.igstAmount()),
                    IndianCurrencyFormatter.format(slab.cessAmount()),
                    IndianCurrencyFormatter.format(slab.totalTax())
            ));
        }

        // 6. Totals
        String words = invoice.getAmountInWords();
        if (words == null || words.isBlank()) {
            words = IndianNumberToWords.convert(invoice.getGrandTotal());
        }

        InvoicePdfViewModel.TotalsDetails totals = new InvoicePdfViewModel.TotalsDetails(
                IndianCurrencyFormatter.format(invoice.getSubtotalBeforeDiscount()),
                IndianCurrencyFormatter.format(invoice.getTotalDiscount()),
                IndianCurrencyFormatter.format(invoice.getTaxableValue()),
                IndianCurrencyFormatter.format(invoice.getTotalCgst()),
                IndianCurrencyFormatter.format(invoice.getTotalSgst()),
                IndianCurrencyFormatter.format(invoice.getTotalIgst()),
                IndianCurrencyFormatter.format(invoice.getTotalCess()),
                IndianCurrencyFormatter.format(invoice.getTotalTax()),
                (invoice.getRoundOff().compareTo(BigDecimal.ZERO) >= 0 ? "+" : "") + IndianCurrencyFormatter.format(invoice.getRoundOff()),
                IndianCurrencyFormatter.format(invoice.getGrandTotal()),
                words
        );

        // 7. Bank details & UPI URI
        String upiUri = null;
        if (org.getUpiId() != null && !org.getUpiId().isBlank()) {
            try {
                String pa = URLEncoder.encode(org.getUpiId(), StandardCharsets.UTF_8);
                String pn = URLEncoder.encode(org.getLegalName(), StandardCharsets.UTF_8);
                String am = invoice.getGrandTotal().toPlainString();
                String tn = URLEncoder.encode(invoice.getInvoiceNumber() != null ? invoice.getInvoiceNumber() : "Invoice", StandardCharsets.UTF_8);
                upiUri = String.format("upi://pay?pa=%s&pn=%s&am=%s&cu=INR&tn=%s", pa, pn, am, tn);
            } catch (Exception ignored) {}
        }

        InvoicePdfViewModel.BankDetails bank = new InvoicePdfViewModel.BankDetails(
                org.getBankName(),
                org.getBankAccountName(),
                org.getBankAccountNumber(),
                org.getBankIfsc(),
                org.getBankBranch(),
                org.getUpiId(),
                upiUri
        );

        String title = (invoice.getTotalTax().compareTo(BigDecimal.ZERO) == 0 && (org.getGstin() == null || org.getGstin().isBlank()))
                ? "BILL OF SUPPLY"
                : "TAX INVOICE";

        return new InvoicePdfViewModel(
                title,
                supplier,
                customerDetails,
                invoiceDetails,
                lineItems,
                rateSlabs,
                totals,
                bank,
                isIntraState
        );
    }
}
