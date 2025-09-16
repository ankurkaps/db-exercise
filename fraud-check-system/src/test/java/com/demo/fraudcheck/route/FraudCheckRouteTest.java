package com.demo.fraudcheck.route;

import com.demo.common.model.FraudCheckResponse;
import com.demo.common.model.FraudCheckStatus;
import com.demo.common.model.PaymentRequest;
import com.demo.fraudcheck.service.FraudCheckService;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.test.spring.junit5.MockEndpoints;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@CamelSpringBootTest
@SpringBootTest
@MockEndpoints
@TestPropertySource(properties = {
    "spring.cloud.consul.enabled=false",
    "spring.cloud.discovery.enabled=false",
    "spring.activemq.broker-url=vm://localhost?broker.persistent=false"
})
@DisplayName("Fraud Check Route Integration Tests")
class FraudCheckRouteTest {

    @Autowired
    private CamelContext camelContext;

    @Autowired
    private ProducerTemplate producerTemplate;

    @MockBean
    private FraudCheckService fraudCheckService;

    private PaymentRequest testPayment;
    private FraudCheckResponse mockResponse;

    @BeforeEach
    void setUp() {
        testPayment = createTestPayment();
        mockResponse = FraudCheckResponse.builder()
            .transactionId(testPayment.getTransactionId())
            .status(FraudCheckStatus.APPROVED)
            .validationTimestamp(Instant.now())
            .build();
    }

    @Test
    @DisplayName("Should process fraud check request successfully")
    void shouldProcessFraudCheckRequestSuccessfully() throws Exception {
        // Given
        when(fraudCheckService.checkFraud(any(PaymentRequest.class))).thenReturn(mockResponse);

        // Create test route to capture the response
        camelContext.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("jms:queue:fraud.check.responses")
                    .to("mock:fraud-check-response");
            }
        });

        MockEndpoint mockEndpoint = camelContext.getEndpoint("mock:fraud-check-response", MockEndpoint.class);
        mockEndpoint.expectedMessageCount(1);

        // Create XML representation of the payment request
        String xmlPayment = convertToXml(testPayment);

        // When
        producerTemplate.sendBody("jms:queue:fraud.check.requests", xmlPayment);

        // Then
        mockEndpoint.assertIsSatisfied(5000); // Wait up to 5 seconds

        String responseBody = mockEndpoint.getReceivedExchanges().get(0).getIn().getBody(String.class);
        assertThat(responseBody).isNotNull();
        assertThat(responseBody).contains("APPROVED");
        assertThat(responseBody).contains(testPayment.getTransactionId().toString());
    }

    @Test
    @DisplayName("Should handle suspicious payment correctly")
    void shouldHandleSuspiciousPaymentCorrectly() throws Exception {
        // Given
        FraudCheckResponse suspiciousResponse = FraudCheckResponse.builder()
            .transactionId(testPayment.getTransactionId())
            .status(FraudCheckStatus.SUSPICIOUS)
            .validationTimestamp(Instant.now())
            .build();

        when(fraudCheckService.checkFraud(any(PaymentRequest.class))).thenReturn(suspiciousResponse);

        // Create test route to capture the response
        camelContext.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("jms:queue:fraud.check.responses")
                    .to("mock:suspicious-response");
            }
        });

        MockEndpoint mockEndpoint = camelContext.getEndpoint("mock:suspicious-response", MockEndpoint.class);
        mockEndpoint.expectedMessageCount(1);

        // Create XML representation of the payment request
        String xmlPayment = convertToXml(testPayment);

        // When
        producerTemplate.sendBody("jms:queue:fraud.check.requests", xmlPayment);

        // Then
        mockEndpoint.assertIsSatisfied(5000);

        String responseBody = mockEndpoint.getReceivedExchanges().get(0).getIn().getBody(String.class);
        assertThat(responseBody).isNotNull();
        assertThat(responseBody).contains("SUSPICIOUS");
        assertThat(responseBody).contains(testPayment.getTransactionId().toString());
    }

    @Test
    @DisplayName("Should preserve correlation ID in response")
    void shouldPreserveCorrelationIdInResponse() throws Exception {
        // Given
        when(fraudCheckService.checkFraud(any(PaymentRequest.class))).thenReturn(mockResponse);

        camelContext.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("jms:queue:fraud.check.responses")
                    .to("mock:correlation-test");
            }
        });

        MockEndpoint mockEndpoint = camelContext.getEndpoint("mock:correlation-test", MockEndpoint.class);
        mockEndpoint.expectedMessageCount(1);

        String correlationId = "test-correlation-123";
        String xmlPayment = convertToXml(testPayment);

        // When
        producerTemplate.sendBodyAndHeader("jms:queue:fraud.check.requests", xmlPayment,
            "JMSCorrelationID", correlationId);

        // Then
        mockEndpoint.assertIsSatisfied(5000);

        String receivedCorrelationId = mockEndpoint.getReceivedExchanges().get(0)
            .getIn().getHeader("JMSCorrelationID", String.class);
        assertThat(receivedCorrelationId).isEqualTo(correlationId);
    }

    private PaymentRequest createTestPayment() {
        PaymentRequest payment = new PaymentRequest();
        payment.setTransactionId(UUID.randomUUID());
        payment.setPayerName("John Doe");
        payment.setPayerBank("Test Bank");
        payment.setPayerCountryCode("USA");
        payment.setPayerAccount("123456789");
        payment.setPayeeName("Jane Smith");
        payment.setPayeeBank("Recipient Bank");
        payment.setPayeeCountryCode("USA");
        payment.setPayeeAccount("987654321");
        payment.setPaymentInstruction("Test payment");
        payment.setExecutionDate(LocalDate.now());
        payment.setAmount(BigDecimal.valueOf(100.00));
        payment.setCurrency("USD");
        payment.setCreationTimestamp(Instant.now());
        return payment;
    }

    private String convertToXml(PaymentRequest payment) {
        // Simple XML representation for testing
        return String.format("""
            <?xml version="1.0" encoding="UTF-8"?>
            <fraudCheckRequest xmlns="urn:example:fraudcheck:v1">
                <transactionId>%s</transactionId>
                <payerName>%s</payerName>
                <payerBank>%s</payerBank>
                <payerCountryCode>%s</payerCountryCode>
                <payerAccount>%s</payerAccount>
                <payeeName>%s</payeeName>
                <payeeBank>%s</payeeBank>
                <payeeCountryCode>%s</payeeCountryCode>
                <payeeAccount>%s</payeeAccount>
                <paymentInstruction>%s</paymentInstruction>
                <executionDate>%s</executionDate>
                <amount>%s</amount>
                <currency>%s</currency>
                <creationTimestamp>%s</creationTimestamp>
            </fraudCheckRequest>
            """,
            payment.getTransactionId(),
            payment.getPayerName(),
            payment.getPayerBank(),
            payment.getPayerCountryCode(),
            payment.getPayerAccount(),
            payment.getPayeeName(),
            payment.getPayeeBank(),
            payment.getPayeeCountryCode(),
            payment.getPayeeAccount(),
            payment.getPaymentInstruction(),
            payment.getExecutionDate(),
            payment.getAmount(),
            payment.getCurrency(),
            payment.getCreationTimestamp()
        );
    }
}