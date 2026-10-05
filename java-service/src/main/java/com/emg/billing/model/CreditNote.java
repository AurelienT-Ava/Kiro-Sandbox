package com.emg.billing.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * A credit note issued against an original invoice.
 *
 * <p>Amounts are computed by {@code CreditNoteCalculator}; this record only carries the result.
 * All monetary fields ({@code netAmount}, {@code discountAmount}, {@code vatAmount},
 * {@code totalAmount}) are negative for issued credit notes.</p>
 */
public record CreditNote(
        String creditNoteNumber,
        String invoiceNumber,
        String customerId,
        LocalDate issueDate,
        List<CreditNoteLine> lines,
        BigDecimal netAmount,
        BigDecimal discountAmount,
        BigDecimal vatAmount,
        BigDecimal totalAmount) {
}
