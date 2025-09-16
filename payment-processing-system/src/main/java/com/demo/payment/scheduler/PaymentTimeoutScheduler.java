package com.demo.payment.scheduler;

import com.demo.payment.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(
    value = "payment.scheduler.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class PaymentTimeoutScheduler {

    @Autowired
    private PaymentService paymentService;

    /**
     * Runs every 5 minutes to check for expired pending payments
     */
    @Scheduled(fixedRate = 300000) // 5 minutes in milliseconds
    public void expirePendingPayments() {
        log.debug("Running scheduled payment timeout check");

        try {
            paymentService.expirePendingPayments();
            log.debug("Completed payment timeout check");
        } catch (Exception e) {
            log.error("Error during payment timeout check", e);
        }
    }

    /**
     * Runs every hour to log payment statistics
     */
    @Scheduled(fixedRate = 3600000) // 1 hour in milliseconds
    public void logPaymentStatistics() {
        try {
            long totalPayments = paymentService.getPaymentCount();
            log.info("Payment Statistics - Total payments in system: {}", totalPayments);

            // Log additional statistics
            log.info("Active payment monitoring and timeout checking is enabled");
        } catch (Exception e) {
            log.error("Error logging payment statistics", e);
        }
    }
}