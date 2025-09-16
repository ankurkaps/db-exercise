package com.demo.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Currency;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ISO4217 Currency validator based on java.util.currency. Hence availability of currencies depends on JDK
 *
 * The authoritative source for the ISO 4217 currency codes is the ISO 4217 Maintenance Agency, which is run by SIX
 * https://www.six-group.com/.
 *
 */
public class CurrencyIso4217Validator implements ConstraintValidator<CurrencyIso4217, String> {
    private static final Set<String> CODES = Currency.getAvailableCurrencies().stream().map(Currency::getCurrencyCode)
            .collect(Collectors.toUnmodifiableSet());

    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        if (value == null)
            return true; // use @NotBlank for null/blank checks
        return CODES.contains(value.trim().toUpperCase(Locale.ROOT));
    }
}
