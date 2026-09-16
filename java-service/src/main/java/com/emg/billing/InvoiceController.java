package com.emg.billing;

import com.emg.billing.model.Customer;
import com.emg.billing.model.Invoice;
import com.emg.billing.model.InvoiceLine;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Invoice endpoints.
 *
 * <p>REST conventions in force on this service:</p>
 * <ul>
 *   <li>plural resource names</li>
 *   <li>error payload {@code { "error": string, "code": number }}</li>
 *   <li>amounts serialised as decimal numbers with 2 decimals</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceCalculator calculator;
    private final CustomerService customerService;
    private final AtomicInteger sequence = new AtomicInteger(1);

    public InvoiceController(InvoiceCalculator calculator, CustomerService customerService) {
        this.calculator = calculator;
        this.customerService = customerService;
    }

    /** Creates an invoice for a customer from a list of lines. */
    @PostMapping("/{customerId}")
    public Invoice createInvoice(@PathVariable String customerId,
                                 @RequestBody List<LineRequest> lineRequests) {
        if (lineRequests == null || lineRequests.isEmpty()) {
            throw new IllegalArgumentException("An invoice must contain at least one line");
        }

        Customer customer = customerService.getCustomer(customerId);

        List<InvoiceLine> lines = lineRequests.stream()
                .map(r -> new InvoiceLine(r.reference(), r.description(), r.quantity(), r.unitPrice()))
                .toList();

        String invoiceNumber = "INV-%05d".formatted(sequence.getAndIncrement());
        return calculator.calculate(invoiceNumber, customer, lines);
    }

    /** Returns the customers available in the sandbox. */
    @GetMapping("/customers")
    public Iterable<Customer> listCustomers() {
        return customerService.listCustomers();
    }

    /** Payload for one requested invoice line. */
    public record LineRequest(
            String reference,
            String description,
            int quantity,
            BigDecimal unitPrice) {
    }
}
