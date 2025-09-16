package com.demo.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.*;

/**
 * IBAN validator according to ISO13616
 *
 * https://en.wikipedia.org/wiki/International_Bank_Account_Number
 */
public class IbanIso13616Validator implements ConstraintValidator<IbanIso13616, String> {

    private boolean sepaOnly;

    // Country-specific IBAN lengths (SEPA/EU + a few common ones)
    private static final Map<String, Integer> IBAN_LENGTHS = Map.ofEntries(
        // EU/EEA/SEPA
        Map.entry("AT", 20), Map.entry("BE", 16), Map.entry("BG", 22), Map.entry("HR", 21),
        Map.entry("CY", 28), Map.entry("CZ", 24), Map.entry("DK", 18), Map.entry("EE", 20),
        Map.entry("FI", 18), Map.entry("FR", 27), Map.entry("DE", 22), Map.entry("GR", 27),
        Map.entry("HU", 28), Map.entry("IS", 26), Map.entry("IE", 22), Map.entry("IT", 27),
        Map.entry("LV", 21), Map.entry("LI", 21), Map.entry("LT", 20), Map.entry("LU", 20),
        Map.entry("MT", 31), Map.entry("MC", 27), Map.entry("NL", 18), Map.entry("NO", 15),
        Map.entry("PL", 28), Map.entry("PT", 25), Map.entry("RO", 24), Map.entry("SM", 27),
        Map.entry("SK", 24), Map.entry("SI", 19), Map.entry("ES", 24), Map.entry("SE", 24),
        Map.entry("GB", 22), Map.entry("CH", 21)
        // add more as needed
    );

    // If you want to hard-restrict to SEPA only, list them here (2-letter codes):
    private static final Set<String> SEPA_COUNTRIES = Set.of(
        "AT","BE","BG","HR","CY","CZ","DK","EE","FI","FR","DE","GR","HU","IS","IE","IT",
        "LV","LI","LT","LU","MT","MC","NL","NO","PL","PT","RO","SM","SK","SI","ES","SE",
        "CH","GB"
    );

    @Override
    public void initialize(IbanIso13616 constraintAnnotation) {
        this.sepaOnly = constraintAnnotation.sepaOnly();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        if (value == null) return true; // use @NotBlank/@NotNull separately

        // Normalize to uppercase
        var iban = value.toUpperCase(Locale.ROOT);

        // Basic shape: CCkk + up to 30 alphanumerics, total 15..34
        if (!iban.matches("^[A-Z]{2}\\d{2}[A-Z0-9]{1,30}$")) return false;
        if (iban.length() < 15 || iban.length() > 34) return false;

        // Country checks
        var cc = iban.substring(0, 2);
        var expectedLen = IBAN_LENGTHS.get(cc);
        if (expectedLen == null) {
            if (sepaOnly) return false; // unknown/non-SEPA country code
        } else if (iban.length() != expectedLen) {
            return false; // wrong length for that country
        }

        if (sepaOnly && !SEPA_COUNTRIES.contains(cc)) return false;

        // MOD-97 check: move first 4 chars to end, A=10..Z=35, numeric string % 97 == 1
        var rearranged = iban.substring(4) + iban.substring(0, 4);
        var numeric = new StringBuilder(rearranged.length() * 2);
        for (var i = 0; i < rearranged.length(); i++) {
            var ch = rearranged.charAt(i);
            if (ch >= 'A' && ch <= 'Z') numeric.append(ch - 'A' + 10);
            else numeric.append(ch); // digits
        }

        return mod97(numeric.toString()) == 1;
    }

    // Efficient mod-97 for big strings without BigInteger
    private static int mod97(String s) {
        var remainder = 0;
        for (var i = 0; i < s.length(); i++) {
            var c = s.charAt(i);
            remainder = (remainder * 10 + (c - '0')) % 97;
        }
        return remainder;
    }
}
