package com.demo.common.model;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum FraudCheckStatus {
    APPROVED("APPROVED", "Nothing found, all okay"),
    SUSPICIOUS("SUSPICIOUS", "Suspicious payment");

    @JsonValue
    private final String code;
    private final String message;
}
