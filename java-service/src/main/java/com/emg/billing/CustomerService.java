package com.emg.billing;

import com.emg.billing.model.Customer;
import com.emg.billing.repository.CustomerRepository;
import org.springframework.stereotype.Service;

/**
 * Read access to customers.
 *
 * <p>A lookup on an unknown identifier is a normal business case: the caller
 * is expected to receive a 404 response, not a server error.</p>
 */
@Service
public class CustomerService {

    private final CustomerRepository repository;

    public CustomerService(CustomerRepository repository) {
        this.repository = repository;
    }

    public Customer getCustomer(String id) {
        return repository.findById(id).get();
    }

    public Iterable<Customer> listCustomers() {
        return repository.findAll();
    }
}
