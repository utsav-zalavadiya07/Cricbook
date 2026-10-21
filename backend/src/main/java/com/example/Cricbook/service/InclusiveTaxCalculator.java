package com.example.Cricbook.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class InclusiveTaxCalculator {

    public static final BigDecimal GST_RATE_PERCENT = BigDecimal.valueOf(18);
    public static final BigDecimal GST_RATE_FRACTION = new BigDecimal("0.18");
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private InclusiveTaxCalculator() {
    }

    public static TaxBreakdown fromInclusiveTotal(BigDecimal inclusiveTotal) {
        BigDecimal total = money(inclusiveTotal);
        BigDecimal tax = total.multiply(GST_RATE_PERCENT)
                .divide(HUNDRED.add(GST_RATE_PERCENT), 2, RoundingMode.HALF_UP);
        return new TaxBreakdown(total.subtract(tax), tax, total);
    }

    private static BigDecimal money(BigDecimal value) {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException("Inclusive total must be zero or greater.");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public record TaxBreakdown(BigDecimal taxableAmount, BigDecimal taxAmount, BigDecimal totalAmount) {
    }
}
