package com.demo.fraudcheck.service;

import com.demo.common.model.FraudCheckResponse;
import com.demo.common.model.FraudCheckStatus;
import com.demo.common.model.PaymentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("Fraud Check Service Tests")
class FraudCheckServiceTest {

    private FraudCheckService fraudCheckService;

    @BeforeEach
    void setUp() {
        fraudCheckService = new FraudCheckService();
    }

    @Test
    @DisplayName("Should approve clean payment with no blacklisted data")
    void shouldApproveCleanPayment() {
        // Given
        PaymentRequest payment = createValidPayment();

        // When
        FraudCheckResponse response = fraudCheckService.checkFraud(payment);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTransactionId()).isEqualTo(payment.getTransactionId());
        assertThat(response.getStatus()).isEqualTo(FraudCheckStatus.APPROVED);
        assertThat(response.getValidationTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should reject payment with blacklisted payer name")
    void shouldRejectPaymentWithBlacklistedPayerName() {
        // Given
        PaymentRequest payment = createValidPayment();
        payment.setPayerName("Mark Imaginary");

        // When
        FraudCheckResponse response = fraudCheckService.checkFraud(payment);

        // Then
        assertThat(response.getStatus()).isEqualTo(FraudCheckStatus.SUSPICIOUS);
    }

    @Test
    @DisplayName("Should reject payment with blacklisted payee name")
    void shouldRejectPaymentWithBlacklistedPayeeName() {
        // Given
        PaymentRequest payment = createValidPayment();
        payment.setPayeeName("Govind Real");

        // When
        FraudCheckResponse response = fraudCheckService.checkFraud(payment);

        // Then
        assertThat(response.getStatus()).isEqualTo(FraudCheckStatus.SUSPICIOUS);
    }

    @Test
    @DisplayName("Should reject payment with blacklisted payer country")
    void shouldRejectPaymentWithBlacklistedPayerCountry() {
        // Given
        PaymentRequest payment = createValidPayment();
        payment.setPayerCountryCode("IRN");

        // When
        FraudCheckResponse response = fraudCheckService.checkFraud(payment);

        // Then
        assertThat(response.getStatus()).isEqualTo(FraudCheckStatus.SUSPICIOUS);
    }

    @Test
    @DisplayName("Should reject payment with blacklisted payee country")
    void shouldRejectPaymentWithBlacklistedPayeeCountry() {
        // Given
        PaymentRequest payment = createValidPayment();
        payment.setPayeeCountryCode("PRK");

        // When
        FraudCheckResponse response = fraudCheckService.checkFraud(payment);

        // Then
        assertThat(response.getStatus()).isEqualTo(FraudCheckStatus.SUSPICIOUS);
    }

    @Test
    @DisplayName("Should reject payment with blacklisted payer bank")
    void shouldRejectPaymentWithBlacklistedPayerBank() {
        // Given
        PaymentRequest payment = createValidPayment();
        payment.setPayerBank("BANK OF KUNLUN");

        // When
        FraudCheckResponse response = fraudCheckService.checkFraud(payment);

        // Then
        assertThat(response.getStatus()).isEqualTo(FraudCheckStatus.SUSPICIOUS);
    }

    @Test
    @DisplayName("Should reject payment with blacklisted payee bank")
    void shouldRejectPaymentWithBlacklistedPayeeBank() {
        // Given
        PaymentRequest payment = createValidPayment();
        payment.setPayeeBank("KARAMAY CITY COMMERCIAL BANK");

        // When
        FraudCheckResponse response = fraudCheckService.checkFraud(payment);

        // Then
        assertThat(response.getStatus()).isEqualTo(FraudCheckStatus.SUSPICIOUS);
    }

    @Test
    @DisplayName("Should reject payment with blacklisted payment instruction")
    void shouldRejectPaymentWithBlacklistedInstruction() {
        // Given
        PaymentRequest payment = createValidPayment();
        payment.setPaymentInstruction("Artillery Procurement");

        // When
        FraudCheckResponse response = fraudCheckService.checkFraud(payment);

        // Then
        assertThat(response.getStatus()).isEqualTo(FraudCheckStatus.SUSPICIOUS);
    }

    @Test
    @DisplayName("Should handle null payment instruction")
    void shouldHandleNullPaymentInstruction() {
        // Given
        PaymentRequest payment = createValidPayment();
        payment.setPaymentInstruction(null);

        // When
        FraudCheckResponse response = fraudCheckService.checkFraud(payment);

        // Then
        assertThat(response.getStatus()).isEqualTo(FraudCheckStatus.APPROVED);
    }

    @Test
    @DisplayName("Should reject payment instruction with extra whitespace")
    void shouldRejectPaymentInstructionWithWhitespace() {
        // Given
        PaymentRequest payment = createValidPayment();
        payment.setPaymentInstruction("  Lethal Chemicals payment  ");

        // When
        FraudCheckResponse response = fraudCheckService.checkFraud(payment);

        // Then
        assertThat(response.getStatus()).isEqualTo(FraudCheckStatus.SUSPICIOUS);
    }

    @Test
    @DisplayName("Should test all blacklisted names")
    void shouldTestAllBlacklistedNames() {
        String[] blacklistedNames = {"Mark Imaginary", "Govind Real", "Shakil Maybe", "Chang Imagine"};

        for (String name : blacklistedNames) {
            PaymentRequest payment = createValidPayment();
            payment.setPayerName(name);

            FraudCheckResponse response = fraudCheckService.checkFraud(payment);
            assertThat(response.getStatus())
                .as("Name '%s' should be blacklisted", name)
                .isEqualTo(FraudCheckStatus.SUSPICIOUS);
        }
    }

    @Test
    @DisplayName("Should test all blacklisted countries")
    void shouldTestAllBlacklistedCountries() {
        String[] blacklistedCountries = {"CUB", "IRQ", "IRN", "PRK", "SDN", "SYR"};

        for (String country : blacklistedCountries) {
            PaymentRequest payment = createValidPayment();
            payment.setPayerCountryCode(country);

            FraudCheckResponse response = fraudCheckService.checkFraud(payment);
            assertThat(response.getStatus())
                .as("Country '%s' should be blacklisted", country)
                .isEqualTo(FraudCheckStatus.SUSPICIOUS);
        }
    }

    @Test
    @DisplayName("Should test all blacklisted banks")
    void shouldTestAllBlacklistedBanks() {
        String[] blacklistedBanks = {"BANK OF KUNLUN", "KARAMAY CITY COMMERCIAL BANK"};

        for (String bank : blacklistedBanks) {
            PaymentRequest payment = createValidPayment();
            payment.setPayerBank(bank);

            FraudCheckResponse response = fraudCheckService.checkFraud(payment);
            assertThat(response.getStatus())
                .as("Bank '%s' should be blacklisted", bank)
                .isEqualTo(FraudCheckStatus.SUSPICIOUS);
        }
    }

    @Test
    @DisplayName("Should test all blacklisted payment instructions")
    void shouldTestAllBlacklistedInstructions() {
        String[] blacklistedInstructions = {"Artillery Procurement", "Lethal Chemicals payment"};

        for (String instruction : blacklistedInstructions) {
            PaymentRequest payment = createValidPayment();
            payment.setPaymentInstruction(instruction);

            FraudCheckResponse response = fraudCheckService.checkFraud(payment);
            assertThat(response.getStatus())
                .as("Instruction '%s' should be blacklisted", instruction)
                .isEqualTo(FraudCheckStatus.SUSPICIOUS);
        }
    }

    private PaymentRequest createValidPayment() {
        PaymentRequest payment = new PaymentRequest();
        payment.setTransactionId(UUID.randomUUID());
        payment.setPayerName("John Doe");
        payment.setPayerBank("Chase Bank");
        payment.setPayerCountryCode("USA");
        payment.setPayerAccount("123456789");
        payment.setPayeeName("Jane Smith");
        payment.setPayeeBank("Wells Fargo");
        payment.setPayeeCountryCode("USA");
        payment.setPayeeAccount("987654321");
        payment.setPaymentInstruction("Salary payment");
        payment.setExecutionDate(LocalDate.now());
        payment.setAmount(BigDecimal.valueOf(1000.00));
        payment.setCurrency("USD");
        payment.setCreationTimestamp(Instant.now());
        return payment;
    }
}