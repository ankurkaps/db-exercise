package com.demo.payment.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.demo.common.model.PaymentRequest;
import com.demo.payment.exception.GlobalExceptionHandler.ValidationError;
import com.demo.payment.exception.GlobalExceptionHandler.ValidationErrorResponse;

public class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler globalExceptionHandler;

    @BeforeEach
    void setUp() {
        globalExceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("Should handle multiple validation errors and return all in response")
    void testMultipleValidationErrors() {
        // Create a mock MethodArgumentNotValidException with multiple errors
        PaymentRequest paymentRequest = new PaymentRequest();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(paymentRequest, "paymentRequest");

        // Add multiple field errors
        bindingResult.addError(new FieldError("paymentRequest", "payerAccount", "123456", false, null, null, "Payer account number is required, and cannot be blank and should be between 8 to 34 characters"));
        bindingResult.addError(new FieldError("paymentRequest", "payeeCountryCode", "IDR", false, null, null, "must be a valid ISO3166-1 alpha-3 country code (e.g., DEU, GBR)"));

        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

        // Handle the exception
        ResponseEntity<ValidationErrorResponse> response = globalExceptionHandler.handleMethodArgumentNotValidException(exception);

        // Verify the response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().getMessage()).isEqualTo("Request validation failed with 2 error(s)");

        List<ValidationError> errors = response.getBody().getErrors();
        assertThat(errors).hasSize(2);

        // Check first error
        ValidationError firstError = errors.get(0);
        assertThat(firstError.getField()).isEqualTo("payerAccount");
        assertThat(firstError.getRejectedValue()).isEqualTo("123456");
        assertThat(firstError.getMessage()).contains("Payer account number is required");

        // Check second error
        ValidationError secondError = errors.get(1);
        assertThat(secondError.getField()).isEqualTo("payeeCountryCode");
        assertThat(secondError.getRejectedValue()).isEqualTo("IDR");
        assertThat(secondError.getMessage()).contains("must be a valid ISO3166-1 alpha-3 country code");
    }

    @Test
    @DisplayName("Should handle PaymentNotFoundException with proper error response")
    void testPaymentNotFoundException() {
        PaymentNotFoundException exception = new PaymentNotFoundException("Payment with transaction ID 12345 not found");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = globalExceptionHandler.handlePaymentNotFoundException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("PAYMENT_NOT_FOUND");
        assertThat(response.getBody().getMessage()).isEqualTo("Payment with transaction ID 12345 not found");
    }

    @Test
    @DisplayName("Should handle PaymentAlreadyExistsException with proper error response")
    void testPaymentAlreadyExistsException() {
        PaymentAlreadyExistsException exception = new PaymentAlreadyExistsException("Payment with transaction ID 12345 already exists");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = globalExceptionHandler.handlePaymentAlreadyExistsException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("DUPLICATE_PAYMENT");
        assertThat(response.getBody().getMessage()).isEqualTo("Payment with transaction ID 12345 already exists");
    }
}