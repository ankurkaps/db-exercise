package com.demo.common.model;

import jakarta.xml.bind.annotation.XmlRegistry;

/**
 * ObjectFactory for JAXB context creation.
 * This class is required by JAXB to create instances of model classes for XML binding.
 */
@XmlRegistry
public class ObjectFactory {

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.demo.common.model
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link PaymentRequest}
     */
    public PaymentRequest createPaymentRequest() {
        return new PaymentRequest();
    }

    /**
     * Create an instance of {@link FraudCheckResponse}
     */
    public FraudCheckResponse createFraudCheckResponse() {
        return new FraudCheckResponse();
    }

    /**
     * Create an instance of {@link FraudCheckStatus}
     */
    public FraudCheckStatus createFraudCheckStatus() {
        return FraudCheckStatus.APPROVED;
    }
}