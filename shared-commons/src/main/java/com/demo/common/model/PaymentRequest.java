package com.demo.common.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.demo.common.jaxb.adapters.InstantIso8601UtcXmlAdapter;
import com.demo.common.jaxb.adapters.LocalDateIso8601XmlAdapter;
import com.demo.common.validation.CountryIso3166Alpha3;
import com.demo.common.validation.CurrencyIso4217;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payment Request
 *
 * Used for payment processing and fraud check
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "fraudCheckRequest", namespace = "urn:example:fraudcheck:v1")
@XmlType(propOrder = { "transactionId", "payerName", "payerBank", "payerCountryCode", "payerAccount", "payeeName",
        "payeeBank", "payeeCountryCode", "payeeAccount", "paymentInstruction", "executionDate", "amount", "currency",
        "creationTimestamp" })
@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class PaymentRequest {

    /**
     * Transaction ID in UUID format. Must be unique across the system and E2E traceable
     * MANDATORY
     */
    @NotNull(message = "Transaction ID is mandatory")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private UUID transactionId; // Mandatory, UUID format

    /**
     * Payer's first and last name, for example: "Munster Muller"
     *
     * Size Considerations:
     *   SWIFT MT messages: typically 35 characters per line, up to 4 lines roughly 140 chars.
     *   ISO 20022: allows up to 140 characters for Nm (name).
     *   SEPA enforces 1-70. - Truncates longer than that
     *
     * MANDATORY
     */
    @NotBlank(message = "Payer name is mandatory")
    @Size(min = 1, max=70, message = "Payer name is required, and cannot be blank and should be between 1 to 70 characters")
    private String payerName; // Mandatory

    /**
     * Name of the payer's bank, for example: "Bank of America"
     *
     * Treated similar to name
     * Sizing
     *   SWIFT allows upto 35 chars per line and up to 4 lines for total of 140 characters.
     *   SEPA enforces 1-70
     *
     * MANDATORY
     */
    @NotBlank(message = "Payer bank is mandatory")
    @Size(min = 1, max=70, message = "Payer bank name is required, and cannot be blank and should be between 1 to 70 characters")
    private String payerBank; // Mandatory

    /**
     * ISO3166-1 alpha-3 country code, for example: DEU, GBR,USA
     * https://en.wikipedia.org/wiki/ISO_3166-1_alpha-3
     * 
     * MANDATORY
     */
    @NotBlank(message = "Payer country code is mandatory")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Payer country code must be ISO3166-1 alpha-3 format like example: DEU, GBR,USA etc.")
    @CountryIso3166Alpha3
    private String payerCountryCode; // Mandatory, ISO 3166-1 alpha-3

    /**
     * Payer's account number
     *
     * https://en.wikipedia.org/wiki/International_Bank_Account_Number
     *
     * IBAN countries (EU/EEA + others like Turkey, Saudi, Brazil) → already covered, they fit 15–34 alphanumeric.
     * US, Canada, Australia, India, Japan, China, etc. → local account numbers are typically numeric, 6–17 digits, sometimes paired with routing/branch codes.
     * Those values will pass the 8–34, alphanumeric rule as long as you normalize them into one field.
     * Latin America (e.g. Mexico CLABE = 18 digits, Brazil accounts with check digits) → still inside 8–34.
     * Asia → Japan 7-digit account numbers + branch codes, India 10–18 digits, China up to 19 → still inside 8–34.
     *
     * Simplified validation to letters and numbers
     *
     * MANDATORY
     */
    @NotBlank(message = "Payer account is mandatory")
    @Size(min = 8, max=34, message = "Payer account number is required, and cannot be blank and should be between 8 to 34 characters")
    @Pattern(regexp = "^[A-Z0-9]+$", message = "Account must contain only letters and digits")
    private String payerAccount; // Mandatory

    /**
     * Payee's first and last name, for example: "Munster Muller"
     *
     * Same as the payee name 1-70
     *
     * MANDATORY
     */
    @NotBlank(message = "Payee name is mandatory")
    @Size(min = 1, max=70, message = "Payee name is required, and cannot be blank and should be between 1 to 70 characters")
    private String payeeName; // Mandatory

    /**
     * Name of the payee's bank, for example: "BNP Paribas"
     *
     * Treated similar to name
     * Sizing
     *   SWIFT/ISO 20022 allow 140 characters .
     *   SEPA enforces 1-70
     *
     * MANDATORY
     */
    @NotBlank(message = "Payee bank is mandatory")
    @Size(min = 1, max=70, message = "Payee bank name is required, and cannot be blank and should be between 1 to 70 characters")
    private String payeeBank; // Mandatory

    /**
     * ISO alpha-3 country code, for example: DEU, GBR, USA
     *
     * MANDATORY
     */
    @NotBlank(message = "Payee country code is mandatory")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Payer country code must be ISO alpha-3 format like example: DEU, GBR,USA ")
    @CountryIso3166Alpha3
    private String payeeCountryCode; // Mandatory, ISO 3166-1 alpha-3

    /**
     * Payee's account  number
     * https://en.wikipedia.org/wiki/International_Bank_Account_Number
     *
     * IBAN countries (EU/EEA + others like Turkey, Saudi, Brazil) → already covered, they fit 15–34 alphanumeric.
     * US, Canada, Australia, India, Japan, China, etc. → local account numbers are typically numeric, 6–17 digits, sometimes paired with routing/branch codes.
     * Those values will pass the 8–34, alphanumeric rule as long as you normalize them into one field.
     * Latin America (e.g. Mexico CLABE = 18 digits, Brazil accounts with check digits) → still inside 8–34.
     * Asia → Japan 7-digit account numbers + branch codes, India 10–18 digits, China up to 19 → still inside 8–34.
     *
     * Simplified validation to letters and numbers
     *
     * MANDATORY
     */
    @NotBlank(message = "Payee account is mandatory")
    @Size(min = 8, max=34, message = "Payer account number is required, and cannot be blank and should be between 8 to 34 characters")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "Account must contain only letters and digits")
    private String payeeAccount; // Mandatory

    /**
     * Free text, for example: "Loan Repayment", "Tax Reimbursements", etc
     *
     * ISO20022 & SWIFT - 140 chars
     * SEPA - 140 chars
     */
    @Size(max=140, message = "Payment instructions can be maximum of 140 characters")
    private String paymentInstruction; // Optional

    /**
     * ISO8601 date format YYYY-MM-DD, for example: 2020-02-21
     */
    @NotNull(message = "Execution date is mandatory")
    @JsonFormat(shape = Shape.STRING, pattern="yyyy-MM-dd")
    @XmlJavaTypeAdapter(LocalDateIso8601XmlAdapter.class)
    private LocalDate executionDate; // Mandatory, ISO-8601 YYYY-MM-DD

    /**
     * Transaction Amount
     * 2 decimal places must be supplied, for example: 17.45
     */
    @NotNull(message = "Amount is mandatory")
    @DecimalMin(value = "0.01", inclusive = true, message = "amount must be >= 0.01")
    @Digits(integer = 12, fraction = 2, message = "amount can have up to 12 digits and 2 decimals")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal amount; // Mandatory, 2 decimal places must be supplied, for example: 17.45

    /**
     * ISO4217 currency code, for example: EUR, GBP
     */
    @NotBlank(message = "Currency is mandatory")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be ISO4217 format e.g. USD, EUR, GBP etc")
    @CurrencyIso4217
    private String currency; // Mandatory, ISO 4217 currency code, for example: EUR, GBP

    /**
     * ISO8601 UTC timestamp format YYYY-MM-DDThh:mm:ssZ, for example: 2004-02-21T17:00:00Z
     */
    @NotNull(message = "Creation timestamp is mandatory")
    @JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssX", timezone = "UTC")
    @XmlJavaTypeAdapter(InstantIso8601UtcXmlAdapter.class)
    private Instant creationTimestamp; // Mandatory, ISO-8601 Timestamp YYYY-MM-DDThh:mm:ssZ
}
