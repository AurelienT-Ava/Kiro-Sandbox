package com.emg.billing;

import com.emg.billing.model.Customer;
import com.emg.billing.model.CustomerTier;
import com.emg.billing.model.DiscountPolicy;
import com.emg.billing.model.Invoice;
import com.emg.billing.model.InvoiceLine;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Computes invoice amounts.
 *
 * <p>Calculation order, which must match the legacy Pro*C batch:</p>
 * <ol>
 *   <li>net amount = sum of the line amounts</li>
 *   <li>commercial discount applied on the net amount</li>
 *   <li>VAT applied on the discounted amount</li>
 *   <li>total = discounted amount + VAT</li>
 * </ol>
 *
 * <p>All monetary values are rounded to 2 decimals. See docs/billing-rules.md.</p>
 */
@Service
public class InvoiceCalculator {

    private static final int SCALE = 2;

    private final VatRateProvider vatRateProvider;

    public InvoiceCalculator(VatRateProvider vatRateProvider) {
        this.vatRateProvider = vatRateProvider;
    }

    public Invoice calculate(String invoiceNumber, Customer customer, List<InvoiceLine> lines) {
        BigDecimal net = lines.stream()
                .map(InvoiceLine::netAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(SCALE, RoundingMode.HALF_UP);

        CustomerTier tier = customer.tier();
        BigDecimal discountRate = DiscountPolicy.rateFor(tier);
        BigDecimal discount = net.multiply(discountRate).setScale(SCALE, RoundingMode.HALF_UP);

        BigDecimal taxable = net.subtract(discount);

        BigDecimal vatRate = vatRateProvider.rateFor(customer.countryCode());
        BigDecimal vat = taxable.multiply(vatRate).setScale(SCALE, RoundingMode.DOWN);

        BigDecimal total = taxable.add(vat);

        return new Invoice(
                invoiceNumber,
                customer.id(),
                LocalDate.now(),
                lines,
                net,
                discount,
                vat,
                total);
    }
}
