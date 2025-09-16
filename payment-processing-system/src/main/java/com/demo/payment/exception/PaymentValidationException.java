package com.demo.payment.exception;

public class PaymentValidationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public PaymentValidationException(String message) {
        super(message);
    }
}
