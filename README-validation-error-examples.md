# Improved Error Message Examples

## Enhanced Global Exception Handler

### 1. Timestamp with Milliseconds Error

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Request format validation failed",
  "errors": [
    {
      "field": "creationTimestamp",
      "rejectedValue": "2024-01-01T10:00:00.000Z",
      "message": "Invalid timestamp format for field 'creationTimestamp'. Remove milliseconds from timestamp. Expected format: yyyy-MM-ddTHH:mm:ssZ (e.g., 2024-01-15T10:30:00Z). Received with milliseconds: 2024-01-01T10:00:00.000Z. Try: 2024-01-01T10:00:00Z"
    }
  ]
}
```

### 2. LocalDate with Timestamp Error

```json
{
  "field": "executionDate",
  "rejectedValue": "2024-01-01T10:00:00.000Z",
  "message": "Invalid date format for field 'executionDate'. Expected date only in format yyyy-MM-dd (e.g., 2024-01-15). Received timestamp format: 2024-01-01T10:00:00.000Z. Use executionDate for date fields, not timestamp format."
}
```

### 3. Multiple Bean Validation Errors

**After:**
```json
{
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed with 2 error(s)",
  "errors": [
    {
      "field": "payerAccount",
      "rejectedValue": "123456",
      "message": "Payer account number is required, and cannot be blank and should be between 8 to 34 characters"
    },
    {
      "field": "payeeCountryCode",
      "rejectedValue": "IDR",
      "message": "must be a valid ISO3166-1 alpha-3 country code (e.g., DEU, GBR)"
    }
  ]
}
```

### 4. UUID Format Error

```

**After:**
```json
{
  "field": "transactionId",
  "rejectedValue": "invalid-uuid",
  "message": "Invalid UUID format for field 'transactionId'. Expected format: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx (e.g., 123e4567-e89b-12d3-a456-426614174000). Received: invalid-uuid"
}
```

### 5. BigDecimal Format Error

**After (Enhanced):**
```json
{
  "field": "amount",
  "rejectedValue": "not-a-number",
  "message": "Invalid decimal format for field 'amount'. Expected numeric value with up to 2 decimal places (e.g., 100.50, 1500.75). Received: not-a-number"
}
```

## Key Improvements

1. **Specific millisecond handling**: Detects `.000Z` and provides exact fix
2. **Actionable guidance**: Shows exactly what to change
3. **Clear examples**: Provides concrete examples in error messages
4. **Error count indication**: Shows total validation errors in message
5. **Context-aware messages**: Different messages for different error scenarios
6. **Field name extraction**: Better field name resolution from JSON paths
7. **Root cause analysis**: Cleaner error messages by removing Jackson verbosity