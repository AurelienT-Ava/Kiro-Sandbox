package com.emg.billing.model;

import java.math.BigDecimal;

/**
 * A billable customer.
 *
 * @param id             internal customer identifier (matches CUST_ID in the legacy Oracle schema)
 * @param name           legal name as printed on the invoice
 * @param countryCode    ISO 3166-1 alpha-2 country code, drives the VAT rate
 * @param rollingRevenue rolling 12-month revenue, used to compute the commercial tier
 */
public record Customer(
        String id,
        String name,
        String countryCode,
        BigDecimal rollingRevenue) {

    public CustomerTier tier() {
        return DiscountPolicy.tierFor(rollingRevenue);
    }
}
