package com.emg.billing.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * A customer invoice.
 *
 * <p>Amounts are computed by {@code InvoiceCalculator}; this record only carries the result.</p>
 */
public record Invoice(
        String invoiceNumber,
        String customerId,
        LocalDate issueDate,
        List<InvoiceLine> lines,
        BigDecimal netAmount,
        BigDecimal discountAmount,
        BigDecimal vatAmount,
        BigDecimal totalAmount) {
}
