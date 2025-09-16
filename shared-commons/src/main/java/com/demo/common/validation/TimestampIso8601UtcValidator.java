 package com.demo.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class TimestampIso8601UtcValidator implements ConstraintValidator<TimestampIso8601Utc, String> {

    @Override
    public void initialize(TimestampIso8601Utc constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return true; // Let @NotNull/@NotBlank handle null/empty validation
        }

        try {
            Instant.parse(value);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}