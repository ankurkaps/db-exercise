package com.demo.payment.exception;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.CamelExecutionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        int errorCount = ex.getBindingResult().getErrorCount();
        log.warn("Bean validation failed with {} error(s): {}", errorCount, ex.getMessage());

        List<ValidationError> validationErrors = new ArrayList<>();

        // Extract field validation errors
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            ValidationError error = new ValidationError(
                fieldError.getField(),
                fieldError.getRejectedValue() != null ? fieldError.getRejectedValue().toString() : null,
                fieldError.getDefaultMessage()
            );
            validationErrors.add(error);
        }

        // Extract global validation errors
        ex.getBindingResult().getGlobalErrors().forEach(globalError -> {
            ValidationError error = new ValidationError(
                globalError.getObjectName(),
                null,
                globalError.getDefaultMessage()
            );
            validationErrors.add(error);
        });

        String message = String.format("Request validation failed with %d error(s)", errorCount);
        ValidationErrorResponse response = new ValidationErrorResponse(
            "VALIDATION_ERROR",
            message,
            validationErrors
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle parse exceptions and provide more useful error
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ValidationErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.warn("JSON parsing/format error: {}", ex.getMessage());

        List<ValidationError> validationErrors = new ArrayList<>();

        // Check if it's an InvalidFormatException (like invalid UUID format, date format, etc.)
        Throwable cause = ex.getCause();
        if (cause instanceof InvalidFormatException) {
            InvalidFormatException formatEx = (InvalidFormatException) cause;

            String fieldName = extractFieldName(formatEx);
            String rejectedValue = formatEx.getValue() != null ? formatEx.getValue().toString() : null;
            String message = generateFormatErrorMessage(fieldName, rejectedValue, formatEx);

            ValidationError error = new ValidationError(fieldName, rejectedValue, message);
            validationErrors.add(error);
        } else {
            // Generic JSON parsing error - try to extract field name from error message
            String errorMessage = extractRootCauseMessage(ex);
            String fieldName = extractFieldNameFromMessage(ex.getMessage());

            ValidationError error = new ValidationError(fieldName, null, "Invalid JSON format: " + errorMessage);
            validationErrors.add(error);
        }

        ValidationErrorResponse response = new ValidationErrorResponse(
            "VALIDATION_ERROR",
            "Request format validation failed",
            validationErrors
        );

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    private String generateFormatErrorMessage(String fieldName, String rejectedValue, InvalidFormatException formatEx) {
        Class<?> targetType = formatEx.getTargetType();

        if (targetType != null) {
            // UUID format errors
            if (targetType.equals(java.util.UUID.class)) {
                return String.format("Invalid UUID format for field '%s'. Expected format: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx (e.g., 123e4567-e89b-12d3-a456-426614174000). Received: %s",
                    fieldName, rejectedValue);
            }

            // LocalDate format errors
            if (targetType.equals(java.time.LocalDate.class)) {
                if (rejectedValue != null && rejectedValue.contains("T")) {
                    return String.format("Invalid date format for field '%s'. Expected date only in format yyyy-MM-dd (e.g., 2024-01-15). " +
                        "Received timestamp format: %s. Use executionDate for date fields, not timestamp format.",
                        fieldName, rejectedValue);
                }
                return String.format("Invalid date format for field '%s'. Expected format: yyyy-MM-dd (e.g., 2024-01-15). Received: %s",
                    fieldName, rejectedValue);
            }

            // Instant format errors
            if (targetType.equals(java.time.Instant.class)) {
                if (rejectedValue != null && rejectedValue.contains(".000Z")) {
                    return String.format("Invalid timestamp format for field '%s'. Remove milliseconds from timestamp. " +
                        "Expected format: yyyy-MM-ddTHH:mm:ssZ (e.g., 2024-01-15T10:30:00Z). " +
                        "Received with milliseconds: %s. Try: %s",
                        fieldName, rejectedValue, rejectedValue.replace(".000Z", "Z"));
                }
                if (rejectedValue != null && rejectedValue.matches(".*\\.\\d{3}Z$")) {
                    return String.format("Invalid timestamp format for field '%s'. Milliseconds not supported. " +
                        "Expected format: yyyy-MM-ddTHH:mm:ssZ (e.g., 2024-01-15T10:30:00Z). " +
                        "Received: %s. Remove the milliseconds (.###) part.",
                        fieldName, rejectedValue);
                }
                return String.format("Invalid timestamp format for field '%s'. Expected format: yyyy-MM-ddTHH:mm:ssZ (e.g., 2024-01-15T10:30:00Z). " +
                    "Received: %s", fieldName, rejectedValue);
            }

            // BigDecimal format errors
            if (targetType.equals(java.math.BigDecimal.class)) {
                return String.format("Invalid decimal format for field '%s'. Expected numeric value with up to 2 decimal places (e.g., 100.50, 1500.75). " +
                    "Received: %s", fieldName, rejectedValue);
            }

            // Enum format errors
            if (targetType.isEnum()) {
                Object[] enumConstants = targetType.getEnumConstants();
                String validValues = java.util.Arrays.stream(enumConstants)
                    .map(Object::toString)
                    .collect(java.util.stream.Collectors.joining(", "));
                return String.format("Invalid value for field '%s'. Must be one of: [%s]. Received: %s",
                    fieldName, validValues, rejectedValue);
            }
        }

        // Generic format error with cause
        String causeMessage = formatEx.getCause() != null ? formatEx.getCause().getMessage() : formatEx.getMessage();
        return String.format("Invalid format for field '%s'. %s. Received: %s", fieldName, causeMessage, rejectedValue);
    }

    private String extractRootCauseMessage(Throwable ex) {
        Throwable rootCause = ex;
        String message = ex.getMessage();

        // Traverse the cause chain to find the most meaningful error
        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
            // Keep the more descriptive message from the chain
            if (rootCause.getMessage() != null &&
                (rootCause instanceof java.time.format.DateTimeParseException ||
                 rootCause instanceof java.lang.NumberFormatException ||
                 rootCause instanceof java.lang.IllegalArgumentException)) {
                message = rootCause.getMessage();
            }
        }

        // Clean up common unhelpful prefixes
        if (message != null) {
            message = message.replaceFirst("^JSON parse error: ", "")
                           .replaceFirst("^Cannot deserialize value of type .* from String \".*\": ", "")
                           .replaceFirst("^Failed to deserialize .*: ", "");
        }

        return message != null ? message : "Invalid request format";
    }

    private String extractFieldName(InvalidFormatException formatEx) {
        String fieldName = "unknown";
        if (formatEx.getPath() != null && !formatEx.getPath().isEmpty()) {
            // Get the last element in the path which should be the field name
            com.fasterxml.jackson.databind.JsonMappingException.Reference lastRef =
                formatEx.getPath().get(formatEx.getPath().size() - 1);

            if (lastRef.getFieldName() != null) {
                fieldName = lastRef.getFieldName();
            } else if (lastRef.getIndex() >= 0) {
                // If it's an array element, include the index
                fieldName = "element[" + lastRef.getIndex() + "]";
            }
        }
        return fieldName;
    }

    private String extractFieldNameFromMessage(String message) {
        if (message == null) {
            return "request";
        }

        // Try to extract field name from common error message patterns
        // Pattern: "...through reference chain: ObjectName[\"fieldName\"])"
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(".*\\[\"([^\"]+)\"\\].*");
        java.util.regex.Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            return matcher.group(1);
        }

        // Pattern: "...at [Source: ...; line: X, column: Y] (through reference chain: ObjectName[\"fieldName\"])"
        pattern = java.util.regex.Pattern.compile(".*reference chain:.*\\[\"([^\"]+)\"\\]");
        matcher = pattern.matcher(message);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return "request";
    }

    @ExceptionHandler(value = {
            PaymentValidationException.class,
            JsonParseException.class,
            org.springframework.boot.json.JsonParseException.class})
    public ResponseEntity<ErrorResponse> handleValidationException(Exception ex) {
        log.warn("Validation exception: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse("VALIDATION_ERROR", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(PaymentAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handlePaymentAlreadyExistsException(PaymentAlreadyExistsException ex) {
        log.warn("Payment already exists: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse("DUPLICATE_PAYMENT", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePaymentNotFoundException(PaymentNotFoundException ex) {
        log.warn("Payment not found: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse("PAYMENT_NOT_FOUND", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(CamelExecutionException.class)
    public ResponseEntity<ErrorResponse> handleCamelExecutionException(Exception ex) {
        // Dont print the exception here again
        log.error("Camel execution exception error occurred {}", ex.getMessage());
        
        Throwable cause = ex.getCause();
        if(cause!=null) {
            if (cause instanceof PaymentAlreadyExistsException pae) {
                // 'pae' is auto-cast to PaymentAlreadyExistsException
                return handlePaymentAlreadyExistsException(pae);
            } else {
                return handleGenericException(cause); // or other handling
            }
        }
        
        return handleGenericException(ex);
    }    
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Throwable ex) {
        log.error("Unexpected error occurred", ex);
        ErrorResponse error = new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred. Unable to process the request");
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }


    @Data
    @AllArgsConstructor
    public static class ValidationErrorResponse {
        private String code;
        private String message;
        private List<ValidationError> errors;
    }

    @Data
    @AllArgsConstructor
    public static class ValidationError {
        private String field;
        private String rejectedValue;
        private String message;
    }

    @Data
    @AllArgsConstructor
    public static class ErrorResponse {
        private String code;
        private String message;
    }
}
