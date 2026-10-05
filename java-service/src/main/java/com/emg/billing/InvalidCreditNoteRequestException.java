package com.emg.billing;

public class InvalidCreditNoteRequestException extends RuntimeException {

    public InvalidCreditNoteRequestException(String message) {
        super(message);
    }
}
