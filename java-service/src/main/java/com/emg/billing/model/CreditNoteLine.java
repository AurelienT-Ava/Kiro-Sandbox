package com.emg.billing.model;

import java.math.BigDecimal;

/**
 * A single line of a credit note.
 *
 * @param reference       product or service reference, matching the original invoice line
 * @param description     free text shown on the printed credit note
 * @param creditedQuantity number of units being credited, always strictly positive
 * @param unitPrice       price per unit, excluding VAT
 */
public record CreditNoteLine(
        String reference,
        String description,
        int creditedQuantity,
        BigDecimal unitPrice) {

    /** Line total excluding VAT and before any commercial discount. */
    public BigDecimal netAmount() {
        return unitPrice.multiply(BigDecimal.valueOf(creditedQuantity));
    }
}
