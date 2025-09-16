package com.demo.payment.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.demo.common.model.PaymentRequest;
import com.demo.payment.exception.PaymentAlreadyExistsException;
import com.demo.payment.model.PaymentRecord;
import com.demo.payment.model.PaymentStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("In-Memory Payment Repository Tests")
class InMemoryPaymentRepositoryTest {

    private InMemoryPaymentRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryPaymentRepository();
    }

    @Test
    @DisplayName("Should save payment record successfully")
    void shouldSavePaymentRecordSuccessfully() {
        // Given
        PaymentRecord record = createTestPaymentRecord();

        // When
        PaymentRecord saved = repository.save(record);

        // Then
        assertThat(saved).isEqualTo(record);
        assertThat(repository.count()).isEqualTo(1);
        assertThat(repository.existsByTransactionId(record.getTransactionId())).isTrue();
    }

    @Test
    @DisplayName("Should throw exception when saving duplicate transaction ID")
    void shouldThrowExceptionWhenSavingDuplicateTransactionId() {
        // Given
        PaymentRecord record1 = createTestPaymentRecord();
        PaymentRecord record2 = createTestPaymentRecord();
        record2.setTransactionId(record1.getTransactionId()); // Same transaction ID

        repository.save(record1);

        // When/Then
        assertThatThrownBy(() -> repository.save(record2))
            .isInstanceOf(PaymentAlreadyExistsException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Should find payment by transaction ID")
    void shouldFindPaymentByTransactionId() {
        // Given
        PaymentRecord record = createTestPaymentRecord();
        repository.save(record);

        // When
        Optional<PaymentRecord> found = repository.findByTransactionId(record.getTransactionId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get()).isEqualTo(record);
    }

    @Test
    @DisplayName("Should return empty when payment not found")
    void shouldReturnEmptyWhenPaymentNotFound() {
        // When
        Optional<PaymentRecord> found = repository.findByTransactionId(UUID.randomUUID());

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should find payments by status")
    void shouldFindPaymentsByStatus() {
        // Given
        PaymentRecord pending1 = createTestPaymentRecord();
        PaymentRecord pending2 = createTestPaymentRecord();
        PaymentRecord approved = createTestPaymentRecord();
        approved.setStatus(PaymentStatus.APPROVED);

        repository.save(pending1);
        repository.save(pending2);
        repository.save(approved);

        // When
        List<PaymentRecord> pendingPayments = repository.findByStatus(PaymentStatus.PENDING_FRAUD_CHECK);
        List<PaymentRecord> approvedPayments = repository.findByStatus(PaymentStatus.APPROVED);

        // Then
        assertThat(pendingPayments).hasSize(2);
        assertThat(approvedPayments).hasSize(1);
        assertThat(pendingPayments).containsExactlyInAnyOrder(pending1, pending2);
        assertThat(approvedPayments).contains(approved);
    }

    @Test
    @DisplayName("Should find all payments sorted by submission time")
    void shouldFindAllPaymentsSortedBySubmissionTime() {
        // Given
        PaymentRecord older = createTestPaymentRecord();
        older.setSubmittedTimestamp(Instant.now().minusSeconds(3600)); // 1 hour ago

        PaymentRecord newer = createTestPaymentRecord();
        newer.setSubmittedTimestamp(Instant.now());

        repository.save(older);
        repository.save(newer);

        // When
        List<PaymentRecord> allPayments = repository.findAll();

        // Then
        assertThat(allPayments).hasSize(2);
        assertThat(allPayments.get(0)).isEqualTo(newer); // Should be first (most recent)
        assertThat(allPayments.get(1)).isEqualTo(older); // Should be second (oldest)
    }

    @Test
    @DisplayName("Should find pending payments older than timestamp")
    void shouldFindPendingPaymentsOlderThanTimestamp() {
        // Given
        Instant cutoffTime = Instant.now().minusSeconds(1800); // 30 minutes ago

        PaymentRecord oldPending = createTestPaymentRecord();
        oldPending.setSubmittedTimestamp(cutoffTime.minusSeconds(600)); // 40 minutes ago
        oldPending.setStatus(PaymentStatus.PENDING_FRAUD_CHECK);

        PaymentRecord newPending = createTestPaymentRecord();
        newPending.setSubmittedTimestamp(cutoffTime.plusSeconds(600)); // 20 minutes ago
        newPending.setStatus(PaymentStatus.PENDING_FRAUD_CHECK);

        PaymentRecord oldApproved = createTestPaymentRecord();
        oldApproved.setSubmittedTimestamp(cutoffTime.minusSeconds(600)); // 40 minutes ago
        oldApproved.setStatus(PaymentStatus.APPROVED);

        repository.save(oldPending);
        repository.save(newPending);
        repository.save(oldApproved);

        // When
        List<PaymentRecord> expiredPayments = repository.findPendingOlderThan(cutoffTime);

        // Then
        assertThat(expiredPayments).hasSize(1);
        assertThat(expiredPayments).contains(oldPending);
    }

    @Test
    @DisplayName("Should update payment record successfully")
    void shouldUpdatePaymentRecordSuccessfully() {
        // Given
        PaymentRecord record = createTestPaymentRecord();
        repository.save(record);

        record.updateStatus(PaymentStatus.APPROVED);

        // When
        PaymentRecord updated = repository.update(record);

        // Then
        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        

        Optional<PaymentRecord> retrieved = repository.findByTransactionId(record.getTransactionId());
        assertThat(retrieved.get().getStatus()).isEqualTo(PaymentStatus.APPROVED);
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent payment")
    void shouldThrowExceptionWhenUpdatingNonExistentPayment() {
        // Given
        PaymentRecord record = createTestPaymentRecord();

        // When/Then
        assertThatThrownBy(() -> repository.update(record))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("does not exist");
    }

    @Test
    @DisplayName("Should delete payment successfully")
    void shouldDeletePaymentSuccessfully() {
        // Given
        PaymentRecord record = createTestPaymentRecord();
        repository.save(record);

        // When
        boolean deleted = repository.deleteByTransactionId(record.getTransactionId());

        // Then
        assertThat(deleted).isTrue();
        assertThat(repository.count()).isEqualTo(0);
        assertThat(repository.existsByTransactionId(record.getTransactionId())).isFalse();
    }

    @Test
    @DisplayName("Should return false when deleting non-existent payment")
    void shouldReturnFalseWhenDeletingNonExistentPayment() {
        // When
        boolean deleted = repository.deleteByTransactionId(UUID.randomUUID());

        // Then
        assertThat(deleted).isFalse();
    }

    @Test
    @DisplayName("Should clear all payments")
    void shouldClearAllPayments() {
        // Given
        repository.save(createTestPaymentRecord());
        repository.save(createTestPaymentRecord());
        assertThat(repository.count()).isEqualTo(2);

        // When
        repository.clear();

        // Then
        assertThat(repository.count()).isEqualTo(0);
        assertThat(repository.findAll()).isEmpty();
    }

    private PaymentRecord createTestPaymentRecord() {
        PaymentRequest request = new PaymentRequest();
        request.setTransactionId(UUID.randomUUID());
        request.setPayerName("Test Payer");
        request.setPayerBank("Test Bank");
        request.setPayerCountryCode("USA");
        request.setPayerAccount("123456");
        request.setPayeeName("Test Payee");
        request.setPayeeBank("Test Payee Bank");
        request.setPayeeCountryCode("USA");
        request.setPayeeAccount("654321");
        request.setPaymentInstruction("Test payment");
        request.setExecutionDate(LocalDate.now());
        request.setAmount(BigDecimal.valueOf(100.00));
        request.setCurrency("USD");
        request.setCreationTimestamp(Instant.now());

        return PaymentRecord.fromRequest(request);
    }
}