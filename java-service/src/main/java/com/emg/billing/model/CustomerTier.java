package com.emg.billing.model;

/**
 * Commercial tier of a customer. Drives the discount policy.
 *
 * <p>Tier assignment is based on the customer's rolling 12-month revenue.
 * See docs/billing-rules.md for the business rules.</p>
 */
public enum CustomerTier {
    STANDARD,
    SILVER,
    GOLD
}
