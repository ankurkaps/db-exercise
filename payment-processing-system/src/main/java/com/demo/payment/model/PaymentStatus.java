package com.demo.payment.model;

public enum PaymentStatus {
    NEW,
    PENDING_FRAUD_CHECK,
    APPROVED,
    REJECTED,
    FAILED,
    EXPIRED
}