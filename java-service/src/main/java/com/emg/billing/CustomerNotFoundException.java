package com.emg.billing;

/** Raised when a customer identifier does not exist. */
public class CustomerNotFoundException extends RuntimeException {

    public CustomerNotFoundException(String customerId) {
        super("Unknown customer: " + customerId);
    }
}
