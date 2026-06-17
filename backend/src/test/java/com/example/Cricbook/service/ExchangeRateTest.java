package com.example.Cricbook.service;

import com.example.Cricbook.entity.ExchangeRate;
import com.example.Cricbook.repository.CityConfigRepository;
import com.example.Cricbook.repository.ExchangeRateRepository;
import com.example.Cricbook.repository.TranslationRepository;
import com.example.Cricbook.service.global.GlobalSportsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ExchangeRateTest {

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    @Mock
    private TranslationRepository translationRepository;

    @Mock
    private CityConfigRepository cityConfigRepository;

    @InjectMocks
    private GlobalSportsService globalSportsService;

    @Test
    void testConvertCurrency_DirectRateFound() {
        BigDecimal amount = BigDecimal.valueOf(1000);
        ExchangeRate inrToAed = ExchangeRate.builder()
                .baseCurrency("INR")
                .targetCurrency("AED")
                .rate(BigDecimal.valueOf(0.044))
                .build();

        when(exchangeRateRepository.findByBaseCurrencyAndTargetCurrency("INR", "AED"))
                .thenReturn(Optional.of(inrToAed));

        BigDecimal result = globalSportsService.convertCurrency(amount, "INR", "AED");

        // 1000 * 0.044 = 44.00
        assertEquals(0, BigDecimal.valueOf(44.00).compareTo(result));
    }

    @Test
    void testConvertCurrency_SameCurrencies() {
        BigDecimal amount = BigDecimal.valueOf(1000);

        BigDecimal result = globalSportsService.convertCurrency(amount, "USD", "USD");

        assertEquals(amount, result);
        verify(exchangeRateRepository, never()).findByBaseCurrencyAndTargetCurrency(anyString(), anyString());
    }

    @Test
    void testConvertCurrency_InverseRateResolution() {
        BigDecimal amount = BigDecimal.valueOf(44);
        ExchangeRate inrToAed = ExchangeRate.builder()
                .baseCurrency("INR")
                .targetCurrency("AED")
                .rate(BigDecimal.valueOf(0.044))
                .build();

        when(exchangeRateRepository.findByBaseCurrencyAndTargetCurrency("AED", "INR"))
                .thenReturn(Optional.empty());
        when(exchangeRateRepository.findByBaseCurrencyAndTargetCurrency("INR", "AED"))
                .thenReturn(Optional.of(inrToAed));

        BigDecimal result = globalSportsService.convertCurrency(amount, "AED", "INR");

        // 44 / 0.044 = 1000.00
        assertEquals(0, BigDecimal.valueOf(1000.00).compareTo(result));
    }

    @Test
    void testConvertTimezone_IndiaToDubai() {
        // Asia/Kolkata is UTC+5:30, Asia/Dubai is UTC+4:00 (1.5 hours behind)
        LocalTime kolkataTime = LocalTime.of(18, 30);

        LocalTime dubaiTime = globalSportsService.convertTimezone(kolkataTime, "Asia/Kolkata", "Asia/Dubai");

        assertNotNull(dubaiTime);
        assertEquals(17, dubaiTime.getHour());
        assertEquals(0, dubaiTime.getMinute());
    }
}
