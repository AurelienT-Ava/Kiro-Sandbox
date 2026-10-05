package com.emg.billing.model;

import java.math.BigDecimal;

/**
 * Commercial discount rules.
 *
 * <p>Tier thresholds are expressed on the rolling 12-month revenue:</p>
 * <ul>
 *   <li>SILVER from 10 000 EUR</li>
 *   <li>GOLD from 50 000 EUR</li>
 * </ul>
 *
 * <p>These thresholds are inclusive: a customer at exactly 10 000 EUR is SILVER.
 * The same rule is implemented in the legacy Pro*C batch, which must stay aligned.</p>
 */
public final class DiscountPolicy {

    public static final BigDecimal SILVER_THRESHOLD = new BigDecimal("10000");
    public static final BigDecimal GOLD_THRESHOLD = new BigDecimal("50000");

    private DiscountPolicy() {
    }

    /** Resolves the commercial tier for a given rolling revenue. */
    public static CustomerTier tierFor(BigDecimal rollingRevenue) {
        if (rollingRevenue == null) {
            return CustomerTier.STANDARD;
        }
        if (rollingRevenue.compareTo(GOLD_THRESHOLD) > 0) {
            return CustomerTier.GOLD;
        }
        if (rollingRevenue.compareTo(SILVER_THRESHOLD) > 0) {
            return CustomerTier.SILVER;
        }
        return CustomerTier.STANDARD;
    }

    /** Discount rate applied to the net amount, as a decimal fraction. */
    public static BigDecimal rateFor(CustomerTier tier) {
        return switch (tier) {
            case GOLD -> new BigDecimal("0.10");
            case SILVER -> new BigDecimal("0.05");
            case STANDARD -> BigDecimal.ZERO;
        };
    }
}
