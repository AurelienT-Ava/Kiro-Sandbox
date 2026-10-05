package com.emg.billing;

import static org.assertj.core.api.Assertions.assertThat;

import com.emg.billing.model.Customer;
import com.emg.billing.repository.CustomerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CustomerServiceTest {

    private final CustomerService service = new CustomerService(new CustomerRepository());

    @Test
    @DisplayName("An existing customer is returned")
    void returnsKnownCustomer() {
        Customer customer = service.getCustomer("C-1003");

        assertThat(customer.name()).isEqualTo("Atlas Manufacturing");
        assertThat(customer.countryCode()).isEqualTo("DE");
    }

    @Test
    @DisplayName("The sandbox ships five customers")
    void listsCustomers() {
        assertThat(service.listCustomers()).hasSize(5);
    }
}
