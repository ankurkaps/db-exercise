package com.demo.payment.validation;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.demo.common.model.PaymentRequest;
import com.demo.payment.exception.PaymentValidationException;

@ExtendWith(MockitoExtension.class)
@DisplayName("Payment Validator Tests")
class PaymentValidatorTest {

    private PaymentValidator paymentValidator;

    @BeforeEach
    void setUp() {
        paymentValidator = new PaymentValidator();
    }

    @Test
    @DisplayName("Should validate a valid payment request successfully")
    void shouldValidateValidPaymentRequest() {
        // Given
        PaymentRequest request = createValidPaymentRequest();

        // When & Then
        assertDoesNotThrow(() -> paymentValidator.validate(request));
    }

    @Test
    @DisplayName("Should throw exception when payment request is null")
    void shouldThrowExceptionWhenPaymentRequestIsNull() {
        // When & Then
        assertThatThrownBy(() -> paymentValidator.validate(null))
            .isInstanceOf(PaymentValidationException.class)
            .hasMessage("Payment request cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when transaction ID is null")
    void shouldThrowExceptionWhenTransactionIdIsNull() {
        // Given
        PaymentRequest request = createValidPaymentRequest();
        request.setTransactionId(null);

        // When & Then
        assertThatThrownBy(() -> paymentValidator.validate(request))
            .isInstanceOf(PaymentValidationException.class)
            .hasMessage("transactionId is mandatory");
    }

    @Test
    @DisplayName("Should throw exception when payer name is null")
    void shouldThrowExceptionWhenPayerNameIsNull() {
        // Given
        PaymentRequest request = createValidPaymentRequest();
        request.setPayerName(null);

        // When & Then
        assertThatThrownBy(() -> paymentValidator.validate(request))
            .isInstanceOf(PaymentValidationException.class)
            .hasMessage("payerName is mandatory");
    }

    @Test
    @DisplayName("Should throw exception when payee name is null")
    void shouldThrowExceptionWhenPayeeNameIsNull() {
        // Given
        PaymentRequest request = createValidPaymentRequest();
        request.setPayeeName(null);

        // When & Then
        assertThatThrownBy(() -> paymentValidator.validate(request))
            .isInstanceOf(PaymentValidationException.class)
            .hasMessage("payeeName is mandatory");
    }

    @Test
    @DisplayName("Should throw exception when amount is null")
    void shouldThrowExceptionWhenAmountIsNull() {
        // Given
        PaymentRequest request = createValidPaymentRequest();
        request.setAmount(null);

        // When & Then
        assertThatThrownBy(() -> paymentValidator.validate(request))
            .isInstanceOf(PaymentValidationException.class)
            .hasMessage("amount is mandatory");
    }

    @Test
    @DisplayName("Should throw exception when currency is null")
    void shouldThrowExceptionWhenCurrencyIsNull() {
        // Given
        PaymentRequest request = createValidPaymentRequest();
        request.setCurrency(null);

        // When & Then
        assertThatThrownBy(() -> paymentValidator.validate(request))
            .isInstanceOf(PaymentValidationException.class)
            .hasMessage("currency is mandatory");
    }

    @Test
    @DisplayName("Should throw exception when execution date is null")
    void shouldThrowExceptionWhenExecutionDateIsNull() {
        // Given
        PaymentRequest request = createValidPaymentRequest();
        request.setExecutionDate(null);

        // When & Then
        assertThatThrownBy(() -> paymentValidator.validate(request))
            .isInstanceOf(PaymentValidationException.class)
            .hasMessage("executionDate is mandatory");
    }

    @Test
    @DisplayName("Should throw exception when creation timestamp is null")
    void shouldThrowExceptionWhenCreationTimestampIsNull() {
        // Given
        PaymentRequest request = createValidPaymentRequest();
        request.setCreationTimestamp(null);

        // When & Then
        assertThatThrownBy(() -> paymentValidator.validate(request))
            .isInstanceOf(PaymentValidationException.class)
            .hasMessage("creationTimestamp is mandatory");
    }

    @Test
    @DisplayName("Should throw exception for invalid payer country code format")
    void shouldThrowExceptionForInvalidPayerCountryCode() {
        // Given
        PaymentRequest request = createValidPaymentRequest();
        request.setPayerCountryCode("US"); // Only 2 characters instead of 3

        // When & Then
        assertThatThrownBy(() -> paymentValidator.validate(request))
            .isInstanceOf(PaymentValidationException.class)
            .hasMessage("Invalid payer country code format. Must be ISO alpha-3");
    }

    @Test
    @DisplayName("Should throw exception for lowercase country code")
    void shouldThrowExceptionForLowercaseCountryCode() {
        // Given
        PaymentRequest request = createValidPaymentRequest();
        request.setPayerCountryCode("usa"); // Lowercase instead of uppercase

        // When & Then
        assertThatThrownBy(() -> paymentValidator.validate(request))
            .isInstanceOf(PaymentValidationException.class)
            .hasMessage("Invalid payer country code format. Must be ISO alpha-3");
    }

    @Test
    @DisplayName("Should throw exception for country code with numbers")
    void shouldThrowExceptionForCountryCodeWithNumbers() {
        // Given
        PaymentRequest request = createValidPaymentRequest();
        request.setPayerCountryCode("U5A"); // Contains number

        // When & Then
        assertThatThrownBy(() -> paymentValidator.validate(request))
            .isInstanceOf(PaymentValidationException.class)
            .hasMessage("Invalid payer country code format. Must be ISO alpha-3");
    }

    @Test
    @DisplayName("Should throw exception when payer account is empty")
    void shouldThrowExceptionWhenPayerAccountIsEmpty() {
        // Given
        PaymentRequest request = createValidPaymentRequest();
        request.setPayerAccount("   "); // Only whitespace

        // When & Then
        assertThatThrownBy(() -> paymentValidator.validate(request))
            .isInstanceOf(PaymentValidationException.class)
            .hasMessage("Payer account number cannot be empty");
    }

    @Test
    @DisplayName("Should throw exception when payee account is empty")
    void shouldThrowExceptionWhenPayeeAccountIsEmpty() {
        // Given
        PaymentRequest request = createValidPaymentRequest();
        request.setPayeeAccount(""); // Empty string

        // When & Then
        assertThatThrownBy(() -> paymentValidator.validate(request))
            .isInstanceOf(PaymentValidationException.class)
            .hasMessage("Payee account number cannot be empty");
    }

    @Test
    @DisplayName("Should throw exception when amount has more than 2 decimal places")
    void shouldThrowExceptionWhenAmountHasMoreThan2DecimalPlaces() {
        // Given
        PaymentRequest request = createValidPaymentRequest();
        request.setAmount(BigDecimal.valueOf(100.123)); // 3 decimal places

        // When & Then
        assertThatThrownBy(() -> paymentValidator.validate(request))
            .isInstanceOf(PaymentValidationException.class)
            .hasMessage("Amount must have exactly 2 decimal places");
    }

    @Test
    @DisplayName("Should throw exception when amount is zero")
    void shouldThrowExceptionWhenAmountIsZero() {
        // Given
        PaymentRequest request = createValidPaymentRequest();
        request.setAmount(BigDecimal.ZERO);

        // When & Then
        assertThatThrownBy(() -> paymentValidator.validate(request))
            .isInstanceOf(PaymentValidationException.class)
            .hasMessage("Amount must be greater than zero");
    }

    @Test
    @DisplayName("Should throw exception when amount is negative")
    void shouldThrowExceptionWhenAmountIsNegative() {
        // Given
        PaymentRequest request = createValidPaymentRequest();
        request.setAmount(BigDecimal.valueOf(-100.00));

        // When & Then
        assertThatThrownBy(() -> paymentValidator.validate(request))
            .isInstanceOf(PaymentValidationException.class)
            .hasMessage("Amount must be greater than zero");
    }

    @Test
    @DisplayName("Should accept amount with exactly 2 decimal places")
    void shouldAcceptAmountWithExactly2DecimalPlaces() {
        // Given
        PaymentRequest request = createValidPaymentRequest();
        request.setAmount(BigDecimal.valueOf(100.50));

        // When & Then
        assertDoesNotThrow(() -> paymentValidator.validate(request));
    }

    @Test
    @DisplayName("Should accept amount with 1 decimal place")
    void shouldAcceptAmountWith1DecimalPlace() {
        // Given
        PaymentRequest request = createValidPaymentRequest();
        request.setAmount(BigDecimal.valueOf(100.5));

        // When & Then
        assertDoesNotThrow(() -> paymentValidator.validate(request));
    }

    @Test
    @DisplayName("Should accept amount with no decimal places")
    void shouldAcceptAmountWithNoDecimalPlaces() {
        // Given
        PaymentRequest request = createValidPaymentRequest();
        request.setAmount(BigDecimal.valueOf(100));

        // When & Then
        assertDoesNotThrow(() -> paymentValidator.validate(request));
    }

    @Test
    @DisplayName("Should throw exception for invalid currency code")
    void shouldThrowExceptionForInvalidCurrencyCode() {
        // Given
        PaymentRequest request = createValidPaymentRequest();
        request.setCurrency("INVALID");

        // When & Then
        assertThatThrownBy(() -> paymentValidator.validate(request))
            .isInstanceOf(PaymentValidationException.class)
            .hasMessage("Invalid currency code. Must be ISO4217 currency code");
    }

    @Test
    @DisplayName("Should accept valid ISO4217 currency codes")
    void shouldAcceptValidCurrencyCodes() {
        String[] validCurrencies = {"USD", "EUR", "GBP", "JPY", "CHF", "CAD", "AUD"};

        for (String currency : validCurrencies) {
            PaymentRequest request = createValidPaymentRequest();
            request.setCurrency(currency);

            assertDoesNotThrow(() -> paymentValidator.validate(request),
                "Currency " + currency + " should be valid");
        }
    }

    @Test
    @DisplayName("Should accept valid ISO3166 alpha-3 country codes")
    void shouldAcceptValidCountryCodes() {
        String[] validCountries = {"USA", "GBR", "DEU", "FRA", "JPN", "CHN", "IND"};

        for (String country : validCountries) {
            PaymentRequest request = createValidPaymentRequest();
            request.setPayerCountryCode(country);
            request.setPayeeCountryCode(country);

            assertDoesNotThrow(() -> paymentValidator.validate(request),
                "Country code " + country + " should be valid");
        }
    }

    private PaymentRequest createValidPaymentRequest() {
        PaymentRequest request = new PaymentRequest();
        request.setTransactionId(UUID.randomUUID());
        request.setPayerName("John Doe");
        request.setPayerBank("Chase Bank");
        request.setPayerCountryCode("USA");
        request.setPayerAccount("123456789");
        request.setPayeeName("Jane Smith");
        request.setPayeeBank("Wells Fargo");
        request.setPayeeCountryCode("USA");
        request.setPayeeAccount("987654321");
        request.setPaymentInstruction("Salary payment");
        request.setExecutionDate(LocalDate.now());
        request.setAmount(BigDecimal.valueOf(1000.00));
        request.setCurrency("USD");
        request.setCreationTimestamp(Instant.now());
        return request;
    }
}