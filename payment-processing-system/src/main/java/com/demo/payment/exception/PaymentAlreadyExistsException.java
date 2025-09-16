package com.demo.payment.exception;

public class PaymentAlreadyExistsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PaymentAlreadyExistsException(String message) {
        super(message);
    }

    public PaymentAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}