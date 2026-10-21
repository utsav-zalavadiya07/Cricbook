package com.example.Cricbook.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InclusiveTaxCalculatorTest {

    @Test
    void extractsEighteenPercentGstFromInclusivePlanPrice() {
        InclusiveTaxCalculator.TaxBreakdown result =
                InclusiveTaxCalculator.fromInclusiveTotal(new BigDecimal("4999.00"));

        assertEquals(new BigDecimal("4236.44"), result.taxableAmount());
        assertEquals(new BigDecimal("762.56"), result.taxAmount());
        assertEquals(new BigDecimal("4999.00"), result.totalAmount());
        assertEquals(result.totalAmount(), result.taxableAmount().add(result.taxAmount()));
    }

    @Test
    void rejectsNegativeInclusivePrice() {
        assertThrows(IllegalArgumentException.class,
                () -> InclusiveTaxCalculator.fromInclusiveTotal(new BigDecimal("-1.00")));
    }
}
