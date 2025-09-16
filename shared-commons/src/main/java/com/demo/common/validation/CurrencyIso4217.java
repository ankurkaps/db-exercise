package com.demo.common.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Annotation for ISO4217 Currency validator based on java.util.currency (e.g., EUR, GBP)
 */
@Documented
@Constraint(validatedBy = CurrencyIso4217Validator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrencyIso4217 {
    String message() default "must be a valid ISO4217 currency code (e.g., EUR, GBP)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
