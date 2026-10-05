package com.emg.billing;

/** Raised when an invoice number does not correspond to any persisted invoice. */
public class InvoiceNotFoundException extends RuntimeException {

    public InvoiceNotFoundException(String invoiceNumber) {
        super("Invoice not found: " + invoiceNumber);
    }
}
