package com.emg.billing;

import java.util.List;

/**
 * Request body for issuing a credit note against an existing invoice.
 *
 * @param fullReversal when {@code true} the entire invoice is reversed; {@code lines} is ignored
 * @param lines        individual line reversals for a partial credit note
 */
public record CreditNoteRequest(
        boolean fullReversal,
        List<CreditNoteLineRequest> lines) {

    /**
     * A single line reversal within a credit note request.
     *
     * @param reference product or service reference, must match an existing invoice line
     * @param quantity  number of units to credit, must be &ge; 1
     */
    public record CreditNoteLineRequest(
            String reference,
            int quantity) {
    }
}
