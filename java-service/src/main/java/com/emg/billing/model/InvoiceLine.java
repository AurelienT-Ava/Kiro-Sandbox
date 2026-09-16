package com.emg.billing.model;

import java.math.BigDecimal;

/**
 * A single line of an invoice.
 *
 * @param reference   product or service reference
 * @param description free text shown on the printed invoice
 * @param quantity    number of units billed, always strictly positive
 * @param unitPrice   price per unit, excluding VAT
 */
public record InvoiceLine(
        String reference,
        String description,
        int quantity,
        BigDecimal unitPrice) {

    /** Line total excluding VAT and before any commercial discount. */
    public BigDecimal netAmount() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
