package com.emg.billing;

import static org.assertj.core.api.Assertions.assertThat;

import com.emg.billing.model.CustomerTier;
import com.emg.billing.model.DiscountPolicy;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DiscountPolicyTest {

    @Test
    @DisplayName("Low revenue stays on the standard tier")
    void standardTier() {
        assertThat(DiscountPolicy.tierFor(new BigDecimal("4200"))).isEqualTo(CustomerTier.STANDARD);
    }

    @Test
    @DisplayName("Mid revenue reaches the silver tier")
    void silverTier() {
        assertThat(DiscountPolicy.tierFor(new BigDecimal("27500"))).isEqualTo(CustomerTier.SILVER);
    }

    @Test
    @DisplayName("High revenue reaches the gold tier")
    void goldTier() {
        assertThat(DiscountPolicy.tierFor(new BigDecimal("81000"))).isEqualTo(CustomerTier.GOLD);
    }

    @Test
    @DisplayName("A missing revenue falls back to the standard tier")
    void nullRevenue() {
        assertThat(DiscountPolicy.tierFor(null)).isEqualTo(CustomerTier.STANDARD);
    }

    @Test
    @DisplayName("Discount rates match the commercial grid")
    void discountRates() {
        assertThat(DiscountPolicy.rateFor(CustomerTier.STANDARD)).isEqualByComparingTo("0.00");
        assertThat(DiscountPolicy.rateFor(CustomerTier.SILVER)).isEqualByComparingTo("0.05");
        assertThat(DiscountPolicy.rateFor(CustomerTier.GOLD)).isEqualByComparingTo("0.10");
    }
}
