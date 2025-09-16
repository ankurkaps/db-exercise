package com.demo.payment.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.demo.common.model.FraudCheckResponse;
import com.demo.common.model.FraudCheckStatus;
import com.demo.common.model.PaymentRequest;
import com.demo.payment.exception.PaymentAlreadyExistsException;
import com.demo.payment.model.PaymentRecord;
import com.demo.payment.model.PaymentStatus;
import com.demo.payment.repository.PaymentRepository;
import com.demo.payment.validation.PaymentValidator;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@NoArgsConstructor
@Slf4j
@Service
public class PaymentService {
    
    @Autowired
    private ProducerTemplate producerTemplate;

    @Autowired
    private PaymentValidator paymentValidator;

    @Autowired
    private PaymentRepository paymentRepository;

    @Value("${pps.broker.service.url:http://localhost:8082}")
    private String brokerServiceUrl;

    @Value("${payment.timeout.minutes:30}")
    private long timeoutMinutes;

    public PaymentService(PaymentRepository paymentRepository, PaymentValidator paymentValidator, ProducerTemplate producerTemplate) {
        this.paymentRepository = paymentRepository;
        this.paymentValidator = paymentValidator;
        this.producerTemplate = producerTemplate;
    }

    public PaymentRecord submitPayment(PaymentRequest request, boolean useRest) {
        log.info("Processing payment submission for transaction: {}", request.getTransactionId());

        // Validate the payment request
        paymentValidator.validate(request);

        try {
            return producerTemplate.requestBodyAndHeader("direct:processPaymentWithTracking", request, "useRest", true,  PaymentRecord.class);

        } catch (PaymentAlreadyExistsException e) {
            log.warn("Payment already exists for transaction: {}", request.getTransactionId());
            throw e;
        }
    }

    public FraudCheckResponse processPaymentJms(PaymentRequest request) {
        paymentValidator.validate(request);
        return producerTemplate.requestBody("direct:processPaymentJms", request, FraudCheckResponse.class);
    }

    public FraudCheckResponse processPaymentRest(PaymentRequest request) {
        paymentValidator.validate(request);
        return producerTemplate.requestBody("direct:processPaymentRest", request, FraudCheckResponse.class);
    }
    
    public PaymentRecord save(PaymentRequest request) {
        PaymentRecord paymentRecord = PaymentRecord.fromRequest(request);
        return paymentRepository.save(paymentRecord);
    }

    public PaymentRecord processPayment(FraudCheckResponse fraudResponse) {
        log.info("Process payment for transaction: {} with fraud check result: {}",
                fraudResponse.getTransactionId(), fraudResponse.getStatus());
        
        // Update payment status
        PaymentRecord paymentRecord = updatePaymentStatus(fraudResponse);
        
        // TODO Process payment
        

        return paymentRecord;
    }
    
    public PaymentRecord updatePaymentStatus(FraudCheckResponse fraudResponse) {
        log.info("Updating payment status for transaction: {} with fraud check result: {}",
                fraudResponse.getTransactionId(), fraudResponse.getStatus());

        Optional<PaymentRecord> paymentOpt = paymentRepository.findByTransactionId(fraudResponse.getTransactionId());

        if (paymentOpt.isEmpty()) {
            // TODO Custom Exception
            throw new RuntimeException("Payment record not found for transaction: " + fraudResponse.getTransactionId());
        }

        PaymentRecord payment = paymentOpt.get();

        // Map fraud check status to payment status
        PaymentStatus newStatus;

        if (fraudResponse.getStatus() == FraudCheckStatus.APPROVED) {
            newStatus = PaymentStatus.APPROVED;
        } else {
            newStatus = PaymentStatus.REJECTED;
        }

        payment.updateStatus(newStatus);
        paymentRepository.update(payment);

        log.info("Updated payment {} status to: {}", fraudResponse.getTransactionId(), newStatus);
        
        return payment;
    }    

    public Optional<PaymentRecord> getPaymentByTransactionId(UUID transactionId) {
        return paymentRepository.findByTransactionId(transactionId);
    }

    public List<PaymentRecord> getPaymentsByStatus(PaymentStatus status) {
        return paymentRepository.findByStatus(status);
    }

    public List<PaymentRecord> getAllPayments() {
        return paymentRepository.findAll();
    }

    public void expirePendingPayments() {
        Instant cutoffTime = Instant.now().minusSeconds(timeoutMinutes * 60);
        List<PaymentRecord> expiredPayments = paymentRepository.findPendingOlderThan(cutoffTime);

        log.info("Found {} payments to expire (older than {} minutes)", expiredPayments.size(), timeoutMinutes);

        for (PaymentRecord payment : expiredPayments) {
            payment.updateStatus(PaymentStatus.EXPIRED);
            paymentRepository.update(payment);
            log.info("Expired payment: {}", payment.getTransactionId());
        }
    }

    public void markPaymentFailed(UUID transactionId, String reason) {
        Optional<PaymentRecord> paymentOpt = paymentRepository.findByTransactionId(transactionId);

        if (paymentOpt.isPresent()) {
            PaymentRecord payment = paymentOpt.get();
            payment.updateStatus(PaymentStatus.FAILED);
            paymentRepository.update(payment);
            log.error("Marked payment {} as FAILED: {}", transactionId, reason);
        }
    }

    public long getPaymentCount() {
        return paymentRepository.count();
    }

    public boolean paymentExists(UUID transactionId) {
        return paymentRepository.existsByTransactionId(transactionId);
    }
}
