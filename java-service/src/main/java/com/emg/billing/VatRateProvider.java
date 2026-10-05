package com.emg.billing;

import java.math.BigDecimal;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Supplies the applicable VAT rate for a country.
 *
 * <p>Rates are hardcoded for the sandbox. In production they come from the
 * TAX_RATE table loaded by the legacy Pro*C batch.</p>
 */
@Component
public class VatRateProvider {

    private static final Map<String, BigDecimal> RATES = Map.of(
            "FR", new BigDecimal("0.20"),
            "BE", new BigDecimal("0.21"),
            "DE", new BigDecimal("0.19"),
            "MA", new BigDecimal("0.20"),
            "IN", new BigDecimal("0.18"));

    private static final BigDecimal DEFAULT_RATE = new BigDecimal("0.20");

    public BigDecimal rateFor(String countryCode) {
        return RATES.getOrDefault(countryCode, DEFAULT_RATE);
    }
}
