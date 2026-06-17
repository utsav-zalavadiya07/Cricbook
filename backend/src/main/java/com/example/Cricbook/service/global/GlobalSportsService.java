package com.example.Cricbook.service.global;

import com.example.Cricbook.entity.CityConfig;
import com.example.Cricbook.entity.ExchangeRate;
import com.example.Cricbook.entity.Translation;
import com.example.Cricbook.exception.ApiException;
import com.example.Cricbook.repository.CityConfigRepository;
import com.example.Cricbook.repository.ExchangeRateRepository;
import com.example.Cricbook.repository.TranslationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class GlobalSportsService {

    private final AtomicBoolean defaultsSeeded = new AtomicBoolean(false);
    private final Object seedLock = new Object();

    private final TranslationRepository translationRepository;
    private final ExchangeRateRepository exchangeRateRepository;
    private final CityConfigRepository cityConfigRepository;

    public BigDecimal convertCurrency(BigDecimal baseAmount, String baseCurrency, String targetCurrency) {
        if (baseAmount == null) {
            return BigDecimal.ZERO;
        }
        if (baseCurrency == null || targetCurrency == null || baseCurrency.equalsIgnoreCase(targetCurrency)) {
            return baseAmount;
        }

        Optional<ExchangeRate> rateOpt = exchangeRateRepository.findByBaseCurrencyAndTargetCurrency(
                baseCurrency.toUpperCase(), targetCurrency.toUpperCase());

        if (rateOpt.isPresent()) {
            return baseAmount.multiply(rateOpt.get().getRate()).setScale(2, RoundingMode.HALF_UP);
        }

        Optional<ExchangeRate> inverseOpt = exchangeRateRepository.findByBaseCurrencyAndTargetCurrency(
                targetCurrency.toUpperCase(), baseCurrency.toUpperCase());
        if (inverseOpt.isPresent() && inverseOpt.get().getRate().compareTo(BigDecimal.ZERO) > 0) {
            return baseAmount.divide(inverseOpt.get().getRate(), 2, RoundingMode.HALF_UP);
        }

        double fallbackRate = 1.0;
        if (baseCurrency.equalsIgnoreCase("INR") && targetCurrency.equalsIgnoreCase("AED")) {
            fallbackRate = 0.044;
        } else if (baseCurrency.equalsIgnoreCase("INR") && targetCurrency.equalsIgnoreCase("USD")) {
            fallbackRate = 0.012;
        } else if (baseCurrency.equalsIgnoreCase("INR") && targetCurrency.equalsIgnoreCase("GBP")) {
            fallbackRate = 0.0094;
        }

        return baseAmount.multiply(BigDecimal.valueOf(fallbackRate)).setScale(2, RoundingMode.HALF_UP);
    }

    public LocalTime convertTimezone(LocalTime originalTime, String sourceTimezone, String targetTimezone) {
        if (originalTime == null || sourceTimezone == null || targetTimezone == null) {
            return originalTime;
        }
        try {
            ZoneId sourceZone = ZoneId.of(sourceTimezone);
            ZoneId targetZone = ZoneId.of(targetTimezone);
            ZonedDateTime sourceZdt = ZonedDateTime.now(sourceZone).with(originalTime);
            ZonedDateTime targetZdt = sourceZdt.withZoneSameInstant(targetZone);
            return targetZdt.toLocalTime();
        } catch (Exception e) {
            log.warn("Timezone offset conversion failed: {} -> {}", sourceTimezone, targetTimezone);
            return originalTime;
        }
    }

    public void seedDefaultData() {
        if (defaultsSeeded.get()) {
            return;
        }

        synchronized (seedLock) {
            if (defaultsSeeded.get()) {
                return;
            }

            seedDefaultDataInternal();
            defaultsSeeded.set(true);
        }
    }

    @Transactional
    protected void seedDefaultDataInternal() {
        try {
            seedExchangeRate("INR", "AED", BigDecimal.valueOf(0.044));
            seedExchangeRate("INR", "USD", BigDecimal.valueOf(0.012));
            seedExchangeRate("INR", "GBP", BigDecimal.valueOf(0.0094));
            seedExchangeRate("INR", "SGD", BigDecimal.valueOf(0.016));
            seedExchangeRate("INR", "AUD", BigDecimal.valueOf(0.018));

            seedTranslation("hi", "Book Turf", "Book Turf");
            seedTranslation("hi", "Personalized Feed", "Personalized Feed");
            seedTranslation("gu", "Book Turf", "Book Turf");
            seedTranslation("ar", "Book Turf", "Book Turf");
            seedTranslation("fr", "Book Turf", "Book Turf");

            if (cityConfigRepository.findAll().isEmpty()) {
                cityConfigRepository.save(CityConfig.builder().city("Ahmedabad").country("India").currency("INR").locale("gu").timeZone("Asia/Kolkata").active(true).build());
                cityConfigRepository.save(CityConfig.builder().city("Mumbai").country("India").currency("INR").locale("hi").timeZone("Asia/Kolkata").active(true).build());
                cityConfigRepository.save(CityConfig.builder().city("Dubai").country("UAE").currency("AED").locale("ar").timeZone("Asia/Dubai").active(true).build());
                cityConfigRepository.save(CityConfig.builder().city("London").country("UK").currency("GBP").locale("en").timeZone("Europe/London").active(true).build());
                cityConfigRepository.save(CityConfig.builder().city("Singapore").country("Singapore").currency("SGD").locale("en").timeZone("Asia/Singapore").active(true).build());
            }
        } catch (DataIntegrityViolationException ex) {
            log.warn("Global expansion seed hit a duplicate/constraint race and will continue: {}", ex.getMessage());
            defaultsSeeded.set(true);
        } catch (Exception ex) {
            log.error("Failed to seed global expansion data", ex);
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "GLOBAL_SEED_FAILED", "Global configuration data is temporarily unavailable. Please try again.");
        }
    }

    private void seedExchangeRate(String base, String target, BigDecimal rate) {
        if (exchangeRateRepository.findByBaseCurrencyAndTargetCurrency(base, target).isEmpty()) {
            exchangeRateRepository.save(ExchangeRate.builder()
                    .baseCurrency(base)
                    .targetCurrency(target)
                    .rate(rate)
                    .build());
        }
    }

    private void seedTranslation(String locale, String key, String value) {
        if (translationRepository.findByLocaleAndTranslationKey(locale, key).isEmpty()) {
            translationRepository.save(Translation.builder()
                    .locale(locale)
                    .translationKey(key)
                    .translatedValue(value)
                    .build());
        }
    }
}
