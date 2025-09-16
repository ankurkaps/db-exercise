package com.demo.payment.repository;

import com.demo.payment.exception.PaymentAlreadyExistsException;
import com.demo.payment.model.PaymentRecord;
import com.demo.payment.model.PaymentStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class InMemoryPaymentRepository implements PaymentRepository {

    private final Map<UUID, PaymentRecord> payments = new ConcurrentHashMap<>();

    @Override
    public PaymentRecord save(PaymentRecord paymentRecord) {
        UUID transactionId = paymentRecord.getTransactionId();

        if (payments.containsKey(transactionId)) {
            throw new PaymentAlreadyExistsException(
                    "Payment with transaction ID " + transactionId + " already exists");
        }

        payments.put(transactionId, paymentRecord);
        log.info("Saved payment record for transaction ID: {}", transactionId);

        return paymentRecord;
    }

    @Override
    public PaymentRecord update(PaymentRecord paymentRecord) {
        UUID transactionId = paymentRecord.getTransactionId();

        if (!payments.containsKey(transactionId)) {
            throw new IllegalArgumentException(
                "Payment with transaction ID " + transactionId + " does not exist");
        }

        payments.put(transactionId, paymentRecord);
        log.info("Updated payment record for transaction ID: {} with status: {}",
            transactionId, paymentRecord.getStatus());

        return paymentRecord;
    }

    @Override
    public Optional<PaymentRecord> findByTransactionId(UUID transactionId) {
        return Optional.ofNullable(payments.get(transactionId));
    }

    @Override
    public List<PaymentRecord> findByStatus(PaymentStatus status) {
        return payments.values().stream()
            .filter(payment -> payment.getStatus() == status)
            .sorted(Comparator.comparing(PaymentRecord::getSubmittedTimestamp).reversed())
            .collect(Collectors.toList());
    }

    @Override
    public List<PaymentRecord> findAll() {
        return payments.values().stream()
            .sorted(Comparator.comparing(PaymentRecord::getSubmittedTimestamp).reversed())
            .collect(Collectors.toList());
    }

    @Override
    public List<PaymentRecord> findPendingOlderThan(Instant timestamp) {
        return payments.values().stream()
            .filter(payment -> payment.getStatus() == PaymentStatus.PENDING_FRAUD_CHECK)
            .filter(payment -> payment.getSubmittedTimestamp().isBefore(timestamp))
            .sorted(Comparator.comparing(PaymentRecord::getSubmittedTimestamp))
            .collect(Collectors.toList());
    }

    @Override
    public boolean deleteByTransactionId(UUID transactionId) {
        PaymentRecord removed = payments.remove(transactionId);
        if (removed != null) {
            log.info("Deleted payment record for transaction ID: {}", transactionId);
            return true;
        }
        return false;
    }

    @Override
    public long count() {
        return payments.size();
    }

    @Override
    public boolean existsByTransactionId(UUID transactionId) {
        return payments.containsKey(transactionId);
    }

    // Additional utility methods for monitoring and debugging
    public Map<PaymentStatus, Long> getStatusCounts() {
        return payments.values().stream()
            .collect(Collectors.groupingBy(
                PaymentRecord::getStatus,
                Collectors.counting()
            ));
    }

    public void clear() {
        payments.clear();
        log.info("Cleared all payment records from repository");
    }
}