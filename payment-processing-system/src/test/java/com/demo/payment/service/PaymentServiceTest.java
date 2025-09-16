package com.demo.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.camel.ProducerTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.demo.common.model.FraudCheckResponse;
import com.demo.common.model.FraudCheckStatus;
import com.demo.common.model.PaymentRequest;
import com.demo.payment.exception.PaymentAlreadyExistsException;
import com.demo.payment.model.PaymentRecord;
import com.demo.payment.model.PaymentStatus;
import com.demo.payment.repository.PaymentRepository;
import com.demo.payment.validation.PaymentValidator;

@ExtendWith(MockitoExtension.class)
@DisplayName("Payment Service Tests")
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentValidator paymentValidator;

    @Mock
    private ProducerTemplate producerTemplate;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentRepository, paymentValidator, producerTemplate);
    }

    @Test
    @DisplayName("Should submit payment successfully")
    void shouldSubmitPaymentSuccessfully() {
        // Given
        PaymentRequest request = createTestPaymentRequest();
        PaymentRecord expectedResponse = new PaymentRecord().builder().transactionId(request.getTransactionId()).build();

        when(paymentRepository.save(any(PaymentRecord.class))).thenReturn(PaymentRecord.fromRequest(request));
        when(producerTemplate.requestBody(eq("direct:processPaymentWithTracking"), eq(request), eq(PaymentRecord.class)))
            .thenReturn(expectedResponse);

        // When
        PaymentRecord response = paymentService.save(request);

        // Then
        assertThat(response).isEqualTo(expectedResponse);
        verify(paymentValidator).validate(request);
        verify(paymentRepository).save(any(PaymentRecord.class));
        verify(producerTemplate).requestBody(eq("direct:processPaymentWithTracking"), eq(request), eq(PaymentRecord.class));
    }

    @Test
    @DisplayName("Should throw exception when payment already exists")
    void shouldThrowExceptionWhenPaymentAlreadyExists() {
        // Given
        PaymentRequest request = createTestPaymentRequest();
        when(paymentRepository.save(any(PaymentRecord.class)))
            .thenThrow(new PaymentAlreadyExistsException("Payment already exists"));

        // When/Then
        assertThatThrownBy(() -> paymentService.save(request))
            .isInstanceOf(PaymentAlreadyExistsException.class);

        verify(paymentValidator).validate(request);
        verify(paymentRepository).save(any(PaymentRecord.class));
        verify(producerTemplate, never()).requestBody(any(String.class), any(), any(Class.class));
    }

    @Test
    @DisplayName("Should update payment status to approved")
    void shouldUpdatePaymentStatusToApproved() {
        // Given
        UUID transactionId = UUID.randomUUID();
        PaymentRecord paymentRecord = createTestPaymentRecord(transactionId);
        FraudCheckResponse fraudResponse = FraudCheckResponse.builder()
            .transactionId(transactionId)
            .status(FraudCheckStatus.APPROVED)
            .validationTimestamp(Instant.now())
            .build();

        when(paymentRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(paymentRecord));
        when(paymentRepository.update(any(PaymentRecord.class))).thenReturn(paymentRecord);

        // When
        paymentService.updatePaymentStatus(fraudResponse);

        // Then
        verify(paymentRepository).findByTransactionId(transactionId);
        verify(paymentRepository).update(argThat(record ->
            record.getStatus() == PaymentStatus.APPROVED
        ));
    }

    @Test
    @DisplayName("Should update payment status to rejected")
    void shouldUpdatePaymentStatusToRejected() {
        // Given
        UUID transactionId = UUID.randomUUID();
        PaymentRecord paymentRecord = createTestPaymentRecord(transactionId);
        FraudCheckResponse fraudResponse = FraudCheckResponse.builder()
            .transactionId(transactionId)
            .status(FraudCheckStatus.SUSPICIOUS)
            .validationTimestamp(Instant.now())
            .build();

        when(paymentRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(paymentRecord));
        when(paymentRepository.update(any(PaymentRecord.class))).thenReturn(paymentRecord);

        // When
        paymentService.updatePaymentStatus(fraudResponse);

        // Then
        verify(paymentRepository).update(argThat(record ->
            record.getStatus() == PaymentStatus.REJECTED
        ));
    }

    @Test
    @DisplayName("Should fail on missing payment record")
    void shouldFailOnMissingPaymentRecord() {
        // Given
        UUID transactionId = UUID.randomUUID();
        FraudCheckResponse fraudResponse = FraudCheckResponse.builder()
            .transactionId(transactionId)
            .status(FraudCheckStatus.APPROVED)
            .build();

        when(paymentRepository.findByTransactionId(transactionId)).thenReturn(Optional.empty());

        // When
        paymentService.updatePaymentStatus(fraudResponse);

        // Then
        verify(paymentRepository).findByTransactionId(transactionId);
        verify(paymentRepository, never()).update(any(PaymentRecord.class));
    }

    @Test
    @DisplayName("Should expire pending payments")
    void shouldExpirePendingPayments() {
        // Given
        PaymentRecord oldPayment1 = createTestPaymentRecord(UUID.randomUUID());
        PaymentRecord oldPayment2 = createTestPaymentRecord(UUID.randomUUID());
        List<PaymentRecord> expiredPayments = Arrays.asList(oldPayment1, oldPayment2);

        when(paymentRepository.findPendingOlderThan(any(Instant.class))).thenReturn(expiredPayments);
        when(paymentRepository.update(any(PaymentRecord.class))).thenReturn(oldPayment1, oldPayment2);

        // When
        paymentService.expirePendingPayments();

        // Then
        verify(paymentRepository).findPendingOlderThan(any(Instant.class));
        verify(paymentRepository, times(2)).update(argThat(record ->
            record.getStatus() == PaymentStatus.EXPIRED 
        ));
    }

    @Test
    @DisplayName("Should mark payment as failed")
    void shouldMarkPaymentAsFailed() {
        // Given
        UUID transactionId = UUID.randomUUID();
        PaymentRecord paymentRecord = createTestPaymentRecord(transactionId);
        String failureReason = "Network timeout";

        when(paymentRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(paymentRecord));
        when(paymentRepository.update(any(PaymentRecord.class))).thenReturn(paymentRecord);

        // When
        paymentService.markPaymentFailed(transactionId, failureReason);

        // Then
        verify(paymentRepository).update(argThat(record ->
            record.getStatus() == PaymentStatus.FAILED
        ));
    }

    @Test
    @DisplayName("Should get payment by transaction ID")
    void shouldGetPaymentByTransactionId() {
        // Given
        UUID transactionId = UUID.randomUUID();
        PaymentRecord expectedRecord = createTestPaymentRecord(transactionId);
        when(paymentRepository.findByTransactionId(transactionId)).thenReturn(Optional.of(expectedRecord));

        // When
        Optional<PaymentRecord> result = paymentService.getPaymentByTransactionId(transactionId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(expectedRecord);
        verify(paymentRepository).findByTransactionId(transactionId);
    }

    @Test
    @DisplayName("Should get payments by status")
    void shouldGetPaymentsByStatus() {
        // Given
        PaymentStatus status = PaymentStatus.APPROVED;
        List<PaymentRecord> expectedRecords = Arrays.asList(
            createTestPaymentRecord(UUID.randomUUID()),
            createTestPaymentRecord(UUID.randomUUID())
        );
        when(paymentRepository.findByStatus(status)).thenReturn(expectedRecords);

        // When
        List<PaymentRecord> result = paymentService.getPaymentsByStatus(status);

        // Then
        assertThat(result).isEqualTo(expectedRecords);
        verify(paymentRepository).findByStatus(status);
    }

    @Test
    @DisplayName("Should get all payments")
    void shouldGetAllPayments() {
        // Given
        List<PaymentRecord> expectedRecords = Arrays.asList(
            createTestPaymentRecord(UUID.randomUUID()),
            createTestPaymentRecord(UUID.randomUUID()),
            createTestPaymentRecord(UUID.randomUUID())
        );
        when(paymentRepository.findAll()).thenReturn(expectedRecords);

        // When
        List<PaymentRecord> result = paymentService.getAllPayments();

        // Then
        assertThat(result).isEqualTo(expectedRecords);
        verify(paymentRepository).findAll();
    }

    @Test
    @DisplayName("Should check if payment exists")
    void shouldCheckIfPaymentExists() {
        // Given
        UUID transactionId = UUID.randomUUID();
        when(paymentRepository.existsByTransactionId(transactionId)).thenReturn(true);

        // When
        boolean exists = paymentService.paymentExists(transactionId);

        // Then
        assertThat(exists).isTrue();
        verify(paymentRepository).existsByTransactionId(transactionId);
    }

    @Test
    @DisplayName("Should get payment count")
    void shouldGetPaymentCount() {
        // Given
        long expectedCount = 42L;
        when(paymentRepository.count()).thenReturn(expectedCount);

        // When
        long count = paymentService.getPaymentCount();

        // Then
        assertThat(count).isEqualTo(expectedCount);
        verify(paymentRepository).count();
    }

    private PaymentRequest createTestPaymentRequest() {
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
        return request;
    }

    private PaymentRecord createTestPaymentRecord(UUID transactionId) {
        PaymentRequest request = createTestPaymentRequest();
        request.setTransactionId(transactionId);
        return PaymentRecord.fromRequest(request);
    }
}