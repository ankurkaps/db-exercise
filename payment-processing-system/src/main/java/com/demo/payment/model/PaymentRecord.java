package com.demo.payment.model;

import com.demo.common.model.PaymentRequest;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRecord {
    private UUID transactionId;
    private PaymentRequest paymentRequest;
    private PaymentStatus status;
    @JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssX", timezone = "UTC")
    private Instant submittedTimestamp;
    @JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssX", timezone = "UTC")
    private Instant lastUpdatedTimestamp;

    public static PaymentRecord fromRequest(PaymentRequest request) {
        Instant now = Instant.now();
        return PaymentRecord.builder()
            .transactionId(request.getTransactionId())
            .paymentRequest(request)
            .status(PaymentStatus.PENDING_FRAUD_CHECK)
            .submittedTimestamp(now)
            .lastUpdatedTimestamp(now)
            .build();
    }

    public void updateStatus(PaymentStatus newStatus) {
        this.status = newStatus;
        this.lastUpdatedTimestamp = Instant.now();
    }
}