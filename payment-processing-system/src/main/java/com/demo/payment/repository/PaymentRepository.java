package com.demo.payment.repository;

import com.demo.payment.model.PaymentRecord;
import com.demo.payment.model.PaymentStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {

    /**
     * Save a payment record. Throws exception if transaction ID already exists.
     */
    PaymentRecord save(PaymentRecord paymentRecord);

    /**
     * Update an existing payment record.
     */
    PaymentRecord update(PaymentRecord paymentRecord);

    /**
     * Find payment by transaction ID.
     */
    Optional<PaymentRecord> findByTransactionId(UUID transactionId);

    /**
     * Find all payments with the specified status.
     */
    List<PaymentRecord> findByStatus(PaymentStatus status);

    /**
     * Find all payments.
     */
    List<PaymentRecord> findAll();

    /**
     * Find payments that are pending and older than the specified timestamp.
     */
    List<PaymentRecord> findPendingOlderThan(Instant timestamp);

    /**
     * Delete payment by transaction ID.
     */
    boolean deleteByTransactionId(UUID transactionId);

    /**
     * Get total count of payments.
     */
    long count();

    /**
     * Check if payment exists by transaction ID.
     */
    boolean existsByTransactionId(UUID transactionId);
}