package com.fastbills.api.invoice.pdf;

import java.util.List;

public record InvoicePdfViewModel(
        String title,
        SupplierDetails supplier,
        CustomerDetails customer,
        InvoiceDetails invoice,
        List<LineItemDetails> lines,
        List<RateSlabDetails> rateSlabs,
        TotalsDetails totals,
        BankDetails bank,
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

    public record InvoiceDetails(
            String invoiceNumber,
            String invoiceDate,
            String dueDate,
            String placeOfSupplyCode,
            String placeOfSupplyName,
            String supplyType,
            String isReverseCharge,
            String poNumber,
            String poDate,
            String collectionReference,
            String notes,
            String termsText
    ) {}

    public record LineItemDetails(
            int lineNumber,
            String itemDescription,
            String hsnOrSacCode,
            String quantity,
            String unitOfMeasure,
            String unitPrice,
            String discountAmount,
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
            String subtotalBeforeDiscount,
            String totalDiscount,
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

    public record BankDetails(
            String bankName,
            String bankAccountName,
            String bankAccountNumber,
            String bankIfsc,
            String bankBranch,
            String upiId,
            String upiUri
    ) {}
}
