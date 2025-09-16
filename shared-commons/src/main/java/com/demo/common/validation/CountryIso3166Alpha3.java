package com.demo.common.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Annotation for the ISO3166 alpha-3 validation.
 * Validates the field according to ISO3166 Alpha-3 Country code (e.g., DEU, GBR)
 */
@Documented
@Constraint(validatedBy = CountryIso3166Alpha3Validator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface CountryIso3166Alpha3 {
    String message() default "must be a valid ISO3166-1 alpha-3 country code (e.g., DEU, GBR)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
