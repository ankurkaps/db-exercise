package com.demo.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;


/**
 * Annotation for validating a filed according to IBAN. With additional validation checks for SEPA/EU+EEA
 * {@link https://en.wikipedia.org/wiki/International_Bank_Account_Number}
 */
@Documented
@Constraint(validatedBy = IbanIso13616Validator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface IbanIso13616 {
    String message() default "Invalid IBAN";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    /** If true, only allow countries in SEPA/EU+EEA. */
    boolean sepaOnly() default true;
}
