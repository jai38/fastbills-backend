package com.fastbills.api.creditnote.pdf;

import java.util.List;

public record CreditNotePdfViewModel(
        String title,
        SupplierDetails supplier,
        CustomerDetails customer,
        CreditNoteDetails creditNote,
        List<LineItemDetails> lines,
        List<RateSlabDetails> rateSlabs,
        TotalsDetails totals,
        boolean isIntraState
) {
    public record SupplierDetails(
            String legalName,
            String tradeName,
            String gstin,
            String pan,
            String addressLine1,
            String addressLine2,
            String city,
            String stateCode,
            String stateName,
            String pincode,
            String phone,
            String email,
            String logoDataUri
    ) {}

    public record CustomerDetails(
            String legalName,
            String tradeName,
            String gstin,
            String pan,
            boolean isRegistered,
            String billingAddressLine1,
            String billingAddressLine2,
            String billingCity,
            String billingStateCode,
            String billingStateName,
            String billingPincode,
            String shippingAddressLine1,
            String shippingAddressLine2,
            String shippingCity,
            String shippingStateCode,
            String shippingStateName,
            String shippingPincode,
            String phone,
            String email
    ) {}

    public record CreditNoteDetails(
            String creditNoteNumber,
            String creditNoteDate,
            String originalInvoiceNumber,
            String originalInvoiceDate,
            String reason,
            String reasonNotes,
            String placeOfSupplyCode,
            String placeOfSupplyName,
            String state
    ) {}

    public record LineItemDetails(
            int lineNumber,
            String itemDescription,
            String hsnOrSacCode,
            String quantity,
            String unitPrice,
            String taxableValue,
            String taxRatePercent,
            String cgstRatePercent,
            String cgstAmount,
            String sgstRatePercent,
            String sgstAmount,
            String igstRatePercent,
            String igstAmount,
            String cessAmount,
            String lineTotal
    ) {}

    public record RateSlabDetails(
            String taxRatePercent,
            String taxableValue,
            String cgstAmount,
            String sgstAmount,
            String igstAmount,
            String cessAmount,
            String totalTax
    ) {}

    public record TotalsDetails(
            String taxableValue,
            String totalCgst,
            String totalSgst,
            String totalIgst,
            String totalCess,
            String totalTax,
            String roundOff,
            String grandTotal,
            String amountInWords
    ) {}
}
