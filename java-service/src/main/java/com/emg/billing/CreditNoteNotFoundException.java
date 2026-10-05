package com.emg.billing;

/** Raised when a credit note number does not correspond to any persisted credit note. */
public class CreditNoteNotFoundException extends RuntimeException {

    public CreditNoteNotFoundException(String creditNoteNumber) {
        super("Credit note not found: " + creditNoteNumber);
    }
}
