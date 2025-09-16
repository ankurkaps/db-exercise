package com.demo.fraudcheck.service;

import java.time.Instant;
import java.util.Set;

import com.demo.common.model.FraudCheckResponse;
import com.demo.common.model.FraudCheckStatus;
import com.demo.common.model.PaymentRequest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@org.springframework.stereotype.Service
public class FraudCheckService {

    // Blacklist data from the requirements. Using Sets for efficient lookup.
    private static final Set<String> BLACKLISTED_NAMES = Set.of("Mark Imaginary", "Govind Real", "Shakil Maybe", "Chang Imagine");
    private static final Set<String> BLACKLISTED_COUNTRIES = Set.of("CUB","IRQ","IRN","PRK","SDN","SYR");
    private static final Set<String> BLACKLISTED_BANKS = Set.of("BANK OF KUNLUN", "KARAMAY CITY COMMERCIAL BANK");
    private static final Set<String> BLACKLISTED_INSTRUCTIONS = Set.of("Artillery Procurement", "Lethal Chemicals payment");

    public FraudCheckResponse checkFraud(PaymentRequest payment) {
        log.info("Checking payment: {}", payment);

        // Perform fraud checks based on the blacklists.
        var status = FraudCheckStatus.APPROVED;

        if (
            BLACKLISTED_NAMES.contains(payment.getPayeeName()) || 
            BLACKLISTED_NAMES.contains(payment.getPayerName()) ||
            BLACKLISTED_COUNTRIES.contains(payment.getPayeeCountryCode()) || 
            BLACKLISTED_COUNTRIES.contains(payment.getPayerCountryCode()) ||
            BLACKLISTED_BANKS.contains(payment.getPayeeBank()) ||  
            BLACKLISTED_BANKS.contains(payment.getPayerBank()) ||
            (payment.getPaymentInstruction() != null && BLACKLISTED_INSTRUCTIONS.contains(payment.getPaymentInstruction().strip()))
        ){
            status = FraudCheckStatus.SUSPICIOUS;
        }
        
        var response = FraudCheckResponse.builder()
                .transactionId(payment.getTransactionId())
                .status(status)
                .validationTimestamp(Instant.now())
                .build();
        
        log.info("Fraud check result for {}:{}", payment.getTransactionId(), status);
        return response;
    }
}
