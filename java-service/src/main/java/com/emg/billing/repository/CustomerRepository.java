package com.emg.billing.repository;

import com.emg.billing.model.Customer;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * In-memory customer store.
 *
 * <p>Stands in for the Oracle CUSTOMER table so the sandbox needs no database.</p>
 */
@Repository
public class CustomerRepository {

    private final Map<String, Customer> customers = new LinkedHashMap<>();

    public CustomerRepository() {
        save(new Customer("C-1001", "Orion Industries", "FR", new BigDecimal("4200.00")));
        save(new Customer("C-1002", "Delta Logistics", "BE", new BigDecimal("10000.00")));
        save(new Customer("C-1003", "Atlas Manufacturing", "DE", new BigDecimal("27500.00")));
        save(new Customer("C-1004", "Sahara Trading", "MA", new BigDecimal("50000.00")));
        save(new Customer("C-1005", "Ganges Software", "IN", new BigDecimal("81000.00")));
    }

    public final void save(Customer customer) {
        customers.put(customer.id(), customer);
    }

    public Optional<Customer> findById(String id) {
        return Optional.ofNullable(customers.get(id));
    }

    public Iterable<Customer> findAll() {
        return customers.values();
    }
}
