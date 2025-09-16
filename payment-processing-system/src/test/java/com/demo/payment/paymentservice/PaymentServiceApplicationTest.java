package com.demo.payment.paymentservice;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.cloud.consul.enabled=false",
    "spring.cloud.discovery.enabled=false"
})
@DisplayName("Payment Service Application Tests")
class PaymentServiceApplicationTest {

    @Test
    @DisplayName("Application context loads successfully")
    void contextLoads() {
        // Test that the Spring Boot application context loads without errors
    }
}