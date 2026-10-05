package com.emg.billing;

import java.math.BigDecimal;

/** Raised when a credit note request attempts to reverse more quantity than remains eligible on a given line. */
public class ExcessiveReversalException extends RuntimeException {

    public ExcessiveReversalException(String lineReference, BigDecimal remainingQty) {
        super("Excessive reversal for line: " + lineReference + ", remaining reversible quantity: " + remainingQty);
    }
}
