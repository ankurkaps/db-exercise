package com.demo.payment.controller;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.demo.common.model.PaymentRequest;
import com.demo.payment.exception.PaymentNotFoundException;
import com.demo.payment.model.PaymentRecord;
import com.demo.payment.model.PaymentStatus;
import com.demo.payment.service.PaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Payment Processing", description = "Payment processing and status management API")
public class PaymentControllerV1 {

    @Autowired
    private PaymentService paymentService;
    
    @Autowired
    private ProducerTemplate producerTemplate;

    @PostMapping("/payments")
    @Operation(summary = "Submit payment for processing using REST",
               description = "Submits a payment request for validation and fraud checking")
    public ResponseEntity<PaymentRecord> submitPaymentRest(@Valid @RequestBody PaymentRequest request) {
        return submitPayment(request, true);
    }

    @PostMapping("/payments/jms")
    @Operation(summary = "Process payment via JMS",
               description = "Submits a payment request for validation and fraud checking")
    public ResponseEntity<PaymentRecord> processPaymentJms(@Valid @RequestBody PaymentRequest request) {
        return submitPayment(request, false);
    }
    
    private ResponseEntity<PaymentRecord> submitPayment(@Valid @RequestBody PaymentRequest request, boolean useRest) {
        PaymentRecord response = producerTemplate.requestBodyAndHeader("direct:processPaymentWithTracking", request, "useRest", useRest, PaymentRecord.class);
        return ResponseEntity.accepted().body(response);
    }

    @GetMapping("/payments/{transactionId}")
    @Operation(summary = "Get payment by transaction ID",
               description = "Retrieves payment details and current status by transaction ID")
    public ResponseEntity<PaymentRecord> getPaymentByTransactionId(
            @Parameter(description = "Payment transaction ID")
            @PathVariable UUID transactionId) {

        Optional<PaymentRecord> payment = paymentService.getPaymentByTransactionId(transactionId);

        if (payment.isPresent()) {
            return ResponseEntity.ok(payment.get());
        } else {
            throw new PaymentNotFoundException("Payment with transaction ID " + transactionId + " not found");
        }
    }

    @GetMapping("/payments")
    @Operation(summary = "Get all payments",
               description = "Retrieves all payments with optional status filtering")
    public ResponseEntity<List<PaymentRecord>> getAllPayments(
            @Parameter(description = "Filter by payment status")
            @RequestParam(required = false) PaymentStatus status) {

        List<PaymentRecord> payments;

        if (status != null) {
            payments = paymentService.getPaymentsByStatus(status);
        } else {
            payments = paymentService.getAllPayments();
        }

        return ResponseEntity.ok(payments);
    }

}
