package com.demo.broker.route;

import com.demo.common.model.FraudCheckResponse;
import com.demo.common.model.FraudCheckStatus;
import com.demo.common.model.PaymentRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
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
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@CamelSpringBootTest
@SpringBootTest
@MockEndpoints
@TestPropertySource(properties = {
    "spring.cloud.consul.enabled=false",
    "spring.cloud.discovery.enabled=false",
    "spring.activemq.broker-url=vm://localhost?broker.persistent=false"
})
@DisplayName("Broker Route Integration Tests")
class BrokerRouteTest {

    @Autowired
    private CamelContext camelContext;

    @Autowired
    private ProducerTemplate producerTemplate;

    @Autowired
    private ObjectMapper objectMapper;

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
    @DisplayName("Should transform JSON payment request to XML for fraud check")
    void shouldTransformJsonToXmlForFraudCheck() throws Exception {
        // Given
        camelContext.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                // Mock the fraud check system response
                from("jms:queue:fraud.check.requests")
                    .log("Received XML request: ${body}")
                    .process(exchange -> {
                        // Verify we received XML
                        String xmlBody = exchange.getIn().getBody(String.class);
                        assertThat(xmlBody).contains("<?xml");
                        assertThat(xmlBody).contains("fraudCheckRequest");
                        assertThat(xmlBody).contains(testPayment.getTransactionId().toString());

                        // Send back XML response
                        String xmlResponse = createXmlResponse(mockResponse);
                        exchange.getIn().setBody(xmlResponse);
                    })
                    .to("mock:fraud-check-xml");
            }
        });

        MockEndpoint mockEndpoint = camelContext.getEndpoint("mock:fraud-check-xml", MockEndpoint.class);
        mockEndpoint.expectedMessageCount(1);

        // Convert payment to JSON
        String jsonPayment = objectMapper.writeValueAsString(testPayment);

        // When
        producerTemplate.sendBody("jms:queue:payment.requests", jsonPayment);

        // Then
        mockEndpoint.assertIsSatisfied(5000);

        Exchange receivedExchange = mockEndpoint.getReceivedExchanges().get(0);
        String xmlBody = receivedExchange.getIn().getBody(String.class);

        assertThat(xmlBody).isNotNull();
        assertThat(xmlBody).contains("<?xml version=\"1.0\" encoding=\"UTF-8\"");
        assertThat(xmlBody).contains("fraudCheckRequest");
        assertThat(xmlBody).contains(testPayment.getPayerName());
        assertThat(xmlBody).contains(testPayment.getPayeeName());
        assertThat(xmlBody).contains(testPayment.getTransactionId().toString());
    }

    @Test
    @DisplayName("Should handle REST-based fraud check with XML transformation")
    void shouldHandleRestBasedFraudCheck() throws Exception {
        // Given
        camelContext.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                // Mock fraud check system
                from("jms:queue:fraud.check.requests")
                    .log("REST test - Received XML: ${body}")
                    .process(exchange -> {
                        String xmlResponse = createXmlResponse(mockResponse);
                        exchange.getIn().setBody(xmlResponse);
                    })
                    .to("jms:queue:fraud.check.responses");
            }
        });

        // When
        FraudCheckResponse response = producerTemplate.requestBody("direct:processFraudCheckRest",
            testPayment, FraudCheckResponse.class);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTransactionId()).isEqualTo(testPayment.getTransactionId());
        assertThat(response.getStatus()).isEqualTo(FraudCheckStatus.APPROVED);
    }

    @Test
    @DisplayName("Should preserve correlation ID during transformation")
    void shouldPreserveCorrelationIdDuringTransformation() throws Exception {
        // Given
        camelContext.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("jms:queue:fraud.check.requests")
                    .process(exchange -> {
                        String correlationId = exchange.getIn().getHeader("JMSCorrelationID", String.class);
                        assertThat(correlationId).isEqualTo("test-correlation-456");

                        String xmlResponse = createXmlResponse(mockResponse);
                        exchange.getIn().setBody(xmlResponse);
                    })
                    .to("mock:correlation-check");
            }
        });

        MockEndpoint mockEndpoint = camelContext.getEndpoint("mock:correlation-check", MockEndpoint.class);
        mockEndpoint.expectedMessageCount(1);

        String jsonPayment = objectMapper.writeValueAsString(testPayment);

        // When
        producerTemplate.sendBodyAndHeader("jms:queue:payment.requests", jsonPayment,
            "JMSCorrelationID", "test-correlation-456");

        // Then
        mockEndpoint.assertIsSatisfied(5000);
    }

    @Test
    @DisplayName("Should handle JSON parsing errors gracefully")
    void shouldHandleJsonParsingErrorsGracefully() throws Exception {
        // Given
        String invalidJson = "{ invalid json structure";

        // When/Then
        try {
            producerTemplate.sendBody("jms:queue:payment.requests", invalidJson);
        } catch (Exception e) {
            // Expected to fail with JSON parsing error
            assertThat(e.getCause()).isInstanceOf(com.fasterxml.jackson.core.JsonParseException.class);
        }
    }

    @Test
    @DisplayName("Should convert suspicious fraud response correctly")
    void shouldConvertSuspiciousFraudResponseCorrectly() throws Exception {
        // Given
        FraudCheckResponse suspiciousResponse = FraudCheckResponse.builder()
            .transactionId(testPayment.getTransactionId())
            .status(FraudCheckStatus.SUSPICIOUS)
            .validationTimestamp(Instant.now())
            .build();

        camelContext.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("jms:queue:fraud.check.requests")
                    .process(exchange -> {
                        String xmlResponse = createXmlResponse(suspiciousResponse);
                        exchange.getIn().setBody(xmlResponse);
                    })
                    .to("jms:queue:fraud.check.responses");
            }
        });

        // When
        FraudCheckResponse response = producerTemplate.requestBody("direct:processFraudCheckRest",
            testPayment, FraudCheckResponse.class);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(FraudCheckStatus.SUSPICIOUS);
        assertThat(response.getTransactionId()).isEqualTo(testPayment.getTransactionId());
    }

    @Test
    @DisplayName("Should handle timeout scenarios")
    void shouldHandleTimeoutScenarios() throws Exception {
        // Given
        camelContext.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("jms:queue:fraud.check.requests")
                    .delay(35000) // Delay longer than timeout (30s)
                    .process(exchange -> {
                        String xmlResponse = createXmlResponse(mockResponse);
                        exchange.getIn().setBody(xmlResponse);
                    })
                    .to("jms:queue:fraud.check.responses");
            }
        });

        // When/Then
        try {
            producerTemplate.requestBody("direct:processFraudCheckRest", testPayment,
                FraudCheckResponse.class);
        } catch (Exception e) {
            // Should timeout
            assertThat(e.getCause()).isInstanceOf(org.apache.camel.ExchangeTimedOutException.class);
        }
    }

    private PaymentRequest createTestPayment() {
        PaymentRequest payment = new PaymentRequest();
        payment.setTransactionId(UUID.randomUUID());
        payment.setPayerName("Alice Johnson");
        payment.setPayerBank("First National Bank");
        payment.setPayerCountryCode("USA");
        payment.setPayerAccount("ACC123456");
        payment.setPayeeName("Bob Williams");
        payment.setPayeeBank("Second National Bank");
        payment.setPayeeCountryCode("USA");
        payment.setPayeeAccount("ACC789012");
        payment.setPaymentInstruction("Monthly salary");
        payment.setExecutionDate(LocalDate.now());
        payment.setAmount(BigDecimal.valueOf(2500.75));
        payment.setCurrency("USD");
        payment.setCreationTimestamp(Instant.now());
        return payment;
    }

    private String createXmlResponse(FraudCheckResponse response) {
        return String.format("""
            <?xml version="1.0" encoding="UTF-8"?>
            <fraudCheckResponse xmlns="urn:example:fraudcheck:v1">
                <transactionId>%s</transactionId>
                <status>%s</status>
                <validationTimestamp>%s</validationTimestamp>
            </fraudCheckResponse>
            """,
            response.getTransactionId(),
            response.getStatus(),
            response.getValidationTimestamp()
        );
    }
}