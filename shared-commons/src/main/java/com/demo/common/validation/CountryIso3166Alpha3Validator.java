package com.demo.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ISO3166 alpha-3 country code validator uses JDK Locale.
 *
 * Depending on the JDK some countries may be missing and may need to be added. Apparently if you need up to date
 * info you need to pull from source.
 *
 * The official authoritative source for ISO 3166-1 alpha-3 codes is the ISO 3166 Maintenance Agency (ISO 3166/MA)
 *
 * Potential alternatives (libraries):
 *
 * https://github.com/TakahikoKawasaki/nv-i18n - Not updated (potentially not maintained last update 4yrs ago)
 * https://github.com/scout-2766/Iso4J - Unmaintained
 * https://github.com/hervegirod/countryiso3166 - Last update 5yrs ago
 *
 */
public class CountryIso3166Alpha3Validator implements ConstraintValidator<CountryIso3166Alpha3, String> {

    private static final Set<String> ALPHA3;

    static {
        // Build the alpha-3 set from JDK locales
        @SuppressWarnings("deprecation")
        Set<String> alpha3 = Arrays.stream(Locale.getISOCountries()).map(code2 -> {
            try {
                return new Locale("", code2).getISO3Country();
            } catch (MissingResourceException e) {
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toSet());

        // Include XKX (Kosovo) which may be missing on some JDKs. Not official but
        // widely used in EU & IMF
        alpha3.add("XKX");

        ALPHA3 = Collections.unmodifiableSet(alpha3);
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        if (value == null)
            return true; // use @NotBlank to enforce presence
        return ALPHA3.contains(value.toUpperCase(Locale.ROOT));
    }
}