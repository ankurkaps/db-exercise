package com.demo.common.model;

import java.time.Instant;
import java.util.UUID;

import com.demo.common.jaxb.adapters.InstantIso8601UtcXmlAdapter;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;

import jakarta.validation.constraints.NotNull;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@XmlRootElement(name = "fraudCheckResponse", namespace = "urn:example:fraudcheck:v1")
@XmlAccessorType(XmlAccessType.FIELD)
@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class FraudCheckResponse {
    private UUID transactionId;
    private FraudCheckStatus status;
    
    @NotNull
    @JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssX", timezone = "UTC")
    @XmlJavaTypeAdapter(InstantIso8601UtcXmlAdapter.class)
    private Instant validationTimestamp;
}
