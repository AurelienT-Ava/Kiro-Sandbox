package com.emg.billing;

import static org.assertj.core.api.Assertions.assertThat;

import com.emg.billing.model.Customer;
import com.emg.billing.model.Invoice;
import com.emg.billing.model.InvoiceLine;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InvoiceCalculatorTest {

    private final InvoiceCalculator calculator = new InvoiceCalculator(new VatRateProvider());

    private static Customer standardCustomer() {
        return new Customer("C-1001", "Orion Industries", "FR", new BigDecimal("4200.00"));
    }

    @Test
    @DisplayName("Net amount is the sum of the line amounts")
    void computesNetAmount() {
        List<InvoiceLine> lines = List.of(
                new InvoiceLine("REF-1", "Support pack", 2, new BigDecimal("100.00")),
                new InvoiceLine("REF-2", "Licence", 1, new BigDecimal("50.00")));

        Invoice invoice = calculator.calculate("INV-1", standardCustomer(), lines);

        assertThat(invoice.netAmount()).isEqualByComparingTo("250.00");
    }

    @Test
    @DisplayName("A standard customer gets no discount")
    void appliesNoDiscountForStandardTier() {
        List<InvoiceLine> lines = List.of(
                new InvoiceLine("REF-1", "Support pack", 1, new BigDecimal("200.00")));

        Invoice invoice = calculator.calculate("INV-2", standardCustomer(), lines);

        assertThat(invoice.discountAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("VAT is applied on the discounted amount")
    void appliesVatOnTaxableAmount() {
        List<InvoiceLine> lines = List.of(
                new InvoiceLine("REF-1", "Support pack", 1, new BigDecimal("200.00")));

        Invoice invoice = calculator.calculate("INV-3", standardCustomer(), lines);

        assertThat(invoice.vatAmount()).isEqualByComparingTo("40.00");
        assertThat(invoice.totalAmount()).isEqualByComparingTo("240.00");
    }

    @Test
    @DisplayName("The invoice keeps its lines and its number")
    void keepsInvoiceMetadata() {
        List<InvoiceLine> lines = List.of(
                new InvoiceLine("REF-1", "Support pack", 1, new BigDecimal("10.00")));

        Invoice invoice = calculator.calculate("INV-4", standardCustomer(), lines);

        assertThat(invoice.invoiceNumber()).isEqualTo("INV-4");
        assertThat(invoice.customerId()).isEqualTo("C-1001");
        assertThat(invoice.lines()).hasSize(1);
    }
}
