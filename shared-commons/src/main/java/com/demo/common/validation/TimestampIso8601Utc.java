package com.demo.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = TimestampIso8601UtcValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface TimestampIso8601Utc {
    String message() default "Invalid timestamp format, timestamp should be in ISO8601 UTC format YYYY-MM-DDThh:mm:ssZ, for example: 2004-02-21T17:00:00Z";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}