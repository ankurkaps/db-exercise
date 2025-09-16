package com.demo.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import com.demo.common.model.PaymentRequest;
import com.demo.common.model.FraudCheckResponse;
import com.demo.common.model.FraudCheckStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.util.DefaultXmlPrettyPrinter;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.extern.slf4j.Slf4j;
@Slf4j
public class ModelSerializationTest {

    private ObjectMapper objectMapper;
    private XmlMapper xmlMapper;

    @BeforeEach
    void setupObjectMapper() {
        // Build ObjectMapper the same way Spring Boot does (registers jsr310, other modules)
        this.objectMapper = Jackson2ObjectMapperBuilder
            .json()
            .findModulesViaServiceLoader(true)
            .indentOutput(true)
            .build();

        // XmlMapper setup (register JavaTimeModule for proper Instant/LocalDate serialization)
        this.xmlMapper = new XmlMapper();
        xmlMapper.registerModule(new JavaTimeModule());
    }

    private PaymentRequest createSamplePaymentRequest() {
        return PaymentRequest.builder()
            .transactionId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"))
            .payerName("John Doe")
            .payerBank("Chase Bank")
            .payerCountryCode("USA")
            .payerAccount("12345678901234")
            .payeeName("Jane Smith")
            .payeeBank("Wells Fargo")
            .payeeCountryCode("USA")
            .payeeAccount("87654321098765")
            .paymentInstruction("Monthly rent payment")
            .executionDate(LocalDate.of(2025, 9, 15))
            .amount(new BigDecimal("1500.75"))
            .currency("USD")
            .creationTimestamp(Instant.parse("2025-09-15T14:47:19Z"))
            .build();
    }

    private FraudCheckResponse createSampleFraudCheckResponse() {
        return FraudCheckResponse.builder()
            .transactionId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"))
            .status(FraudCheckStatus.APPROVED)
            .validationTimestamp(Instant.parse("2025-09-15T14:47:20Z"))
            .build();
    }
    @Nested
    @DisplayName("JSON Serialization Tests")
    class JsonSerializationTests {

        @Test
        @DisplayName("PaymentRequest: Object to JSON to Object")
        void testPaymentRequestJsonRoundTrip() throws JsonProcessingException {
            PaymentRequest original = createSamplePaymentRequest();

            // Object -> JSON
            String json = objectMapper.writeValueAsString(original);
            log.info("PaymentRequest JSON: {}", json);

            // JSON -> Object
            PaymentRequest deserialized = objectMapper.readValue(json, PaymentRequest.class);

            // Verify round-trip
            assertThat(deserialized).isEqualTo(original);
            assertThat(deserialized.getTransactionId()).isEqualTo(original.getTransactionId());
            assertThat(deserialized.getPayerName()).isEqualTo(original.getPayerName());
            assertThat(deserialized.getExecutionDate()).isEqualTo(original.getExecutionDate());
            assertThat(deserialized.getCreationTimestamp()).isEqualTo(original.getCreationTimestamp());
            assertThat(deserialized.getAmount()).isEqualTo(original.getAmount());

            // Verify JSON format annotations are working
            JsonNode jsonNode = objectMapper.readTree(json);
            assertThat(jsonNode.get("transactionId").isTextual()).isTrue();
            assertThat(jsonNode.get("amount").isTextual()).isTrue();
            assertThat(jsonNode.get("executionDate").isTextual()).isTrue();
            assertThat(jsonNode.get("creationTimestamp").isTextual()).isTrue();
        }

        @Test
        @DisplayName("FraudCheckResponse: Object to JSON to Object")
        void testFraudCheckResponseJsonRoundTrip() throws JsonProcessingException {
            FraudCheckResponse original = createSampleFraudCheckResponse();

            // Object -> JSON
            String json = objectMapper.writeValueAsString(original);
            log.info("FraudCheckResponse JSON: {}", json);

            // JSON -> Object
            FraudCheckResponse deserialized = objectMapper.readValue(json, FraudCheckResponse.class);

            // Verify round-trip
            assertThat(deserialized).isEqualTo(original);
            assertThat(deserialized.getTransactionId()).isEqualTo(original.getTransactionId());
            assertThat(deserialized.getStatus()).isEqualTo(original.getStatus());
            assertThat(deserialized.getValidationTimestamp()).isEqualTo(original.getValidationTimestamp());

            // Verify enum serialization
            JsonNode jsonNode = objectMapper.readTree(json);
            assertThat(jsonNode.get("status").textValue()).isEqualTo("APPROVED");
        }

        @Test
        @DisplayName("PaymentRequest: JSON string to Object")
        void testPaymentRequestFromJsonString() throws JsonProcessingException {
            String jsonString = """
                {
                    "transactionId": "123e4567-e89b-12d3-a456-426614174000",
                    "payerName": "John Doe",
                    "payerBank": "Chase Bank",
                    "payerCountryCode": "USA",
                    "payerAccount": "12345678901234",
                    "payeeName": "Jane Smith",
                    "payeeBank": "Wells Fargo",
                    "payeeCountryCode": "USA",
                    "payeeAccount": "87654321098765",
                    "paymentInstruction": "Monthly rent payment",
                    "executionDate": "2025-09-15",
                    "amount": "1500.75",
                    "currency": "USD",
                    "creationTimestamp": "2025-09-15T14:47:19Z"
                }
                """;

            PaymentRequest request = objectMapper.readValue(jsonString, PaymentRequest.class);

            assertThat(request.getTransactionId()).hasToString("123e4567-e89b-12d3-a456-426614174000");
            assertThat(request.getPayerName()).isEqualTo("John Doe");
            assertThat(request.getExecutionDate()).isEqualTo(LocalDate.of(2025, 9, 15));
            assertThat(request.getAmount()).isEqualTo(new BigDecimal("1500.75"));
            assertThat(request.getCreationTimestamp()).isEqualTo(Instant.parse("2025-09-15T14:47:19Z"));
        }

        @Test
        @DisplayName("FraudCheckResponse: JSON string to Object")
        void testFraudCheckResponseFromJsonString() throws JsonProcessingException {
            String jsonString = """
                {
                    "transactionId": "123e4567-e89b-12d3-a456-426614174000",
                    "status": "APPROVED",
                    "validationTimestamp": "2025-09-15T14:47:20Z"
                }
                """;

            FraudCheckResponse response = objectMapper.readValue(jsonString, FraudCheckResponse.class);

            assertThat(response.getTransactionId()).hasToString("123e4567-e89b-12d3-a456-426614174000");
            assertThat(response.getStatus()).isEqualTo(FraudCheckStatus.APPROVED);
            assertThat(response.getValidationTimestamp()).isEqualTo(Instant.parse("2025-09-15T14:47:20Z"));
        }
    }

    @Nested
    @DisplayName("XML Serialization Tests")
    class XmlSerializationTests {

        @Test
        @DisplayName("PaymentRequest: Object to XML to Object")
        void testPaymentRequestXmlRoundTrip() throws JsonProcessingException {
            PaymentRequest original = createSamplePaymentRequest();

            // Object -> XML
            String xml = xmlMapper.writer(new DefaultXmlPrettyPrinter()).writeValueAsString(original);
            log.info("PaymentRequest XML:\n{}", xml);

            // XML -> Object
            PaymentRequest deserialized = xmlMapper.readValue(xml, PaymentRequest.class);

            // Verify round-trip
            assertThat(deserialized).isEqualTo(original);
            assertThat(deserialized.getTransactionId()).isEqualTo(original.getTransactionId());
            assertThat(deserialized.getPayerName()).isEqualTo(original.getPayerName());
            assertThat(deserialized.getExecutionDate()).isEqualTo(original.getExecutionDate());
            assertThat(deserialized.getCreationTimestamp()).isEqualTo(original.getCreationTimestamp());
            assertThat(deserialized.getAmount()).isEqualTo(original.getAmount());

            // Verify XML structure
            assertThat(xml).contains("<PaymentRequest>");
        }

        @Test
        @DisplayName("FraudCheckResponse: Object to XML to Object")
        void testFraudCheckResponseXmlRoundTrip() throws JsonProcessingException {
            FraudCheckResponse original = createSampleFraudCheckResponse();

            // Object -> XML
            String xml = xmlMapper.writer(new DefaultXmlPrettyPrinter()).writeValueAsString(original);
            log.info("FraudCheckResponse XML:\n{}", xml);

            // XML -> Object
            FraudCheckResponse deserialized = xmlMapper.readValue(xml, FraudCheckResponse.class);

            // Verify round-trip
            assertThat(deserialized).isEqualTo(original);
            assertThat(deserialized.getTransactionId()).isEqualTo(original.getTransactionId());
            assertThat(deserialized.getStatus()).isEqualTo(original.getStatus());
            assertThat(deserialized.getValidationTimestamp()).isEqualTo(original.getValidationTimestamp());

            // Verify XML structure
            assertThat(xml).contains("<FraudCheckResponse>");
        }

        @Test
        @DisplayName("PaymentRequest: XML string to Object")
        void testPaymentRequestFromXmlString() throws JsonProcessingException {
            String xmlString = """
                <fraudCheckRequest xmlns="urn:example:fraudcheck:v1">
                    <transactionId>123e4567-e89b-12d3-a456-426614174000</transactionId>
                    <payerName>John Doe</payerName>
                    <payerBank>Chase Bank</payerBank>
                    <payerCountryCode>USA</payerCountryCode>
                    <payerAccount>12345678901234</payerAccount>
                    <payeeName>Jane Smith</payeeName>
                    <payeeBank>Wells Fargo</payeeBank>
                    <payeeCountryCode>USA</payeeCountryCode>
                    <payeeAccount>87654321098765</payeeAccount>
                    <paymentInstruction>Monthly rent payment</paymentInstruction>
                    <executionDate>2025-09-15</executionDate>
                    <amount>1500.75</amount>
                    <currency>USD</currency>
                    <creationTimestamp>2025-09-15T14:47:19Z</creationTimestamp>
                </fraudCheckRequest>
                """;

            PaymentRequest request = xmlMapper.readValue(xmlString, PaymentRequest.class);

            assertThat(request.getTransactionId()).hasToString("123e4567-e89b-12d3-a456-426614174000");
            assertThat(request.getPayerName()).isEqualTo("John Doe");
            assertThat(request.getExecutionDate()).isEqualTo(LocalDate.of(2025, 9, 15));
            assertThat(request.getAmount()).isEqualTo(new BigDecimal("1500.75"));
            assertThat(request.getCreationTimestamp()).isEqualTo(Instant.parse("2025-09-15T14:47:19Z"));
        }

        @Test
        @DisplayName("FraudCheckResponse: XML string to Object")
        void testFraudCheckResponseFromXmlString() throws JsonProcessingException {
            String xmlString = """
                <fraudCheckResponse xmlns="urn:example:fraudcheck:v1">
                    <transactionId>123e4567-e89b-12d3-a456-426614174000</transactionId>
                    <status>APPROVED</status>
                    <validationTimestamp>2025-09-15T14:47:20Z</validationTimestamp>
                </fraudCheckResponse>
                """;

            FraudCheckResponse response = xmlMapper.readValue(xmlString, FraudCheckResponse.class);

            assertThat(response.getTransactionId()).hasToString("123e4567-e89b-12d3-a456-426614174000");
            assertThat(response.getStatus()).isEqualTo(FraudCheckStatus.APPROVED);
            assertThat(response.getValidationTimestamp()).isEqualTo(Instant.parse("2025-09-15T14:47:20Z"));
        }
    }

    @Nested
    @DisplayName("Cross-Format Conversion Tests")
    class CrossFormatConversionTests {

        @Test
        @DisplayName("PaymentRequest: JSON -> Object -> XML -> Object -> JSON")
        void testPaymentRequestCrossFormatConversion() throws JsonProcessingException {
            String originalJson = """
                {
                    "transactionId": "123e4567-e89b-12d3-a456-426614174000",
                    "payerName": "John Doe",
                    "payerBank": "Chase Bank",
                    "payerCountryCode": "USA",
                    "payerAccount": "12345678901234",
                    "payeeName": "Jane Smith",
                    "payeeBank": "Wells Fargo",
                    "payeeCountryCode": "USA",
                    "payeeAccount": "87654321098765",
                    "paymentInstruction": "Monthly rent payment",
                    "executionDate": "2025-09-15",
                    "amount": "1500.75",
                    "currency": "USD",
                    "creationTimestamp": "2025-09-15T14:47:19Z"
                }
                """;

            // JSON -> Object
            PaymentRequest fromJson = objectMapper.readValue(originalJson, PaymentRequest.class);
            log.info("Parsed from JSON: {}", fromJson);

            // Object -> XML
            String xml = xmlMapper.writer(new DefaultXmlPrettyPrinter()).writeValueAsString(fromJson);
            log.info("Converted to XML:\n{}", xml);

            // XML -> Object
            PaymentRequest fromXml = xmlMapper.readValue(xml, PaymentRequest.class);
            log.info("Parsed from XML: {}", fromXml);

            // Object -> JSON
            String finalJson = objectMapper.writeValueAsString(fromXml);
            log.info("Final JSON: {}", finalJson);

            // Verify all objects are equal
            assertThat(fromJson).isEqualTo(fromXml);

            // Parse both JSON strings for comparison (ignore formatting differences)
            PaymentRequest originalParsed = objectMapper.readValue(originalJson, PaymentRequest.class);
            PaymentRequest finalParsed = objectMapper.readValue(finalJson, PaymentRequest.class);
            assertThat(finalParsed).isEqualTo(originalParsed);
        }

        @Test
        @DisplayName("FraudCheckResponse: XML -> Object -> JSON -> Object -> XML")
        void testFraudCheckResponseCrossFormatConversion() throws JsonProcessingException {
            String originalXml = """
                <fraudCheckResponse xmlns="urn:example:fraudcheck:v1">
                    <transactionId>123e4567-e89b-12d3-a456-426614174000</transactionId>
                    <status>SUSPICIOUS</status>
                    <validationTimestamp>2025-09-15T14:47:20Z</validationTimestamp>
                </fraudCheckResponse>
                """;

            // XML -> Object
            FraudCheckResponse fromXml = xmlMapper.readValue(originalXml, FraudCheckResponse.class);
            log.info("Parsed from XML: {}", fromXml);

            // Object -> JSON
            String json = objectMapper.writeValueAsString(fromXml);
            log.info("Converted to JSON: {}", json);

            // JSON -> Object
            FraudCheckResponse fromJson = objectMapper.readValue(json, FraudCheckResponse.class);
            log.info("Parsed from JSON: {}", fromJson);

            // Object -> XML
            String finalXml = xmlMapper.writer(new DefaultXmlPrettyPrinter()).writeValueAsString(fromJson);
            log.info("Final XML:\n{}", finalXml);

            // Verify all objects are equal
            assertThat(fromXml).isEqualTo(fromJson);
            assertThat(fromJson.getStatus()).isEqualTo(FraudCheckStatus.SUSPICIOUS);
        }
    }
    

}
