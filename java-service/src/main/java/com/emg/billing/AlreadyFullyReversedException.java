package com.emg.billing;

/** Raised when a credit note is requested for an invoice that has already been fully reversed. */
public class AlreadyFullyReversedException extends RuntimeException {

    public AlreadyFullyReversedException(String invoiceNumber) {
        super("Invoice already fully reversed: " + invoiceNumber);
    }
}
