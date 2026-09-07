package com.fastbills.api.creditnote;

public enum CreditNoteReason {
    SALES_RETURN("Sales Return"),
    POST_SALE_DISCOUNT("Post-sale Discount"),
    DEFICIENCY_IN_SERVICE("Deficiency in Service"),
    CORRECTION_IN_INVOICE("Correction in Invoice"),
    CHANGE_IN_POS_OR_RATE("Change in Place of Supply or Tax Rate"),
    OTHER("Other");

    private final String description;

    CreditNoteReason(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
