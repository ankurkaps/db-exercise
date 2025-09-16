package com.demo.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class DateIso8601Validator implements ConstraintValidator<DateIso8601, String> {

    private DateTimeFormatter formatter;

    @Override
    public void initialize(DateIso8601 constraintAnnotation) {
        this.formatter = DateTimeFormatter.ofPattern(constraintAnnotation.pattern());
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return true; // Let @NotNull/@NotBlank handle null/empty validation
        }

        try {
            formatter.parse(value);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}