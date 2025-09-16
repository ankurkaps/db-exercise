package com.demo.payment.validation;

import org.springframework.stereotype.Component;

import com.demo.common.model.PaymentRequest;

import lombok.extern.slf4j.Slf4j;


/**
 * Payment Validator - Placeholder for cross field and complex validation rather than Single field
 */
@Slf4j
@Component
public class PaymentValidator {
    public void validate(PaymentRequest request) {
        log.info("Complex validation of Payment Request: {}", request);
        
        // NOP
        // Check country and currency combinations etc.

    }
}
